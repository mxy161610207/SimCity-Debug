package nju.ics.lixiaofan.control;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.consistency.middleware.Middleware;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.resource.Resource;
/**
 * NORMAL -> RESET, SUSPEND
 * <p>
 * RESET  -> SUSPEND
 * @author leslie
 *
 */
public class StateSwitcher {
	private static State state = State.NORMAL;
	private static final Object OBJ = new Object();
	private static ConcurrentHashMap<Thread, Boolean> threadStatus = new ConcurrentHashMap<>();//reset or not
	
	public static enum State {NORMAL, RESET, SUSPEND}
	
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
		for(Thread t : threadStatus.keySet())
			t.interrupt();
	}
	
	public static void wakeUp(){
		synchronized (OBJ) {
			OBJ.notify();
		}
	}
	
	public static ResetTask resetTask = new ResetTask();
	public static class ResetTask implements Runnable {
		private final long maxWaitingTime = 1500;
		private Set<Car> car2Locate = new HashSet<Car>();
		private Map<Car, CarInfo> carInfo = new HashMap<Car, CarInfo>();
		public boolean isRealInconsistency;//real inconsistency
		public Car locatedCar = null;
		public long lastStopInstrTime;
		
		private ResetTask() {}
		
		public void run() {
			state = State.RESET;
			//first step: stop the world
			interruptAll();
			Command.stopAllCars();
			if(isRealInconsistency){//all cars need to be located
				car2Locate.addAll(Resource.getConnectedCars());
			}
			else{//Only moving cars and crashed cars need to be located 
				for(Car car :Resource.getConnectedCars()){
					if(car.getRealStatus() != Car.STOPPED)
						car2Locate.add(car);
					if(car.getRealLoc() != null){
						Set<Car> crashedCars = new HashSet<Car>(car.getRealLoc().realCars);
						for(Car c : car.getRealLoc().cars)
							if(c.isReal())
								crashedCars.add(c);
						if(crashedCars.size() > 1)
							car2Locate.addAll(crashedCars);
					}
				}
			}
			//store the info of cars that have no need to locate
			for(Car car :Resource.getConnectedCars()){
				if(car2Locate.contains(car))
					continue;
				carInfo.put(car, new CarInfo(car.getRealLoc(), car.getRealDir()));
			}
			
			if(!allReset()){
				synchronized (OBJ) {
					try {
						OBJ.wait();//wait for all threads reaching their safe points
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			long elapsedTime = System.currentTimeMillis() - lastStopInstrTime;
			if(elapsedTime < maxWaitingTime)
				try {
					Thread.sleep(maxWaitingTime - elapsedTime);//wait for all cars to stop
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			
			//second step: clear all commands and statuses
			TrafficMap.reset();
			Middleware.reset();
			
			//third step: resolve the inconsistency
			//for false inconsistency, just restore its loc and dir
			for(Map.Entry<Car, CarInfo> e : carInfo.entrySet())
				e.getValue().restore(e.getKey());

			//for real inconsistency, locate cars one by one
			for(Car car : car2Locate){
				locatedCar = car;
				Command.drive(car);
				synchronized (OBJ) {
					try {
						OBJ.wait();//wait for any readings from sensors
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				elapsedTime = System.currentTimeMillis() - lastStopInstrTime;
				if(elapsedTime < maxWaitingTime)
					try {
						Thread.sleep(maxWaitingTime - elapsedTime);//wait for the car to stop
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
			
			//fourth step: recover the world
			resetThreadStatus();
			locatedCar = null;
			car2Locate.clear();
			carInfo.clear();
			state = State.NORMAL;
			Dashboard.repaintTrafficMap();
			Dashboard.updateVC();
//			Dashboard.enableResetButton(true);
			Dashboard.enableCtrlUI(true);
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
	};
	
	private static State prevState = null;
	private static final Lock SUSPEND_LOCK = new ReentrantLock();
	private static Set<Car> movingCars = new HashSet<>();
	public static void suspend(){//TODO
		if(prevState != null)//already called this method before
			return;
		SUSPEND_LOCK.lock();
		prevState = state;
		state = State.SUSPEND;
		Dashboard.enableCtrlUI(false);
		Dashboard.showDeviceDialog(true);
		for(Car car : Resource.getConnectedCars()){
			Command.stop(car);
			if (car.getRealStatus() == Car.MOVING
					|| car.getRealStatus() == Car.UNCERTAIN
					&& car.trend == Car.MOVING)
				movingCars.add(car);
		}
		SUSPEND_LOCK.unlock();
	}
	
	public static void resume(){//TODO
		if(prevState == null)//suspend() not called in advance
			return;
		SUSPEND_LOCK.lock();
		for(Car car : movingCars)
			Command.drive(car);
		movingCars.clear();
		Dashboard.closeDeviceDialog();
		Dashboard.enableCtrlUI(true);
		state = prevState;
		prevState = null;
		SUSPEND_LOCK.unlock();
	}
}
