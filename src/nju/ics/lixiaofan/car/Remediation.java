package nju.ics.lixiaofan.car;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;

public class Remediation implements Runnable{
	public static List<Command> queue = new LinkedList<Command>();
	public static Object getwork = new Object(), workdone = new Object();
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
		new Thread(wakeTask).start();
	}
	
	public void run() {
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
					//forward cmd
					if(cmd.cmd == 1){
						cmd.deadline = getDeadline(cmd.car.type, 1, ++cmd.level);
						Command.send(cmd, false);
						addCmd(cmd);
					}
					//stop cmd
					else if (cmd.cmd == 0) {
						cmd.car.state = 0;
						cmd.car.stopTime = System.currentTimeMillis();
						if(cmd.car.dest != null && (cmd.car.dest == cmd.car.loc || cmd.car.dest.isCombined && cmd.car.dest.combined.contains(cmd.car.loc)
								&& cmd.car.dt != null)){
							cmd.car.isLoading = true;
							cmd.car.loc.icon.repaint();
							//trigger start loading event
							if(cmd.car.dt.phase == 1 && EventManager.hasListener(Event.Type.CAR_START_LOADING))
								EventManager.trigger(new Event(Event.Type.CAR_START_LOADING, cmd.car.name, cmd.car.loc.name));
							//trigger start unloading event
							else if(cmd.car.dt.phase == 2 && EventManager.hasListener(Event.Type.CAR_START_UNLOADING))
								EventManager.trigger(new Event(Event.Type.CAR_START_UNLOADING, cmd.car.name, cmd.car.loc.name));
						}
						cmd.car.sendRequest(2);
						//trigger stop event
						if(EventManager.hasListener(Event.Type.CAR_STOP))
							EventManager.trigger(new Event(Event.Type.CAR_STOP, cmd.car.name, cmd.car.loc.name));
						
						if (queue.isEmpty())
							break;
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
			return System.currentTimeMillis() + 1000;
		switch(level){
		default:
			if(cmd == 0)
				return System.currentTimeMillis() + 500;
			else
				return System.currentTimeMillis() + 500;
		}
	}
	
	public static void printQueue(){
		for(Iterator<Command> it = queue.iterator();it.hasNext();){
			Command cmd = it.next();
			System.out.println(cmd.car.name+"\t"+((cmd.cmd==0)?"S":"F")+"\t"+cmd.level+"\t"+cmd.deadline);
		}		
		System.out.println("-----------------------");
	}
}