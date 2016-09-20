package nju.xiaofanli;

import nju.xiaofanli.city.Section;
import nju.xiaofanli.city.TrafficMap;
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
	
	public static boolean isThreadReset(Thread thread){
		if(threadStatus.get(thread))
			return true;
		threadStatus.put(thread, true);
		if(allReset())
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

    public static void setLastStopInstrTime(long time){
        resetTask.lastStopInstrTime = time;
    }

    public static void detectedBy(Sensor sensor){
        Car car = resetTask.car2Locate;
        if(car != null && car.loc == null){//still not located, then locate it
            resetTask.car2Locate = null;
            Command.stop(car);
//			car.enter(sensor.nextSection);
            car.loc = sensor.nextSection;
            car.dir = car.loc.dir[1] == -1 ? car.loc.dir[0] : sensor.dir;
            sensor.nextSection.cars.add(car);
            resetTask.locatedCars.add(car);
            wakeUp();
        }
    }

	private static ResetTask resetTask = new ResetTask();
	private static class ResetTask implements Runnable {
		private final long maxWaitingTime = 1500;
		private Set<Car> cars2Locate = new HashSet<>();
		private Map<Car, CarInfo> carInfo = new HashMap<>();
		boolean isRealInconsistency;//real inconsistency
        private Car car2Locate = null;
        private long lastStopInstrTime;
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
			if(isRealInconsistency){//all cars need to be located
				cars2Locate.addAll(Resource.getConnectedCars());
			}
			else{//Only moving cars and crashed cars need to be located 
				for(Car car :Resource.getConnectedCars()){
					if(car.getRealState() != Car.STOPPED)
						cars2Locate.add(car);
					if(car.getRealLoc() != null){
						Set<Car> crashedCars = new HashSet<>(car.getRealLoc().realCars);
                        crashedCars.addAll(car.getRealLoc().cars.stream().filter(x -> !x.hasPhantom()).collect(Collectors.toList()));

						if(crashedCars.size() > 1)
							cars2Locate.addAll(crashedCars);
					}
				}
			}
			//store the info of cars that have no need to locate
			for(Car car :Resource.getConnectedCars()){
				if(cars2Locate.contains(car))
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
			
			long duration = maxWaitingTime - (System.currentTimeMillis() - lastStopInstrTime);
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
			//second step: clear all commands and statuses
			checkIfSuspended();
			TrafficMap.reset();
			Middleware.reset();
			//third step: resolve the inconsistency
			checkIfSuspended();
			//for false inconsistency, just restore its loc and dir
			for(Map.Entry<Car, CarInfo> e : carInfo.entrySet())
				e.getValue().restore(e.getKey());
			//for real inconsistency, locate cars one by one
			for(Car car : cars2Locate){
				car2Locate = car;
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
				
				duration = maxWaitingTime - (System.currentTimeMillis() - lastStopInstrTime);
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
			car2Locate = null;
			cars2Locate.clear();
			locatedCars.clear();
			carInfo.clear();
			Dashboard.repaintTrafficMap();
			Dashboard.updateVC();
//			Dashboard.enableResetButton(true);
			Dashboard.enableCtrlUI(true);
			setState(State.NORMAL);
		}
		
		private void checkIfSuspended(){
			while(state == State.SUSPEND)
				try {
					synchronized (SUSPEND_OBJ) {
						SUSPEND_OBJ.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		
		private class CarInfo{
			private Section loc;
			private int dir;
			private CarInfo(Section loc, int dir){
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
	private static Set<Car> movingCars = new HashSet<>();
	public static void suspend(){
		if(prevState != null)//already called this method before
			return;
		SUSPEND_LOCK.lock();
		prevState = state;
		setState(State.SUSPEND);
		for(Car car : Resource.getConnectedCars()){
			Command.stop(car);
			if(prevState != State.NORMAL)
				continue;
			if (car.getRealState() == Car.MOVING
					|| car.getRealState() == Car.UNCERTAIN
					&& car.trend == Car.MOVING)
				movingCars.add(car);
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
		if(prevState == State.NORMAL){
            movingCars.forEach(Command::drive);
			Dashboard.enableCtrlUI(true);
		}
		movingCars.clear();
		setState(prevState);
		//must invoked after state changed back
		if(prevState == State.NORMAL){
			interruptAll();//drive all threads away from safe points
		}
		else if(prevState == State.RESET){
			synchronized (SUSPEND_OBJ) {
				SUSPEND_OBJ.notify();
			}
		}
		prevState = null;
		SUSPEND_LOCK.unlock();
	}
}
