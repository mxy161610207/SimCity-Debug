package nju.xiaofanli.event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import nju.xiaofanli.application.Delivery;

public class Event {
	public String time = null;
	public Type type = null;
	public String car = null;
	public String location = null;
	public Set<String> crashedCars = null;// only for CAR_CRASH type
	public Delivery.DeliveryTask dt = null;//only for DELIVERY type
	public int cmd = -1;//only for REQUEST and RESPONSE type
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
	
	public enum Type{
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

	public Event(Type type, String car, String location, Delivery.DeliveryTask dt) {
		this.time = sdf.format(new Date());
		this.type = type;
		this.car = car;
		this.location = location;
		this.dt = dt;
	}

	public Event(Type type, Set<String> crashedCars, String location) {
		this.time = sdf.format(new Date());
		this.type = type;
		this.crashedCars = crashedCars;
		this.location = location;
	}
	
	public Event(Type type, Delivery.DeliveryTask dt) {
		this.time = sdf.format(new Date());
		this.type = type;
		this.dt = dt;
	}

	public Event(Type type, String car, String location, int cmd) {
		this.time = sdf.format(new Date());
		this.type = type;
		this.car = car;
		this.location = location;
		this.cmd = cmd;
	}
}
