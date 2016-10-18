package nju.xiaofanli;

import java.awt.*;
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

import nju.xiaofanli.dashboard.Citizen;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.sensor.Sensor;

public class Resource {
    public static final Font bold16dialog = new Font(Font.DIALOG, Font.BOLD, 16);
    public static final Font bold15dialog = new Font(Font.DIALOG, Font.BOLD, 15);
    public static final Font plain17dialog = new Font(Font.DIALOG, Font.PLAIN, 17);
    public static final Font bold20dialog = new Font(Font.DIALOG, Font.BOLD, 20);
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
	private static Map<String, String> brickAddr = new HashMap<>();
	public final static ImageIcon GREEN_BALLOON_ICON, RED_BALLOON_ICON, BLACK_QUESTION_ICON, GREEN_CHECK_ICON,
			ORANGE_CHECK_ICON, RED_X_ICON, MOVING_ICON, STOP_ICON, UP_ARROW_ICON, DOWN_ARROW_ICON, LEFT_ARROW_ICON,
            RIGHT_ARROW_ICON, QUESTION_MARK_ICON;
	private final static JSch JSCH = new JSch();
	public final static Color LIGHT_SKY_BLUE = new Color(135, 206, 250);
	public final static Color DEEP_SKY_BLUE = new Color(0, 191, 255);
    public static final Color SILVER = new Color(192, 192, 192);
    private static Color LIGHT_GREEN = new Color(0, 255, 127);
    public static Color CHOCOLATE = new Color(139, 69, 19);

    //	private static Map<String, Session> sessions = new HashMap<>();
	static{
		GREEN_BALLOON_ICON = loadImage("res/green_balloon.png", Sensor.BalloonIcon.WIDTH, Sensor.BalloonIcon.HEIGHT);
		RED_BALLOON_ICON = loadImage("res/red_balloon.png", Sensor.BalloonIcon.WIDTH, Sensor.BalloonIcon.HEIGHT);
		BLACK_QUESTION_ICON = loadImage("res/black_question_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		GREEN_CHECK_ICON = loadImage("res/green_checked_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		ORANGE_CHECK_ICON = loadImage("res/orange_checked_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		RED_X_ICON = loadImage("res/red_x_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
        MOVING_ICON = loadImage("res/play_icon.png", plain17dialog.getSize(), plain17dialog.getSize());
        STOP_ICON = loadImage("res/stop_icon.png", plain17dialog.getSize(), plain17dialog.getSize());
        UP_ARROW_ICON = loadImage("res/up_arrow.png", plain17dialog.getSize(), plain17dialog.getSize());
        DOWN_ARROW_ICON = loadImage("res/down_arrow.png", plain17dialog.getSize(), plain17dialog.getSize());
        LEFT_ARROW_ICON = loadImage("res/left_arrow.png", plain17dialog.getSize(), plain17dialog.getSize());
        RIGHT_ARROW_ICON = loadImage("res/right_arrow.png", plain17dialog.getSize(), plain17dialog.getSize());
        QUESTION_MARK_ICON = loadImage("res/question_mark_icon.png", plain17dialog.getSize(), plain17dialog.getSize());
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
	
	public static Road.Crossroad[] getCrossroads(){
		return TrafficMap.crossroads;
	}
	
	public static Road.Street[] getStreets(){
		return TrafficMap.streets;
	}
	
	public static Map<String, Road> getRoads(){
		return TrafficMap.roads;
	}
	
	public static Road getRoad(String name){
		return TrafficMap.roads.get(name);
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
