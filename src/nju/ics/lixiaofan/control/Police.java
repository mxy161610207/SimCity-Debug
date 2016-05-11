package nju.ics.lixiaofan.control;

import java.util.LinkedList;
import java.util.Queue;
import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;

public class Police implements Runnable{
	public static Queue<Request> req = new LinkedList<Request>();
	private static Notifier notifier = new Notifier();
	public Police() {
		new Thread(notifier, "Traffice Notifier").start();
		new Thread(this, "Police").start();
	}
	
	public static final int GONNA_STOP = Car.STOPPED;
	public static final int GONNA_MOVE = Car.MOVING;
	public static final int ALREADY_STOPPED = 2;
	public static final int ALREADY_ENTERED = 3;
	
	public void run() {
		Thread curThread = Thread.currentThread();
		Reset.addThread(curThread);
		while(true){
			while(req.isEmpty()){
				synchronized (req) {
					try {
						req.wait();
					} catch (InterruptedException e) {
//						e.printStackTrace();
						if(Reset.isResetting() && !Reset.isThreadReset(curThread))
							clear();
					}
				}
			}
			if(Reset.isResetting()){
				if(!Reset.isThreadReset(curThread))
					clear();
				continue;
			}
//			System.out.println("Traffic Police awake!!!");
			Request r = req.poll();
			Section reqSec = r.loc.adjs.get(r.dir);
//			System.out.println(r.loc.name+" "+r.dir);
			if(reqSec == null){
				System.err.println("reqSec is null");
				continue;
			}
			
			synchronized (reqSec.waiting) {
				switch (r.cmd) {
				case GONNA_STOP:
					if(r.car.finalState == Car.STOPPED)
						reqSec.removeWaitingCar(r.car);
					else
						reqSec.addWaitingCar(r.car);
					if(r.car == reqSec.getPermitted()){
						reqSec.setPermitted(null);
						sendNotice(reqSec);
					}
					break;
				case GONNA_MOVE:
					if(reqSec.isOccupied()){
						boolean real = false;
						for(Car car : reqSec.cars)
							if(car.isReal()){
								real = true;
								break;
							}
						if(!real)
							Dashboard.playErrorSound();
						//tell the car to stop
						System.out.println(r.car.name+" need to STOP!!!");
						reqSec.addWaitingCar(r.car);
						Command.send(r.car, Command.STOP);
						Command.send(r.car, Command.HORN);
						//trigger recv response event
						if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
							EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.STOP));
					}
					else if(reqSec.getPermitted() != null && r.car != reqSec.getPermitted()){
						//tell the car to stop
						System.out.println(r.car.name+" need to STOP!!!2");
						reqSec.addWaitingCar(r.car);
						Command.send(r.car, Command.STOP);
						//trigger recv response event
						if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
							EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.STOP));
					}
					else{
						//tell the car to enter
						System.out.println(r.car.name+" can ENTER!!!");
						reqSec.setPermitted(r.car);
						Command.send(r.car, Command.FORWARD);
						//trigger recv response event
						if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
							EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.FORWARD));
					}
					break;
				case ALREADY_STOPPED:
					//the car is already stopped
					if(r.car.finalState == Car.MOVING){
						reqSec.addWaitingCar(r.car);
						System.out.println(r.car.name+" waits for "+reqSec.name);
					}
					break;
				case ALREADY_ENTERED:
					//inform the traffic police of the entry event
					r.next.removeWaitingCar(r.car);
					if(!reqSec.sameAs(r.next)){
						reqSec.removeWaitingCar(r.car);
						if(r.car == reqSec.getPermitted()){
							reqSec.setPermitted(null);
							sendNotice(reqSec);
						}
					}
					break;
				}
			}
		}
	}

	private static class Notifier implements Runnable {
		Queue<Request> req = new LinkedList<Request>();
		
		public void run() {
			Thread curThread = Thread.currentThread();
			Reset.addThread(curThread);
			while(true){
				while(req.isEmpty()){
					synchronized (req) {
						try {
							req.wait();
						} catch (InterruptedException e) {
//							e.printStackTrace();
							if(Reset.isResetting() && !Reset.isThreadReset(curThread))
								clear();
						}
					}
				}
				if(Reset.isResetting()){
					if(!Reset.isThreadReset(curThread))
						clear();
					continue;
				}
				
				Section loc = req.poll().loc;
				if(loc.isOccupied())
					continue;
				synchronized (loc.waiting) {
					if(loc.waiting.isEmpty())
						loc.setPermitted(null);
					else{
						Car car = loc.waiting.peek();
						if(car.loc.cars.size() == 1){
							loc.setPermitted(car);
							Command.send(car, Command.FORWARD);
							System.out.println(loc.name + " notify " + car.name + " to enter");
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, car.name, car.loc.name, Command.FORWARD));
						}
						else{
							for(Car wcar : loc.waiting)
								if(wcar.loc.cars.peek() == wcar){
									loc.setPermitted(wcar);
									Command.send(wcar, Command.FORWARD);
									System.out.println(loc.name + " notify " + wcar.name + " to enter");
									//trigger recv response event
									if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
										EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, wcar.name, wcar.loc.name, Command.FORWARD));
									break;
								}
						}
					}
				}
			}	
		}
		
		public void add(Section loc){
			if(Reset.isResetting())
				return;
			synchronized (req) {
				req.add(new Request(loc));
				req.notify();
			}
		}
		
		public void clear(){
			synchronized (req) {
				req.clear();
			}
		}
	};
	
	public static void add(Car car, int dir, Section loc, int cmd){
		add(car, dir, loc, cmd, null);
	}
	
	public static void add(Car car, int dir, Section loc, int cmd, Section next) {
		if(Reset.isResetting())
			return;
		synchronized (req) {
			req.add(new Request(car, dir, loc, cmd, next));
			req.notify();
		}
		System.out.println(car.name+" send Request "+cmd+" to Police");
		//trigger send request event
		if(EventManager.hasListener(Event.Type.CAR_SEND_REQUEST))
			EventManager.trigger(new Event(Event.Type.CAR_SEND_REQUEST, car.name, loc.name, cmd));
	}
	
	public static void sendNotice(Section loc){
		notifier.add(loc);
	}
	
	public static void clear(){
		synchronized (req) {
			req.clear();
		}
	}
	
	private static class Request{
		Car car;
		int dir;
		Section loc, next;
		int cmd;
		public Request(Car car, int dir, Section loc, int cmd, Section next) {
			this.car = car;
			this.dir = dir;
			this.loc = loc;
			this.cmd = cmd;
			this.next = next;
		}
		
		public Request(Section loc) {
			this.loc = loc;
		}
	}
}
