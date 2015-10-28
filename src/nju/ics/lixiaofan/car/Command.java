package nju.ics.lixiaofan.car;

import java.util.Iterator;

import nju.ics.lixiaofan.dashboard.Dashboard;

public class Command {
	public Car car = null;
	public int cmd = -1;
	public int level = 1;
	public long deadline = 1000;
	
	public int type = 0;//0: normal 1: wake car 2: wake rc
	public CarRC rc = null;
	
	public final static int STOP = 0;
	public final static int FORWARD = 1;
	public final static int BACKWARD = 2;
	public final static int LEFT = 3;
	public final static int RIGHT = 4;
	public final static int NO_STEER = 5;
	public final static int HORN = 6;
	
	public Command(CarRC rc) {
		this.rc = rc;
		type = 2;
	}
	
	public Command(Car car, int cmd) {
		this.car = car;
		this.cmd = cmd;
		deadline = Remediation.getDeadline(car.type, cmd, 1);
	}
	
	public Command(Car car, int cmd, int type) {
		this.car = car;
		this.cmd = cmd;
		this.type = type;
		deadline = Remediation.getDeadline(type, cmd, 1);
	}

	//cmd:	0: stop	1: forward	2: backward	3: left	4: right
	public static void send(Car car, int cmd){
//		if(car == null)
//			return;
		send(car, cmd, 1, true);
	}	
	
	public static void send(Car car, int cmd, int level, boolean remedy){
		if(car == null || car.state == cmd)
			return;
//		if(cmd == 0 && level > 4)
//			return;

		CmdSender.send(car, cmd);
		if(cmd >= STOP && cmd <= RIGHT){
			car.expectation = cmd;
			if(cmd != car.state)
				car.state = -1;
			if(cmd == 0 && car.lastInstr == 1)
				car.lastStopInstrTime = System.currentTimeMillis();
			car.lastInstr = cmd;
			car.lastInstrTime = System.currentTimeMillis();
			
			if(remedy)
				addRemedyCommand(car, cmd);
		}
	}
	
	public static void send(Command cmd, boolean remedy) {
		send(cmd.car, cmd.cmd, cmd.level, remedy);
	}
	
	public static void wake(Car car){
		if(car == null || !car.isConnected || car.state == -1)
			return;
		
		CmdSender.send(car, car.state);
		car.expectation = car.state;
		if(car.state == 0 && car.lastInstr == 1)
			car.lastStopInstrTime = System.currentTimeMillis();
		car.lastInstr = car.state;
		car.lastInstrTime = System.currentTimeMillis();
	}
	
//	public static void wake(CarRC rc){
//		if(rc == null)
//			return;
//		CmdSender.send(rc);
//	}
	
	public static void addRemedyCommand(Car car, int cmd){
		boolean addition = true;
		synchronized (Remediation.queue) {
			for(Iterator<Command> it = Remediation.queue.iterator();it.hasNext();){
				Command cmd2 = it.next();
				if(cmd2.car == car){
					if(cmd2.cmd != cmd)
						it.remove();
					else
						addition = false;
					break;
				}
			}
			if(addition)
					Remediation.addCmd(new Command(car, cmd));
			Dashboard.updateRemedyQ();
			Remediation.printQueue();
		}
		synchronized (Remediation.getwork) {
			Remediation.getwork.notify();
		}
	}
}
