package nju.xiaofanli.control;

import java.util.LinkedList;
import java.util.Queue;

import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.city.Section;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

public class Police implements Runnable{
	private static final Queue<Request> req = new LinkedList<>();
	private static Notifier notifier = new Notifier();
	public Police() {
		new Thread(notifier, "Traffic Notifier").start();
		new Thread(this, "Police").start();
	}

	public static final int REQUEST2STOP = Car.STOPPED;
	public static final int REQUEST2ENTER = Car.MOVING;
//	public static final int ALREADY_STOPPED = 2;
	public static final int BEFORE_ENTRY = 3;
	public static final int AFTER_ENTRY = 4;
	public static final int BEFORE_LEAVE = 5;
	public static final int AFTER_LEAVE = 6;
	public static final int BEFORE_VANISH = 7;
	public static final int AFTER_VANISH = 8;

	public void run() {
		Thread thread = Thread.currentThread();
		StateSwitcher.register(thread);
		//noinspection InfiniteLoopStatement
		while(true){
			while(req.isEmpty() || !StateSwitcher.isNormal()){
				synchronized (req) {
					try {
						req.wait();
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
//			System.out.println("Traffic Police awake!!!");
			Request r;
			synchronized (req) {
				r = req.poll();
			}
			if(r == null || r.requested == null)
				continue;

			//noinspection SynchronizeOnNonFinalField
			synchronized (r.requested.waiting) {
				switch (r.cmd) {
					case REQUEST2STOP:
						Command.send(r.car, Command.STOP);
						if(r.car.finalState == Car.STOPPED)
							r.requested.removeWaitingCar(r.car);
						else
							r.requested.addWaitingCar(r.car);
						if(r.car == r.requested.getPermitted()){
							r.requested.setPermitted(null);
							sendNotice(r.requested);
						}
						break;
					case REQUEST2ENTER:
						if(r.requested.isOccupied()){
							boolean real = false;
							for(Car car : r.requested.cars)
								if(!car.hasPhantom()){
									real = true;
									break;
								}
							if(!real)
								Dashboard.playErrorSound();
							//tell the car to stop
							System.out.println(r.car.name + " need to STOP!!!");
							r.requested.addWaitingCar(r.car);
							Command.send(r.car, Command.STOP);
							Command.send(r.car, Command.URGE);
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.STOP));
						}
						else if(r.requested.getPermitted() != null && r.car != r.requested.getPermitted()){
							//tell the car to stop
							System.out.println(r.car.name + " need to STOP!!!2");
							r.requested.addWaitingCar(r.car);
							Command.send(r.car, Command.STOP);
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.STOP));
						}
						else{
							//tell the car to enter
							System.out.println(r.car.name + " can ENTER!!!");
							r.requested.setPermitted(r.car);
							Command.send(r.car, Command.MOVE_FORWARD);
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.MOVE_FORWARD));
						}
						break;
//					case ALREADY_STOPPED:
//						//the car is already stopped
//						if(r.car.finalState == Car.MOVING){
//							reqSec.addWaitingCar(r.car);
//							System.out.println(r.car.name+" waits for "+reqSec.name);
//						}
//						break;
					case BEFORE_ENTRY:
						r.requested.removeWaitingCar(r.car);
						break;
					case AFTER_ENTRY:
						r.requested.removeWaitingCar(r.car);
						break;
					case BEFORE_LEAVE:
						r.requested.removeWaitingCar(r.car);
						break;
					case AFTER_LEAVE:
						sendNotice(r.requested);
						break;
					case BEFORE_VANISH:
						r.requested.removeWaitingCar(r.car);
						if(r.requested.getPermitted() == r.car){
							r.requested.setPermitted(null);
							sendNotice(r.requested);
						}
						break;
					case AFTER_VANISH:
						sendNotice(r.requested);
						break;
				}
			}
		}
	}

	private static class Notifier implements Runnable {
		final Queue<Request> req = new LinkedList<>();

		public void run() {
			Thread thread = Thread.currentThread();
			StateSwitcher.register(thread);
			//noinspection InfiniteLoopStatement
			while(true){
				while(req.isEmpty() || !StateSwitcher.isNormal()){
					synchronized (req) {
						try {
							req.wait();
						} catch (InterruptedException e) {
//							e.printStackTrace();
							if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
								clear();
						}
					}
				}
//				if(StateSwitcher.isResetting()){
//					if(!StateSwitcher.isThreadReset(thread))
//						clear();
//					continue;
//				}

				Section loc;
				synchronized (req) {
					loc = req.poll().loc;
				}
				if(loc == null || loc.isOccupied())
					continue;
				//noinspection SynchronizeOnNonFinalField
				synchronized (loc.waiting) {
					if(loc.waiting.isEmpty())
						loc.setPermitted(null);
					else{
						Car car = loc.waiting.peek();
						if(car.loc.cars.size() == 1){
							loc.setPermitted(car);
							Command.send(car, Command.MOVE_FORWARD);
							System.out.println(loc.name + " notify " + car.name + " to enter");
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, car.name, car.loc.name, Command.MOVE_FORWARD));
						}
						else{
							for(Car wcar : loc.waiting)
								if(wcar.loc.cars.peek() == wcar){
									loc.setPermitted(wcar);
									Command.send(wcar, Command.MOVE_FORWARD);
									System.out.println(loc.name + " notify " + wcar.name + " to enter");
									//trigger recv response event
									if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
										EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, wcar.name, wcar.loc.name, Command.MOVE_FORWARD));
									break;
								}
						}
					}
				}
			}
		}

		public void add(Section loc){
			if(StateSwitcher.isResetting())
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
	}

	public static void add(Car car, int dir, Section loc, int cmd, Section requested) {
		if(StateSwitcher.isResetting())
			return;
		synchronized (req) {
			req.add(new Request(car, dir, loc, cmd, requested));
			req.notify();
		}
		System.out.println(car.name+" send Request "+cmd+" to Police");
		//trigger send request event
		if(EventManager.hasListener(Event.Type.CAR_SEND_REQUEST))
			EventManager.trigger(new Event(Event.Type.CAR_SEND_REQUEST, car.name, loc.name, cmd));
	}

	private static void sendNotice(Section loc){
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
		Section loc, requested;
		int cmd;
		/**
		 * for requests that police handles
		 */
		Request(Car car, int dir, Section loc, int cmd, Section requested) {
			this.car = car;
			this.dir = dir;
			this.loc = loc;
			this.cmd = cmd;
			this.requested = requested;
		}

		/**
		 * for requests that notifier handles
		 */
		Request(Section loc) {
			this.loc = loc;
		}
	}
}
