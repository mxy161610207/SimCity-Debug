package nju.ics.lixiaofan.control;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.city.Building;
import nju.ics.lixiaofan.city.Citizen;
import nju.ics.lixiaofan.city.Location;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;

public class Delivery {
	public static Queue<DeliveryTask> searchTasks = new LinkedList<DeliveryTask>();
	public static Set<DeliveryTask> deliveryTasks = new HashSet<DeliveryTask>();
	public static int taskid = 0;
	public static boolean allBusy = false;
	
	public Delivery() {
		new Thread(carSearcher).start();
		new Thread(carMonitor).start();
	}
	
	private Runnable carSearcher = new Runnable(){
		public void run() {
			while(true){
				while(searchTasks.isEmpty() || allBusy)
					synchronized (searchTasks) {
						try {
							searchTasks.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				DeliveryTask dt = null;
				synchronized (searchTasks) {
					dt = searchTasks.peek();
					Result res = null;
					Car car = null;
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
					if(allBusy){
						Dashboard.appendLog("All cars are busy");
						continue;
					}
					car = res.car;
					Dashboard.appendLog("find "+car.name+" at "+car.loc.name);
					if(!car.isReal())
						TrafficMap.playOhNOSound();
					car.dt = dt;
					dt.car = car;
					dt.phase = 1;
					car.dest = res.section;
					if(car.dest == car.loc || (car.dest.isCombined && car.dest.combined.contains(car.loc))){
						if(car.status == Car.STILL){
							car.isLoading = true;
							//trigger start loading event
							if(EventManager.hasListener(Event.Type.CAR_START_LOADING))
								EventManager.trigger(new Event(Event.Type.CAR_START_LOADING, car.name, car.loc.name));
						}
						else{
							car.finalState = 0;
							Command.send(car, 0);
							car.sendRequest(0);
						}
						Dashboard.appendLog(car.name+" reached dest");
						//trigger reach dest event
						if(EventManager.hasListener(Event.Type.CAR_REACH_DEST))
							EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
					}
					else{
						car.finalState = 1;
						car.sendRequest(1);
						Dashboard.appendLog(car.name+" heads for src "+car.dest.name);
					}
					searchTasks.poll();
				}
				synchronized (deliveryTasks) {
					if(dt != null){
						deliveryTasks.add(dt);
						deliveryTasks.notify();
					}
				}
				Dashboard.updateDelivQ();
			}
		}

		//search for the nearest car that drives to sect
		private Result searchCar(Section start){
			if(start == null)
				return null;
			Queue<Section> queue = new LinkedList<Section>();
			queue.add(start);
			Section[] prev = {null, null};
			int dis[] = {-1, 0}, i = 0;
			boolean oneWay = true;
			while(!queue.isEmpty()){
				Section sect = queue.poll();
				dis[i]++;
//				System.out.println(sect.name+"\t"+dis[i]);
				if(!sect.cars.isEmpty())
					for(Car car:sect.cars)
						if(car.dest == null)
							return new Result(car, dis[i], start);
				
				if(prev[0] == null){
					prev[0] = sect;
					for(Section next : sect.entrances.values())
						queue.add(next);
					
					if(queue.size() == 2){
						oneWay = false;
						prev[1] = sect;
					}
				}
				else{
					Section next = sect.entrances.get(prev[i]);
					if(next == null && prev[i].isCombined)
						for(Section s : prev[i].combined){
							next = sect.entrances.get(s);
							if(next != null)
								break;
						}
					if(next != start && (!start.isCombined || !start.combined.contains(next))){
						queue.add(next);
						prev[i] = sect;
						if(!oneWay)
							i = 1-i;
					}
					else if(!oneWay){
						oneWay = true;
						i = 1-i;
					}
				}
			}
			allBusy = true;
			return null;
		}
	};
	
	private class Result{
		public Car car;
		public int dis;
		public Section section;
		public Result(Car car, int dis, Section section) {
			this.car = car;
			this.dis = dis;
			this.section = section;
		}
	}
	
	private Runnable carMonitor = new Runnable(){
		public void run() {
			while(true){
				while(deliveryTasks.isEmpty())
					synchronized (deliveryTasks) {
						try {
							deliveryTasks.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				synchronized (deliveryTasks) {
					for(Iterator<DeliveryTask> it = deliveryTasks.iterator();it.hasNext();){
						DeliveryTask dt = it.next();
						Car car = dt.car;
						long recent = Math.max(car.stopTime, dt.startTime);
						if ((car.loc == car.dest || car.dest.isCombined
								&& car.dest.combined.contains(car.loc)) && car.status == Car.STILL
								&& System.currentTimeMillis() - recent > 3000) {
							//head for the src
							if(dt.phase == 1){
								dt.phase = 2;
								car.dest = dt.dst instanceof Section ? (Section)dt.dst : selectNearestSection(car.loc, car.dir, ((Building)dt.dst).addrs);
								car.isLoading = false;
								Dashboard.appendLog(car.name+" finished loading");
								//trigger end loading event
								if(EventManager.hasListener(Event.Type.CAR_END_LOADING))
									EventManager.trigger(new Event(Event.Type.CAR_END_LOADING, car.name, car.loc.name));
								
								if(car.dest == car.loc || (car.dest.isCombined && car.dest.combined.contains(car.loc))){
									car.isLoading = true;
									dt.startTime = System.currentTimeMillis();
//									car.loc.icon.repaint();
									//trigger start unloading event
									if(EventManager.hasListener(Event.Type.CAR_START_UNLOADING))
										EventManager.trigger(new Event(Event.Type.CAR_START_UNLOADING, car.name, car.loc.name));
									Dashboard.appendLog(car.name+" reached dest");
									//trigger reach dest event
									if(EventManager.hasListener(Event.Type.CAR_REACH_DEST))
										EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
								}
								else{
									car.finalState = 1;
									car.sendRequest(1);
									Dashboard.appendLog(car.name+" heads for dst "+car.dest.name);
								}
							}
							//head for the dst
							else{
								dt.phase = 3;
								car.dt = null;
								car.dest = null;
								car.finalState = 1;
								car.isLoading = false;
								car.sendRequest(1);
								it.remove();
								allBusy = false;
								Dashboard.appendLog(car.name+" finished unloading");
								//trigger end unloading event
								if(EventManager.hasListener(Event.Type.CAR_END_UNLOADING))
									EventManager.trigger(new Event(Event.Type.CAR_END_UNLOADING, car.name, car.loc.name));
								//trigger complete event
								if(EventManager.hasListener(Event.Type.DELIVERY_COMPLETED))
									try {
										EventManager.trigger(new Event(Event.Type.DELIVERY_COMPLETED, dt.clone()));
									} catch (CloneNotSupportedException e) {
										e.printStackTrace();
									}
							}
							Dashboard.updateDelivQ();
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
					e.printStackTrace();
				}
			}
		}
	};
	
	private Section selectNearestSection(Section start, int dir, Set<Section> sects){
		if(sects == null)
			return null;
		if(sects.contains(start))
			return start;
		Queue<Section> queue = new LinkedList<Section>();
		queue.add(start.adjs.get(dir));//may be wrong
		Section prev = start;
		while(!queue.isEmpty()){
			Section sect = queue.poll();
			if(sects.contains(sect))
				return sect;
			
			Section next = sect.exits.get(prev);
			if(next == null && prev.isCombined)
				for(Section s : prev.combined){
					next = sect.exits.get(s);
					if(next != null)
						break;
				}
			if(next != start && (!start.isCombined || !start.combined.contains(next)))
				queue.add(next);
			prev = sect;
		}
		return null;
	}
	
	private static void add(DeliveryTask dtask){
		if(dtask == null)
			return;
		synchronized (searchTasks) {
			searchTasks.add(dtask);
			searchTasks.notify();
		}
		Dashboard.updateDelivQ();
		//trigger release event
		if(EventManager.hasListener(Event.Type.DELIVERY_RELEASED))
			try {
				EventManager.trigger(new Event(Event.Type.DELIVERY_RELEASED, dtask.clone()));
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
	}
	
	public static void add(Location src, Location dst, Citizen citizen){
		add(new DeliveryTask(src, dst, citizen));
	}
	
	public static void add(Location src, Location dst){
		add(src, dst, null);
	}
	
	public static class DeliveryTask implements Cloneable{
		public int id;
		public Location src = null, dst = null;
		public Car car;
		public int phase;//0: search car; 1: to src 2: to dest
		public long startTime = 0;
		public Citizen citizen = null;
		
		public DeliveryTask(Location src, Location dst, Citizen citizen) {
			this.id = Delivery.taskid++;
			this.src = src;
			this.dst = dst;
			this.phase = 0;
			this.startTime = System.currentTimeMillis();
			this.citizen = citizen;
		}
		
		protected DeliveryTask clone() throws CloneNotSupportedException {
			return (DeliveryTask) super.clone();
		}
	}
}
