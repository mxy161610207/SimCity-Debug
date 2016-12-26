package nju.xiaofanli;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.consistency.context.Rule;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.dashboard.Citizen;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.sensor.Sensor;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Resource {
	public static final Font en15bold = new Font(Font.DIALOG, Font.BOLD, 15);
	public static final Font ch15bold = new Font("Microsoft YaHei", Font.BOLD, 15);
    public static final Font en16bold = new Font(Font.DIALOG, Font.BOLD, 16);
	public static final Font ch16bold = new Font("Microsoft YaHei", Font.BOLD, 16);
    public static final Font en17plain = new Font(Font.DIALOG, Font.PLAIN, 17);
	public static final Font ch17plain = new Font("Microsoft YaHei", Font.PLAIN, 17);
	public static final Font en20bold = new Font(Font.DIALOG, Font.BOLD, 20);
	private static ExecutorService threadPool = Executors.newCachedThreadPool();
	private final static Map<String, String> brickAddr = new HashMap<>();
	private final static Map<String, Integer> brickSensorNum = new HashMap<>();
	public final static ImageIcon GREEN_BALLOON_ICON, RED_BALLOON_ICON, BLACK_QUESTION_ICON, GREEN_CHECK_ICON,
			ORANGE_CHECK_ICON, RED_X_ICON, MOVING_ICON, STOP_ICON, UP_ARROW_ICON, DOWN_ARROW_ICON, LEFT_ARROW_ICON,
            RIGHT_ARROW_ICON, QUESTION_MARK_ICON, CROSSROAD_ICON, STREET_ICON;
    public final static Map<String, ImageIcon[]> CAR_ICONS = new HashMap<>();
	public final static Map<String, ImageIcon> CITIZEN_ICONS = new HashMap<>();
	public final static ImageIcon CRASH_LETTERS;
	private final static JSch JSCH = new JSch();
	public final static Color LIGHT_SKY_BLUE = new Color(135, 206, 250);
	public final static Color DEEP_SKY_BLUE = new Color(0, 191, 255);
    public static final Color SILVER = new Color(192, 192, 192);
    private static final Color LIGHT_GREEN = new Color(0, 255, 127);
    public static final Color CHOCOLATE = new Color(139, 69, 19);
    public static final Color SNOW4 = new Color(139, 137, 137);
    public static final Map<String, Map<String, Integer>> timeouts = new HashMap<>();

    //	private static Map<String, Session> sessions = new HashMap<>();
	static{
		GREEN_BALLOON_ICON = loadImage("res/green_balloon.png", Sensor.BalloonIcon.WIDTH, Sensor.BalloonIcon.HEIGHT);
		RED_BALLOON_ICON = loadImage("res/red_balloon.png", Sensor.BalloonIcon.WIDTH, Sensor.BalloonIcon.HEIGHT);
		BLACK_QUESTION_ICON = loadImage("res/black_question_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		GREEN_CHECK_ICON = loadImage("res/green_checked_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		ORANGE_CHECK_ICON = loadImage("res/orange_checked_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
		RED_X_ICON = loadImage("res/red_x_mark.png", Dashboard.MARK_SIZE, Dashboard.MARK_SIZE);
        MOVING_ICON = new ImageIcon("res/play_icon.png");
        STOP_ICON = new ImageIcon("res/stop_icon.png");
        UP_ARROW_ICON = new ImageIcon("res/up_arrow.png");
        DOWN_ARROW_ICON = new ImageIcon("res/down_arrow.png");
        LEFT_ARROW_ICON = new ImageIcon("res/left_arrow.png");
        RIGHT_ARROW_ICON = new ImageIcon("res/right_arrow.png");
        QUESTION_MARK_ICON = new ImageIcon("res/question_mark_icon.png");
		CROSSROAD_ICON = new ImageIcon("res/crossroad_icon.png");
        STREET_ICON = new ImageIcon("res/street_icon.png");
		CRASH_LETTERS = loadImage("res/crash_letters.png", TrafficMap.U3, TrafficMap.U3);

        CAR_ICONS.put(Car.BLACK, new ImageIcon[]{
                loadImage("res/black_car_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/black_car_fake_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/black_car_real_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
				loadImage("res/black_car_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/black_car_fake_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/black_car_real_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2)
        });
        CAR_ICONS.put(Car.GREEN, new ImageIcon[]{
                loadImage("res/green_car_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/green_car_fake_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/green_car_real_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
				loadImage("res/green_car_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/green_car_fake_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/green_car_real_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2)
        });
        CAR_ICONS.put(Car.ORANGE, new ImageIcon[]{
                loadImage("res/orange_car_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/orange_car_fake_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/orange_car_real_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
				loadImage("res/orange_car_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/orange_car_fake_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/orange_car_real_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2)
        });
        CAR_ICONS.put(Car.RED, new ImageIcon[]{
                loadImage("res/red_car_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/red_car_fake_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/red_car_real_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
				loadImage("res/red_car_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/red_car_fake_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/red_car_real_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2)
        });
        CAR_ICONS.put(Car.SILVER, new ImageIcon[]{
                loadImage("res/silver_suv_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/silver_suv_fake_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/silver_suv_real_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
				loadImage("res/silver_suv_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/silver_suv_fake_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/silver_suv_real_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2)
        });
        CAR_ICONS.put(Car.WHITE, new ImageIcon[]{
                loadImage("res/white_car_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/white_car_fake_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
                loadImage("res/white_car_real_icon.png", Car.CarIcon.SIZE, Car.CarIcon.SIZE),
				loadImage("res/white_car_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/white_car_fake_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2),
				loadImage("res/white_car_real_icon.png", Car.CarIcon.SIZE2, Car.CarIcon.SIZE2)
        });

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

	public static Sensor getSensor(int bid, int sid) {
		if (TrafficMap.sensors == null || TrafficMap.sensors.length <= bid || TrafficMap.sensors[bid].length <= sid)
			return null;
		return TrafficMap.sensors[bid][sid];
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

	public static String getBrickAddr(String name){
		return brickAddr.get(name);
	}
	
	public static Set<String> getBricks(){
		return brickAddr.keySet();
	}
	
	public static void setBrickAddr(String brick, String addr){
		brickAddr.put(brick, addr);
	}

	public static int getSensorNum(String name){
		return brickSensorNum.get(name);
	}

	public static void setSensorNum(String brick, int num){
		brickSensorNum.put(brick, num);
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

	public static Map<String, ImageIcon[]> getCarIcons() {
        return CAR_ICONS;
    }

    public static ImageIcon[] getCarIcons(String name) {
        return CAR_ICONS.get(name);
    }

    static void setCitizenIcons() {
		TrafficMap.citizens.forEach(citizen -> CITIZEN_ICONS.put(citizen.name, loadImage(citizen.icon.imageIcon, TrafficMap.SH/2, TrafficMap.SH/2)));
	}

	public static Map<String, ImageIcon> getCitizenIcons() {
		return CITIZEN_ICONS;
	}

	public static Map<String, Rule> getRules() {
		return Middleware.getRules();
	}

	public static ImageIcon loadImage(String filename, int width, int height) {
		return loadImage(new ImageIcon(filename), width, height);
	}

    public static ImageIcon loadImage(ImageIcon imageIcon, int width, int height) {
        Image image = imageIcon.getImage();
        if(imageIcon.getIconWidth() > imageIcon.getIconHeight())
            image = image.getScaledInstance(width, -1, Image.SCALE_SMOOTH);
        else
            image = image.getScaledInstance(-1, height, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

}
