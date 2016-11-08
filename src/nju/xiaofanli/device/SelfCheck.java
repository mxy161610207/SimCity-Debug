package nju.xiaofanli.device;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import nju.xiaofanli.Main;
import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelfCheck{
	private	static final Object OBJ = new Object();
	private static final Map<String, Boolean> deviceStatus = new HashMap<>();
	
	/**
	 * This method will block until all devices are ready
	 */
	public SelfCheck() {
		List<BrickChecking> brickCheckingThreads = new ArrayList<>();
		for(String name : Resource.getBricks()){
            deviceStatus.put(name, false);
			BrickChecking thread = new BrickChecking(name);
			brickCheckingThreads.add(thread);
			thread.start();
		}

        List<CarChecking> carCheckingThreads = new ArrayList<>();
		for(Car car : Resource.getCars()){
            deviceStatus.put(car.name, false);
            CarChecking thread = new CarChecking(car);
            carCheckingThreads.add(thread);
            thread.start();
        }

        Runnable timer = () -> {
            //noinspection InfiniteLoopStatement
            while (true){
                long curTime = System.currentTimeMillis();
                for(CarChecking thread : carCheckingThreads)
                    if(thread.car.isConnected() && curTime - thread.lastRecvTime > 1500) {
                        thread.lastRecvTime = Long.MAX_VALUE;
                        thread.car.disconnect(); //TODO when debugging, disable this.
                    }

                for(BrickChecking thread : brickCheckingThreads){
					if(thread.startTime > thread.endTime && curTime - thread.startTime > 8000){
						if(thread.channel != null)
							thread.channel.disconnect();
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
        private long lastRecvTime;
		CarChecking(Car car) {
			this.car = car;
		}
		public void run() {
			setName("Car Checking: " + car.name);
            //noinspection InfiniteLoopStatement
            while(true){
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
                while(true){
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
                                if(!Main.initial)
                                    StateSwitcher.suspend();
                            }
                            else{
                                deviceStatus.put(car.name, car.isConnected());
                                if(allReady()){//false -> true
                                    if(Main.initial)
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
	}
	
	private class BrickChecking extends Thread{
		private final String name, addr;
		private long startTime, endTime;
		private Session session;
		private Channel channel;
        BrickChecking(String name) {
			this.name = name;
			addr = Resource.getBrickAddr(name);
			if(addr == null){
				System.err.println("Brick " + name + " has no address");
				System.exit(-1);
			}
		}
		public void run() {
			setName("Brick Checking: " + name);
            //noinspection InfiniteLoopStatement
            while(true){
				startTime = endTime = 0;
				//start sample program in brick
				try {
//					System.out.println(name + " get session");
					session = Resource.getSession(name);
//					System.out.println(name + " connect session");
//					startTime = System.currentTimeMillis();
					session.connect();
//					endTime = System.currentTimeMillis();
//					System.out.println(name + " session connected");
					channel = session.openChannel("exec");
//					System.out.println(name + " set command");
					((ChannelExec) channel).setCommand("./start.sh");
//					System.out.println(name + " connect channel");
					channel.connect();
//					System.out.println(name + " reading");
					startTime = System.currentTimeMillis();
                    //noinspection ResultOfMethodCallIgnored
                    channel.getInputStream().read();//assure sample program is started, may get blocked FOREVER!
					endTime = System.currentTimeMillis();
//					System.out.println(name + " read");
					channel.disconnect();
//					System.out.println(name + " channel disconnected");
                    Dashboard.setDeviceStatus(name + " conn", true);
				} catch (JSchException | IOException e) {
//					e.printStackTrace();
					if(channel != null)
					    channel.disconnect();
					session.disconnect();
					Dashboard.setDeviceStatus(name + " conn", false);
					startTime = endTime = 0;
					continue;
				}
				
				//check if sample program is running
				boolean sampling = false;
				while(true){
					try {
//						System.out.println(name + " exec");
						channel = session.openChannel("exec");
//						System.out.println(name + " exec2");
						((ChannelExec) channel).setCommand("ps -ef | grep 'python sample.py' | grep -v grep");
//						System.out.println(name + " exec3");
						channel.connect();
//						System.out.println(name + " exec4");
						startTime = System.currentTimeMillis();
						sampling = channel.getInputStream().read() > 0; //may get blocked FOREVER!
						endTime = System.currentTimeMillis();
//						System.out.println(name + " exec5");
						channel.disconnect();
//						System.out.println(name + " exec6");
					} catch (JSchException | IOException e) {
						e.printStackTrace();
						channel.disconnect();
						session.disconnect();
						sampling = false;
                        Dashboard.setDeviceStatus(name + " sample", false);
						startTime = endTime = 0;
					}
					finally{
						if(sampling ^ deviceStatus.get(name)){
							Dashboard.setDeviceStatus(name + " sample", sampling);
							if(allReady()){//true -> false
								deviceStatus.put(name, sampling);
								if(!Main.initial)
									StateSwitcher.suspend();
							}
							else{
								deviceStatus.put(name, sampling);
								if(allReady()){//false -> true
									if(Main.initial)
										synchronized (OBJ) {
											OBJ.notify();
										}
									else
										StateSwitcher.resume();
								}
							}
						}
					}
                    if(!sampling)
						break;
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
