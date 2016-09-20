package nju.xiaofanli.application;

import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventListener;

public class VehicleConditionMonitor implements EventListener {

	public void eventTriggered(Event event) {
		switch (event.type) {
		case ADD_CAR:
//		case CAR_CRASH:
		case CAR_ENTER:
		case CAR_MOVE:
		case CAR_LEAVE:
		case CAR_STOP:
		case CAR_START_LOADING:
		case CAR_END_LOADING:
		case CAR_START_UNLOADING:
		case CAR_END_UNLOADING:
//			System.out.println(event.type.toString() + "\t" + event.car);
			Dashboard.updateVehicleConditionPanel(Car.carOf(event.car));
			break;
		default:
			break;
		}
	}

}
