package nju.xiaofanli.control;

import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

import java.util.LinkedList;
import java.util.Queue;

public class Police implements Runnable{
	private static final Queue<Request> req = new LinkedList<>();
	public Police() {
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
			synchronized (req) {
				 while(req.isEmpty() || !StateSwitcher.isNormal()) {
					try {
						req.wait();
					} catch (InterruptedException e) {
//						e.printStackTrace();
						if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
							clear();
					}
				}
			}

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
                        r.car.setAvailCmd(Command.MOVE_FORWARD);
                        r.requested.removeWaitingCar(r.car);
						if(r.car == r.requested.permitted) {
							r.requested.permitted = null;
							triggerEventAfterLeaving(r.requested);
						}
						break;
					case REQUEST2ENTER:
					    if(TrafficMap.crashOccurred && !r.fromUser) {
                            Command.send(r.car, Command.STOP);
                            r.car.setAvailCmd(Command.MOVE_FORWARD);
                            r.requested.removeWaitingCar(r.car);
                            if(r.car == r.requested.permitted){
								r.requested.permitted = null;
								triggerEventAfterLeaving(r.requested);
							}
                        }
						else if(r.requested.isOccupied()){
							//tell the car to stop
							System.out.println(r.car.name + " need to STOP!!!");
							r.requested.addWaitingCar(r.car);
							Command.send(r.car, Command.STOP);
                            r.car.setAvailCmd(Command.STOP);
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.STOP));
						}
						else if(r.requested.permitted != null && r.requested.permitted != r.car){
							//tell the car to stop
							System.out.println(r.car.name + " need to STOP!!!2");
							r.requested.addWaitingCar(r.car);
							Command.send(r.car, Command.STOP);
                            r.car.setAvailCmd(Command.STOP);
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.STOP));
						}
						else{
							//tell the car to enter
							System.out.println(r.car.name + " can ENTER!!!");
							r.requested.permitted = r.car;
							Command.send(r.car, Command.MOVE_FORWARD);
                            r.car.setAvailCmd(Command.STOP);
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
						if (r.requested.permitted != null && r.requested.permitted != r.car && r.requested.permitted.loc != r.requested) { //enforce to enter
							Car permittedCar = r.requested.permitted;
							r.requested.permitted = r.car;
							//tell the permitted car to stop
							System.out.println(permittedCar.name + " need to STOP!!!3");
							r.requested.addWaitingCar(permittedCar);
							Command.send(permittedCar, Command.STOP);
							permittedCar.setAvailCmd(Command.STOP);
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, permittedCar.name, permittedCar.loc.name, Command.STOP));
						}
						break;
					case BEFORE_LEAVE:
						r.requested.removeWaitingCar(r.car);
						break;
					case AFTER_LEAVE:
						if(r.requested.permitted == r.car)
							r.requested.permitted = null;
						triggerEventAfterLeaving(r.requested);
						break;
					case BEFORE_VANISH:
						r.requested.removeWaitingCar(r.car);
						if(r.requested.permitted == r.car) {
							r.requested.permitted = null;
							triggerEventAfterLeaving(r.requested);
						}
						break;
					case AFTER_VANISH:
						if(r.requested.permitted == r.car)
							r.requested.permitted = null;
						triggerEventAfterLeaving(r.requested);
						break;
				}
			}
		}
	}

	private void triggerEventAfterLeaving(Road road) {
		if(road == null || road.isOccupied())
			return;

		synchronized (road.waiting) {
			if(road.waiting.isEmpty())
				road.permitted = null;
			else if (road.permitted == null) {
				for(Car car : road.waiting) {
					if (car.isInCrash)
						road.removeWaitingCar(car);
					else if (car.loc.cars.peek() == car && !TrafficMap.crashOccurred) {
						road.permitted = car;
						Command.send(car, Command.MOVE_FORWARD);
						car.setAvailCmd(Command.STOP);
						System.out.println(road.name + " notify " + car.name + " to enter");
						//trigger recv response event
						if (EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
							EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, car.name, car.loc.name, Command.MOVE_FORWARD));
						break;
					}
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

    public static void add(Car car, TrafficMap.Direction dir, Road loc, int cmd, Road requested) {
        add(new Request(car, dir, loc, cmd, requested));
    }

    public static void add(Car car, TrafficMap.Direction dir, Road loc, int cmd, Road requested, boolean fromUser) {
        add(new Request(car, dir, loc, cmd, requested, fromUser));
    }

	public static void clear(){
        synchronized (req) {
            req.clear();
        }
    }

	private static class Request{
		Car car;
		TrafficMap.Direction dir;
		Road loc, requested;
		int cmd;
        boolean fromUser = false; // whether this request is sent by user

		Request(Car car, TrafficMap.Direction dir, Road loc, int cmd, Road requested) {
			this.car = car;
			this.dir = dir;
			this.loc = loc;
			this.cmd = cmd;
			this.requested = requested;
		}

        Request(Car car, TrafficMap.Direction dir, Road loc, int cmd, Road requested, boolean fromUser) {
            this(car, dir, loc, cmd, requested);
            this.fromUser = fromUser;
        }
	}
}
