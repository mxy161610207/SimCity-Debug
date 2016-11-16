package nju.xiaofanli;

import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.SelfCheck;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.sensor.BrickHandler;
import nju.xiaofanli.device.sensor.Sensor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * NORMAL <-> RESET, RELOCATE, SUSPEND
 * <p>
 * RESET, RELOCATE <-> SUSPEND
 * @author leslie
 *
 */
public class StateSwitcher {
	private static volatile State state = State.NORMAL;
    private static ConcurrentHashMap<Thread, Boolean> threadStatus = new ConcurrentHashMap<>();//reset or not

	private enum State {NORMAL, RESET, SUSPEND, RELOCATE}

	private StateSwitcher(){}

	public static boolean isNormal(){
		return state == State.NORMAL;
	}

	public static boolean isResetting(){
		return state == State.RESET;
	}

	public static boolean isSuspending(){
		return state == State.SUSPEND;
	}

	public static boolean isRelocating() {
        return state == State.RELOCATE;
	}

	private static void setState(State s){
        //noinspection SynchronizeOnNonFinalField
        synchronized (state) {
			state = s;
		}
	}

	public static void register(Thread thread){
		threadStatus.put(thread, false);
	}

    public static void unregister(Thread thread){
        threadStatus.remove(thread);
        if(isResetting() && allReset())
            wakeUp(ResetTask.OBJ);
    }

	public static boolean isThreadReset(Thread thread){
		if(threadStatus.get(thread))
			return true;
		threadStatus.put(thread, true);
		if(isResetting() && allReset())
			wakeUp(ResetTask.OBJ);
		return false;
	}

	private static boolean allReset(){
		for(Boolean b : threadStatus.values()){
			if(!b)
				return false;
		}
		return true;
	}

	private static void resetThreadStatus(){
		for(Thread t : threadStatus.keySet())
			threadStatus.put(t, false);
	}

	private static void interruptAll(){
		threadStatus.keySet().forEach(Thread::interrupt);
	}

	private static void wakeUp(final Object obj){
		synchronized (obj) {
            obj.notify();
		}
	}

	public static void startResetting(){
        Resource.execute(resetTask);
    }

	public static void setInconsistencyType(boolean isReal){
        resetTask.isRealInconsistency = isReal;
    }

    public static void setLastStopCmdTime(long time){
        resetTask.lastStopCmdTime = time;
    }

    public static void detectedBy(Sensor sensor) {
        if (isResetting())
            ResetTask.detectedBy(sensor);
        else if (isRelocating())
            Relocation.detectedBy(sensor);
    }

	private static ResetTask resetTask = new ResetTask();
	private static class ResetTask implements Runnable {
        private static final Object OBJ = new Object();
        private final long maxWaitingTime = 1000;
		private Set<Car> cars2locate = new HashSet<>();
		private Map<Car, CarInfo> carInfo = new HashMap<>();
		boolean isRealInconsistency;//real inconsistency
        private Car car2locate = null;
        private long lastStopCmdTime;
		private Set<Car> locatedCars = new HashSet<>();

		private ResetTask() {
		}

		public void run() {
			checkIfSuspended();
			setState(State.RESET);
			Dashboard.enableCtrlUI(false);
			//first step: stop the world
			interruptAll();
			Command.stopAllCars();
			Command.silenceAllCars();
			if(isRealInconsistency){//all cars need to be located
				cars2locate.addAll(Resource.getConnectedCars());
			}
			else{//Only moving cars and crashed cars need to be located
				for(Car car :Resource.getConnectedCars()){
					if(car.getState() != Car.STOPPED)
						cars2locate.add(car);
					if(car.getRealLoc() != null){
						Set<Car> crashedCars = new HashSet<>(car.getRealLoc().realCars);
                        crashedCars.addAll(car.getRealLoc().cars.stream().filter(x -> !x.hasPhantom()).collect(Collectors.toList()));

						if(crashedCars.size() > 1)
							cars2locate.addAll(crashedCars);
					}
				}
			}
			//store the info of cars that have no need to locate
			for(Car car :Resource.getConnectedCars()){
				if(cars2locate.contains(car))
					continue;
				carInfo.put(car, new CarInfo(car.getRealLoc(), car.getRealDir()));
			}

			while(!allReset()){
				try {
					synchronized (OBJ) {
						OBJ.wait();//wait for all threads reaching their safe points
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					checkIfSuspended();
				}
			}

			long duration = maxWaitingTime - (System.currentTimeMillis() - lastStopCmdTime);
			while(duration > 0){
				long startTime = System.currentTimeMillis();
				try {
					Thread.sleep(duration);//wait for all cars to stop
				} catch (InterruptedException e) {
					e.printStackTrace();
					checkIfSuspended();
				}
				finally{
					duration -= System.currentTimeMillis() - startTime;
				}
			}
			//second step: clear all statuses
			checkIfSuspended();
			TrafficMap.reset();
			Middleware.reset();
            Delivery.reset();

			//third step: resolve the inconsistency
			checkIfSuspended();
			//for false inconsistency, just restore its loc and dir
			for(Map.Entry<Car, CarInfo> e : carInfo.entrySet())
				e.getValue().restore(e.getKey());
			//for real inconsistency, locate cars one by one
			for(Car car : cars2locate){
				car2locate = car;
				Command.drive(car);
				while(!locatedCars.contains(car)){
					try {
						synchronized (OBJ) {
							OBJ.wait();// wait for any readings from sensors
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						checkIfSuspended();
					}
				}

				duration = maxWaitingTime - (System.currentTimeMillis() - lastStopCmdTime);
				while(duration > 0){
					long startTime = System.currentTimeMillis();
					try {
						Thread.sleep(duration);//wait for the car to stop
					} catch (InterruptedException e) {
						e.printStackTrace();
						checkIfSuspended();
					}
					finally{
						duration -= System.currentTimeMillis() - startTime;
					}
				}
			}
			//fourth step: recover the world
			checkIfSuspended();
			resetThreadStatus();
			car2locate = null;
			cars2locate.clear();
			locatedCars.clear();
			carInfo.clear();

			Dashboard.enableCtrlUI(true);
			Dashboard.reset();
			setState(State.NORMAL);

            TrafficMap.checkRealCrash();
		}

        public static void detectedBy(Sensor sensor) {
            if (isResetting()) {
                Car car = resetTask.car2locate;
                if (car != null && car.loc == null) {//still not located, then locate it
                    resetTask.car2locate = null;
                    Command.stop(car);
                    car.initLocAndDir(sensor);
                    resetTask.locatedCars.add(car);
                    wakeUp(ResetTask.OBJ);
                }
            }
        }

		private class CarInfo{
			private Sensor sensor;
			private CarInfo(Road loc, int dir){
				sensor = loc.adjSensors.get(dir).prevSensor;
			}

			private void restore(Car car){
                car.initLocAndDir(sensor);
			}
		}
	}

	private static State prevState = null;
	private static final Lock SUSPEND_LOCK = new ReentrantLock();
	private static final Object SUSPEND_OBJ = new Object();
	private static Set<Car> movingCars = new HashSet<>(), whistlingCars = new HashSet<>();
    private static boolean isSuspended = false;
	public static void suspend(){
		if(!isSuspended && !SelfCheck.allReady()) {
            SUSPEND_LOCK.lock();
            if (!isSuspended && !SelfCheck.allReady()) {
                System.out.println("*** SUSPEND ***");
                isSuspended = true;
                prevState = state;
                setState(State.SUSPEND);
                for (Car car : Resource.getConnectedCars()) {
                    if (car.trend == Car.MOVING)
                        movingCars.add(car);
                    Command.stop(car);
                    if (car.lastHornCmd == Command.HORN_ON && car.isHornOn)
                        whistlingCars.add(car);
                    Command.silence(car);
                }
                if (prevState == State.NORMAL)
                    Dashboard.enableCtrlUI(false);
                Dashboard.showDeviceDialog(false);
            }
            SUSPEND_LOCK.unlock();
        }
	}

	public static void resume(){
        if(isSuspended && SelfCheck.allReady()) {
            SUSPEND_LOCK.lock();
            if (isSuspended && SelfCheck.allReady()) {
                System.out.println("*** RESUME ***");
                isSuspended = false;
                Dashboard.closeDeviceDialog();
                if (prevState == State.NORMAL)
                    Dashboard.enableCtrlUI(true);

                movingCars.forEach(Command::drive);
                whistlingCars.forEach(Command::whistle);
                setState(prevState);
                interruptAll(); //must invoked after state changed back, drive all threads away from safe points
                if (prevState == State.RESET || prevState == State.RELOCATE) {
                    synchronized (SUSPEND_OBJ) {
                        SUSPEND_OBJ.notify();
                    }
                }
                movingCars.clear();
                whistlingCars.clear();
                prevState = null;
            }
            SUSPEND_LOCK.unlock();
        }
	}

    private static void checkIfSuspended(){
        while(isSuspending()) {
            try {
                synchronized (SUSPEND_OBJ) {
                    SUSPEND_OBJ.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

//    public static void startRelocating(Car car, Sensor reportedSensor) {
//        if (isResetting())
//            return;
//		else if (isNormal())
//			setState(State.RELOCATE); // make sure only one relocation task is running
//        relocation.car2relocate = car;
//        relocation.reportedSensor = reportedSensor;
//        Resource.execute(relocation);
//    }

    /**
     * @param car2relocate the car needed to relocate
     * @param prevSensor the sensor behind the car's known location
     * @param nextSensor the sensor in font of the car's known location
     */
    public static void startRelocating(Car car2relocate, Sensor prevSensor, Sensor nextSensor) {
        if (isResetting())
            return;
        Relocation.add(car2relocate, prevSensor, nextSensor);
    }

//    public static boolean isReportedSensor(Sensor sensor) {
//        return isRelocating() && sensor == relocation.reportedSensor;
//    }

//    public static boolean isInterestedSensor(Sensor sensor) {
//        return isRelocating() && (sensor == relocation.reportedSensor.prevSensor || sensor == relocation.reportedSensor.prevSensor.prevSensor);
//    }

	private static Relocation relocation = new Relocation();
    public static class Relocation extends Thread {
        private static final Object OBJ = new Object();
        private static final Queue<Request> queue = new LinkedList<>();
        private static Car car2relocate = null;
        private static Sensor locatedSensor = null, prevSensor = null, nextSensor = null;
        private static Set<Car> movingCars = new HashSet<>(), whistlingCars = new HashSet<>();
        private static boolean isPreserved = false, isInterested = false;

        private Relocation(){}
        @Override
        public void run() {
            setName("Relocation Thread");
            //noinspection InfiniteLoopStatement
            while (true) {
                while (queue.isEmpty() || isSuspending()) {
                    if (queue.isEmpty() && isPreserved && !isSuspending()) {
                        isPreserved = false;
                        movingCars.forEach(Command::drive);
                        whistlingCars.forEach(Command::whistle);
                        Dashboard.closeRelocationDialog();
                        Dashboard.enableCtrlUI(true);
                        movingCars.clear();
                        whistlingCars.clear();
                        setState(StateSwitcher.State.NORMAL);
                        interruptAll();
                    }
                    try {
                        synchronized (queue) {
                            queue.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                checkIfSuspended();

                Request r;
                synchronized (queue) {
                    r = queue.poll();
                    if (r == null || r.car2relocate == null || r.prevSensor == null || r.nextSensor == null)
                        continue;
                    car2relocate = r.car2relocate;
                    prevSensor = r.prevSensor;
                    nextSensor = r.nextSensor;
                }

                if (!isPreserved) {
                    isPreserved = true;
                    setState(StateSwitcher.State.RELOCATE);
                    for (Car car : Resource.getConnectedCars()) {
                        if (car.trend == Car.MOVING)
                            movingCars.add(car);
                        Command.stop(car);
                        if (car.lastHornCmd == Command.HORN_ON && car.isHornOn)
                            whistlingCars.add(car);
                        Command.silence(car);
                    }
                    Dashboard.enableCtrlUI(false);
                    try {
                        Thread.sleep(1000); // wait for all cars to stop
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    checkIfSuspended();
                }

                int prevRoadDir = prevSensor.nextRoad.dir[1] == TrafficMap.UNKNOWN_DIR ? prevSensor.nextRoad.dir[0] : prevSensor.dir;
                // car moves slower when moving backward, so better multiply by a factor
                long timeout = (long) (prevSensor.nextRoad.timeouts.get(prevRoadDir).get(car2relocate.name) * 1.2);
                Dashboard.clearRelocationDialog();
                Dashboard.showRelocationDialog(car2relocate);
                isInterested = true;
                Command.back(car2relocate);
//                while (locatedSensor == null) {
//                    try {
//                        synchronized (OBJ) {
//                            OBJ.wait();// wait for any readings from sensors
//                        }
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                if (locatedSensor == null) {
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException e) {
//                        e.printStackTrace();
                    }
                }
                Command.stop(car2relocate);
                isInterested = false;
                checkIfSuspended();

                if (locatedSensor == null) { // timeout reached, relocation failed
                    Dashboard.showRelocationDialog(car2relocate, false, prevSensor.nextRoad);
                    synchronized (OBJ) {
                        try {
                            OBJ.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    checkIfSuspended();
                    // the car is manually relocated
                    int dir = prevSensor.nextRoad.dir[1] == TrafficMap.UNKNOWN_DIR ? prevSensor.nextRoad.dir[0] : prevSensor.dir;
                    car2relocate.timeout = prevSensor.nextRoad.timeouts.get(dir).get(car2relocate.name); // reset its timeout
                }
                else {
                    Dashboard.showRelocationDialog(car2relocate, true, null);
                    if (locatedSensor == nextSensor) {
                        locatedSensor.state = Sensor.UNDETECTED;
                        BrickHandler.switchState(locatedSensor, 0, System.currentTimeMillis());
                    }
                    else {
                        int dir = locatedSensor.nextRoad.dir[1] == TrafficMap.UNKNOWN_DIR ? locatedSensor.nextRoad.dir[0] : locatedSensor.dir;
                        car2relocate.timeout = locatedSensor.nextRoad.timeouts.get(dir).get(car2relocate.name); // reset its timeout
                    }
                }
//                Dashboard.closeRelocationDialog();
                car2relocate = null;
                locatedSensor = prevSensor = nextSensor = null;
            }
        }

        public static void manuallyRelocated() {
            synchronized (OBJ) {
                OBJ.notify();
            }
        }

        public static void detectedBy(Sensor sensor) {
            if (isInterestedSensor(sensor)) {
                isInterested = false;
                Command.stop(Relocation.car2relocate);
                Relocation.locatedSensor = sensor;
//                wakeUp(Relocation.OBJ);
                relocation.interrupt();
            }
        }

        public static boolean isInterestedSensor(Sensor sensor) {
            return isRelocating() && isInterested && (sensor == Relocation.prevSensor || sensor == Relocation.nextSensor);
        }

        public static void add(Car car2relocate, Sensor prevSensor, Sensor nextSensor) {
            if (!relocation.isAlive()) {
                relocation.start();
//                System.out.println("!!!start relocation thread!!!");
            }
            Request req = new Request(car2relocate, prevSensor, nextSensor);
            synchronized (queue) {
                if (Relocation.car2relocate == car2relocate)
                    return;
                for (Request r : queue) {
                    if (r.car2relocate == car2relocate)
                        return;
                }
                queue.add(req);
                queue.notify();
            }
        }

        private static class Request {
            private Car car2relocate;
            private Sensor prevSensor, nextSensor;
            Request(Car car2relocate, Sensor prevSensor, Sensor nextSensor) {
                this.car2relocate = car2relocate;
                this.prevSensor = prevSensor;
                this.nextSensor = nextSensor;
            }
        }
    }
}
