package nju.ics.lixiaofan.sensor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.car.Remediation;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.consistency.middleware.Middleware;
import nju.ics.lixiaofan.context.Context;
import nju.ics.lixiaofan.context.ContextManager;
import nju.ics.lixiaofan.control.TrafficPolice;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.data.DataProvider;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;

public class BrickHandler extends Thread{
	private static List<Command> queue = Remediation.queue;
	private static int enteringValue[][] = new int[10][4];
	private static int leavingValue[][] = new int[10][4];
	private static Queue<SensoryData> rawData = new LinkedList<SensoryData>();
	
	private static Queue<SensoryInfo> checkedData = new LinkedList<SensoryInfo>();
	private Thread checkedDataHandler = new Thread(){
		public void run() {
			while(true){
				while(checkedData.isEmpty()){
					try {
						synchronized (checkedData) {
							checkedData.wait();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				SensoryInfo info = null;
				synchronized (checkedData) {
					info = checkedData.poll();
				}
				if(info == null)
					continue;
				
				switch (info.type) {
				case nju.ics.lixiaofan.consistency.context.Context.Normal:
					stateSwitch(info.car, info.sensor, true);
					break;
				case nju.ics.lixiaofan.consistency.context.Context.FN:
					if(Middleware.isDetectionEnabled()){
						info.sensor.nextSection.displayBalloon(info.type, info.sensor.getName(), info.car.name);
						if(Middleware.isResolutionEnabled())
							stateSwitch(info.car, info.sensor, true);
					}
					break;
				case nju.ics.lixiaofan.consistency.context.Context.FP:
					if(Middleware.isDetectionEnabled())
						info.sensor.nextSection.displayBalloon(info.type, info.sensor.getName(), info.car.name);
					if(!Middleware.isResolutionEnabled())
						stateSwitch(info.car, info.sensor, false);
					break;
				default:
					break;
				}
			}
		}
	};
	
	public BrickHandler() {
		checkedDataHandler.start();
	}
	
	@Override
	public void run() {
		while(true){
			while(rawData.isEmpty()){
				try {
					synchronized (rawData) {
						rawData.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			SensoryData data = null;
			synchronized (rawData) {
				data = rawData.poll();
			}
			if(data == null || data.bid >= DataProvider.getSensors().size()
				|| data.sid >= DataProvider.getSensors().get(data.bid).size())
				continue;
			SensorManager.trigger(DataProvider.getSensors().get(data.bid).get(data.sid), data.d);
			stateSwitch(data.bid, data.sid, data.d);
		}
	}
	
	private void stateSwitch(Car car, Sensor sensor, boolean isCtxTrue){
		switch(sensor.state){
		case Sensor.UNDETECTED:{
			sensor.state = Sensor.DETECTED;
			System.out.println("B"+sensor.bid+"S"+(sensor.sid+1)+" ENTERING!!!");
			
//				calibrateAngle(car, sensor[id]); 
			System.out.println("Entering Car: "+car.name);
			
			if(isCtxTrue && sensor.prevSensor.car == car && sensor.prevSensor.state == Sensor.DETECTED)
				sensor.prevSensor.state = Sensor.UNDETECTED;
			
			//TODO need supplements about handling phantoms
			if(isCtxTrue){
				if(car.realLoc != null){
					car.realLoc.realCars.remove(car.name);
					car.realLoc = null;
				}
			}
			else{
				car.realLoc = car.loc;
				car.loc.realCars.add(car.name);
			}
			
			Section prev = car.loc;
			//inform the traffic police of the entry event
			car.sendRequest(sensor.nextSection);
//			sensor.nextSection.removeWaitingCar(car);
			Dashboard.carEnter(car, sensor.nextSection);
			car.dir = sensor.nextSection.dir[1] == -1 ? sensor.nextSection.dir[0] : sensor.dir;
			if(car.status != Car.MOVING)
				car.status = Car.MOVING;
			car.lastDetectedTime = System.currentTimeMillis();
			sensor.car = car;
			sensor.isTriggered = true;
//				setCarDir(car, sensor);
			//trigger context
			if(ContextManager.hasListener())
				ContextManager.trigger(new Context(""+sensor.bid +(sensor.sid+1), car.name, car.getDir()));
			
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
			if(!queue.isEmpty()){
				synchronized (queue) {
					Command newCmd = null;
					boolean donesth = false;
					for(Iterator<Command> it = queue.iterator();it.hasNext();){
						Command cmd = it.next();
						if(cmd.car == car){
							donesth = true;
							it.remove();
							//stop command
							if(cmd.cmd == 0){
								cmd.level = 1;
								cmd.deadline = Remediation.getDeadline(cmd.car.type, 0, 1);
								Command.send(cmd, false);
								cmd.car.lastStopInstrTime = System.currentTimeMillis();
								newCmd = cmd;
							}
							break;
						}
					}
					Remediation.addCmd(newCmd);
					if(donesth){
						Dashboard.updateRemedyQ();
						Remediation.printQueue();
					}
				}
			}
			
			//do triggered stuff		
//				System.out.println(TrafficMap.nameOf(car.location)+"\t"+TrafficMap.nameOf(car.dest));
			if(car.dest != null){
				if(car.dest.sameAs(car.loc)){
					car.finalState = 0;
					Command.send(car, 0);
					car.sendRequest(0);
					Dashboard.appendLog(car.name+" reached dest");
					//trigger reach dest event
					if(EventManager.hasListener(Event.Type.CAR_REACH_DEST))
						EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
				}
				else if(car.finalState == 0){
					car.finalState = 1;
					car.sendRequest(1);
					Dashboard.appendLog(car.name+" failed to stop at dest, keep going");
				}
				else
					car.sendRequest(car.expectation == 1 ? 1 : 0);
			}
			else
				car.sendRequest(car.expectation == 1 ? 1 : 0);
		
			TrafficPolice.sendNotice(prev);
		}
		break;
		}
	}

	private void stateSwitch(int bid, int sid, int d){
		Sensor sensor = DataProvider.getSensors().get(bid).get(sid);
		switch(sensor.state){
//		case Sensor.INITIAL:
//			if(d > leavingValue[bid][sid])
//				sensor.state = 2;
//			else if(d < enteringValue[bid][sid])
//				sensor.state = 1;
//			break;
		case Sensor.DETECTED:
			if(d > leavingValue[bid][sid]){
				if(isFalsePositive2(sensor)){
					System.out.println("B"+bid+"S"+(sid+1)+" !!!FALSE POSITIVE!!!"
							+"\tread: " + d + "\tleavingValue: " + leavingValue[bid][sid]);
					break;
				}
				sensor.state = Sensor.UNDETECTED;
//				System.out.println(sdf.format(new Date()));
				System.out.println("B"+bid+"S"+(sid+1)+" LEAVING!!!" +
						"\tread: " + d + "\tleavingValue: " + leavingValue[bid][sid]);
			}
			break;
		case Sensor.UNDETECTED:
			if(d < enteringValue[bid][sid]){
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
				for(Car tmp : sensor.prevSection.cars){
					if(tmp.dir == sensor.dir){
						car = tmp;
						break;
					}
				}
				if(car == null){
					System.out.println("Can't find car!3");
					sensor.state = Sensor.UNDETECTED;
					break;
				}
				
				Middleware.add(car.name, car.dir, car.status, "movement", "enter",
						sensor.prevSection.name, sensor.nextSection.name, System.currentTimeMillis(), car, sensor);
			}
			break;
		}
	}

	public static void add(int bid, int sid, int d){
		SensoryData datum = new SensoryData(bid, sid, d);
		synchronized (rawData) {
			rawData.add(datum);
			rawData.notify();
		}
	}
	
	public static void add(Car car, Sensor sensor, int type){
		SensoryInfo info = new SensoryInfo(car, sensor, type);
		synchronized (checkedData) {
			checkedData.add(info);
			checkedData.notify();
		}
	}
	
//	private boolean isFalsePositive(Sensor sensor){
//		Section section = sensor.prevSection;
//		if(section.isOccupied()){
//			for(Car car : section.cars)
//				if(car.dir == sensor.dir && car.status != 0)
//					return false;
//				else
//					System.out.println("Sensor dir: "+sensor.dir+"\tCar dir: " + car.dir +"\tCar state: "+car.status);
//		}
//		return true;
//	}
	
	private boolean isFalsePositive2(Sensor sensor){
		Section section = sensor.nextSection;
		if(section.isOccupied()){
			for(Car car : section.cars)
				if(car.status != Car.STILL)
					return false;
		}
		return true;
	}
	
	public static void resetState(){
		for(List<Sensor> list : DataProvider.getSensors())
			for(Sensor sensor : list)
				sensor.state = Sensor.UNDETECTED;
	}
	
	public static void initValues(){
		for(int i = 0;i < enteringValue.length;i++)
			for(int j = 0;j < enteringValue[0].length;j++){
				enteringValue[i][j] = 11;
				leavingValue[i][j] = 10;
			}
	}
	
	private static class SensoryData{
		public int bid, sid, d;
		public SensoryData(int bid, int sid, int d) {
			this.bid = bid;
			this.sid = sid;
			this.d = d;
		}
	}
	
	private static class SensoryInfo{
		public Car car;
		public Sensor sensor;
		public int type;
		public SensoryInfo(Car car, Sensor sensor, int type) {
			this.car = car;
			this.sensor = sensor;
			this.type = type;
		}
	}
}
