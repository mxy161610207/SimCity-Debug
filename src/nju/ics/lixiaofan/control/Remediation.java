package nju.ics.lixiaofan.control;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.car.RCServer;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;

public class Remediation implements Runnable{
	public static List<Command> queue = new LinkedList<Command>();
	public static Object getwork = new Object(), workdone = new Object();
//	public static long dur = 2000, minDur = 1000;//millisecond
	private Runnable wakeTask = new Runnable() {
		public void run() {
			while(true){
				long currentTime = System.currentTimeMillis();
				for(Car car:RCServer.cars.values())
					if(car.isConnected && car.state != -1 && currentTime - car.lastInstrTime > 60000)
						Command.wake(car);
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	public Remediation() {
	}
	
	@Override
	public void run() {
		new Thread(wakeTask).start();
		
		while(true){
			while(queue.isEmpty()){
				synchronized (workdone) {
					workdone.notify();
				}
				synchronized (getwork) {
					try {
						getwork.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			//remedy and update cars' states
			synchronized (queue) {
				Command cmd = queue.get(0);
				boolean donesth = false;
				while(cmd.deadline < System.currentTimeMillis()){
					donesth = true;
					queue.remove(0);
//					printQueue();
					//forward cmd
					if(cmd.cmd == 1){
						cmd.deadline = getDeadline(cmd.car.type, 1, ++cmd.level);
						Command.send(cmd, false);
						addCmd(cmd);
					}
					//stop cmd
					else if (cmd.cmd == 0) {
//						long undetected = System.currentTimeMillis() - cmd.car.lastStopInstrTime;
//						if (cmd.car.type != 3 && (undetected < 4000 || ((cmd.car.loc == TrafficMap.crossings[2] || cmd.car.loc == TrafficMap.crossings[6]) && undetected < 6000))) {
//							cmd.deadline = getDeadline(cmd.car.type, 0, ++cmd.level);
//							Command.send(cmd, false);
//							addCmd(cmd);
//						} else {
						cmd.car.state = 0;
						cmd.car.stopTime = System.currentTimeMillis();
						if(cmd.car.dest == cmd.car.loc){
							cmd.car.isLoading = true;
							cmd.car.loc.btn.repaint();
						}
						cmd.car.sendRequest(2);
						//trigger stop event
						if(EventManager.hasListener(Event.Type.CAR_STOP))
							EventManager.trigger(new Event(Event.Type.CAR_STOP, cmd.car.name, cmd.car.loc.name));
						
//						Dashboard.mapRepaint();
						if (queue.isEmpty())
							break;
//						}
					}
					cmd = queue.get(0);
				}
				if(donesth){
					printQueue();
					Dashboard.updateRemedyQ();
				}
			}
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void addCmd(Command cmd){
		if(cmd == null)
			return;
		synchronized (queue) {
			int i;
			for(i = 0;i < queue.size();i++)
				if(queue.get(i).deadline > cmd.deadline){
					queue.add(i, cmd);
					return;
				}
			if(i == queue.size())
				queue.add(cmd);
		}
	}
	
	public static long getDeadline(int type, int cmd, int level){
		if(type == 3)
			return System.currentTimeMillis() + 1500;
		switch(level){
//		case 1:
//			return System.currentTimeMillis() + 3000;//millisecond
//		case 2:
//			return System.currentTimeMillis() + 2000;
		default:
			if(cmd == 0)
				return System.currentTimeMillis() + 500;
			else
				return System.currentTimeMillis() + 500;
		}
	}
	
	public static void printQueue(){
//		System.out.println("Current Time: " + System.currentTimeMillis());
		for(Iterator<Command> it = queue.iterator();it.hasNext();){
			Command cmd = it.next();
			System.out.println(cmd.car.name+"\t"+((cmd.cmd==0)?"S":"F")+"\t"+cmd.level+"\t"+cmd.deadline);
		}		
		System.out.println("-----------------------");
	}
}