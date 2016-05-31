package nju.ics.lixiaofan.control;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.consistency.middleware.Middleware;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.resource.Resource;

public class Reset {
	private static boolean resetting = false;
	private static Object obj = new Object();
	private static ConcurrentHashMap<Thread, Boolean> status = new ConcurrentHashMap<>();//reset or not
	
	public static boolean isResetting(){
		return resetting;
	}
	
//	public static void setReset(boolean b){
//		resetting = b;
//	}
//	
//	public static void setThread(Thread thread){
//		status.put(thread, true);
//	}
	
//	public static boolean getThread(Thread thread){
//		return status.get(thread);
//	}
	
	public static void addThread(Thread thread){
		status.put(thread, false);
		allResetFlag = false;
	}
	
	public static boolean isThreadReset(Thread thread){
		if(status.get(thread))
			return true;
		status.put(thread, true);
		if(areAllThreadsReset())
			wakeUp();
		return false;
	}
	
	private static boolean allResetFlag = status.isEmpty();
	private static boolean areAllThreadsReset(){
		if(allResetFlag)
			return true;
		for(Boolean b : status.values()){
			if(!b)
				return false;
		}
		allResetFlag = true;
		return true;
	}
	
	private static void resetStatus(){
		allResetFlag = status.isEmpty();
		for(Thread t : status.keySet())
			status.put(t, false);
	}
	
	private static void interruptAll(){
		for(Thread t : status.keySet())
			t.interrupt();
	}
	
	public static void wakeUp(){
		synchronized (obj) {
			obj.notify();
		}
	}
	
	public static boolean isRealInc;//real inconsistency
	public static Car locatedCar = null;
	public static long lastStopInstrTime;
	public static Runnable resetTask = new Runnable() {
		private final long maxWaitingTime = 1500;
		private Set<Car> car2Locate = new HashSet<Car>();
		private Map<Car, CarInfo> carInfo = new HashMap<Car, CarInfo>();
		public void run() {
			resetting = true;
			//first step: stop the world
			interruptAll();
			Command.stopAllCars();
			if(isRealInc){//all cars need to be located
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
			
			if(!allResetFlag){
				synchronized (obj) {
					try {
						obj.wait();//wait for all threads reaching their safe points
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
				synchronized (obj) {
					try {
						obj.wait();//wait for any readings from sensors
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
			resetStatus();
			locatedCar = null;
			car2Locate.clear();
			carInfo.clear();
			resetting = false;
			Dashboard.repaintTrafficMap();
			Dashboard.updateVC();
			Dashboard.enableResetButton(true);
		}
		
		class CarInfo{
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
}
