package nju.xiaofanli.device.car;

import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.city.Citizen;
import nju.xiaofanli.city.Section;
import nju.xiaofanli.city.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.sensor.Sensor;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class Car {
	public final String name;
	public int state = STOPPED;//0: stopped	1: moving	-1: uncertain
	public int lastCmd = Command.STOP;
    public int trend = STOPPED; // Only used by suspend!
	public int finalState = STOPPED;
    public Section loc = null;
	public int dir = TrafficMap.UNKNOWN_DIR;//0: N	1: S	2: W	3: E
	public Delivery.DeliveryTask dt = null;
	public Section dest = null;
	public boolean isLoading = false;//loading or unloading
	public long lastCmdTime = System.currentTimeMillis();//used for waking cars
	public long stopTime = System.currentTimeMillis();//used for delivery
	public CarIcon icon = null;
	public Set<Citizen> passengers = new HashSet<>();
    public boolean isHornOn = false; //only for crash
	
	public Section realLoc = null;//if this car become a phantom, then this variable stores it's real location 
	public int realDir;

    private final String url;
    private StreamConnection conn = null;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
	public boolean tried = false;//whether tried to connect to this car
	public final Object TRIED_OBJ = new Object();
	
	public static final int STOPPED = 0;
	public static final int MOVING = 1;
//	public static final int UNCERTAIN = -1;
	
	public static final String SILVER = "Silver SUV";
	public static final String GREEN = "Green Car";
	public static final String RED = "Red Car";
	public static final String WHITE = "White Car";
	public static final String BLACK = "Black Car";
	public static final String ORANGE = "Orange Car";
    public static final String[] allCarNames = { SILVER, GREEN, RED, WHITE, BLACK, ORANGE };
	
	public Car(String name, Section loc, String url) {
		this.name = name;
		this.loc = loc;
        this.url = url;
		this.icon = new CarIcon(name);
        this.icon.addActionListener(e -> Dashboard.setSelectedCar(this));
	}
	
	public void reset(){
		state = STOPPED;
		lastCmd = Command.STOP;
        trend = STOPPED;
		finalState = STOPPED;
        loc = null;
		dir = TrafficMap.UNKNOWN_DIR;
		dt = null;
		dest = null;
		isLoading = false;
		passengers.clear();
		resetRealInfo();
        isHornOn = false;
        firstEntry = true;
	}

	public void notifyPolice(int cmd) {
		Police.add(this, dir, loc, cmd, loc.adjSects.get(dir));
	}

	public void notifyPolice(int cmd, Section requested) {
		if(requested == null)
			return;
		Police.add(this, dir, loc, cmd, requested);
	}

	private void notifySelfCheck(){
		if(!tried){
			tried = true;
			synchronized (TRIED_OBJ) {
				TRIED_OBJ.notify();
			}
		}
	}

	private boolean firstEntry = true;
    public void initLocAndDir(Sensor sensor) {
        if(sensor == null || !firstEntry)
            return;
        firstEntry = false;
        loc = sensor.nextSection;
        if(dir == TrafficMap.UNKNOWN_DIR) {
            dir = loc.dir[1] == TrafficMap.UNKNOWN_DIR ? loc.dir[0] : sensor.dir;
            PkgHandler.send(new AppPkg().setDir(name, dir));
        }
        sensor.nextSection.cars.add(this);
        PkgHandler.send(new AppPkg().setCar(name, dir, loc.name));
        Middleware.addInitialContext(name, dir, Car.MOVING, "movement", "enter", sensor.prevSection.name, sensor.nextSection.name,
                System.currentTimeMillis(), null, null);
    }

	public void init() {
		if(loc != null && firstEntry){
            Sensor sensor = null;
            for(Sensor s : loc.adjSensors.values())
                if(s.nextSection.sameAs(loc)) {
                    sensor = s;
                    break;
                }
            if(sensor == null)
                throw new NullPointerException();
            initLocAndDir(sensor);
		}

        TrafficMap.connectedCars.add(this);
        Dashboard.addCar(this);
        //calibrate
//        if(name.equals(Car.BLACK) || name.equals(Car.RED))
//            write(Command.codes.get(Command.LEFT));

        synchronized (Delivery.searchTasks) {
            if(Delivery.allBusy){
                Delivery.allBusy = false;
                Delivery.searchTasks.notify();
            }
        }

        //trigger add car event
        if(EventManager.hasListener(Event.Type.ADD_CAR))
            EventManager.trigger(new Event(Event.Type.ADD_CAR, name, loc != null ? loc.name : null));
    }

	public boolean isConnected(){
        return conn != null;
    }

    public DataInputStream getDIS(){
        return dis;
    }

	public void connect(){
        if(isConnected())
            return;
        System.out.println("connecting " + name);
        try {
            conn = (StreamConnection) Connector.open(url);
            dos = conn.openDataOutputStream();
            dis = conn.openDataInputStream();
        } catch (IOException e) {
//            e.printStackTrace();
            disconnect();
        }
        if(isConnected())
            init();
        notifySelfCheck();
    }

    public void disconnect(){
        if(!isConnected())
            return;
        System.out.println("disconnecting " + name);
        try {
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn = null;
        dos = null;
        dis = null;
//        notifySelfCheck();
    }

	void write(byte[] code) {
        if(isConnected() && code != null){
			try {
                dos.write(code);
                lastCmdTime = System.currentTimeMillis();
                if(code == Command.codes.get(Command.MOVE_FORWARD))
                    trend = Car.MOVING;
                else if(code == Command.codes.get(Command.STOP))
                    trend = Car.STOPPED;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
	
	public void enter(Section section){
		if(section == null || section.sameAs(loc))
			return;
		notifyPolice(Police.BEFORE_ENTRY, section);
		Section prev = loc;
		loc = section;
		leave(prev, true);
		section.cars.add(this);
		notifyPolice(Police.AFTER_ENTRY, section);
		section.icon.repaintAll();

        Set<Car> allRealCars = new HashSet<>(section.realCars);
        allRealCars.addAll(section.cars.stream().filter(car -> !car.hasPhantom()).collect(Collectors.toSet()));
        if(allRealCars.size() > 1) {
            allRealCars.stream().filter(car -> !car.isHornOn).forEach(car -> Command.send(car, Command.HORN_ON));
            // stop all crashed cars to keep the scene intact
            allRealCars.forEach(car -> {
                car.finalState = STOPPED;
                car.notifyPolice(Police.REQUEST2STOP);
            });

            Dashboard.playCrashSound();
        }
        else{
            allRealCars.stream().filter(car -> car.isHornOn).forEach(car -> Command.send(car, Command.HORN_OFF));
        }

        //trigger entering event
        if(EventManager.hasListener(Event.Type.CAR_ENTER))
            EventManager.trigger(new Event(Event.Type.CAR_ENTER, name, loc.name));

        if(loc.cars.size() > 1){
            Set<String> crashedCarNames = loc.cars.stream().map(crashedCar -> crashedCar.name).collect(Collectors.toSet());
            //trigger crash event
            if(EventManager.hasListener(Event.Type.CAR_CRASH))
                EventManager.trigger(new Event(Event.Type.CAR_CRASH, crashedCarNames, loc.name));
        }
	}

//	public void leave(Section section){
//		leave(section, true);
//	}

	public void leave(Section section, boolean withEntry){
		if(section == null)
			return;
		notifyPolice(withEntry ? Police.BEFORE_LEAVE : Police.BEFORE_VANISH, section.adjSects.get(dir));
		section.cars.remove(this);
		if(loc == section)
			loc = null;
		notifyPolice(withEntry ? Police.AFTER_LEAVE : Police.AFTER_VANISH, section);
		section.icon.repaintAll();

        Set<Car> allRealCars = new HashSet<>(section.realCars);
        allRealCars.addAll(section.cars.stream().filter(car -> !car.hasPhantom()).collect(Collectors.toSet()));
        if(allRealCars.size() > 1){
            allRealCars.stream().filter(car -> !car.isHornOn).forEach(car -> Command.send(car, Command.HORN_ON));
        }
        else{
            allRealCars.stream().filter(car -> car.isHornOn).forEach(car -> Command.send(car, Command.HORN_OFF));
        }

		//trigger leaving event
		if(EventManager.hasListener(Event.Type.CAR_LEAVE))
			EventManager.trigger(new Event(Event.Type.CAR_LEAVE, name, section.name));
	}
	
	public void setLoading(boolean loading){
		isLoading = loading;
		if(loc == null)
			return;
		loc.icon.repaintAll();
	}

	public void saveRealInfo(){
        loc.realCars.add(this);
        realLoc = loc;
        realDir = dir;
    }

    public void loadRealInfo(){
        Section fakeLoc = loc;
        loc = realLoc;
        leave(fakeLoc, false);
        realLoc.realCars.remove(this);
        dir = realDir;
        resetRealInfo();
    }

    public void setRealInfo(Section loc, int dir) {
        realLoc.realCars.remove(this);
        realLoc.icon.repaintAll();
        if (this.loc.sameAs(loc) && this.dir == dir) {
            resetRealInfo();
        }
        else {
            loc.realCars.add(this);
            realLoc = loc;
            realDir = dir;
        }
        loc.icon.repaintAll();
    }

    public void resetRealInfo() {
        realLoc = null;
        realDir = TrafficMap.UNKNOWN_DIR;
    }

	public String getDirStr(){
		return TrafficMap.dirOf(dir);
	}
	
	public String getRealDirStr(){
		return TrafficMap.dirOf(getRealDir());
	}
	
	public int getRealDir(){
		return !hasPhantom() ? dir : realDir;
	}

    public boolean hasPhantom(){
		return realLoc != null;
	}
	
	public static Car carOf(String name){
		return TrafficMap.cars.get(name);
	}

	public int getState(){
        return state;
    }

    public void setState(int state){
        this.state = state;
    }

	public String getStateStr(){
		return stateOf(state);
	}

    private static String stateOf(int state){
		switch(state){
		case STOPPED:
			return "Stopped";
		case MOVING:
			return "Moving";
		default:
			return null;	
		}
	}

	public static Color colorOf(String name) {
        switch(name){
            case Car.ORANGE:
                return Color.ORANGE;
            case Car.BLACK:
                return Color.BLACK;
            case Car.WHITE:
                return Color.WHITE;
            case Car.RED:
                return Color.RED;
            case Car.GREEN:
                return Color.GREEN;
            case Car.SILVER:
                return CarIcon.SILVER;
            default:
                return null;
        }
    }

    public Section getRealLoc(){
		return !hasPhantom() ? loc : realLoc;
	}

	private static Random random = new Random();
	public static String getACarName(){
        return allCarNames[random.nextInt(allCarNames.length)];
    }

	public static class CarIcon extends JButton{
		private static final long serialVersionUID = 1L;
		private final String name;
		public static final int SIZE = (int) (0.8*TrafficMap.SH);
		public static final int INSET = (int) (0.2*TrafficMap.SH);
		public static final Color SILVER = new Color(192, 192, 192);
        public final Color color;
		CarIcon(String name) {
			setOpaque(false);
			setContentAreaFilled(false);
			setPreferredSize(new Dimension(SIZE, SIZE));
//			setBorderPainted(false);
			this.name = name;
            color = colorOf(name);
//			setSize(AVATAR_SIZE, AVATAR_SIZE);
//			setMinimumSize(new Dimension(size, size));
		}
		
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(color);
			g.fillRect(0, 0, SIZE, SIZE);
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, SIZE, SIZE);
		}
	}
}

