package nju.xiaofanli.schedule;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;
import nju.xiaofanli.util.Counter;

import java.util.*;

public class Police implements Runnable{
	private static final Queue<Request> req = new LinkedList<>();
	private static final Map<Car, Set<Road>> permittingRoads = new HashMap<>(); //roads that the car is permitted to enter
	private static final Map<Car, Set<Road>> waitedRoads = new HashMap<>(); //roads that the car is waiting for
	private static final Map<Car, Boolean> engineStarted = new HashMap<>();
	public Police() {
		Resource.getConnectedCars().forEach(car -> {
			permittingRoads.put(car, new HashSet<>());
			waitedRoads.put(car, new HashSet<>());
			engineStarted.put(car, false);
		});
		new Thread(this, "Police").start();
	}

	public static final int REQUEST2STOP = Car.STOPPED;
	public static final int REQUEST2ENTER = Car.MOVING;
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
							reset();
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
						Command.send(r.car, Command.STOP, !r.manual);
                        r.car.setAvailCmd(Command.MOVE_FORWARD);
						waitedRoads.get(r.car).forEach(road -> road.removeWaitingCar(r.car));
						waitedRoads.get(r.car).clear();
						if(r.car == r.requested.permitted) {
							permittingRoads.get(r.requested.permitted).remove(r.requested);
							r.requested.permitted = null;
							triggerEventAfterLeaving(r.requested);
						}
						if (engineStarted.get(r.car) && r.manual)
							engineStarted.put(r.car, false);
						Counter.increaseStop2Stop();
						break;
					case REQUEST2ENTER:
					    if(!engineStarted.get(r.car) && !r.manual) {
                            Command.send(r.car, Command.STOP, true);
                            r.car.setAvailCmd(Command.MOVE_FORWARD);
							waitedRoads.get(r.car).forEach(road -> road.removeWaitingCar(r.car));
							waitedRoads.get(r.car).clear();
                            if(r.car == r.requested.permitted){
								permittingRoads.get(r.requested.permitted).remove(r.requested);
								r.requested.permitted = null;
								triggerEventAfterLeaving(r.requested);
							}
                        }
						else if(r.requested.isOccupied() || r.requested.permitted != null && r.requested.permitted != r.car){
							//tell the car to stop
							System.out.println(r.car.name + " need to STOP!!!");
							r.requested.addWaitingCar(r.car);
							waitedRoads.get(r.car).add(r.requested);
							Command.send(r.car, Command.STOP, !r.manual);
                            r.car.setAvailCmd(Command.STOP);
							engineStarted.put(r.car, true);
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.STOP));
							Counter.increaseEnter2Stop();
						}
						else{
							//tell the car to enter
							System.out.println(r.car.name + " can ENTER!!!");
							r.requested.permitted = r.car;
							permittingRoads.get(r.requested.permitted).add(r.requested);
							Command.send(r.car, Command.MOVE_FORWARD);
                            r.car.setAvailCmd(Command.STOP);
							engineStarted.put(r.car, true);
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, r.car.name, r.car.loc.name, Command.MOVE_FORWARD));
							Counter.increaseEnter2Enter();
						}
						break;
					case BEFORE_ENTRY:
						//remove the permission which the car already gets but doesn't need any more
						for (Iterator<Road> iter = permittingRoads.get(r.car).iterator(); iter.hasNext();) {
							Road road = iter.next();
							if (road != r.requested) {
								iter.remove();
								road.permitted = null;
								triggerEventAfterLeaving(road);
							}
						}
						break;
					case AFTER_ENTRY:
						if (r.requested.permitted != null && r.requested.permitted != r.car && r.requested.permitted.loc != r.requested) { //forced to enter
							Car permittedCar = r.requested.permitted;
							r.requested.permitted = r.car;
							permittingRoads.get(r.requested.permitted).add(r.requested);
							//tell the permitted car to stop
							System.out.println(permittedCar.name + " need to STOP!!!3");
							Command.send(permittedCar, Command.STOP);
							permittedCar.setAvailCmd(Command.STOP);
							r.requested.addWaitingCar(permittedCar);
							waitedRoads.get(permittedCar).add(r.requested);
							//trigger recv response event
							if(EventManager.hasListener(Event.Type.CAR_RECV_RESPONSE))
								EventManager.trigger(new Event(Event.Type.CAR_RECV_RESPONSE, permittedCar.name, permittedCar.loc.name, Command.STOP));
						}
						break;
					case BEFORE_LEAVE:
						waitedRoads.get(r.car).forEach(road -> road.removeWaitingCar(r.car));
						waitedRoads.get(r.car).clear();
						break;
					case AFTER_LEAVE:
						if(r.requested.permitted == r.car) {
							permittingRoads.get(r.requested.permitted).remove(r.requested);
							r.requested.permitted = null;
						}
						triggerEventAfterLeaving(r.requested);
						break;
					case BEFORE_VANISH:
						waitedRoads.get(r.car).forEach(road -> road.removeWaitingCar(r.car));
						waitedRoads.get(r.car).clear();

						if(r.requested.permitted == r.car) {
							permittingRoads.get(r.requested.permitted).remove(r.requested);
							r.requested.permitted = null;
							triggerEventAfterLeaving(r.requested);
						}
						break;
					case AFTER_VANISH:
						if(r.requested.permitted == r.car) {
							permittingRoads.get(r.requested.permitted).remove(r.requested);
							r.requested.permitted = null;
						}
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
			if(road.waiting.isEmpty()) {
				if (road.permitted != null) {
					permittingRoads.get(road.permitted).remove(road);
					road.permitted = null;
				}
			}
			else if (road.permitted == null) {
				for(Car car : road.waiting) {
					if (car.loc.cars.peek() == car) {
						road.permitted = car;
						permittingRoads.get(road.permitted).add(road);
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

    public static void add(Car car, TrafficMap.Direction dir, Road loc, int cmd, Road requested, boolean manual) {
        add(new Request(car, dir, loc, cmd, requested, manual));
    }

	public static void reset(){
        synchronized (req) {
            req.clear();
        }

        permittingRoads.values().forEach(Set::clear);
        waitedRoads.values().forEach(Set::clear);
		for (Map.Entry<Car, Boolean> entry : engineStarted.entrySet())
			entry.setValue(false);
    }

	/**
	 * Only for debugging purpose.
	 */
	public static void addCarInConsole(Car car) {
		permittingRoads.put(car, new HashSet<>());
		waitedRoads.put(car, new HashSet<>());
	}

	private static class Request{
		Car car;
		TrafficMap.Direction dir;
		Road loc;
		Road requested;
		int cmd;
        boolean manual = false; // whether this request is sent by user

		Request(Car car, TrafficMap.Direction dir, Road loc, int cmd, Road requested, boolean manual) {
			this.car = car;
			this.dir = dir;
			this.loc = loc;
			this.cmd = cmd;
			this.requested = requested;
            this.manual = manual;
        }
	}
}
