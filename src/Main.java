import java.io.File;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.RCServer;
import nju.ics.lixiaofan.city.Building;
import nju.ics.lixiaofan.city.Citizen;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.consistency.middleware.Middleware;
import nju.ics.lixiaofan.control.CitizenControl;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.control.Police;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;
import nju.ics.lixiaofan.module.AppMonitor;
import nju.ics.lixiaofan.module.VehicleConditionMonitor;
import nju.ics.lixiaofan.monitor.AppServer;
import nju.ics.lixiaofan.sensor.BrickServer;

public class Main {
	public static void main(String[] args) {
		readConfig();
		addModule();
		new Dashboard();
		new Middleware();
		new RCServer();
		new Delivery();
		new Police();
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
	
	@SuppressWarnings("unchecked")
	private static void readConfig(){
		SAXReader reader = new SAXReader();
		Document doc = null;
		try {
			doc = reader.read(new File(ConfigGenerator.filename));
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Element root = doc.getRootElement();
		List<Element> list = root.elements("car");
		for(Element elm : list){
			Car car = new Car(3, elm.attributeValue("name"), Section.sectionOf(elm.attributeValue("loc")));
			TrafficMap.cars.put(car.name, car);
		}
		
		list = root.elements("building");
		for(Element elm : list){
			Building b = new Building(elm.attributeValue("name"),
					Building.typeOf(elm.attributeValue("type")),
					Integer.parseInt(elm.attributeValue("loc")));
			TrafficMap.buildings.put(b.type, b);
		}
		
//		list = root.elements("citizen");
//		for(Element elm : list){
//			Citizen citizen = new Citizen(elm.attributeValue("name"),
//					Citizen.genderOf(elm.attributeValue("gender")),
//					Citizen.jobOf(elm.attributeValue("job")));
//			TrafficMap.citizens.add(citizen);
//		}
	}
}
