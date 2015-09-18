package nju.ics.lixiaofan.control;

import java.util.LinkedList;
import java.util.Queue;
import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.Section.Street;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;

public class TrafficPolice implements Runnable{
//	public Sensor[] entrances = {null, null}, exits = {null, null};
	public static Queue<Request> req = new LinkedList<Request>();
//	public Object wakemeup = new Object();
	private static Notifier notifier = new Notifier();
	public TrafficPolice() {
		new Thread(notifier).start();
		new Thread(this).start();
	}
	
	public void run() {
		while(true){
			while(req.isEmpty()){
				synchronized (req) {
					try {
						req.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
//			System.out.println("Crossing "+id+" handler awake!!!");
			System.out.println("Traffic Police awake!!!");
			Request r = req.poll();
			Section reqSec = null;
//			if(r.location == location){
//				if(exits[1] == null)
//					reqSec = exits[0].street;
//				else
//					reqSec = (exits[0].dir == r.car.dir) ? exits[0].street : exits[1].street;
//			}
//			else
//				reqSec = location;
			reqSec = r.loc.adjs.get((int)(r.car.dir));
//			System.out.println(r.loc.name+" "+r.car.dir);
			if(reqSec == null)
				System.out.println("reqSec is null");
			
			synchronized (reqSec.mutex) {
				if(r.cmd == 0){
					if(r.car.finalState == 0)
						reqSec.removeWaitingCar(r.car);
					else
						reqSec.addWaitingCar(r.car);
					if(r.car == reqSec.admittedCar){
						reqSec.setAdmittedCar(null);
//						synchronized (wakemeup) {
//							wakemeup.notify();
//						}
						sendNotice(reqSec);
					}
				}
				else if(r.cmd == 1){
					if(reqSec.isOccupied){
						//tell the car to stop
						System.out.println(r.car.name+" need to STOP!!!");
						reqSec.addWaitingCar(r.car);
						Command.send(r.car, 0);
						//trigger recv response event
						if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
							EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, 0));
					}
					else if(reqSec.admittedCar != null && r.car != reqSec.admittedCar){
						//tell the car to stop
						System.out.println(r.car.name+" need to STOP!!!2");
						reqSec.addWaitingCar(r.car);
						Command.send(r.car, 0);
						//trigger recv response event
						if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
							EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, 0));
					}
					else{
						//tell the car to enter
						System.out.println(r.car.name+" can ENTER!!!");
						reqSec.setAdmittedCar(r.car);
						Command.send(r.car, 1);
						//trigger recv response event
						if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
							EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, 1));
					}
				}
				//the car is already stopped
				else if(r.cmd == 2){
					if(r.car.finalState == 1){
						reqSec.addWaitingCar(r.car);
						System.out.println(r.car.name+" waits for "+reqSec.name);
					}
				}
			}
		}
	}

	static class Notifier implements Runnable {
		Queue<Request> req = new LinkedList<Request>();
		
		public void run() {
			while(true){
				while(req.isEmpty()){
					synchronized (req) {
						try {
							req.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				
				Request r = req.poll();
				Section loc = r.loc;
				if(loc.isOccupied)
					continue;
				synchronized (loc.mutex) {
					synchronized (loc.waitingCars) {
						if(loc.waitingCars.isEmpty())
							loc.setAdmittedCar(null);
						else{
							Car car = loc.waitingCars.peek();
							if(car.loc.cars.size() == 1){
								loc.setAdmittedCar(car);
								Command.send(car, 1);
								System.out.println((loc instanceof Street?"Street ":"Crossing ")+loc.id+" notify "+car.name+" to enter");
								//trigger recv response event
								if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
									EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, car.name, car.loc.name, 1));
							}
							else{
								for(Car wcar : loc.waitingCars)
									if(wcar.loc.cars.size() == 1 || wcar.loc.cars.peek() == wcar){
										loc.setAdmittedCar(wcar);
										Command.send(wcar, 1);
										System.out.println((loc instanceof Street?"Street ":"Crossing ")+loc.id+" notify "+wcar.name+" to enter");
										//trigger recv response event
										if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
											EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, wcar.name, wcar.loc.name, 1));
										break;
									}
							}
						}
					}
				}
			}	
		}
		
		public void sendNotice(Section loc){
			synchronized (req) {
				req.add(new Request(loc));
				req.notify();
			}
		}
	};
	
	public static void sendRequest(Car car, Section location, int cmd){
		synchronized (req) {
			req.add(new Request(car, location, cmd));
			req.notify();
		}
//		System.out.println(car.name+" send Request "+cmd+" to Crossing "+id);
		System.out.println(car.name+" send Request "+cmd+" to Traffic Police");
		//trigger send request event
		if(EventManager.hasListener(Event.Type.CAR_SEND_REQUEST))
			EventManager.trigger(new Event(Event.Type.CAR_SEND_REQUEST, car.name, car.loc.name, cmd));
	}
	
	public static void sendNotice(Section loc){
		notifier.sendNotice(loc);
	}
	
	static class Request{
		Car car;
		Section loc;
		int cmd;
		public Request(Car car, Section loc, int cmd) {
			this.car = car;
			this.loc = loc;
			this.cmd = cmd;
		}
		
		public Request(Section loc) {
			this.loc = loc;
		}
	}
}
