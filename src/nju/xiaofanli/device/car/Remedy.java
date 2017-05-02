package nju.xiaofanli.device.car;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

import java.util.*;

public class Remedy implements Runnable{
	private static final List<Command> queue = new ArrayList<>();

    Remedy() {
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
	}
	
	public void run() {
		Thread thread = Thread.currentThread();
		StateSwitcher.register(thread);
		//noinspection InfiniteLoopStatement
		while(true){
			synchronized (queue) {
				while(queue.isEmpty() || !StateSwitcher.isNormal()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
//						e.printStackTrace();
						if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
							clear();
					}
				}
			}
			
			//remedy and update cars' states
			synchronized (queue) {
				boolean donesth = false;
				while(!queue.isEmpty()){
					Command cmd = queue.get(0);
					if (cmd.deadline > System.currentTimeMillis())
						break;
					donesth = true;
					queue.remove(0);
					if (cmd.cmd == Command.MOVE_FORWARD || cmd.cmd == Command.MOVE_BACKWARD) {
						cmd.car.setState(Car.MOVING);
						TrafficMap.allCarsStopped = false;
						Dashboard.enableScenarioButton(false);
						//trigger move event
						if(EventManager.hasListener(Event.Type.CAR_MOVE))
							EventManager.trigger(new Event(Event.Type.CAR_MOVE, cmd.car.name, cmd.car.loc.name));
					}
					else if (cmd.cmd == Command.STOP) {
						cmd.car.setState(Car.STOPPED);
						cmd.car.stopTime = System.currentTimeMillis();
						if(cmd.car.dest != null && cmd.car.dest == cmd.car.loc && cmd.car.dt != null){
							cmd.car.setLoading(true);
							//trigger start loading event
							if(cmd.car.dt.phase == Delivery.DeliveryTask.HEAD4SRC) {
                                Command.send(cmd.car, Command.WHISTLE);
								if(EventManager.hasListener(Event.Type.CAR_START_LOADING))
									EventManager.trigger(new Event(Event.Type.CAR_START_LOADING, cmd.car.name, cmd.car.loc.name));
							}
							//trigger start unloading event
							else if(cmd.car.dt.phase == Delivery.DeliveryTask.HEAD4DEST) {
                                Command.send(cmd.car, Command.WHISTLE2);
								if(EventManager.hasListener(Event.Type.CAR_START_UNLOADING))
									EventManager.trigger(new Event(Event.Type.CAR_START_UNLOADING, cmd.car.name, cmd.car.loc.name));
							}
						}

						boolean allStopped = true;
						for (Car car : Resource.getConnectedCars()) {
							if (car.getState() == Car.MOVING) {
								allStopped = false;
								break;
							}
						}
						TrafficMap.allCarsStopped = allStopped;
						if (TrafficMap.allCarsStopped && !TrafficMap.crashOccurred)
							Dashboard.enableScenarioButton(true);

						//trigger stop event
						if(EventManager.hasListener(Event.Type.CAR_STOP))
							EventManager.trigger(new Event(Event.Type.CAR_STOP, cmd.car.name, cmd.car.loc.name));
					}
				}
				if(donesth){
					printQueue();
					Dashboard.updateRemedyCommandPanel();
				}
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
//				e.printStackTrace();
				if(StateSwitcher.isResetting())
					thread.interrupt();
			}
		}
	}
	
	public static void updateRemedyQWhenDetect(Car car){
		synchronized (queue) {
			if(queue.isEmpty())
				return;
			Command newCmd = null;
			boolean donesth = false;
			for(Iterator<Command> it = queue.iterator();it.hasNext();){
				Command cmd = it.next();
				if(cmd.car == car){
					donesth = true;
					it.remove();
					if(cmd.cmd == Command.STOP){
						cmd.deadline = System.currentTimeMillis() + 1000;
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
				insert(new Command(car, cmd, cmd == Command.STOP ? System.currentTimeMillis()+1000 : System.currentTimeMillis()));
			Dashboard.updateRemedyCommandPanel();
			printQueue();
		}
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
	
	private static void printQueue(){
        StringBuilder sb = new StringBuilder();
        queue.forEach(x -> sb.append(x.car.name).append("\t").append((x.cmd == Command.STOP) ? "S" : "F").append("\t").append(x.deadline).append("\t\t"));
        System.out.println(sb.length() != 0 ? sb.toString() : "Remedy queue is empty.");
	}
	
	public static List<Command> getQueue(){
		return queue;
	}
}