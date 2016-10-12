package nju.xiaofanli.application;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.city.*;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

import java.awt.*;
import java.util.*;

public class Delivery {
	public static final Queue<DeliveryTask> searchTasks = new LinkedList<>();
	public static final Set<DeliveryTask> deliveryTasks = new HashSet<>();
	private static int taskid = 0;
	public static boolean allBusy = false;
    private static int MAX_USER_DELIV_NUM, MAX_SYS_DELIV_NUM;
    private static int userDelivNum = 0, sysDelivNum = 0;
    public static int completedUserDelivNum = 0, completedSysDelivNum = 0;

    public Delivery() {
        MAX_USER_DELIV_NUM = Resource.getCars().size() > 1 ? 1 : 0;
        MAX_SYS_DELIV_NUM = Resource.getCars().size() - MAX_USER_DELIV_NUM;

		new Thread(carSearcher, "Car Searcher").start();
		new Thread(carMonitor, "Car Monitor").start();
	}
	
	private Runnable carSearcher = new Runnable(){
		public void run() {
			Thread thread = Thread.currentThread();
			StateSwitcher.register(thread);
			//noinspection InfiniteLoopStatement
			while(true){
				while(searchTasks.isEmpty() || allBusy || !StateSwitcher.isNormal())
					synchronized (searchTasks) {
						try {
							searchTasks.wait();
						} catch (InterruptedException e) {
//							e.printStackTrace();
							if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
								clearSearchTasks();
						}
					}
				DeliveryTask dt;
				Car car;
				Result res = null;
				synchronized (searchTasks) {
					dt = searchTasks.peek();
					int minDis = Integer.MAX_VALUE;
					if(dt.src instanceof Section)
						res = searchCar((Section) dt.src);
					else{
						for(Section src : ((Building)dt.src).addrs){
							Result tmp = searchCar(src);
							if(allBusy)
								break;
							if(tmp.dis < minDis){
								minDis = tmp.dis;
								res = tmp;
							}
						}
					}
					if(allBusy || res == null){
						Dashboard.log("All cars are busy!\n", Color.RED);
						continue;
					}
					searchTasks.poll();
				}
				car = res.car;
				car.dest = res.section;
				car.dt = dt;
				dt.car = car;
				dt.phase = DeliveryTask.HEAD4SRC;
				if(car.hasPhantom())
					Dashboard.playErrorSound();
				if(car.dest.sameAs(car.loc) && car.state == Car.STOPPED){
                    car.setLoading(true);
                    Command.send(car, Command.WHISTLE2);
                    //trigger start loading event
                    if(EventManager.hasListener(Event.Type.CAR_START_LOADING))
                        EventManager.trigger(new Event(Event.Type.CAR_START_LOADING, car.name, car.loc.name));
					//trigger reach dest event
					if(EventManager.hasListener(Event.Type.CAR_REACH_DEST))
						EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
				}
				else{
					car.finalState = Car.MOVING;
					car.notifyPolice(Police.REQUEST2ENTER);
				}
					
				if(!StateSwitcher.isResetting()){
					synchronized (deliveryTasks) {
						deliveryTasks.add(dt);
						deliveryTasks.notify();
					}
				}
				Dashboard.updateDeliveryTaskPanel();
			}
		}

		//search for the nearest car that drives to sect
		private Result searchCar(Section start){
			if(start == null)
				return null;
			for(Car car : start.cars)
			    if(car.dest == null)
			        return new Result(car, 0, start);

			Queue<Section> queue = new LinkedList<>(start.exit2entrance.values());
			Section[] prev = {start, start};
			int dis[] = {0, 0}, i = 0;
			boolean oneWay = start.exit2entrance.size() == 1;
			while(!queue.isEmpty()){
				Section sect = queue.poll();
				dis[i]++;
//				System.out.println(sect.name+"\t"+dis[i]);
                for(Car car : sect.cars)
                    if(car.dest == null && sect.adjSects.get(car.dir).sameAs(prev[i])) // the car is empty and its dir is right
                        return new Result(car, dis[i], start);

                Section next = sect.exit2entrance.get(prev[i]);
                if(next == null) {
                    for (Section s : prev[i].combined) {
                        next = sect.exit2entrance.get(s);
                        if (next != null)
                            break;
                    }
                }
                if(next == null)
                    throw new NullPointerException();
                if(!next.sameAs(start)){
                    queue.add(next);
                    prev[i] = sect;
                    if(!oneWay)
                        i = 1 - i;
                }
                else if(!oneWay){
                    oneWay = true;
                    i = 1 - i;
                }
			}
			allBusy = true;
			return null;
		}

		class Result{
			public Car car;
			public int dis;
			public Section section;
			public Result(Car car, int dis, Section section) {
				this.car = car;
				this.dis = dis;
				this.section = section;
			}
		}
	};
	
	private Runnable carMonitor = () -> {
        Thread thread = Thread.currentThread();
        StateSwitcher.register(thread);
		//noinspection InfiniteLoopStatement
		while(true){
            while(deliveryTasks.isEmpty() || !StateSwitcher.isNormal())
                synchronized (deliveryTasks) {
                    try {
                        deliveryTasks.wait();
                    } catch (InterruptedException e) {
//							e.printStackTrace();
                        if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
                            clearDeliveryTasks();
                    }
                }

            synchronized (deliveryTasks) {
                for(Iterator<DeliveryTask> iter = deliveryTasks.iterator();iter.hasNext();){
                    DeliveryTask dt = iter.next();
                    if(dt.phase == DeliveryTask.COMPLETED && System.currentTimeMillis() - dt.startTime > 3000) {
                        iter.remove();
                        allBusy = false;
                        completedSysDelivNum++;
                        if(dt.createdByUser) {
                            userDelivNum--;
                            Dashboard.enableDeliveryButton(true);
                        }
                        else {
                            sysDelivNum--;
                            Location src = TrafficMap.getALocation();
                            Location dest = TrafficMap.getALocationExcept(src);
                            add(src, dest, false);
                        }
                        if(EventManager.hasListener(Event.Type.DELIVERY_COMPLETED))
                            EventManager.trigger(new Event(Event.Type.DELIVERY_COMPLETED, dt));
                        Dashboard.updateDeliveryTaskPanel();
                        continue;
                    }
                    Car car = dt.car;
                    if (car.loc.sameAs(car.dest) && car.state == Car.STOPPED
                            && System.currentTimeMillis() - Math.max(car.stopTime, dt.startTime) > 3000) {
                        //head for the src
                        if(dt.phase == DeliveryTask.HEAD4SRC){
                            dt.phase = DeliveryTask.HEAD4DEST;
                            car.dest = dt.dest instanceof Section ? (Section)dt.dest : selectNearestSection(car.loc, car.dir, ((Building)dt.dest).addrs);
                            car.setLoading(false);
//                            Dashboard.log(car.name+" finished loading");
                            //trigger end loading event
                            if(EventManager.hasListener(Event.Type.CAR_END_LOADING))
                                EventManager.trigger(new Event(Event.Type.CAR_END_LOADING, car.name, car.loc.name));

                            if(car.dest.sameAs(car.loc)){
                                car.setLoading(true);
                                dt.startTime = System.currentTimeMillis();
//									car.loc.icon.repaint();
								Command.send(car, Command.WHISTLE3);
                                //trigger start unloading event
                                if(EventManager.hasListener(Event.Type.CAR_START_UNLOADING))
                                    EventManager.trigger(new Event(Event.Type.CAR_START_UNLOADING, car.name, car.loc.name));
//                                Dashboard.log(car.name+" reached destination");
                                //trigger reach dest event
                                if(EventManager.hasListener(Event.Type.CAR_REACH_DEST))
                                    EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
                            }
                            else{
                                car.finalState = Car.MOVING;
                                car.notifyPolice(Police.REQUEST2ENTER);
//                                Dashboard.log(car.name+" heads for dest "+car.dest.name);
                            }
                        }
                        //head for the dest
                        else if(dt.phase == DeliveryTask.HEAD4DEST){
//                            iter.remove();
                            dt.phase = DeliveryTask.COMPLETED;
                            dt.startTime = System.currentTimeMillis();
                            car.dt = null;
                            car.dest = null;
                            car.finalState = Car.MOVING;
                            car.setLoading(false);
                            car.loc.icon.repaint();
                            car.notifyPolice(Police.REQUEST2ENTER);
//                            allBusy = false;
//
//                            if(dt.createdByUser) {
//                                userDelivNum--;
//                                completedUserDelivNum++;
//                                Dashboard.enableDeliveryButton(true);
//                            }
//                            else {
//                                sysDelivNum--;
//                                completedSysDelivNum++;
//                                Location src = TrafficMap.getALocation();
//                                Location dest = TrafficMap.getALocationExcept(src);
//                                add(src, dest, false);
//                            }
//                            Dashboard.log(car.name+" finished unloading");
                            //trigger end unloading event
                            if(EventManager.hasListener(Event.Type.CAR_END_UNLOADING))
                                EventManager.trigger(new Event(Event.Type.CAR_END_UNLOADING, car.name, car.loc.name));
//                            //trigger complete event
//                            if(EventManager.hasListener(Event.Type.DELIVERY_COMPLETED))
//                                try {
//                                    EventManager.trigger(new Event(Event.Type.DELIVERY_COMPLETED, dt.clone()));
//                                } catch (CloneNotSupportedException e) {
//                                    e.printStackTrace();
//                                }
                        }
                        Dashboard.updateDeliveryTaskPanel();
                    }
                }
            }
            if(!allBusy)
                synchronized (searchTasks) {
                    searchTasks.notify();
                }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
//                e.printStackTrace();
				if(StateSwitcher.isResetting())
				    thread.interrupt();
            }
        }
    };
	
	private Section selectNearestSection(Section start, int dir, Set<Section> sects){
		if(sects == null)
			return null;
		if(sects.contains(start))
			return start;
		Queue<Section> queue = new LinkedList<>();
		queue.add(start.adjSects.get(dir));//may be wrong
		Section prev = start;
		while(!queue.isEmpty()){
			Section sect = queue.poll();
			if(sects.contains(sect))
				return sect;
			
			Section next = sect.entrance2exit.get(prev);
			if(next == null) {
                for (Section s : prev.combined) {
                    next = sect.entrance2exit.get(s);
                    if (next != null)
                        break;
                }
            }
            if(next == null)
                throw new NullPointerException();
			if(!next.sameAs(start))
				queue.add(next);
			prev = sect;
		}
		return null;
	}
	
	public static void add(DeliveryTask dtask){
		if(dtask == null || StateSwitcher.isResetting())
			return;
		synchronized (searchTasks) {
			searchTasks.add(dtask);
			searchTasks.notify();
		}
		Dashboard.updateDeliveryTaskPanel();
		//trigger release event
		if(EventManager.hasListener(Event.Type.DELIVERY_RELEASED))
			try {
				EventManager.trigger(new Event(Event.Type.DELIVERY_RELEASED, dtask.clone()));
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
	}

	public static void startSysDelivery() {
        for (int i = 0;i < MAX_SYS_DELIV_NUM;i++) {
            Location src = TrafficMap.getALocation();
            Location dest = TrafficMap.getALocationExcept(src);
            add(src, dest, false);
        }
        if(MAX_USER_DELIV_NUM == 0)
            Dashboard.enableDeliveryButton(false);
    }

	public static void add(Location src, Location dest, Citizen citizen, boolean releasedByUser){
        if(src != null && dest != null && citizen != null)
		    add(new DeliveryTask(src, dest, citizen, releasedByUser));
	}

    public static void add(Location src, Location dest, boolean createdByUser){
        if(createdByUser){
            if(userDelivNum == MAX_USER_DELIV_NUM)
                return;
        }
        else{
            if(sysDelivNum == MAX_SYS_DELIV_NUM)
                return;
        }
        //randomly pick a free citizen
        Citizen citizen = TrafficMap.removeAFreeCitizen();
        if(citizen == null)
            return;

        if(createdByUser){
            if(++userDelivNum == MAX_USER_DELIV_NUM)
                Dashboard.enableDeliveryButton(false);
        }
        else
            sysDelivNum++;

        citizen.loc = src;
        citizen.dest = dest;
        citizen.setAction(Citizen.Action.HailATaxi);
        citizen.createdByUser = createdByUser;
//        Resource.execute(citizen);
        citizen.startAction();
	}
	
	private static void clearSearchTasks(){
		synchronized (searchTasks) {
			searchTasks.clear();
		}
	}
	
	private static void clearDeliveryTasks(){
		synchronized (deliveryTasks) {
			deliveryTasks.clear();
		}
	}

	public static void reset() {
        allBusy = false;
        userDelivNum = sysDelivNum = 0;
        completedSysDelivNum = completedUserDelivNum = 0;
    }
	
	public static class DeliveryTask implements Cloneable{
		public int id;
		public Location src = null, dest = null;
		public Car car;
		public int phase;//0: search car; 1: to src 2: to dest
		public long startTime = 0;
		public Citizen citizen = null;
        public boolean createdByUser = false;

        public static final int SEARCH_CAR = 0;
        public static final int HEAD4SRC = 1;
        public static final int HEAD4DEST = 2;
        public static final int COMPLETED = 3;
		
		public DeliveryTask(Location src, Location dest, Citizen citizen, boolean createdByUser) {
			this.id = Delivery.taskid++;
			this.src = src;
			this.dest = dest;
			this.phase = SEARCH_CAR;
			this.startTime = System.currentTimeMillis();
			this.citizen = citizen;
            this.createdByUser = createdByUser;
		}
		
		protected DeliveryTask clone() throws CloneNotSupportedException {
			return (DeliveryTask) super.clone();
		}

		public static final String css = "<style type=\"text/css\">\n" +
                "table.user\n" +
                "  {\n" +
                "  font-family:\"Trebuchet MS\", Arial, Helvetica, sans-serif;\n" +
                "  border-collapse:collapse;\n" +
                "  font-size: 10px;\n" +
                "  margin: 0px 0px 4px 0px;\n" +
                "  white-space: nowrap;\n" +
                "  }\n" +
                "\n" +
                "table.user td, table.user th \n" +
                "  {\n" +
                "  border:1px solid #98bf21;\n" +
                "  border-collapse:collapse;\n" +
                "  text-align:left;\n" +
                "  margin: 0;\n" +
                "  padding: 0;\n" +
                "  }\n" +
                "\n" +
                "table.user th \n" +
                "  {\n" +
                "  background-color:#A7C942;\n" +
                "  color:#ffffff;\n" +
                "  }\n" +
                "\n" +
                "table.sys\n" +
                "  {\n" +
                "  font-family:\"Trebuchet MS\", Arial, Helvetica, sans-serif;\n" +
                "  border-collapse:collapse;\n" +
                "  font-size: 10px;\n" +
                "  margin: 0px 0px 4px 0px;\n" +
                "  white-space: nowrap;\n" +
                "  }\n" +
                "\n" +
                "table.sys td, table.sys th \n" +
                "  {\n" +
                "  border:1px solid #000000;\n" +
                "  text-align:left;\n" +
                "  margin: 0;\n" +
                "  padding: 0;\n" +
                "  }\n" +
                "\n" +
                "table.sys th \n" +
                "  {\n" +
                "  background-color:#404040;\n" +
                "  color:#ffffff;\n" +
                "  }\n" +
                "</style>";

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
//            sb.append("<html>");
            sb.append("<table class="+ (createdByUser ? "user" : "sys") + ">");
            sb.append("<tr><th>").append("Src").append("</th>");
            sb.append("<td>").append(src.name).append("</td></tr>");
            sb.append("<tr><th>").append("Dest").append("</th>");
            sb.append("<td>").append(dest.name).append("</td></tr>");
            if(citizen != null) {
                sb.append("<tr><th>").append("Pax").append("</th>");
                sb.append("<td>").append(citizen.name).append("</td></tr>");
            }
            sb.append("<tr><th>").append("Stat").append("</th>");
            String phaseStr;
            switch (phase){
                case SEARCH_CAR:
                    phaseStr = "Search Car"; break;
                case HEAD4SRC:
                    phaseStr = "Head for Src"; break;
                case HEAD4DEST:
                    phaseStr = "Head for Dest"; break;
                case COMPLETED:
                    phaseStr = "Completed"; break;
                default:
                    phaseStr = "Unknown"; break;
            }
            sb.append("<td>").append(phaseStr).append("</td></tr>");
            sb.append("</table>");
//            sb.append("</html>");
            return sb.toString();
        }
    }
}
