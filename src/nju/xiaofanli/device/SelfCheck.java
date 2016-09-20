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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelfCheck{
	private	final Object OBJ = new Object();
	private final Map<String, Boolean> deviceStatus = new HashMap<>();
	
	/**
	 * This method will block until all devices are ready
	 */
	public SelfCheck() {
		for(String name : Resource.getBricks()){
            deviceStatus.put(name, false);
            new BrickChecking(name).start();
		}

        List<CarChecking> carCheckingList = new ArrayList<>();
		for(Car car : Resource.getCars()){
            deviceStatus.put(car.name, false);
            CarChecking thread = new CarChecking(car);
            carCheckingList.add(thread);
            thread.start();
        }

        Runnable timer = () -> {
            //noinspection InfiniteLoopStatement
            while (true){
                long curTime = System.currentTimeMillis();
                for(CarChecking thread : carCheckingList)
                    if(thread.car.isConnected() && curTime - thread.lastRecvTime > 1500) {
                        thread.lastRecvTime = Long.MAX_VALUE;
                        thread.car.disconnect();
                    }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

//        new Thread(timer, "Car Checking Timer").start();

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
	
	private boolean allReady(){
		for(boolean b : deviceStatus.values())
			if(!b)	return false;
		return true;
	}

	private class CarChecking extends Thread{
		public final Car car;
        long lastRecvTime;
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
                        e.printStackTrace();
                        lastRecvTime = Long.MAX_VALUE;
                        car.disconnect();
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
//		private final Session session;
//		byte[] buf = new byte[1024];
        BrickChecking(String name) {
			this.name = name;
			addr = Resource.getBrickAddr(name);
			if(addr == null){
				System.err.println("Brick " + name + " has no address");
				System.exit(-1);
			}
//			session = ;
		}
		public void run() {
			setName("Brick Checking: " + name);
            //noinspection InfiniteLoopStatement
            while(true){
				//first, check if brick is reachable
				boolean connected = false;
				while(!connected){
					try {
//						System.out.println(name + " connect");
						connected = InetAddress.getByName(addr).isReachable(5000);
//						System.out.println(name + " connected");
					} catch (IOException e) {
						e.printStackTrace();
						connected = false;
					}
					Dashboard.setDeviceStatus(name + " conn", connected);
				}
				
				//second, start sample program in brick
				Session session = null;
				Channel channel = null;
				try {
//					System.out.println(name + " connect session");
					session = Resource.getSession(name);
					session.connect();
//					System.out.println(name + " connected session");
					channel = session.openChannel("exec");
					((ChannelExec) channel).setCommand("./start.sh");
					channel.setInputStream(null);
					((ChannelExec) channel).setErrStream(System.err);
					channel.connect();
//					System.out.println(name + " reading");
                    //noinspection ResultOfMethodCallIgnored
                    channel.getInputStream().read();//assure sample program is started
//					System.out.println(name + " read");
					channel.disconnect();
				} catch (JSchException | IOException e) {
					e.printStackTrace();
					if(channel != null)
					    channel.disconnect();
					session.disconnect();
					continue;
				}
				
				//third, check if sample program is running
				boolean sampling = false;
				while(true){
					try {
//						System.out.println(name + " exec");
						channel = session.openChannel("exec");
						((ChannelExec) channel).setCommand("ps -ef | grep 'python sample.py' | grep -v grep");
						channel.setInputStream(null);
						((ChannelExec) channel).setErrStream(System.err);
						channel.connect();
						sampling = channel.getInputStream().read() > 0;
						channel.disconnect();
					} catch (JSchException | IOException e) {
						e.printStackTrace();
						channel.disconnect();
						session.disconnect();
						sampling = false;
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
