package nju.xiaofanli;

import java.awt.Image;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import nju.xiaofanli.city.Citizen;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.city.Section;
import nju.xiaofanli.city.TrafficMap;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.sensor.Sensor;

public class Resource {
	private static ExecutorService threadPool = Executors.newCachedThreadPool();
	private static Map<String, String> brickAddr = new HashMap<>();
	private final static ImageIcon GREEN_BALLOON_ICON, RED_BALLOON_ICON;
	private final static ImageIcon BLACK_QUESTION_ICON, GREEN_CHECK_ICON, ORANGE_CHECK_ICON, RED_X_ICON;
	private final static JSch JSCH = new JSch();
//	private static Map<String, Session> sessions = new HashMap<>();
	static{
		GREEN_BALLOON_ICON = loadImage("res/green_balloon.png", Section.BalloonIcon.WIDTH, Section.BalloonIcon.HEIGHT);
		RED_BALLOON_ICON = loadImage("res/red_balloon.png", Section.BalloonIcon.WIDTH, Section.BalloonIcon.HEIGHT);
		BLACK_QUESTION_ICON = loadImage("res/black_question_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		GREEN_CHECK_ICON = loadImage("res/green_checked_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		ORANGE_CHECK_ICON = loadImage("res/orange_checked_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		RED_X_ICON = loadImage("res/red_x_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		try {
			//use ssh-keyscan $IP >> brick/known_hosts
			JSCH.setKnownHosts("brick/known_hosts");
            JSCH.addIdentity("brick/id_rsa");
		} catch (JSchException e) {
			e.printStackTrace();
		} 
	}
	
	public static void execute(Runnable command){
		threadPool.execute(command);
	}
	
	public static JSch getJSch(){
		return JSCH;
	}
	
	public static Session getSession(String name){
		String addr = brickAddr.get(name);
		Session session = null;
		if(addr != null){
			try {
				session = JSCH.getSession("robot", addr);
			} catch (JSchException e) {
				e.printStackTrace();
			}
		}
		return session;
	}

	public static Session getRootSession(String name){
		String addr = brickAddr.get(name);
		Session session = null;
		if(addr != null){
			try {
				session = JSCH.getSession("root", addr);
			} catch (JSchException e) {
				e.printStackTrace();
                session = null;
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
	
	public static Sensor[][] getSensors(){
		return TrafficMap.sensors;
	}
	
	public static Section.Crossing[] getCrossings(){
		return TrafficMap.crossings;
	}
	
	public static Section.Street[] getStreets(){
		return TrafficMap.streets;
	}
	
	public static Map<String, Section> getSections(){
		return TrafficMap.sections;
	}
	
	public static Section getSection(String name){
		return TrafficMap.sections.get(name);
	}

	public static List<Citizen> getCitizens(){
        return TrafficMap.citizens;
    }

	public static List<Citizen> getFreeCitizens(){
		return TrafficMap.freeCitizens;
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
	
	public static Set<Delivery.DeliveryTask> getDelivTasks(){
		Set<Delivery.DeliveryTask> set = new HashSet<>();
		set.addAll(Delivery.searchTasks);
		set.addAll(Delivery.deliveryTasks);
		return set;
	}
	
	public static ImageIcon getRedBalloonImageIcon(){
		return RED_BALLOON_ICON;
	}
	
	public static ImageIcon getGreenBalloonImageIcon(){
		return GREEN_BALLOON_ICON;
	}
	
	public static ImageIcon getBlackQuestionImageIcon(){
		return BLACK_QUESTION_ICON;
	}
	
	public static ImageIcon getGreenCheckImageIcon(){
		return GREEN_CHECK_ICON;
	}
	
	public static Icon getOrangeCheckImageIcon() {
		return ORANGE_CHECK_ICON;
	}
	
	public static ImageIcon getRedXImageIcon(){
		return RED_X_ICON;
	}
	
	public static ImageIcon loadImage(String filename, int width, int height){
		ImageIcon imageIcon = new ImageIcon(filename);
		Image image = imageIcon.getImage();
		if(imageIcon.getIconWidth() > imageIcon.getIconHeight())
			image = image.getScaledInstance(width, -1, Image.SCALE_SMOOTH);
		else
			image = image.getScaledInstance(-1, height, Image.SCALE_SMOOTH);
		return new ImageIcon(image);
	}
}
