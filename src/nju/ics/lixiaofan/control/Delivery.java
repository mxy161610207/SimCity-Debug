package nju.ics.lixiaofan.control;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.Building;
import nju.ics.lixiaofan.city.Citizen;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.Section.Crossing;
import nju.ics.lixiaofan.city.Section.Street;
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
					if(dt.srcSect != null)
						res = searchCar(dt.srcSect);
					else{
						for(Section src : dt.srcB.addrs){
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
					car.dt = dt;
					dt.car = car;
					dt.phase = 1;
//					car.deliveryPhase = 1;
					car.dest = res.sec;//dt.srcSect;
					car.finalState = 1;
					car.sendRequest(1);
					searchTasks.poll();
					Dashboard.appendLog(car.name+" heads for src "+car.dest.name);
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
			Set<Section> visited = new HashSet<>();
			queue.add(start);
			int dis = 0;
			while(!queue.isEmpty()){
				Section sect = queue.poll();
//				System.out.println(sect.name);
				if(!sect.cars.isEmpty())
					for(Car car:sect.cars)
						if(car.dest == null){
							return new Result(car, dis, start);
						}
				dis++;
				visited.add(sect);
				if(sect.isCombined)
					visited.addAll(sect.combined);
				
				Section next;
				if(sect instanceof Crossing){
					Crossing c = (Crossing) sect;
					if(sect.id == 2){
						next = TrafficMap.dir ? c.adj[2] : c.adj[1];
						if(!visited.contains(next))
							queue.add(next);
					}
					else if(sect.id == 6){
						next = TrafficMap.dir ? c.adj[0] : c.adj[3];
						if(!visited.contains(next))
							queue.add(next);
					}
					else{
						next = TrafficMap.dir ? c.adj[0] : c.adj[1];
						if(!visited.contains(next))
							queue.add(next);
						
						next = TrafficMap.dir ? c.adj[2] : c.adj[3];
						if(!visited.contains(next))
							queue.add(next);
					}
				}
				else{
					if(TrafficMap.crossings[2].combined.contains(sect)){
						sect = ((Street) sect).adj[0];
						next = TrafficMap.dir?((Crossing) sect).adj[2]:((Crossing) sect).adj[1];
					}
					else if(TrafficMap.crossings[6].combined.contains(sect)){
						sect = ((Street) sect).adj[0];
						next = TrafficMap.dir?((Crossing) sect).adj[0]:((Crossing) sect).adj[3];
					}
					else
						next = TrafficMap.dir?((Street) sect).adj[0]:((Street) sect).adj[1];
					if(!visited.contains(next))
						queue.add(next);
				}
			}
			allBusy = true;
			return null;
		}
	};
	
	private class Result{
		public Car car;
		public int dis;
		public Section sec;
		public Result(Car car, int dis, Section sec) {
			this.car = car;
			this.dis = dis;
			this.sec = sec;
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
						if ((car.loc == car.dest || car.dest.isCombined
								&& car.dest.combined.contains(car.loc)) && car.state == 0
								&& System.currentTimeMillis() - car.stopTime > 3000) {
							//head for the src
							if(dt.phase == 1){
								dt.phase = 2;
//								car.deliveryPhase = 2;
								car.dest = dt.dstSect != null ? dt.dstSect : selectNearestSection(car.loc, dt.dstB.addrs);
								car.finalState = 1;
								car.isLoading = false;
								car.sendRequest(1);
								Dashboard.appendLog(car.name+" finished loading");
								Dashboard.appendLog(car.name+" heads for dst "+car.dest.name);
								//trigger end loading event
								if(EventManager.hasListener(Event.Type.CAR_END_LOADING))
									EventManager.trigger(new Event(Event.Type.CAR_END_LOADING, car.name, car.loc.name));
							}
							//head for the dst
							else{
								dt.phase = 3;
//								car.deliveryPhase = 0;
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
	
	private Section selectNearestSection(Section start, Set<Section> sects){
		if(sects == null)
			return null;
		Queue<Section> queue = new LinkedList<Section>();
		Set<Section> visited = new HashSet<>();
		queue.add(start);
		while(!queue.isEmpty()){
			Section sect = queue.poll();
//			System.out.println(sect.name);
			if(sects.contains(sect))
				return sect;
			else if(sect.isCombined){
				for(Section s : sect.combined)
					if(sects.contains(s))
						return s;
			}
			visited.add(sect);
			if(sect.isCombined)
				visited.addAll(sect.combined);
			
			Section next;
			if(sect instanceof Crossing){
				Crossing c = (Crossing) sect;
				if(sect.id == 2){
					next = TrafficMap.dir ? c.adj[1] : c.adj[2];
					if(!visited.contains(next))
						queue.add(next);
				}
				else if(sect.id == 6){
					next = TrafficMap.dir ? c.adj[3] : c.adj[0];
					if(!visited.contains(next))
						queue.add(next);
				}
				else{
					next = TrafficMap.dir ? c.adj[1] : c.adj[0];
					if(!visited.contains(next))
						queue.add(next);
					
					next = TrafficMap.dir ? c.adj[3] : c.adj[2];
					if(!visited.contains(next))
						queue.add(next);
				}
			}
			else{
				if(TrafficMap.crossings[2].combined.contains(sect)){
					sect = ((Street) sect).adj[0];
					next = TrafficMap.dir?((Crossing) sect).adj[1]:((Crossing) sect).adj[2];
				}
				else if(TrafficMap.crossings[6].combined.contains(sect)){
					sect = ((Street) sect).adj[0];
					next = TrafficMap.dir?((Crossing) sect).adj[3]:((Crossing) sect).adj[0];
				}
				else
					next = TrafficMap.dir?((Street) sect).adj[1]:((Street) sect).adj[0];
				if(!visited.contains(next))
					queue.add(next);
			}
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
	
	public static void add(Section src, Section dst, Citizen citizen){
		add(new DeliveryTask(src, dst, citizen));
	}
	
	public static void add(Section src, Section dst){
		add(src, dst, null);
	}
	
	public static void add(Section src, Building dst, Citizen citizen){
		add(new DeliveryTask(src, dst, citizen));
	}
	
	public static void add(Section src, Building dst){
		add(src, dst, null);
	}
	
	public static void add(Building src, Section dst, Citizen citizen){
		add(new DeliveryTask(src, dst, citizen));
	}
	
	public static void add(Building src, Section dst){
		add(src, dst, null);
	}
	
	public static void add(Building src, Building dst, Citizen citizen){
		add(new DeliveryTask(src, dst, citizen));
	}
	
	public static void add(Building src, Building dst){
		add(src, dst, null);
	}
	
	public static class DeliveryTask implements Cloneable{
		public int id;
		public Section srcSect = null, dstSect = null;
		public Building srcB = null, dstB = null;
		public Car car;
		public int phase;//0: search car; 1: to src 2: to dest
		public Citizen citizen = null;
		
		public DeliveryTask(Section src, Section dst, Citizen citizen) {
			id = Delivery.taskid++;
			this.srcSect = src;
			this.dstSect = dst;
			phase = 0;
			this.citizen = citizen;
		}
		
		public DeliveryTask(Section src, Building dst, Citizen citizen) {
			id = Delivery.taskid++;
			this.srcSect = src;
			this.dstB = dst;
			phase = 0;
			this.citizen = citizen;
		}
		
		public DeliveryTask(Building src, Section dst, Citizen citizen) {
			id = Delivery.taskid++;
			this.srcB = src;
			this.dstSect = dst;
			phase = 0;
			this.citizen = citizen;
		}
		
		public DeliveryTask(Building src, Building dst, Citizen citizen) {
			id = Delivery.taskid++;
			this.srcB = src;
			this.dstB = dst;
			phase = 0;
			this.citizen = citizen;
		}
		
		protected DeliveryTask clone() throws CloneNotSupportedException {
			return (DeliveryTask) super.clone();
		}
	}
}
