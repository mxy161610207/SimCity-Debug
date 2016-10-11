package nju.xiaofanli.device.car;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

import java.util.*;

public class Remedy implements Runnable{
	private static final List<Command> queue = new ArrayList<>();

    Remedy() {
        Runnable wakeThread = () -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                Resource.getConnectedCars().stream().filter(car -> System.currentTimeMillis() - car.lastCmdTime > 60000).forEach(Command::wake);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(wakeThread, "Wake Thread").start();
	}
	
	public void run() {
		Thread thread = Thread.currentThread();
		StateSwitcher.register(thread);
		//noinspection InfiniteLoopStatement
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
			
			//remedy and update cars' states
			synchronized (queue) {
				Command cmd = queue.get(0);
				boolean donesth = false;
				while(cmd.deadline < System.currentTimeMillis()){
					donesth = true;
					queue.remove(0);
					if (cmd.cmd == Command.STOP) {
						cmd.car.setState(Car.STOPPED);
						cmd.car.stopTime = System.currentTimeMillis();
						if(cmd.car.dest != null && cmd.car.dest.sameAs(cmd.car.loc) && cmd.car.dt != null){
							cmd.car.setLoading(true);
							//trigger start loading event
							if(cmd.car.dt.phase == Delivery.DeliveryTask.HEAD4SRC) {
                                Command.send(cmd.car, Command.WHISTLE2);
								if(EventManager.hasListener(Event.Type.CAR_START_LOADING))
									EventManager.trigger(new Event(Event.Type.CAR_START_LOADING, cmd.car.name, cmd.car.loc.name));
							}
							//trigger start unloading event
							else if(cmd.car.dt.phase == Delivery.DeliveryTask.HEAD4DEST) {
                                Command.send(cmd.car, Command.WHISTLE3);
								if(EventManager.hasListener(Event.Type.CAR_START_UNLOADING))
									EventManager.trigger(new Event(Event.Type.CAR_START_UNLOADING, cmd.car.name, cmd.car.loc.name));
							}
						}
//						cmd.car.notifyPolice(Police.ALREADY_STOPPED);
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
					Dashboard.updateRemedyCommandPanel();
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
//				e.printStackTrace();
				if(StateSwitcher.isResetting())
					thread.interrupt();
			}
		}
	}
	
	public static void updateRemedyQWhenDetect(Car car){
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
					if(cmd.cmd == Command.STOP){
						cmd.deadline = Remedy.getDeadline();
//						Command.send(cmd, false);
                        newCmd = cmd;
					}
					break;
				}
			}
			insert(newCmd);
			if(donesth){
				Dashboard.updateRemedyCommandPanel();
				printQueue();
			}
		}
	}

	private static Comparator<Command> comparator = (o1, o2) -> (int) (o1.deadline - o2.deadline);
	private static void insert(Command cmd){
		if(cmd == null || StateSwitcher.isResetting())
			return;

		synchronized (queue) {
//			int i;
//			for(i = 0;i < queue.size();i++)
//				if(queue.get(i).deadline > cmd.deadline){
//					queue.add(i, cmd);
//					break;
//				}
//			if(i == queue.size())
//				queue.add(cmd);
			int pos = Collections.binarySearch(queue, cmd, comparator);
			if(pos < 0)
				pos = -pos - 1;
			queue.add(pos, cmd);
			queue.notify();
		}
	}
	
	static void addRemedyCommand(Car car, int cmd){
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
			Dashboard.updateRemedyCommandPanel();
			printQueue();
		}
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
	
	static long getDeadline(){
		return System.currentTimeMillis() + 1000;
	}
	
	private static void printQueue(){
        StringBuilder sb = new StringBuilder();
        queue.forEach(x -> sb.append(x.car.name).append("\t").append((x.cmd == Command.STOP) ? "S" : "F").append("\t").append(x.deadline));
//		System.out.println("-----------------------");
        System.out.println(sb.toString());
	}
	
	public static List<Command> getQueue(){
		return queue;
	}
}