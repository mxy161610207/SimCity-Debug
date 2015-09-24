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
import nju.ics.lixiaofan.city.Section.Crossing;
import nju.ics.lixiaofan.city.Section.Street;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.context.Context;
import nju.ics.lixiaofan.context.ContextManager;
import nju.ics.lixiaofan.control.TrafficPolice;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.data.DataProvider;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;

public class BrickHandler extends Thread{
//	private int bid;
//	private DataInputStream in = null;
//	private DataOutputStream out = null;
//	private Hashtable<Integer, BrickHandlerInfo> bhi = null;
//	private int[] states = new int[4];
	private static List<Command> queue = Remediation.queue;
//	public static int id2print = 5;
//	public static Set<Integer> dis2print = new HashSet<Integer>();
//	private double[] enteringValue, leavingValue;
	private static int enteringValue[][] = new int[10][4];
	private static int leavingValue[][] = new int[10][4];
//	private Sensor[] sensor = null;
//	private int sensorNum;
//	private static SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
//	private static List<Car> carList = new ArrayList<Car>();
	private static Queue<SensoryData> sdata = new LinkedList<SensoryData>();
	
//	public BrickHandler(int bid, DataInputStream in, DataOutputStream out, Hashtable<Integer, BrickHandlerInfo> bhi) {
//		this.bid = bid;
//		this.in = in;
//		this.out = out;
//		this.bhi = bhi;
////		enteringValue = BrickServer.getComingValue()[bid];
////		leavingValue = BrickServer.getLeavingValue()[bid];
//		sensorNum = DataProvider.getSensors().get(bid).size();
//		sensor = new Sensor[sensorNum];
//		for(int i = 0;i < sensorNum;i++)
//			sensor[i] = DataProvider.getSensors().get(bid).get(i);
//	}
	
	public BrickHandler() {
	}

	@Override
	public void run() {
		while(true){
			while(sdata.isEmpty()){
				try {
					synchronized (sdata) {
						sdata.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			SensoryData data = null;
			synchronized (sdata) {
				data = sdata.poll();
			}
			if(data == null)
				continue;
//			if(data.sid == 3)
//				System.out.println(bid+" "+(data.sid+1)+" "+data.d);
			stateSwitch(data.bid, data.sid, data.d);
		}
	}
	
	public void stateSwitch(int bid, int sid, int d){
		if(bid >= DataProvider.getSensors().size() || sid >= DataProvider.getSensors().get(bid).size()){
//			System.out.println("IndexOutOfBounds");
			return;
		}
		Sensor sensor = DataProvider.getSensors().get(bid).get(sid);
		switch(sensor.state){
//		//initial
//		case 0:
//			if(d > leavingValue[bid][sid])
//				sensor.state = 2;
//			else if(d < enteringValue[bid][sid])
//				sensor.state = 1;
//			break;
		//entered
		case 1:
			if(d > leavingValue[bid][sid]){
				if(isFalsePositive2(sensor)){
					System.out.println("B"+bid+"S"+(sid+1)+" !!!FALSE POSITIVE!!!"
							+"\tread: " + d + "\tleavingValue: " + leavingValue[bid][sid]);
					break;
				}
				sensor.state = 2;
//				System.out.println(sdf.format(new Date()));
				System.out.println("B"+bid+"S"+(sid+1)+" LEAVING!!!" +
						"\tread: " + d + "\tleavingValue: " + leavingValue[bid][sid]);
			}
			break;
		//left
		case 2:
			if(d < enteringValue[bid][sid]){
				if(isFalsePositive(sensor)){
					System.out.println("B"+bid+"S"+(sid+1)+" !!!FALSE POSITIVE!!!"
							+"\tread: " + d + "\tenteringValue: " + enteringValue[bid][sid]);
//					}
					//trigger context manager
					if(ContextManager.hasListener())
						ContextManager.trigger(new Context(""+bid+(sid+1), null, null));
					break;
				}
				sensor.state = 1;
				System.out.println("B"+bid+"S"+(sid+1)+" ENTERING!!!"
					+"\tread: " + d + "\tenteringValue: " + enteringValue[bid][sid]);
				
				Car car = null;
				Section prev = getLocBefore(sensor);
				if(prev.cars.size() == 1 && prev.cars.peek().dir == sensor.dir)
					car = prev.cars.peek();
				else
					for(Car tmp : prev.cars){
						if(tmp.dir == sensor.dir){
							car = tmp;
							break;
						}
					}
				if(car == null){
					System.out.println("Can't find car!3");
					sensor.state = 2;
					break;
				}
//				calibrateAngle(car, sensor[id]); 
				System.out.println("Entering Car: "+car.name);
//				//trigger leaving event
//				if(EventManager.hasListener(Event.CAR_LEAVE))
//					EventManager.trigger(new Event(Event.CAR_LEAVE, car.name, car.loc.name));
				
				if(sensor.prevSensor.state == 1 && sensor.prevSensor.car == car)
					sensor.prevSensor.state = 2;
				
				if(prev instanceof Crossing){
					sensor.street.removeWaitingCar(car);		
					Dashboard.carEnter(car, sensor.street);
				}
				else{
					sensor.crossing.removeWaitingCar(car);
					Dashboard.carEnter(car, sensor.crossing);
				}
				if(car.state != 1){
					car.state = 1;
//					//trigger move event
//					if(EventManager.hasListener(Event.CAR_MOVE))
//						EventManager.trigger(new Event(Event.CAR_MOVE, car.name, car.loc.name));
//					Dashboard.mapRepaint();
				}
				car.lastDetectedTime = System.currentTimeMillis();
				sensor.car = car;
				sensor.isTriggered = true;
				setCarDir(car, sensor);
				//trigger context
				if(ContextManager.hasListener())
					ContextManager.trigger(new Context(""+bid +(sid+1), car.name, car.getDir()));
				
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
					if(car.dest == car.loc || (car.dest.isCombined && car.dest.combined.contains(car.loc))){
						car.finalState = 0;
						Command.send(car, 0);
						car.sendRequest(0);
						Dashboard.appendLog(car.name+" reached dest");
						//trigger reach dest event
						if(EventManager.hasListener(Event.Type.CAR_REACH_DEST))
							EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
						//trigger start loading event
						if(car.dt != null && car.dt.phase == 1 && EventManager.hasListener(Event.Type.CAR_START_LOADING))
							EventManager.trigger(new Event(Event.Type.CAR_START_LOADING, car.name, car.loc.name));
						//trigger start unloading event
						else if(car.dt != null && car.dt.phase == 2 && EventManager.hasListener(Event.Type.CAR_START_UNLOADING))
							EventManager.trigger(new Event(Event.Type.CAR_START_UNLOADING, car.name, car.loc.name));
					}
					else if(car.finalState == 0){
						car.finalState = 1;
						car.sendRequest(1);
						Dashboard.appendLog(car.name+" failed to stop at dst, keep going");
					}
					else if(car.expectation == 1)
						car.sendRequest(1);
					else
						car.sendRequest(0);
				}
				else if(car.expectation == 1)
					car.sendRequest(1);
				else
					car.sendRequest(0);
			
				TrafficPolice.sendNotice(prev);
//				synchronized (sensor.crossing.handler.wakemeup) {
//					sensor.crossing.handler.wakemeup.notify();
//				}
			}
			break;
		}
		SensorManager.trigger(sensor, d);
	}

	public static void add(int bid, int sid, int d){
		SensoryData data = new SensoryData(bid, sid, d);
		synchronized (sdata) {
			sdata.add(data);
			sdata.notify();
		}
	}
	
	private boolean isFalsePositive(Sensor sloc){
		Section section = getLocBefore(sloc);
		if(section.isOccupied && !section.cars.isEmpty()){
			for(Car car : section.cars)
				if(car.dir == sloc.dir && car.state != 0)
					return false;
				else
					System.out.println("Sensor dir: "+sloc.dir+"\tCar dir: " + car.dir +"\tCar state: "+car.state);
		}
		return true;
	}
	
	private boolean isFalsePositive2(Sensor sloc){
		Section section = getLocAfter(sloc);
		if(section.isOccupied && !section.cars.isEmpty()){
			for(Car car : section.cars)
				if(car.state != 0)
					return false;
		}
		return true;
	}
	
	private void setCarDir(Car car, Sensor sloc){
		Crossing crossing = sloc.crossing;
		if((crossing == TrafficMap.crossings[2] || crossing == TrafficMap.crossings[6]) && sloc.isEntrance){
			car.dir = (byte) ((car.dir + 2) % 4);
			return;
		}
		Street street = sloc.street;
		if(TrafficMap.dir){
			if(crossing == TrafficMap.crossings[0]){
				if(street == TrafficMap.streets[2] || street == TrafficMap.streets[6]){
					car.dir += 1;//N->S or W->E
					return;
				}
			}
			else if(crossing == TrafficMap.crossings[5]){
				if(street == TrafficMap.streets[17]){
					car.dir -= 1;//E->W
					return;
				}
			}
			else if(crossing == TrafficMap.crossings[7]){
				if(street == TrafficMap.streets[28]){
					car.dir -= 1;//S->N
					return;
				}
			}
		}
		else{
			if(crossing == TrafficMap.crossings[1]){
				if(street == TrafficMap.streets[3]){
					car.dir += 1;//N->S
					return;
				}
			}
			else if(crossing == TrafficMap.crossings[3]){
				if(street == TrafficMap.streets[14]){
					car.dir += 1;//W->E
					return;
				}		
			}
			else if(crossing == TrafficMap.crossings[8]){
				if(street == TrafficMap.streets[25] || street == TrafficMap.streets[29]){
					car.dir -= 1;//E->W or S->N
					return;
				}
			}
		}
	}
	
	public int getCarDir(int bid, int id, Car car){
		if(TrafficMap.dir){
			if(bid == 0){
				if(id == 0)
					return 1;
				else if(id == 1)
					return 3;
			}
			else if(bid == 4){
				if(id == 1 || id == 2)
					return 2;
			}
			else if(bid == 8){
				if(id == 0 || id == 1)
					return 0;
			}
		}
		else{
			if(bid == 1){
				if(id == 0 || id == 1)
					return 1;
			}
			else if(bid == 9){
				if(id == 0)
					return 0;
				else if(id == 1)
					return 2;
			}
			else if(bid == 5){
				if(id == 1 || id == 2)
					return 3;
			}
		}
		return car.dir;
	}
	
	public Section getLocBefore(Sensor sloc){
		Section section = getLocAfter(sloc);
		if(section == null)
			return null;
		return (section == sloc.street) ? sloc.crossing : sloc.street;
	}
	
	public static Section getLocAfter(Sensor sloc){
		int bid = sloc.bid, id = sloc.sid;
		
		switch(bid){
		case 0:
			return TrafficMap.dir ? sloc.street : sloc.crossing;
		case 1:
			return ((id == 0) ^ TrafficMap.dir) ? sloc.crossing : sloc.street;
		case 2:
		case 4:
			return ((id < 2) ^ TrafficMap.dir) ? sloc.crossing : sloc.street;
		case 3:
		case 7:
			return ((id % 2 == 1) ^ TrafficMap.dir) ? sloc.crossing : sloc.street;
		case 5:
			return ((id > 1) ^ TrafficMap.dir) ? sloc.crossing : sloc.street;
		case 6:
			return ((id == 0 || id == 3) ^ TrafficMap.dir) ? sloc.crossing : sloc.street;
		case 8:
			return ((id > 0) ^ TrafficMap.dir) ? sloc.crossing : sloc.street;
		case 9:
			return TrafficMap.dir ? sloc.crossing : sloc.street;
		}
		return null;
	}
	
	public static void resetState(){
		for(List<Sensor> list : DataProvider.getSensors())
			for(Sensor sensor : list)
				sensor.state = 2;
	}
	
	public static void initValues(){
		for(int i = 0;i < enteringValue.length;i++)
			for(int j = 0;j < enteringValue[0].length;j++){
				enteringValue[i][j] = 11;
				leavingValue[i][j] = 10;
			}
//		comingValue[0][0] = 9;//8
//		leavingValue[0][0] = 9;//8
//		comingValue[0][1] = 8;
//		leavingValue[0][1] = 8;
//		
//		comingValue[1][0] = 8;
//		leavingValue[1][0] = 8;
//		comingValue[1][1] = 6;
//		leavingValue[1][1] = 5;
//		comingValue[1][2] = 7;
//		leavingValue[1][2] = 7;
//		
//		comingValue[2][0] = 6;
//		leavingValue[2][0] = 5;
//		comingValue[2][1] = 6;
//		leavingValue[2][1] = 5;
//		comingValue[2][2] = 6;
//		leavingValue[2][2] = 6;
//		comingValue[2][3] = 6;
//		leavingValue[2][3] = 6;
//		
//		comingValue[3][0] = 6;
//		leavingValue[3][0] = 7;
//		comingValue[3][1] = 6;
//		leavingValue[3][1] = 7;
//		comingValue[3][2] = 10;
//		leavingValue[3][2] = 10;
//		comingValue[3][3] = 8;
//		leavingValue[3][3] = 8;
//		
//		comingValue[4][0] = 8;
//		leavingValue[4][0] = 8;
//		comingValue[4][1] = 8;
//		leavingValue[4][1] = 8;
//		comingValue[4][2] = 8;
//		leavingValue[4][2] = 8;
//		comingValue[4][3] = 8;
//		leavingValue[4][3] = 8;
//		
//		comingValue[5][0] = 6;
//		leavingValue[5][0] = 6;
//		comingValue[5][1] = 7;
//		leavingValue[5][1] = 6;
//		comingValue[5][2] = 8;
//		leavingValue[5][2] = 8;
//		
//		comingValue[6][0] = 6;
//		leavingValue[6][0] = 6;
//		comingValue[6][1] = 6;
//		leavingValue[6][1] = 6;
//		comingValue[6][2] = 8;
//		leavingValue[6][2] = 8;
//		comingValue[6][3] = 8;//6
//		leavingValue[6][3] = 8;
//		
//		comingValue[7][0] = 6;
//		leavingValue[7][0] = 6;
//		comingValue[7][1] = 9;
//		leavingValue[7][1] = 9;
//		comingValue[7][2] = 6;
//		leavingValue[7][2] = 6;
//		comingValue[7][3] = 6;
//		leavingValue[7][3] = 6;
//		
//		comingValue[8][0] = 8;
//		leavingValue[8][0] = 8;
//		comingValue[8][1] = 6;
//		leavingValue[8][1] = 7;//6
//		comingValue[8][2] = 7;//6
//		leavingValue[8][2] = 8;
//		
//		comingValue[9][0] = 10;
//		leavingValue[9][0] = 10;
//		comingValue[9][1] = 7;
//		leavingValue[9][1] = 8;
	}
	
	/*
	private void calibrateAngle(Car car, Sensor sensor){
		if(car.type != 3)
			return;
		int key = sensor.cid*10 + sensor.sid + 1;
		System.out.println(key);
		if(TrafficMap.dir){
			switch (key) {
			case 1:case 11:case 32:case 42:case 74:
				CmdSender.send(car, 5);
				break;
			case 2:case 53:case 61:case 72:case 82:
				CmdSender.send(car, 5);
				break;
			case 24:case 52:case 62:case 81:case 91:
				CmdSender.send(car, 3);
				break;
			case 12:case 31:case 43:case 23:case 92:
				CmdSender.send(car, 4);
				break;
			}
		}
		else{
			switch (key) {
			case 1:case 11:case 32:case 42:case 74:
				CmdSender.send(car, 3);
				break;
			case 2:case 53:case 61:case 72:case 82:
				CmdSender.send(car, 4);
				break;
			case 24:case 52:case 62:case 81:case 91:
				CmdSender.send(car, 5);
				break;
			case 12:case 31:case 43:case 23:case 92:
				CmdSender.send(car, 5);
				break;
			}
		}
	}
	*/
	public static class SensoryData{
		public int bid, sid, d;
		public SensoryData(int bid, int sid, int d) {
			this.bid = bid;
			this.sid = sid;
			this.d = d;
		}
	}
}
