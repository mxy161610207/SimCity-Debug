package nju.xiaofanli.device;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class SelfCheck{
	private	static final Object OBJ = new Object();
	private static final Map<String, Boolean> deviceStatus = new HashMap<>();
	private static final Map<String, BrickChecking> brickCheckingThreads = new HashMap<>();
	private static final Map<Car, CarChecking> carCheckingThreads = new HashMap<>();
	/**
	 * This method will block until all devices are ready
	 */
	public SelfCheck() {
		//TODO uncomment this
		for(String name : Resource.getBricks()){
			if (brickCheckingThreads.containsKey(name))
				return;
            deviceStatus.put(name, false);
			BrickChecking thread = new BrickChecking(name);
			brickCheckingThreads.put(name, thread);
			thread.start();
		}

		for(Car car : Resource.getCars()){
			if (carCheckingThreads.containsKey(car))
				return;
            deviceStatus.put(car.name, false);
            CarChecking thread = new CarChecking(car);
            carCheckingThreads.put(car, thread);
            thread.start();
        }

        Runnable timer = () -> {
            //noinspection InfiniteLoopStatement
            while (true){
                long curTime = System.currentTimeMillis();
                for(CarChecking thread : carCheckingThreads.values())
                    if(thread.car.isConnected() && curTime - thread.lastRecvTime > 1500) {
                        thread.lastRecvTime = Long.MAX_VALUE;
                        thread.car.disconnect(); //TODO enable car checking
                    }

                for(BrickChecking thread : brickCheckingThreads.values()){
					if(thread.startTime > thread.endTime && curTime - thread.startTime > 10000){
//						if(thread.channel != null)
//							thread.channel.disconnect();
						thread.interrupt();
					}
				}
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(timer, "Checking Timer").start();

		Runnable wakeThread = () -> {
			int count = 0;
			//noinspection InfiniteLoopStatement
			while (true) {
				Resource.getConnectedCars().stream().filter(car -> System.currentTimeMillis() - car.lastCmdTime > 60000).forEach(Command::wake);
				if (count == 0)
					Resource.getConnectedCars().forEach(car -> car.write(Command.RIGHT2)); //calibration
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				count = (count + 1) % 5;
			}
		};

		new Thread(wakeThread, "Wake Thread").start();

        while(!allReady()){
            synchronized (OBJ) {
                try {
                    OBJ.wait();//wait until all devices are ready
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
	public static boolean allReady() {
		for(boolean b : deviceStatus.values())
			if(!b)	return false;
		return true;
	}

	private class CarChecking extends Thread{
		public final Car car;
		private boolean running = true;
        private long lastRecvTime;
		CarChecking(Car car) {
			this.car = car;
		}
		public void run() {
			setName("Car Checking " + car.name);
            //noinspection InfiniteLoopStatement
            while(running){
                lastRecvTime = Long.MAX_VALUE;
                car.connect();
				while(!car.tried){
					synchronized (car.TRIED_OBJ) {
						try {
							car.TRIED_OBJ.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				car.tried = false;
                DataInputStream dis = car.getDIS();
                // keep checking car's connection
                while(running){
                    try {
                        if(dis == null)
                            throw new IOException();
                        else{
                            //noinspection ResultOfMethodCallIgnored
                            dis.read();
                            lastRecvTime = System.currentTimeMillis();
                        }
                    } catch (IOException e) {
//                        e.printStackTrace();
                        lastRecvTime = Long.MAX_VALUE;
                        car.disconnect();
                        Dashboard.setDeviceStatus(car.name, false);
                    }
                    finally {
                        if(car.isConnected() ^ deviceStatus.get(car.name)){
                            Dashboard.setDeviceStatus(car.name, car.isConnected());
                            if(allReady()){//true -> false
                                deviceStatus.put(car.name, car.isConnected());
                                if(!StateSwitcher.isInitializing())
                                    StateSwitcher.suspend();
                            }
                            else{
                                deviceStatus.put(car.name, car.isConnected());
                                if(allReady()){//false -> true
                                    if(StateSwitcher.isInitializing())
                                        synchronized (OBJ) {
                                            OBJ.notify();
                                        }
                                    else
                                        StateSwitcher.resume();
                                }
                            }
                        }
                    }
                    if(!car.isConnected()){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
			}
		}

		void setRunning(boolean running) {
			this.running = running;
		}
	}
	
	private class BrickChecking extends Thread{
		private final String name, addr;
		private final int numSensor;
		private long startTime, endTime;
		private Session session;
		private ChannelExec channel;
        BrickChecking(String name) {
			this.name = name;
			addr = Resource.getBrickAddr(name);
			if(addr == null){
				System.err.println("Brick " + name + " has no address");
				System.exit(-1);
			}
			numSensor = Resource.getSensorNum(name);
		}
		public void run() {
			setName("Brick Checking " + name);
			int timeout = 10000;
            //noinspection InfiniteLoopStatement
            while(true) {
				//get a session
				try {
					session = Resource.getSession(name);
					session.setTimeout(timeout);
					session.connect(timeout);
					Dashboard.setDeviceStatus(name + " conn", true);
				} catch (JSchException e) {
//					e.printStackTrace();
					if (session != null) {
						session.disconnect();
						session = null;
					}
					System.out.println(getName() + ": session disconnected");
					Dashboard.setDeviceStatus(name + " conn", false);
					continue;
				}

				while (session != null && session.isConnected()) {
					startTime = endTime = 0;
					//check if the number of IR sensors is right
					try {
						channel = (ChannelExec) session.openChannel("exec");
						channel.setCommand("ls /sys/class/lego-sensor");
						channel.connect(timeout);
						byte[] buf = new byte[128];
						startTime = System.currentTimeMillis();
						//noinspection ResultOfMethodCallIgnored
						channel.getInputStream().read(buf);//assure sample program is started, may get blocked FOREVER!
						endTime = System.currentTimeMillis();
						channel.disconnect();
						channel = null;

						Set<String> sensors = new HashSet<>();
						Collections.addAll(sensors, new String(buf).trim().split("\n"));
						boolean allSensorsReady = true;
						for (int i = 0;i < numSensor;i++) {
							if (!sensors.contains("sensor"+i)) {
								allSensorsReady = false;
								break;
							}
						}
						if (!allSensorsReady) {
							System.out.println("Brick "+name+" cannot find all sensors. Trying to Reboot it...");
							Session rootSession = Resource.getRootSession(name);
							if (rootSession != null) {
								Channel channel = null;
								try {
									rootSession.connect();
									channel = rootSession.openChannel("exec");
									((ChannelExec) channel).setCommand("reboot");
									channel.connect();
								} catch (JSchException e1) {
//									e1.printStackTrace();
								} finally {
									if (channel != null)
										channel.disconnect();
									rootSession.disconnect();
								}
							}
							throw new JSchException("cannot find all sensors");
						}
					} catch (JSchException | IOException e) {
						e.printStackTrace();
						if (channel != null) {
							channel.disconnect();
							channel = null;
						}
						if (session != null) {
							session.disconnect();
							session = null;
						}
						System.out.println(getName() + ": session disconnected4");
						Dashboard.setDeviceStatus(name + " conn", false);
						startTime = endTime = 0;
						continue;
					}

					//run the sample program on EV3 brick
					try {
						channel = (ChannelExec) session.openChannel("exec");
						channel.setCommand("./start.sh " + InetAddress.getLocalHost().getHostAddress());
						channel.connect(timeout);
						startTime = System.currentTimeMillis();
						//noinspection ResultOfMethodCallIgnored
						channel.getInputStream().read();//assure sample program is started, may get blocked FOREVER!
						endTime = System.currentTimeMillis();
						channel.disconnect();
						channel = null;
					} catch (JSchException | IOException e) {
						e.printStackTrace();
						if (channel != null) {
							channel.disconnect();
							channel = null;
						}
						if (session != null) {
							session.disconnect();
							session = null;
						}
						System.out.println(getName() + ": session disconnected2");
						Dashboard.setDeviceStatus(name + " conn", false);
						startTime = endTime = 0;
						continue;
					}

					//check if sample program is running
					boolean sampling = true;
					while (session != null && session.isConnected() && sampling) {
						try {
							channel = (ChannelExec) session.openChannel("exec");
							channel.setCommand("ps -ef | grep 'python3 sample.py' | grep -v grep");
							channel.connect(timeout);
							startTime = System.currentTimeMillis();
//							byte[] buf = new byte[128];
//							sampling = channel.getInputStream().read(buf) > 0;
//							System.out.println(getName() + ": " + new String(buf));
							sampling = channel.getInputStream().read() > 0; //may get blocked FOREVER!
							endTime = System.currentTimeMillis();
							channel.disconnect();
							channel = null;
//							Dashboard.setDeviceStatus(name + " sample", true);
						} catch (JSchException | IOException e) {
							e.printStackTrace();
							if (channel != null) {
								channel.disconnect();
								channel = null;
							}
							if (session != null) {
								session.disconnect();
								session = null;
							}
							System.out.println(getName() + ": session disconnected3");
							sampling = false;
							Dashboard.setDeviceStatus(name + " sample", false);
							startTime = endTime = 0;
						} finally {
							if (sampling ^ deviceStatus.get(name)) {
								Dashboard.setDeviceStatus(name + " sample", sampling);
								if (allReady()) {//true -> false
									deviceStatus.put(name, sampling);
									if (!StateSwitcher.isInitializing())
										StateSwitcher.suspend();
									System.out.println("[Brick " + name + "] connection broke. " + new Date());
								} else {
									deviceStatus.put(name, sampling);
									if (allReady()) {//false -> true
										if (StateSwitcher.isInitializing())
											synchronized (OBJ) {
												OBJ.notify();
											}
										else
											StateSwitcher.resume();
										System.out.println("[Brick " + name + "] connection resumed. " + new Date());
									}
								}
							}
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
