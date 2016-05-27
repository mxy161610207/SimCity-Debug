package nju.ics.lixiaofan.car;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nju.ics.lixiaofan.control.Police;
import nju.ics.lixiaofan.control.Reset;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;
import nju.ics.lixiaofan.resource.ResourceProvider;
import nju.ics.lixiaofan.sensor.BrickServer;

public class Remedy implements Runnable{
	private static List<Command> queue = new LinkedList<Command>();
	//public static Object getwork = new Object();//, workdone = new Object();
	private Runnable wakeThread = new Runnable() {
		int missed[] = new int[10];
		public void run() {
			for(int i = 0;i < missed.length;i++)
				missed[i] = 0;
//			long start = System.currentTimeMillis();
			while(true){
				long currentTime = System.currentTimeMillis();
				for(Car car : ResourceProvider.getConnectedCars())
					if (car.getRealStatus() != Car.UNCERTAIN && currentTime - car.lastInstrTime > 60000)
						Command.wake(car);
				
				//check heartBeat
				for(int bid = 0;bid < 10;bid++)
					if(BrickServer.recvTime[bid] > 0 && currentTime - BrickServer.recvTime[bid] > 3000){
						missed[bid]++;
//						if(missed[bid] >= 3)
						System.err.println("Brick " + bid + " is down");
						
//						tplcnt++;						
//						System.out.println("TP-LINK:"+tplcnt+" "+(tplcnt == 0?"N/A":(currentTime-start)/tplcnt));
					}
					else
						missed[bid] = 0;
//				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	public Remedy() {
		new Thread(wakeThread, "Wake Thread").start();
	}
	
	public void run() {
		Thread curThread = Thread.currentThread();
		Reset.addThread(curThread);
		while(true){
			while(queue.isEmpty()){
				synchronized (queue) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
//						e.printStackTrace();
						if(Reset.isResetting() && !Reset.isThreadReset(curThread))
							clear();
					}
				}
			}
			
			if(Reset.isResetting()){
				if(!Reset.isThreadReset(curThread))
					clear();
				continue;
			}
			
			//remedy and update cars' states
			synchronized (queue) {
				Command cmd = queue.get(0);
				boolean donesth = false;
				while(cmd.deadline < System.currentTimeMillis()){
					donesth = true;
					queue.remove(0);
					//forward cmd
					if(cmd.cmd == 1){
						cmd.deadline = getDeadline(cmd.car.type, 1, ++cmd.level);
						Command.send(cmd, false);
//						insertCmd(cmd);//comment this line to remedy only once
					}
					//stop cmd
					else if (cmd.cmd == 0) {
						cmd.car.status = Car.STOPPED;
						cmd.car.stopTime = System.currentTimeMillis();
						if(cmd.car.dest != null && cmd.car.dest.sameAs(cmd.car.loc) && cmd.car.dt != null){
							cmd.car.setLoading(true);
							//trigger start loading event
							if(cmd.car.dt.phase == 1 && EventManager.hasListener(Event.Type.CAR_START_LOADING))
								EventManager.trigger(new Event(Event.Type.CAR_START_LOADING, cmd.car.name, cmd.car.loc.name));
							//trigger start unloading event
							else if(cmd.car.dt.phase == 2 && EventManager.hasListener(Event.Type.CAR_START_UNLOADING))
								EventManager.trigger(new Event(Event.Type.CAR_START_UNLOADING, cmd.car.name, cmd.car.loc.name));
						}
						cmd.car.notifyPolice(Police.ALREADY_STOPPED);
						//trigger stop event
						if(EventManager.hasListener(Event.Type.CAR_STOP))
							EventManager.trigger(new Event(Event.Type.CAR_STOP, cmd.car.name, cmd.car.loc.name));
					}
					if (queue.isEmpty())
						break;
					cmd = queue.get(0);
				}
				if(donesth){
					printQueue();
					Dashboard.updateRemedyQ();
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void updateWhenDetected(Car car){
		if(queue.isEmpty())
			return;
		synchronized (queue) {
			Command newCmd = null;
			boolean donesth = false;
			for(Iterator<Command> it = queue.iterator();it.hasNext();){
				Command cmd = it.next();
				if(cmd.car == car){
					donesth = true;
					it.remove();
					//stop command
					if(cmd.cmd == 0){
						cmd.level = 1;
						cmd.deadline = Remedy.getDeadline(cmd.car.type, 0, 1);
						Command.send(cmd, false);
						newCmd = cmd;
					}
					break;
				}
			}
			Remedy.insert(newCmd);
			if(donesth){
				Dashboard.updateRemedyQ();
				printQueue();
			}
		}
	}
	
	public static void insert(Command cmd){
		if(cmd == null || Reset.isResetting())
			return;
		
		synchronized (queue) {
			int i;
			for(i = 0;i < queue.size();i++)
				if(queue.get(i).deadline > cmd.deadline){
					queue.add(i, cmd);
					break;
				}
			if(i == queue.size())
				queue.add(cmd);
			
			queue.notify();
		}
	}
	
	public static void addRemedyCommand(Car car, int cmd){
		boolean addition = true;
		synchronized (queue) {
			for(Iterator<Command> it = queue.iterator();it.hasNext();){
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
				insert(new Command(car, cmd));
			Dashboard.updateRemedyQ();
			printQueue();
		}
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
	
	public static long getDeadline(int type, int cmd, int level){
//		if(type == 3)
			return System.currentTimeMillis() + 1000;
//		switch(level){
//		default:
//			if(cmd == 0)
//				return System.currentTimeMillis() + 500;
//			else
//				return System.currentTimeMillis() + 500;
//		}
	}
	
	public static void printQueue(){
		for(Iterator<Command> it = queue.iterator();it.hasNext();){
			Command cmd = it.next();
			System.out.println(cmd.car.name+"\t"+((cmd.cmd==0)?"S":"F")+"\t"+cmd.level+"\t"+cmd.deadline);
		}		
		System.out.println("-----------------------");
	}
	
	public static final List<Command> getQueue(){
		return queue;
	}
}