package nju.ics.lixiaofan.resource;

import java.awt.Image;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.Section.Crossing;
import nju.ics.lixiaofan.city.Section.Street;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.control.Delivery.DeliveryTask;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.sensor.Sensor;

public class Resource {
	private static ExecutorService threadPool = Executors.newCachedThreadPool();
	private static Map<String, String> brickAddr = new HashMap<String, String>();
	private static ImageIcon greenBalloonIcon = null, redBalloonIcon = null;
	private static ImageIcon questionIcon = null, checkedIcon = null, xIcon = null;
	private static JSch jsch = new JSch();
	private static Map<String, Session> sessions = new HashMap<>();
	static{
		greenBalloonIcon = loadImage("res/green_balloon.png", Section.BalloonIcon.WIDTH, Section.BalloonIcon.HEIGHT);
		redBalloonIcon = loadImage("res/red_balloon.png", Section.BalloonIcon.WIDTH, Section.BalloonIcon.HEIGHT);
		questionIcon = loadImage("res/question_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		checkedIcon = loadImage("res/checked_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		xIcon = loadImage("res/x_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		try {
			//use ssh-keyscan $IP >> brick/known_hosts
			jsch.setKnownHosts("brick/known_hosts");
		} catch (JSchException e) {
			e.printStackTrace();
		} 
	}
	
	public static void execute(Runnable command){
		threadPool.execute(command);
	}
	
	public static JSch getJSch(){
		return jsch;
	}
	
	public static Session getSession(String name){
		Session session = sessions.get(name);
		if(session == null){
			String addr = brickAddr.get(name);
			if(addr != null){
				try {
					session = jsch.getSession("robot", addr);
					session.setPassword("maker");
					sessions.put(name, session);
				} catch (JSchException e) {
					e.printStackTrace();
				}
			}
		}
		return session;
	}
	
	public static Collection<Car> getCars(){
		return TrafficMap.cars.values();
	}
	
	public static Set<Car> getConnectedCars(){
		return TrafficMap.connectedCars;
	}
	
	public static List<List<Sensor>> getSensors(){
		return TrafficMap.sensors;
	}
	
	public static Crossing[] getCrossings(){
		return TrafficMap.crossings;
	}
	
	public static Street[] getStreets(){
		return TrafficMap.streets;
	}
	
	public static Map<String, Section> getSections(){
		return TrafficMap.sections;
	}
	
	public static Section getSection(String name){
		return TrafficMap.sections.get(name);
	}
	
	public static String getBrickAddr(String name){
		return brickAddr.get(name);
	}
	
	public static Set<String> getBricks(){
		return brickAddr.keySet();
	}
	
	public static void setBrickAddr(String brick, String addr){
		brickAddr.put(brick, addr);
	}
	
	public static Set<DeliveryTask> getDelivTasks(){
		Set<DeliveryTask> set = new HashSet<Delivery.DeliveryTask>();
		set.addAll(Delivery.searchTasks);
		set.addAll(Delivery.deliveryTasks);
		return set;
	}
	
	public static ImageIcon getRedBalloonImageIcon(){
		return redBalloonIcon;
	}
	
	public static ImageIcon getGreenBalloonImageIcon(){
		return greenBalloonIcon;
	}
	
	public static ImageIcon getQuestionMarkImageIcon(){
		return questionIcon;
	}
	
	public static ImageIcon getCheckedMarkImageIcon(){
		return checkedIcon;
	}
	
	public static ImageIcon getXMarkImageIcon(){
		return xIcon;
	}
	
	private static ImageIcon loadImage(String filename, int width, int height){
		ImageIcon imageIcon = new ImageIcon(filename);
		Image image = imageIcon.getImage();
		if(imageIcon.getIconWidth() > imageIcon.getIconHeight())
			image = image.getScaledInstance(width, -1, Image.SCALE_SMOOTH);
		else
			image = image.getScaledInstance(-1, height, Image.SCALE_SMOOTH);
		return new ImageIcon(image);
	}
}
