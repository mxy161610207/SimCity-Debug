import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.car.RCClient;
import nju.ics.lixiaofan.city.Building;
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
import nju.ics.lixiaofan.resource.Resource;
import nju.ics.lixiaofan.sensor.BrickServer;

public class Main {
	public static void main(String[] args) throws IOException {
		readConfigFile();
		new RCClient();
		Dashboard dashboard = new Dashboard();
		dashboard.loadCheckUI();
		checkDevices(dashboard);
		dashboard.loadCtrlUI();
		
		addModule();
		new Middleware();
		new Delivery();
		new Police();
		new CitizenControl();
		new BrickServer();
		new AppServer();
		
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
	}
	
	private static void addModule(){
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
	private static void readConfigFile(){
		SAXReader reader = new SAXReader();
		Document doc = null;
		try {
			doc = reader.read(new File(ConfigGenerator.configFile));
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Element root = doc.getRootElement();
		List<Element> list = root.elements("car");
		for(Element elm : list){
			Car car = new Car(elm.attributeValue("name"), Section.sectionOf(elm.attributeValue("loc")));
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
		
		list = root.elements("brick");
		for(Element e : list){
			Resource.setBrickAddr(e.attributeValue("name"),
					e.attributeValue("address"));
		}
	}
	
	private static void checkDevices(final Dashboard dashboard){
		//bricks(connectivity + sampling) + cars + rc
		final int TOTAL = 2 * Resource.getBricks().size() + Resource.getCars().size() + 1;
		if(TOTAL == 0)
			return;
		final int[] cnt = {0};
//		final boolean[] allReady = {true};
		final Object OBJ = new Object();
		
		class SelfCheckingTask implements Runnable{
			private String name;
			private String type;
			
			public SelfCheckingTask(String name, String type) {
				this.name = name;
				this.type = type;
			}
			
			public void run() {
				boolean connected = false;
				if(type.equals("brick conn")){
					String addr = Resource.getBrickAddr(name);
					if(addr != null){
						while(!connected){
							try {
								connected = InetAddress.getByName(addr).isReachable(5000);
							} catch (IOException e) {
								e.printStackTrace();
							}
							dashboard.setDevStateIcon(name + " conn", connected);
						}
						Resource.execute(new SelfCheckingTask(name, "brick sample"));
					}
				}
				else if(type.equals("brick sample")){
					String addr = Resource.getBrickAddr(name);
					if(addr != null){
						int second = 0, timeout = 5;//seconds
						Session session = null;
						Channel channel = null;
						try {
							session = Resource.getSession(name);
							session.connect();
							channel = session.openChannel("exec");
							((ChannelExec) channel).setCommand("./start.sh");
							channel.setInputStream(null);
							((ChannelExec) channel).setErrStream(System.err);
							InputStream in = channel.getInputStream();
							channel.connect();
							byte[] buf = new byte[1024];
							while (!connected) {
								while (in.available() > 0) {
									int i = in.read(buf);
									if (i < 0)
										break;
									String s = new String(buf, 0, i);//followed by CR
									System.out.print(s);
									if(s.startsWith(name)){
										connected = true;
										dashboard.setDevStateIcon(name + " sample", connected);
										channel.disconnect();
										break;
									}
								}
								if (channel.isClosed()) {
									if (in.available() > 0)
										continue;
									System.out.println("exit status: " + channel.getExitStatus());
									break;
								}
								if(++second % timeout == 0){
									connected = false;
									dashboard.setDevStateIcon(name + " sample", connected);
	//								break;
								}
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						} catch (JSchException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
						finally{
							if(channel != null)
								channel.disconnect();
							if(session != null)
								session.disconnect();
						}
					}
				}
				else if(type.equals("RC")){
					while(!connected){
						while(!RCClient.tried){
							synchronized (RCClient.TRIED_LOCK) {
								try {
									RCClient.TRIED_LOCK.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						RCClient.tried = false;
						connected = RCClient.isConnected();
						dashboard.setDevStateIcon(name, connected);
					}
					for(Car car :Resource.getCars())
						Resource.execute(new SelfCheckingTask(car.name, "car"));
				}
				else if(type.equals("car")){
					Car car = Car.carOf(name);
					if(car != null){
						while(!car.isConnected){
							Command.connect(car);
							while(!car.tried){
								synchronized (car.TRIED_LOCK) {
									try {
										car.TRIED_LOCK.wait();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}
							car.tried = false;
							dashboard.setDevStateIcon(name, car.isConnected);
						}
					}
				}
				
				synchronized (cnt) {
					cnt[0]++;
				}
				if(cnt[0] == TOTAL)
					synchronized (OBJ) {
						OBJ.notify();
					}
			}
		}
		
		Resource.execute(new SelfCheckingTask(RCClient.NAME, "RC"));
		for(String name : Resource.getBricks())
			Resource.execute(new SelfCheckingTask(name, "brick conn"));
		
		synchronized (OBJ) {
			try {
				OBJ.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
//		return allReady[0];
	}
}
