package nju.xiaofanli;
import nju.xiaofanli.application.AppMonitor;
import nju.xiaofanli.application.CarPusher;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.application.VehicleConditionMonitor;
import nju.xiaofanli.application.monitor.AppServer;
import nju.xiaofanli.dashboard.*;
import nju.xiaofanli.device.SelfCheck;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.CmdSender;
import nju.xiaofanli.device.sensor.BrickServer;
import nju.xiaofanli.device.sensor.RandomDataGenerator;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;
import nju.xiaofanli.schedule.Police;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {
	public static void main(String[] args) {
		StateSwitcher.init();
		readConfigFile();
		Dashboard.loadSelectionUI(); //blocked until clicking the done button
		Dashboard.loadCheckUI();
		new SelfCheck();//blocked until all devices are ready
		addModule();
        new BrickServer();
		new CmdSender();
		new Police();
		new Delivery();
		new CarPusher();
		new AppServer();
        new RandomDataGenerator();
        StateSwitcher.startRelocationThread();
		Dashboard.loadCtrlUI();
		StateSwitcher.finishInit();
		Dashboard.showInitDialog();
        TrafficMap.checkCrash(); // should never trigger crash, otherwise change cars' initial locations
	}

	private static boolean addModule = false;
	private static void addModule(){
		if (addModule)
			return;
		EventManager.register(new AppMonitor(), Event.Type.ALL);
		EventManager.register(new VehicleConditionMonitor(), Arrays.asList(Event.Type.ADD_CAR, Event.Type.CAR_CRASH,
                Event.Type.CAR_ENTER, Event.Type.CAR_LEAVE, Event.Type.CAR_LEAVE, Event.Type.CAR_MOVE, Event.Type.CAR_STOP,
                Event.Type.CAR_START_LOADING, Event.Type.CAR_END_LOADING, Event.Type.CAR_START_UNLOADING, Event.Type.CAR_END_UNLOADING));
		addModule = true;
	}

	private static boolean readConfigFile = false;
	public static final String configFile = "runtime/config.xml";
	@SuppressWarnings("unchecked")
	private static void readConfigFile(){
		if (readConfigFile)
			return;
		SAXReader reader = new SAXReader();
		Document doc;
		try {
			doc = reader.read(new File(configFile));
		} catch (DocumentException e) {
			e.printStackTrace();
			return;
		}
		Element root = doc.getRootElement();
		List<Element> list = root.elements("car");
		for(Element elm : list){
			Car car = new Car(elm.attributeValue("name"), new Color(Integer.parseInt(elm.attributeValue("color"), 16)),
					Road.roadOf(elm.attributeValue("loc")), elm.attributeValue("url"),
					elm.attributeValue("icon"), elm.attributeValue("fakeIcon"), elm.attributeValue("realIcon"));
			TrafficMap.allCars.add(car);
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
					Citizen.jobOf(elm.attributeValue("job")), elm.attributeValue("icon"), new Color(Integer.parseInt(elm.attributeValue("color"), 16)));
			TrafficMap.citizens.add(citizen);
            TrafficMap.addAFreeCitizen(citizen);
		}
		Resource.setCitizenIcons();

		list = root.elements("brick");
		for(Element e : list){
			String nm = e.attributeValue("name");
			Resource.setBrickAddr(nm, e.attributeValue("address"));
			Resource.setSensorNum(nm, Integer.parseInt(e.attributeValue("sensors")));
		}

        readTimeout();
		readConfigFile = true;
	}

	private static void readTimeout() {
        BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("runtime/timeout"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (br == null)
			return;

		String line;
        try {
            while((line = br.readLine()) != null) {
            	// mxy_edit: allow annotation line start with "#"
            	if (line.startsWith("#")){
            		continue;
				}
				//	System.out.println(line);
				// == EDIT END ==

                String[] strs = line.split("\t");
                String car = strs[0], sensor = strs[1];
                int deadline = Integer.parseInt(strs[2]);
                if (!Resource.timeouts.containsKey(sensor))
                    Resource.timeouts.put(sensor, new HashMap<>());
                Resource.timeouts.get(sensor).put(car, (int) (deadline * 1.5)); //loosen the timeout restriction
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
