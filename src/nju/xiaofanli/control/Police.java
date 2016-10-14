package nju.xiaofanli.control;

import java.util.LinkedList;
import java.util.Queue;

import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.city.Road;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
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
//						if(r.car.finalState == Car.STOPPED)
                        r.requested.removeWaitingCar(r.car); // there's not such a scene where finalState == moving
//						else
//							r.requested.addWaitingCar(r.car);
						if(r.car == r.requested.getPermitted()){
							r.requested.setPermitted(null);
							sendNotice(r.requested);
						}
						break;
					case REQUEST2ENTER:
					    if(r.car.isInCrash && !r.fromUser){
                            Command.send(r.car, Command.STOP);
                            r.requested.removeWaitingCar(r.car);
                            if(r.car == r.requested.getPermitted()){
                                r.requested.setPermitted(null);
                                sendNotice(r.requested);
                            }
                        }
						else if(r.requested.isOccupied()){
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

    public static void add(Request request) {
        if(request == null || StateSwitcher.isResetting())
            return;
        synchronized (req) {
            req.add(request);
            req.notify();
        }
        System.out.println(request.car.name + " send Request " + request.cmd + " to Police");
        //trigger send request event
        if(EventManager.hasListener(Event.Type.CAR_SEND_REQUEST))
            EventManager.trigger(new Event(Event.Type.CAR_SEND_REQUEST, request.car.name, request.loc.name, request.cmd));
    }

    public static void add(Car car, int dir, Road loc, int cmd, Road requested) {
        add(new Request(car, dir, loc, cmd, requested));
    }

    public static void add(Car car, int dir, Road loc, int cmd, Road requested, boolean fromUser) {
        add(new Request(car, dir, loc, cmd, requested, fromUser));
    }

    private static void sendNotice(Road loc){
        notifier.add(loc);
    }

    public static void clear(){
        synchronized (req) {
            req.clear();
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

				Road loc;
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
                        for(Car car : loc.waiting) {
                            if (car.isInCrash) {
                                loc.removeWaitingCar(car);
                            }
                            else if (car.loc.cars.peek() == car) {
                                loc.setPermitted(car);
                                Command.send(car, Command.MOVE_FORWARD);
                                System.out.println(loc.name + " notify " + car.name + " to enter");
                                //trigger recv response event
                                if (EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
                                    EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, car.name, car.loc.name, Command.MOVE_FORWARD));
                                break;
                            }
                        }
					}
				}
			}
		}

		public void add(Road loc){
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

	private static class Request{
		Car car;
		int dir;
		Road loc, requested;
		int cmd;
        boolean fromUser = false; // whether this request is sent by user
		/**
		 * for requests that police handles
		 */
		Request(Car car, int dir, Road loc, int cmd, Road requested) {
			this.car = car;
			this.dir = dir;
			this.loc = loc;
			this.cmd = cmd;
			this.requested = requested;
		}

        Request(Car car, int dir, Road loc, int cmd, Road requested, boolean fromUser) {
            this(car, dir, loc, cmd, requested);
            this.fromUser = fromUser;
        }

		/**
		 * for requests that notifier handles
		 */
		Request(Road loc) {
			this.loc = loc;
		}
	}
}
