package nju.ics.lixiaofan.event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import nju.ics.lixiaofan.control.Delivery.DeliveryTask;

public class Event implements Cloneable{
	public String time = null;
	public Type type = null;
	public String car = null;
	public String location = null;
	public Set<String> crashedCars = null;// only for CAR_CRASH type
	public DeliveryTask dtask = null;//only for DELIVERY type
	public int cmd = -1;//only for REQUEST and RESPONSE type
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
	
//	public static final int types = 14;
//	public static final int ALL = 0;
//
//	public static final int CAR_ENTER = 1;
//	public static final int CAR_LEAVE = 2;
//	public static final int CAR_CRASH = 3;
//	public static final int CAR_MOVE = 4;
//	public static final int CAR_STOP = 5;
//	public static final int ADD_CAR = 15;
//	public static final int REMOVE_CAR = 16;
//
//	public static final int DELIVERY_RELEASED = 6;
//	public static final int DELIVERY_COMPLETED = 7;
//	public static final int CAR_START_LOADING = 8;
//	public static final int CAR_END_LOADING = 9;
//	public static final int CAR_START_UNLOADING = 10;
//	public static final int CAR_END_UNLOADING = 11;
//	public static final int CAR_SEND_REQUEST = 12;
//	public static final int CAR_RECV_RESPONSE = 13;
//	public static final int CAR_REACH_DEST = 14;
	
	public static enum Type{
		ALL,
		//physical events
		CAR_ENTER, CAR_LEAVE, CAR_CRASH, CAR_MOVE, CAR_STOP, ADD_CAR, REMOVE_CAR,
		//logical events
		DELIVERY_RELEASED, DELIVERY_COMPLETED, CAR_START_LOADING, CAR_END_LOADING,
		CAR_START_UNLOADING, CAR_END_UNLOADING, CAR_SEND_REQUEST, CAR_RECV_RESPONSE,
		CAR_REACH_DEST
	}
	
	public Event(Type type, String car, String location) {
		this.time = sdf.format(new Date());
		this.type = type;
		this.car = car;
		this.location = location;
	}

	public Event(Type type, Set<String> crashedCars, String location) {
		this.time = sdf.format(new Date());
		this.type = type;
		this.crashedCars = crashedCars;
		this.location = location;
	}
	
	public Event(Type type, DeliveryTask dtask) {
		this.time = sdf.format(new Date());
		this.type = type;
		this.dtask = dtask;
	}

	public Event(Type type, String car, String location, int cmd) {
		this.time = sdf.format(new Date());
		this.type = type;
		this.car = car;
		this.location = location;
		this.cmd = cmd;
	}

	@Override
	protected Event clone() throws CloneNotSupportedException {
		Event event = (Event) super.clone();
		if(event.crashedCars != null){
			Set<String> crashedCars = new HashSet<String>();
			crashedCars.addAll(event.crashedCars);
			event.crashedCars = crashedCars;
		}
		return event;
	}
}
