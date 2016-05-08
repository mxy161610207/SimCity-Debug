package nju.ics.lixiaofan.car;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Reset;

public class Command {
	public Car car = null;
	public int cmd = -1;
	public int level = 1;
	public long deadline = 1000;
	public int type = 0;//0: normal 1: wake car 2: reset
	
	public final static int STOP = 0;
	public final static int FORWARD = 1;
	public final static int BACKWARD = 2;
	public final static int LEFT = 3;
	public final static int RIGHT = 4;
	public final static int NO_STEER = 5;
	public final static int HORN = 6;
	
	public Command(Car car, int cmd) {
		this.car = car;
		this.cmd = cmd;
		deadline = Remediation.getDeadline(car.type, cmd, 1);
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
		if(cmd >= STOP && cmd <= RIGHT){
			car.trend = cmd;
			if(cmd != car.status)
				car.status = Car.UNCERTAIN;
			if(!car.isReal() && cmd != car.realStatus)
				car.realStatus = Car.UNCERTAIN;
			if(cmd == Car.STILL && car.lastInstr == Car.MOVING)
				car.lastStopInstrTime = System.currentTimeMillis();
			car.lastInstr = cmd;
			car.lastInstrTime = System.currentTimeMillis();
			
			if(remedy)
				Remediation.addRemedyCommand(car, cmd);
		}
	}
	
	public static void send(Command cmd, boolean remedy) {
		send(cmd.car, cmd.cmd, cmd.level, remedy);
	}
	
	public static void wake(Car car){
		if(car == null || !car.isConnected || car.getRealStatus() == Car.UNCERTAIN)
			return;
		CmdSender.send(car, car.getRealStatus(), 1);
//		car.trend = car.status;
		if(car.getRealStatus() == Car.STILL && car.lastInstr == Car.MOVING)
			car.lastStopInstrTime = System.currentTimeMillis();
		car.lastInstr = car.getRealStatus();
		car.lastInstrTime = System.currentTimeMillis();
	}
	
	public static void resetAllCars(){
		CmdSender.clear();
		for(Car car : TrafficMap.cars.values())
			if(car.isConnected){
				try {
					RCServer.rc.out.writeUTF(car.name+"_"+Command.STOP+"_30");
					RCServer.rc.out.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}
}

class CmdSender implements Runnable{
	public static Queue<Command> queue = new LinkedList<Command>();
	
	public void run() {
		//TODO
		Thread curThread = Thread.currentThread();
		Reset.addThread(curThread);
		while(true){
			while(queue.isEmpty()){
				synchronized (queue) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
						//TODO
						if(Reset.isResetting() && Reset.checkThread(curThread))
							clear();
					}
				}
			}
			//TODO
			if(Reset.isResetting()){
				if(Reset.checkThread(curThread))
					clear();
				continue;
			}
			
			Command cmd = null;
			synchronized (queue) {
				cmd = queue.poll();
			}
			CarRC rc = RCServer.rc;
			if(rc == null)
				continue;
			try {
				switch(cmd.cmd){
				case Command.LEFT:case Command.RIGHT:
					rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_3");
					rc.out.flush();
					break;
				case Command.FORWARD:case Command.STOP:case Command.BACKWARD:
					rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_30");
					rc.out.flush();
//					rc.lastInstrTime = System.currentTimeMillis();
					break;
				case Command.HORN:
					rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_2000");
					rc.out.flush();
					break;
				default:
					System.out.println("!!!UNKNOWN COMMAND!!!");
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void send(Car car, int cmd){
		send(car, cmd, 0);
	}
	
	public static void send(Car car, int cmd, int type){
		//TODO
		if(Reset.isResetting())
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
