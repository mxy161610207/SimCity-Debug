package nju.ics.lixiaofan.sensor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.car.Remedy;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.consistency.middleware.Middleware;
import nju.ics.lixiaofan.context.Context;
import nju.ics.lixiaofan.context.ContextManager;
import nju.ics.lixiaofan.control.Reset;
import nju.ics.lixiaofan.control.Police;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;
import nju.ics.lixiaofan.monitor.AppPkg;
import nju.ics.lixiaofan.monitor.PkgHandler;
import nju.ics.lixiaofan.resource.ResourceProvider;

public class BrickHandler extends Thread{
	private static Queue<RawData> rawData = new LinkedList<RawData>();
	private static Queue<CheckedData> checkedData = new LinkedList<CheckedData>();
	private Thread checkedDataHandler = new Thread("Checked Data Handler"){
		public void run() {
			Thread curThread = Thread.currentThread();
			Reset.addThread(curThread);
			while(true){
				while(checkedData.isEmpty()){
					try {
						synchronized (checkedData) {
							checkedData.wait();
						}
					} catch (InterruptedException e) {
//						e.printStackTrace();
						if(Reset.isResetting() && !Reset.isThreadReset(curThread))
							clearCheckedData();
					}
				}
				if(Reset.isResetting()){
					if(!Reset.isThreadReset(curThread))
						clearCheckedData();
					continue;
				}
				CheckedData info = null;
				synchronized (checkedData) {
					info = checkedData.poll();
				}
				if(info == null)
					continue;
				
				switch (info.type) {
				case nju.ics.lixiaofan.consistency.context.Context.Normal:
					switchState(info.car, info.sensor, true);
					break;
				case nju.ics.lixiaofan.consistency.context.Context.FN:
					if(Middleware.isDetectionEnabled()){
						info.sensor.nextSection.displayBalloon(info.type,
								info.sensor.name, info.car.name,
								Middleware.isResolutionEnabled());
						if(Middleware.isResolutionEnabled())
							switchState(info.car, info.sensor, true);
					}
					break;
				case nju.ics.lixiaofan.consistency.context.Context.FP:
					if(Middleware.isDetectionEnabled())
						info.sensor.nextSection.displayBalloon(info.type,
								info.sensor.name, info.car.name,
								Middleware.isResolutionEnabled());
					if(!Middleware.isResolutionEnabled())
						switchState(info.car, info.sensor, false);
					break;
				default:
					break;
				}
			}
		}
	};
	
	public BrickHandler(String name) {
		super(name);
		checkedDataHandler.start();
	}
	
	@Override
	public void run() {
		Thread curThread = Thread.currentThread();
		Reset.addThread(curThread);
		while(true){
			while(rawData.isEmpty()){
				try {
					synchronized (rawData) {
						rawData.wait();
					}
				} catch (InterruptedException e) {
//					e.printStackTrace();
					if(Reset.isResetting() && !Reset.isThreadReset(curThread))
						clearRawData();
				}
			}
			if(Reset.isResetting()){
				if(!Reset.isThreadReset(curThread))
					clearRawData();
				continue;
			}
			RawData data = null;
			synchronized (rawData) {
				data = rawData.poll();
			}
			SensorManager.trigger(data.sensor, data.reading);
			switchState(data.sensor, data.reading);
		}
	}
	
	private void switchState(Car car, Sensor sensor, boolean isCtxTrue){
		switch(sensor.state){
		case Sensor.UNDETECTED:{
			//TODO need supplements about handling phantoms
			if(isCtxTrue){
				sensor.state = Sensor.DETECTED;
				sensor.car = car;
				if(sensor.prevSensor.state == Sensor.DETECTED && sensor.prevSensor.car == car){
					sensor.prevSensor.state = Sensor.UNDETECTED;
					sensor.prevSensor.car = null;
				}
				
				if(!car.isReal()){
					Section fakeLoc = car.loc;
					car.loc = car.realLoc;
					car.realLoc.realCars.remove(car.name);
					car.realLoc = null;
					car.dir = car.realDir;
					car.status = car.realStatus;
					car.leave(fakeLoc);
					PkgHandler.send(new AppPkg().setCarRealLoc(car.name, null));//TODO bug here
				}
			}
			else if(car.isReal()){
				car.loc.realCars.add(car);
				car.realLoc = car.loc;
				car.realDir = car.dir;
				car.realStatus = car.status;
				PkgHandler.send(new AppPkg().setCarRealLoc(car.name, car.realLoc.name));
			}
//			else if(car.loc.sameAs(sensor.nextSection)){
//				//the phantom already in this section
//				break;
//			}
			
			System.out.println("B"+sensor.bid+"S"+(sensor.sid+1)+" detects car: "+car.name);
//			calibrateAngle(car, sensor[id]);
			
			Section prev = car.loc;
			//inform the traffic police of the entry event
			car.notifyPolice(sensor.nextSection);
			car.enter(sensor.nextSection);//set both loc and dir
//			car.dir = sensor.nextSection.dir[1] == -1 ? sensor.nextSection.dir[0] : sensor.dir;
			car.status = Car.MOVING;
			//trigger context
			if(ContextManager.hasListener())
				ContextManager.trigger(new Context(""+sensor.bid +(sensor.sid+1), car.name, car.getDirStr()));
			
			//trigger entering event
			if(EventManager.hasListener(Event.Type.CAR_ENTER))
				EventManager.trigger(new Event(Event.Type.CAR_ENTER, car.name, car.loc.name));
			if(car.loc.cars.size() > 1){
				Set<String> crashedCars = new HashSet<String>();
				for(Car crashedCar : car.loc.cars)
					crashedCars.add(crashedCar.name);
				//trigger crash event
				if(EventManager.hasListener(Event.Type.CAR_CRASH))
					EventManager.trigger(new Event(Event.Type.CAR_CRASH, crashedCars, car.loc.name));
			}
			
			Remedy.updateWhenDetected(car);
			
			//do triggered stuff		
//			System.out.println(TrafficMap.nameOf(car.location)+"\t"+TrafficMap.nameOf(car.dest));
			if(car.dest != null){
				if(car.dest.sameAs(car.loc)){
					car.finalState = Car.STOPPED;
					Command.send(car, Command.STOP);
					car.notifyPolice(Police.GONNA_STOP);
					Dashboard.appendLog(car.name+" reached destination");
					//trigger reach dest event
					if(EventManager.hasListener(Event.Type.CAR_REACH_DEST))
						EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
				}
				else if(car.finalState == Car.STOPPED){
					car.finalState = Car.MOVING;
					car.notifyPolice(Police.GONNA_MOVE);
					Dashboard.appendLog(car.name+" failed to stop at dest, keep going");
				}
				else
					car.notifyPolice(car.trend == Car.MOVING ? Police.GONNA_MOVE : Police.GONNA_STOP);
			}
			else
				car.notifyPolice(car.trend == Car.MOVING ? Police.GONNA_MOVE : Police.GONNA_STOP);
		
			Police.sendNotice(prev);
		}
		break;
		}
	}

	private void switchState(Sensor sensor, int reading){
		switch(sensor.state){
//		case Sensor.INITIAL:
//			if(sensor.entryDetected(reading)){
//				sensor.state = Sensor.DETECTED;
//				switchState(sensor, reading);
//			}
//			else if(sensor.leaveDetected(reading)){
//				sensor.state = Sensor.UNDETECTED;
//				switchState(sensor, reading);
//			}
//			break;
		case Sensor.DETECTED:
			if(sensor.leaveDetected(reading)){
//				if(isFalsePositive2(sensor)){
				if(sensor.car != null && sensor.car.isReal() 
						&& sensor.car.loc.sameAs(sensor.nextSection)
						&& sensor.car.status == Car.STOPPED){
					System.out.println(sensor.name + " !!!FALSE POSITIVE!!!" +"\treading: " + reading);
					break;
				}
				sensor.state = Sensor.UNDETECTED;
				sensor.car = null;
//				System.out.println(sdf.format(new Date()));
				System.out.println(sensor.name + " LEAVING!!!" + "\treading: " + reading);
			}
			break;
		case Sensor.UNDETECTED:
			if(sensor.entryDetected(reading)){
//				if(isFalsePositive(sensor)){
//					System.out.println("B"+bid+"S"+(sid+1)+" !!!FALSE POSITIVE!!!"
//							+"\tread: " + d + "\tenteringValue: " + enteringValue[bid][sid]);
////					}
//					//trigger context manager
//					if(ContextManager.hasListener())
//						ContextManager.trigger(new Context(""+bid+(sid+1), null, null));
//					break;
//				}
//				System.out.println("B"+bid+"S"+(sid+1)+" ENTERING!!!"
//					+"\tread: " + d + "\tenteringValue: " + enteringValue[bid][sid]);
				
				Car car = null;
				int dir = -1, status = 0;
				boolean isReal = false;
				//check real cars first if exists
				for(Car realCar : sensor.prevSection.realCars){
					if(realCar.realDir == sensor.dir){
						isReal = true;
						car = realCar;
						dir = realCar.realDir;
						status = realCar.realStatus;
						break;
					}
				}
				if(car == null){
					for(Car tmp : sensor.prevSection.cars){
						if(tmp.dir == sensor.dir){
							isReal = tmp.isReal();
							car = tmp;
							dir = car.dir;
							status = car.status;
							break;
						}
					}
				}
				if(car == null){
					System.out.println(sensor.name + ": Can't find car!\treading: "+reading);
					sensor.state = Sensor.UNDETECTED;
					break;
				}
				System.out.println(sensor.name + " ENTERING!!!" + "\treading: " + reading);
				//TODO if the car is fake, directly label this context FP and add to checkedData
				if(isReal)
					Middleware.add(car.name, dir, status, "movement", "enter",
							sensor.prevSection.name, sensor.nextSection.name,
							System.currentTimeMillis(), car, sensor);
				else
					BrickHandler.add(car, sensor,
							nju.ics.lixiaofan.consistency.context.Context.FP);
			}
			break;
		}
	}
	
	/**
	 * This method is only called in resetting phase and will locate cars
	 * 
	 */
	private static void switchStateWhenResetting(Sensor sensor, int reading){
		switch(sensor.state){
//		case Sensor.INITIAL:
//			if(sensor.entryDetected(reading)){
//				sensor.state = Sensor.DETECTED;
//				switchStateWhenResetting(sensor, reading);
//			}
//			else if(sensor.leaveDetected(reading)){
//				sensor.state = Sensor.UNDETECTED;
//				switchStateWhenResetting(sensor, reading);
//			}
//			break;
		case Sensor.DETECTED:
			if(sensor.leaveDetected(reading))
				sensor.state = Sensor.UNDETECTED;
			break;
		case Sensor.UNDETECTED:
			if(sensor.entryDetected(reading)){
				sensor.state = Sensor.DETECTED;
				Car car = Reset.locatedCar;
				if(car != null && car.loc == null){//still not located, then locate it
					Reset.locatedCar = null;
					Command.stop(car);
//					car.enter(sensor.nextSection);
					car.loc = sensor.nextSection;
					car.dir = car.loc.dir[1] == -1 ? car.loc.dir[0] : sensor.dir;
					sensor.nextSection.cars.add(car);
					Reset.wakeUp();
				}
			}
			break;
		}
	}

	public static void add(int bid, int sid, int reading){
		Sensor sensor = ResourceProvider.getSensors().get(bid).get(sid);
		sensor.reading = reading;
		if(Reset.isResetting()){
			switchStateWhenResetting(sensor, reading);
			return;
		}
		RawData datum = new RawData(sensor, reading);
		synchronized (rawData) {
			rawData.add(datum);
			rawData.notify();
		}
	}
	
	public static void add(Car car, Sensor sensor, int type){
		if(Reset.isResetting())
			return;
		CheckedData info = new CheckedData(car, sensor, type);
		synchronized (checkedData) {
			checkedData.add(info);
			checkedData.notify();
		}
	}
	
	public static void clearRawData(){
		synchronized (rawData) {
			rawData.clear();
		}
	}
	
	public static void clearCheckedData(){
		synchronized (checkedData) {
			checkedData.clear();
		}
	}
	
//	private boolean isFalsePositive(Sensor sensor){
//		Section section = sensor.prevSection;
//		if(section.isOccupied()){
//			for(Car car : section.cars)
//				if(car.dir == sensor.dir && car.status != 0)
//					return false;
//				else
//					System.out.println("Sensor dir: "+sensor.dir
//							+"\tCar dir: " + car.dir
//							+"\tCar state: "+car.status);
//		}
//		return true;
//	}
	
//	private boolean isFalsePositive2(Sensor sensor){
//		Section section = sensor.nextSection;
//		for(Car car : section.cars)
//			if(car.isReal() && car.status != Car.STILL)
//				return false;
//		for(String name : section.realCars)
//			if(Car.carOf(name).realStatus != Car.STILL)
//				return false;
//		return true;
//	}
	
//	public static void initValues(){
//		for(int i = 0;i < enteringValue.length;i++)
//			for(int j = 0;j < enteringValue[0].length;j++){
//				enteringValue[i][j] = 10;
//				leavingValue[i][j] = 11;
//			}
//	}
	
	private static class RawData{
		public Sensor sensor;
		public int reading;
		public RawData(Sensor sensor, int reading) {
			this.sensor = sensor;
			this.reading = reading;
		}
	}
	
	private static class CheckedData{
		public Car car;
		public Sensor sensor;
		public int type;
		public CheckedData(Car car, Sensor sensor, int type) {
			this.car = car;
			this.sensor = sensor;
			this.type = type;
		}
	}
}
