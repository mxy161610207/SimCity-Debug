package nju.ics.lixiaofan.module;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventListener;
import nju.ics.lixiaofan.monitor.AppPkg;
import nju.ics.lixiaofan.monitor.PkgHandler;

public class AppMonitor implements EventListener{
	public void eventTriggered(Event event) {
		switch (event.type) {
		case ADD_CAR: case CAR_ENTER:{
			Car car = Car.carOf(event.car);
			PkgHandler.send(new AppPkg().setCar(car.name, car.dir, event.location));
			break;
		}
		case DELIVERY_RELEASED:
			PkgHandler.send(new AppPkg().setDelivery(event.dtask.id, null, event.dtask.src.name, event.dtask.dest.name, event.dtask.phase));
			break;
		case DELIVERY_COMPLETED:
			PkgHandler.send(new AppPkg().setDelivery(event.dtask.id, event.dtask.car.name, event.dtask.src.name, event.dtask.dest.name, event.dtask.phase));
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
