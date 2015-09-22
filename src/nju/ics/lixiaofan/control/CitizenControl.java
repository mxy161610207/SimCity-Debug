package nju.ics.lixiaofan.control;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import nju.ics.lixiaofan.city.Citizen;
import nju.ics.lixiaofan.city.Citizen.Activity;
import nju.ics.lixiaofan.city.TrafficMap;

public class CitizenControl implements Runnable{
	private static List<Citizen> citizens = TrafficMap.citizens;
	private static Queue<ActReq> queue = new LinkedList<ActReq>();
	
	public CitizenControl() {
		new Thread(this).start();
		new Thread(picker).start();
	}
	public void run() {
		while(true){
			while(queue.isEmpty()){
				synchronized (queue) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			ActReq ar = queue.poll();
			
			synchronized (ar.citizen) {
				ar.citizen.act = ar.act;
				if(ar.act != null)
					switch (ar.act) {
					case HailATaxi:
						ar.citizen.dest = TrafficMap.sections[(int) (Math.random()*TrafficMap.sections.length)];
						break;
					default:
						break;
					}
				
				if(!ar.fromSelf)
					ar.citizen.notify();
			}
		}
	}
	
	private static void sendActReq(ActReq ar){
		synchronized (queue) {
			queue.add(ar);
			queue.notify();
		}
	};
	
	public static void sendActReq(Citizen citizen, Activity act, boolean fromSelf){
		synchronized (queue) {
			queue.add(new ActReq(citizen, act, fromSelf));
			queue.notify();
		}
	};
	
	private Runnable picker = new Runnable() {
		public void run() {
			while(true){
				for(Citizen citizen : citizens)
					if(citizen.act == null){
						double d = Math.random();
						if(d < 0.5)
							sendActReq(new ActReq(citizen, Activity.Wander));
						else if(citizen.icon.isVisible())
							sendActReq(new ActReq(citizen, Activity.HailATaxi));
					}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	private static class ActReq{
		Citizen citizen;
		Activity act;
		boolean fromSelf;
		
		public ActReq(Citizen citizen, Activity act) {
			this.citizen = citizen;
			this.act = act;
			this.fromSelf = false;
		}
		
		public ActReq(Citizen citizen, Activity act, boolean fromSelf) {
			this.citizen = citizen;
			this.act = act;
			this.fromSelf = fromSelf;
		}
	}
}
