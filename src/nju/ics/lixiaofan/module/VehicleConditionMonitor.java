package nju.ics.lixiaofan.module;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventListener;

public class VehicleConditionMonitor implements EventListener{

	public void eventTriggered(Event event) {
		switch (event.type) {
		case ADD_CAR:
		case CAR_CRASH:
		case CAR_ENTER:
		case CAR_MOVE:
		case CAR_LEAVE:
		case CAR_STOP:
		case CAR_START_LOADING:
		case CAR_END_LOADING:
		case CAR_START_UNLOADING:
		case CAR_END_UNLOADING:
			Dashboard.updateVehicleCondition(Car.carOf(event.car));
			break;
		default:
			break;
		}
	}

}
