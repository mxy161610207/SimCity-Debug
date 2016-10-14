package nju.xiaofanli.application;

import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventListener;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;

public class AppMonitor implements EventListener {
	public void eventTriggered(Event event) {
		switch (event.type) {
		case ADD_CAR: case CAR_ENTER:{
			Car car = Car.carOf(event.car);
			PkgHandler.send(new AppPkg().setCar(car.name, car.dir, event.location));
			break;
		}
		case DELIVERY_RELEASED:
			PkgHandler.send(new AppPkg().setDelivery(event.dt.id, null, event.dt.src.name, event.dt.dest.name, event.dt.phase));
			break;
		case DELIVERY_COMPLETED:
			PkgHandler.send(new AppPkg().setDelivery(event.dt.id, event.dt.car.name, event.dt.src.name, event.dt.dest.name, event.dt.phase));
			break;
		case CAR_START_LOADING:
			PkgHandler.send(new AppPkg().setLoading(event.car, true));
			break;
		case CAR_END_LOADING:
			PkgHandler.send(new AppPkg().setLoading(event.car, false));
			break;
		case CAR_START_UNLOADING:
			PkgHandler.send(new AppPkg().setUnloading(event.car, true));
			break;
		case CAR_END_UNLOADING:
			PkgHandler.send(new AppPkg().setUnloading(event.car, false));
			break;
		default:
			break;
		}
	}
}
