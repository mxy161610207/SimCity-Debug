import nju.ics.lixiaofan.car.RCServer;
import nju.ics.lixiaofan.control.CitizenControl;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.control.TrafficPolice;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;
import nju.ics.lixiaofan.module.AppMonitor;
import nju.ics.lixiaofan.module.VehicleConditionMonitor;
import nju.ics.lixiaofan.monitor.AppServer;
import nju.ics.lixiaofan.sensor.BrickServer;

public class Main {
	public static void main(String[] args) {
		addModule();
		new Dashboard();
		new RCServer();
		new Delivery();
		new TrafficPolice();
		new CitizenControl();
		new BrickServer();
		new AppServer();
	}
	
	public static void addModule(){
		EventManager.register(new AppMonitor(), Event.Type.ALL);
		VehicleConditionMonitor vcm = new VehicleConditionMonitor();
		EventManager.register(vcm, Event.Type.ADD_CAR);
		EventManager.register(vcm, Event.Type.CAR_CRASH);
		EventManager.register(vcm, Event.Type.CAR_ENTER);
		EventManager.register(vcm, Event.Type.CAR_LEAVE);
		EventManager.register(vcm, Event.Type.CAR_MOVE);
		EventManager.register(vcm, Event.Type.CAR_STOP);
		EventManager.register(vcm, Event.Type.CAR_START_LOADING);
		EventManager.register(vcm, Event.Type.CAR_END_LOADING);
		EventManager.register(vcm, Event.Type.CAR_START_UNLOADING);
		EventManager.register(vcm, Event.Type.CAR_END_UNLOADING);
		EventManager.register(CitizenControl.carMonitor, Event.Type.CAR_START_LOADING);
		EventManager.register(CitizenControl.carMonitor, Event.Type.CAR_START_UNLOADING);
	}
}
