package nju.ics.lixiaofan.car;

import java.util.LinkedList;
import java.util.Queue;

import nju.ics.lixiaofan.control.StateSwitcher;
import nju.ics.lixiaofan.resource.Resource;

public class Command {
	public Car car = null;
	public int cmd = -1;
	public int level = 1;
	public long deadline = 1000;
	public int type = 0;//0: normal 1: wake car 2: reset
	
	public final static int STOP = Car.STOPPED;
	public final static int FORWARD = Car.MOVING;
	public final static int BACKWARD = 2;
	public final static int LEFT = 3;
	public final static int RIGHT = 4;
	public final static int NO_STEER = 5;
	public final static int HORN = 6;
	public final static int CONNECT = 7;
	public final static int DISCONNECT = 8;
	
	public Command(Car car, int cmd) {
		this.car = car;
		this.cmd = cmd;
		deadline = Remedy.getDeadline(cmd, 1);
	}
	
	public Command(Car car, int cmd, int type) {
		this(car, cmd);
		this.type = type;
	}

	//cmd:	0: stop	1: forward	2: backward	3: left	4: right
	public static void send(Car car, int cmd){
		send(car, cmd, 1, true);
	}	
	
	public static void send(Car car, int cmd, int level, boolean remedy){
		if(car == null)
			return;
		CmdSender.send(car, cmd);
		if(cmd == STOP || cmd == FORWARD){
			car.trend = cmd;
			if(cmd != car.status)
				car.status = Car.UNCERTAIN;
			if(!car.isReal() && cmd != car.realStatus)
				car.realStatus = Car.UNCERTAIN;
//			car.lastInstr = cmd;
			
			if(remedy)
				Remedy.addRemedyCommand(car, cmd);
		}
	}
	
	public static void send(Command cmd, boolean remedy) {
		send(cmd.car, cmd.cmd, cmd.level, remedy);
	}
	
	public static void wake(Car car){
		if(car == null || !car.isConnected)
			return;
		if(StateSwitcher.isNormal()){
			 if(car.getRealStatus() == Car.MOVING)
				 drive(car);
			 else if(car.getRealStatus() == Car.STOPPED)
				 stop(car);
		}
		else if(StateSwitcher.isSuspending())
			stop(car);//maintain its stopped status
	}
	
	public static void connect(Car car){
		if(!RCClient.isConnected()){
			System.err.println("RC disconnected, cannot connect " + car.name);
			return;
		}
		if(car != null)
			RCClient.rc.write(car.name + "_" + CONNECT + "_0");
	}
	
	public static void disconnect(Car car){
		if(!RCClient.isConnected()){
			System.err.println("RC disconnected, cannot disconnect " + car.name);
			return;
		}
		if(car != null)
			RCClient.rc.write(car.name + "_" + DISCONNECT + "_0");
	}
	
	/**
	 * Only called by Wake, Reset and Suspend 
	 */
	public static void drive(Car car){
		if(!RCClient.isConnected()){
			System.err.println("RC disconnected, cannot drive " + car.name);
			return;
		}
		if(car.isConnected){
			RCClient.rc.write(car.name + "_" + FORWARD + "_30");
			car.lastInstrTime = System.currentTimeMillis();
		}
	}
	
	/**
	 * Only called by Wake, Reset and Suspend
	 */
	public static void stop(Car car){
		if(!RCClient.isConnected()){
			System.err.println("RC disconnected, cannot stop " + car.name);
			return;
		}
		if(car.isConnected){
			RCClient.rc.write(car.name + "_" + STOP + "_30");
			car.lastInstrTime = System.currentTimeMillis();
			if(StateSwitcher.isResetting())
				StateSwitcher.resetTask.lastStopInstrTime = car.lastInstrTime;
		}
	}
	
	/**
	 * Only called by Reset Thread
	 */
	public static void stopAllCars(){
		for(Car car : Resource.getConnectedCars())
			if(car.getRealStatus() != Car.STOPPED)
				stop(car);
	}
}

class CmdSender implements Runnable{
	private static Queue<Command> queue = new LinkedList<Command>();
	
	public void run() {
		Thread thread = Thread.currentThread();
		StateSwitcher.register(thread);
		while(true){
			while(queue.isEmpty() || !StateSwitcher.isNormal()){
				synchronized (queue) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
//						e.printStackTrace();
						if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
							clear();
					}
				}
			}
//			if(StateSwitcher.isResetting()){
//				if(!StateSwitcher.isThreadReset(thread))
//					clear();
//				continue;
//			}
			if(!RCClient.isConnected())
				continue;
			Command cmd = null;
			synchronized (queue) {
				cmd = queue.poll();
			}
			switch(cmd.cmd){
			case Command.LEFT:case Command.RIGHT:
				RCClient.rc.write(cmd.car.name+"_"+cmd.cmd+"_3");
				break;
			case Command.FORWARD:case Command.STOP:case Command.BACKWARD:
				RCClient.rc.write(cmd.car.name+"_"+cmd.cmd+"_30");
				break;
			case Command.HORN:
				RCClient.rc.write(cmd.car.name+"_"+cmd.cmd+"_1000");
				break;
			default:
				System.out.println("!!!UNKNOWN COMMAND!!!");
				break;
			}
			cmd.car.lastInstrTime = System.currentTimeMillis();
		}
	}

	public static void send(Car car, int cmd){
		send(car, cmd, 0);
	}
	
	public static void send(Car car, int cmd, int type){
		if(StateSwitcher.isResetting())
			return;
		synchronized (queue) {
			queue.add(new Command(car, cmd, type));
			queue.notify();
		}
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
}
