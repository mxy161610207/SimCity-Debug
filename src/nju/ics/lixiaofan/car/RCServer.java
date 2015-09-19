package nju.ics.lixiaofan.car;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import nju.ics.lixiaofan.city.Section;


public class RCServer{
	private static ServerSocket server = null;
	public static Hashtable<String, Car> cars = new Hashtable<String, Car>();
	public static CarRC rc = null;
	private static Runnable listener = new Runnable() {
		public void run() {
			try {
				server = new ServerSocket(8888);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			while(true){
				try {
					Socket socket = server.accept();
					rc = new CarRC(0, 0, socket, new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
					Thread t = new Thread(new RCListener(socket));
					t.setDaemon(true);
					t.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	};
	
	public RCServer() {
		readConfig();
		new Thread(listener).start();
		new Thread(new CmdSender()).start();
		new Thread(new Remediation()).start();
	}
	
	private static void readConfig(){
		SAXReader reader = new SAXReader();
		Document doc = null;
		try {
			doc = reader.read(new File(ConfigGenerator.filename));
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		Element root = doc.getRootElement();
		@SuppressWarnings("unchecked")
		List<Element> list = root.elements("car");
		for(Element elm : list){
			Car car = new Car(3, elm.attributeValue("name"));
			car.loc = Section.sectionOf(elm.attributeValue("loc"));
			if(car.loc == null)
				System.out.println("null");
			cars.put(car.name, car);
		}
	}
}
