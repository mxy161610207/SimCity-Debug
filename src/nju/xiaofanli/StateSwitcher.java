package nju.xiaofanli;

import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.sensor.Sensor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * NORMAL -> RESET, SUSPEND
 * <p>
 * RESET  -> SUSPEND
 * @author leslie
 *
 */
public class StateSwitcher {
	private static volatile State state = State.NORMAL;
	private static final Object RESET_OBJ = new Object();
	private static ConcurrentHashMap<Thread, Boolean> threadStatus = new ConcurrentHashMap<>();//reset or not

	private enum State {NORMAL, RESET, SUSPEND}

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
            wakeUp();
    }

	public static boolean isThreadReset(Thread thread){
		if(threadStatus.get(thread))
			return true;
		threadStatus.put(thread, true);
		if(isResetting() && allReset())
			wakeUp();
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

	private static void wakeUp(){
		synchronized (RESET_OBJ) {
			RESET_OBJ.notify();
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

    public static void detectedBy(Sensor sensor){
        Car car = resetTask.car2locate;
        if(car != null && car.loc == null){//still not located, then locate it
            resetTask.car2locate = null;
            Command.stop(car);
//            car.loc = sensor.nextRoad;
//			if(car.dir == TrafficMap.UNKNOWN_DIR)
//            	car.dir = car.loc.dir[1] == TrafficMap.UNKNOWN_DIR ? car.loc.dir[0] : sensor.dir;
//            sensor.nextRoad.cars.add(car);
            car.initLocAndDir(sensor);
            resetTask.locatedCars.add(car);
            wakeUp();
        }
    }

	private static ResetTask resetTask = new ResetTask();
	private static class ResetTask implements Runnable {
		private final long maxWaitingTime = 1500;
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
					synchronized (RESET_OBJ) {
						RESET_OBJ.wait();//wait for all threads reaching their safe points
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
						synchronized (RESET_OBJ) {
							RESET_OBJ.wait();// wait for any readings from sensors
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

		private void checkIfSuspended(){
			while(isSuspending())
				try {
					synchronized (SUSPEND_OBJ) {
						SUSPEND_OBJ.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}

		private class CarInfo{
			private Road loc;
			private int dir;
			private CarInfo(Road loc, int dir){
				this.loc = loc;
				this.dir = dir;
			}

			private void restore(Car car){
				car.loc = loc;
				car.dir = dir;
				loc.cars.add(car);
			}
		}
	}

	private static State prevState = null;
	private static final Lock SUSPEND_LOCK = new ReentrantLock();
	private static final Object SUSPEND_OBJ = new Object();
	private static Set<Car> movingCars = new HashSet<>(), whistlingCars = new HashSet<>();
	public static void suspend(){
		if(prevState != null)//already called this method before
			return;
		SUSPEND_LOCK.lock();
		prevState = state;
		setState(State.SUSPEND);
		for(Car car : Resource.getConnectedCars()){
            if(car.trend == Car.MOVING)
                movingCars.add(car);
			Command.stop(car);
			if(car.isHornOn)
				whistlingCars.add(car);
			Command.silence(car);
		}
		if(prevState == State.NORMAL)
			Dashboard.enableCtrlUI(false);
		Dashboard.showDeviceDialog(false);
		SUSPEND_LOCK.unlock();
	}

	public static void resume(){
		if(prevState == null)//suspend() not called in advance
			return;
		SUSPEND_LOCK.lock();
		Dashboard.closeDeviceDialog();
        movingCars.forEach(Command::drive);
		whistlingCars.forEach(Command::whistle);

        if(prevState == State.NORMAL)
			Dashboard.enableCtrlUI(true);

		setState(prevState);
		//must invoked after state changed back
        interruptAll();//drive all threads away from safe points
		if(prevState == State.RESET){
			synchronized (SUSPEND_OBJ) {
				SUSPEND_OBJ.notify();
			}
		}
        movingCars.clear();
		whistlingCars.clear();
		prevState = null;
		SUSPEND_LOCK.unlock();
	}
}
