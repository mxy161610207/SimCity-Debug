package nju.xiaofanli;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import nju.xiaofanli.application.*;
import nju.xiaofanli.city.Citizen;
import nju.xiaofanli.device.SelfCheck;
import nju.xiaofanli.device.car.CmdSender;
import nju.xiaofanli.util.ConfigGenerator;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.city.Building;
import nju.xiaofanli.city.Section;
import nju.xiaofanli.city.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;
import nju.xiaofanli.application.monitor.AppServer;
import nju.xiaofanli.device.sensor.BrickServer;

public class Main {
	public static boolean initial = true;
	public static void main(String[] args) throws IOException {
		readConfigFile();
		Dashboard.getInstance().loadCheckUI();
		new SelfCheck();//blocked until all devices are ready
		Dashboard.getInstance().loadCtrlUI();
		
		addModule();
        new BrickServer();
		new CmdSender();
		new Middleware();
		new Police();
		new Delivery();
		new CitizenActivityGenerator();
        new AppServer();
		
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		StateSwitcher.suspend();
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		StateSwitcher.resume();
		
//		new Thread() {
//			boolean flip = true;
//			public void run() {
//				while(true){
//					for(Car car : Resource.getConnectedCars())
//						Command.send(car, flip ? Command.FORWARD : Command.STOP);
//				
//					flip = !flip;
//					try {
//						Thread.sleep(500);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}.start();
		initial = false;
	}
	
	private static void addModule(){
		EventManager.register(new AppMonitor(), Event.Type.ALL);
		EventManager.register(new VehicleConditionMonitor(), Arrays.asList(Event.Type.ADD_CAR, Event.Type.CAR_CRASH,
                Event.Type.CAR_ENTER, Event.Type.CAR_LEAVE, Event.Type.CAR_LEAVE, Event.Type.CAR_MOVE, Event.Type.CAR_STOP,
                Event.Type.CAR_START_LOADING, Event.Type.CAR_END_LOADING, Event.Type.CAR_START_UNLOADING, Event.Type.CAR_END_UNLOADING));
		EventManager.register(new CarLoadingMonitor(), Arrays.asList(Event.Type.CAR_START_LOADING, Event.Type.CAR_START_UNLOADING));
	}
	
	@SuppressWarnings("unchecked")
	private static void readConfigFile(){
		SAXReader reader = new SAXReader();
		Document doc;
		try {
			doc = reader.read(new File(ConfigGenerator.FILE));
		} catch (DocumentException e) {
			e.printStackTrace();
			return;
		}
		Element root = doc.getRootElement();
		List<Element> list = root.elements("car");
		for(Element elm : list){
			Car car = new Car(elm.attributeValue("name"), Section.sectionOf(elm.attributeValue("loc")), elm.attributeValue("url"));
			TrafficMap.cars.put(car.name, car);
		}
		
		list = root.elements("building");
		for(Element elm : list){
			Building b = new Building(elm.attributeValue("name"),
					Building.typeOf(elm.attributeValue("type")),
					Integer.parseInt(elm.attributeValue("loc")));
			TrafficMap.buildings.put(b.type, b);
		}
		
		list = root.elements("citizen");
		for(Element elm : list){
			Citizen citizen = new Citizen(elm.attributeValue("name"),
					Citizen.genderOf(elm.attributeValue("gender")),
					Citizen.jobOf(elm.attributeValue("job")));
			TrafficMap.citizens.add(citizen);
		}
		
		list = root.elements("brick");
		for(Element e : list){
			Resource.setBrickAddr(e.attributeValue("name"),
					e.attributeValue("address"));
		}
	}
}
