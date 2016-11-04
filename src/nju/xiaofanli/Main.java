package nju.xiaofanli;
import nju.xiaofanli.application.*;
import nju.xiaofanli.application.monitor.AppServer;
import nju.xiaofanli.dashboard.Building;
import nju.xiaofanli.dashboard.Citizen;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.SelfCheck;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.CmdSender;
import nju.xiaofanli.device.sensor.BrickServer;
import nju.xiaofanli.device.sensor.RandomDataGenerator;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;
import nju.xiaofanli.util.ConfigGenerator;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {
	public static boolean initial = true;
	public static void main(String[] args) throws IOException {
		readConfigFile();
		Dashboard.getInstance().loadCheckUI();
		new SelfCheck();//blocked until all devices are ready

		addModule();
        new BrickServer();
		new CmdSender();
//		new Middleware();
		new Police();
		new Delivery();
		new AppServer();
        new RandomDataGenerator();
		Dashboard.getInstance().loadCtrlUI();
		initial = false;
        TrafficMap.checkRealCrash();
		Dashboard.showRelocationDialog(null);
	}

	private static void addModule(){
		EventManager.register(new AppMonitor(), Event.Type.ALL);
		EventManager.register(new VehicleConditionMonitor(), Arrays.asList(Event.Type.ADD_CAR, Event.Type.CAR_CRASH,
                Event.Type.CAR_ENTER, Event.Type.CAR_LEAVE, Event.Type.CAR_LEAVE, Event.Type.CAR_MOVE, Event.Type.CAR_STOP,
                Event.Type.CAR_START_LOADING, Event.Type.CAR_END_LOADING, Event.Type.CAR_START_UNLOADING, Event.Type.CAR_END_UNLOADING));
//		EventManager.register(new CarLoadingMonitor(), Arrays.asList(Event.Type.CAR_START_LOADING, Event.Type.CAR_END_LOADING,
//				Event.Type.CAR_START_UNLOADING, Event.Type.CAR_END_UNLOADING));
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
			Car car = new Car(elm.attributeValue("name"), Road.roadOf(elm.attributeValue("loc")),
                    elm.attributeValue("url"), elm.attributeValue("icon"));
			TrafficMap.cars.put(car.name, car);
            TrafficMap.carList.add(car);
		}
		
		list = root.elements("building");
		for(Element elm : list){
			Building b = new Building(elm.attributeValue("name"), Building.typeOf(elm.attributeValue("type")),
					Integer.parseInt(elm.attributeValue("loc")), elm.attributeValue("icon"));
			TrafficMap.buildings.put(b.type, b);
		}
		
		list = root.elements("citizen");
		for(Element elm : list){
			Citizen citizen = new Citizen(elm.attributeValue("name"), Citizen.genderOf(elm.attributeValue("gender")),
					Citizen.jobOf(elm.attributeValue("job")), elm.attributeValue("icon"), Integer.parseInt(elm.attributeValue("color"), 16));
			TrafficMap.citizens.add(citizen);
            TrafficMap.freeCitizens.add(citizen);
		}

		list = root.elements("brick");
		for(Element e : list){
			Resource.setBrickAddr(e.attributeValue("name"), e.attributeValue("address"));
		}

        readTimeout("timeout.txt");
	}

	private static void readTimeout(String file) {
        BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
            return;
		}

		String line;
        try {
            while((line = br.readLine()) != null) {
                String[] strs = line.split("\t");
                String car = strs[0], sensor = strs[1];
                int deadline = Integer.parseInt(strs[2]);
                if (!Resource.timeouts.containsKey(sensor))
                    Resource.timeouts.put(sensor, new HashMap<>());
                Resource.timeouts.get(sensor).put(car, deadline);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
