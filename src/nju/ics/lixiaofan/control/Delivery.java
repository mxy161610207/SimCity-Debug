package nju.ics.lixiaofan.control;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import nju.ics.lixiaofan.car.Car;
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
					Car car = null;
					if(dt.srcSect != null)
						car = searchCar(dt.srcSect);
					if(car == null){
						allBusy = true;
						Dashboard.appendLog("All cars are busy");
						continue;
					}
					Dashboard.appendLog("find "+car.name+" at "+car.loc.name);
					car.dt = dt;
					dt.car = car;
					dt.phase = 1;
//					car.deliveryPhase = 1;
					car.dest = dt.srcSect;
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
		private Car searchCar(Section start){
			if(start == null)
				return null;
			Queue<Section> queue = new LinkedList<Section>();
			Set<Section> visited = new HashSet<>();
			queue.add(start);
			while(!queue.isEmpty()){
				Section sect = queue.poll();
//				System.out.println(sect.name);
				if(!sect.cars.isEmpty())
					for(Car car:sect.cars)
						if(car.dest == null)
							return car;
				visited.add(sect);
				if(sect.isCombined)
					visited.addAll(sect.combined);
				
				Section next;
				if(sect instanceof Crossing){
					if(sect.id == 2){
						next = TrafficMap.dir?((Crossing) sect).adj[2]:((Crossing) sect).adj[1];
						if(!visited.contains(next))
							queue.add(next);
					}
					else if(sect.id == 6){
						next = TrafficMap.dir?((Crossing) sect).adj[0]:((Crossing) sect).adj[3];
						if(!visited.contains(next))
							queue.add(next);
					}
					else{
						next = TrafficMap.dir?((Crossing) sect).adj[0]:((Crossing) sect).adj[1];
						if(!visited.contains(next))
							queue.add(next);
						
						next = TrafficMap.dir?((Crossing) sect).adj[2]:((Crossing) sect).adj[3];
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
			return null;
		}
	};
	
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
								car.dest = dt.dstSect;
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
	
	public static void add(Section srcSect, Section dstSect, Citizen citizen){
		DeliveryTask dtask = new DeliveryTask(srcSect, dstSect, citizen);
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
	
	public static void add(Section srcSect, Section dstSect){
		add(srcSect, dstSect, null);
	}
	
	public static class DeliveryTask implements Cloneable{
		public int id;
		public Section srcSect, dstSect;
		public Car car;
		public int phase;//0: search car; 1: to src 2: to dest
		public Citizen citizen;
		
		public DeliveryTask(Section srcSect, Section dstSect, Citizen citizen) {
			id = Delivery.taskid++;
			this.srcSect = srcSect;
			this.dstSect = dstSect;
			phase = 0;
			this.citizen = citizen;
		}
		
		protected DeliveryTask clone() throws CloneNotSupportedException {
			return (DeliveryTask) super.clone();
		}
	}
}
