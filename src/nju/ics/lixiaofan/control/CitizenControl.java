package nju.ics.lixiaofan.control;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.Citizen;
import nju.ics.lixiaofan.city.Citizen.Activity;
import nju.ics.lixiaofan.city.Citizen.Job;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventListener;
import nju.ics.lixiaofan.monitor.AppPkg;
import nju.ics.lixiaofan.monitor.PkgHandler;

public class CitizenControl implements Runnable{
	private static List<Citizen> citizens = TrafficMap.citizens;
	private static Queue<ActReq> queue = new LinkedList<ActReq>();
	
	public CitizenControl() {
		new Thread(this, "Citizen Control").start();
		new Thread(picker, "Citizen Action Picker").start();
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
						e.printStackTrace();
						if(Reset.isResetting() && Reset.checkThread(curThread))
							clear();
					}
				}
			}
			if(Reset.isResetting()){
				if(Reset.checkThread(curThread))
					clear();
				continue;
			}
			
			ActReq ar = queue.poll();
			synchronized (ar.citizen) {
				ar.citizen.act = ar.act;
				PkgHandler.send(new AppPkg().setCitizen(ar.citizen.name, ar.act != null ? ar.act.toString() : "None"));
				if(ar.act != null){
					switch (ar.act) {
//					case HailATaxi:
//						break;
//					case TakeATaxi:
//						break;
//					case GetOff:
//						break;
//					case GoToSchool:case GoToWork:
//						break;
					default:
						break;
					}
				}
				if(!ar.fromSelf)
					ar.citizen.notify();
			}
		}
	}
	
	private static void sendActReq(ActReq ar){
		if(Reset.isResetting())
			return;
		synchronized (queue) {
			queue.add(ar);
			queue.notify();
		}
	};
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
	
	public static void sendActReq(Citizen citizen, Activity act, boolean fromSelf){
		sendActReq(new ActReq(citizen, act, fromSelf));
	};
	
	public static void sendActReq(Citizen citizen, Activity act){
		sendActReq(citizen, act, false);
	}
	
	private Runnable picker = new Runnable() {
		public void run() {
			while(true){
				for(Citizen citizen : citizens){
//					System.out.println(citizen.name);
					if(citizen.act == null){
						if(!citizen.icon.isVisible()){
							sendActReq(citizen, Activity.Wander);
							continue;
						}
						sendActReq(citizen, Activity.GetHungry);
//						double d = Math.random();
//						if(d < 0.01)
//							sendActReq(citizen, Activity.Wander);
//						else if(d < 0.35){
//							if(citizen.job != Citizen.Job.Doctor)
//								sendActReq(citizen, Activity.GetSick);
//						}
//						else if(d < 0.6){
//							sendActReq(citizen, Activity.GetHungry);
//						}
//						else{
//							if(citizen.job == Job.Student)
//								sendActReq(citizen, Activity.GoToSchool);
//							else
//								sendActReq(citizen, Activity.GoToWork);
//						}
					}
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	public static CarMonitor carMonitor = new CarMonitor();
	private static class CarMonitor implements EventListener {
		public void eventTriggered(Event event) {
			switch (event.type) {
			case CAR_START_LOADING:{
				Car car = Car.carOf(event.car);
				if(car.dt != null && car.dt.citizen != null){
					car.passengers.add(car.dt.citizen);
					car.dt.citizen.car = car;
					CitizenControl.sendActReq(car.dt.citizen, Activity.TakeATaxi);
					Dashboard.appendLog(car.name + " picks up passenger "+ car.dt.citizen.name);
				}
				break;
			}
			case CAR_START_UNLOADING:{
				Car car = Car.carOf(event.car);
				if(car.dt != null && car.dt.citizen != null){
					Citizen citizen = car.dt.citizen;
					car.passengers.remove(citizen);
					citizen.car = null;
					citizen.loc = car.loc;
					CitizenControl.sendActReq(citizen, Activity.GetOff);
					Dashboard.appendLog(car.name + " drops off passenger "+ citizen.name);
				}
				break;
			}
			default:
				break;
			}
		}
		
	}
	
	private static class ActReq {
		Citizen citizen;
		Activity act;
		boolean fromSelf;
		
//		public ActReq(Citizen citizen, Activity act) {
//			this.citizen = citizen;
//			this.act = act;
//			this.fromSelf = false;
//		}
		
		public ActReq(Citizen citizen, Activity act, boolean fromSelf) {
			this.citizen = citizen;
			this.act = act;
			this.fromSelf = fromSelf;
		}
	}
}
