package nju.xiaofanli.device.car;

import nju.xiaofanli.Resource;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.dashboard.Citizen;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.dashboard.TrafficMap;
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
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class Car {
	public final String name;
	public int state = STOPPED;//0: stopped	1: moving	-1: uncertain
	public int lastCmd = Command.STOP;
    public int trend = STOPPED; // Only used by suspend and wake!
    public int lastHornCmd = Command.HORN_OFF;
    private int availCmd = Command.MOVE_FORWARD; // available command
    public Road loc = null;
	public TrafficMap.Direction dir = TrafficMap.Direction.UNKNOWN;
	public Delivery.DeliveryTask dt = null;
	public Road dest = null;
	public boolean isLoading = false;//loading or unloading
	public long lastCmdTime = System.currentTimeMillis();//used for waking cars
	public long stopTime = System.currentTimeMillis();//used for delivery
	public CarIcon icon = null;
	public Citizen passenger = null;
    public boolean isHornOn = false; //only for crash
    public boolean isInCrash = false;
    public int timeout = Integer.MAX_VALUE;
	
	private Road realLoc = null;//if this car become a phantom, then this variable stores it's real location
	private TrafficMap.Direction realDir;

    private final String url;
    private StreamConnection conn = null;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
	public boolean tried = false;//whether tried to connect to this car
	public final Object TRIED_OBJ = new Object();
	
	public static final int STOPPED = 0;
	public static final int MOVING = 1;

	public static final String SILVER = "Silver SUV";
	public static final String GREEN = "Green Car";
	public static final String RED = "Red Car";
	public static final String WHITE = "White Car";
	public static final String BLACK = "Black Car";
	public static final String ORANGE = "Orange Car";
    public static final String[] allCarNames = { SILVER, GREEN, RED, WHITE, BLACK, ORANGE };
	
	public Car(String name, Road loc, String url, String iconFile) {
		this.name = name;
		this.loc = loc;
        this.url = url;
		this.icon = new CarIcon(name, iconFile);
	}
	
	public void reset(){
		state = STOPPED;
		lastCmd = Command.STOP;
        availCmd = Command.MOVE_FORWARD;
        trend = STOPPED;
        lastHornCmd = Command.HORN_OFF;
        loc = null;
		dir = TrafficMap.Direction.UNKNOWN;
		dt = null;
		dest = null;
		isLoading = false;
		passenger = null;
		resetRealInfo();
        isInCrash = isHornOn = false;
        firstEntry = true;
        timeout = Integer.MAX_VALUE;
	}

	public void notifyPolice(int cmd) {
		Police.add(this, dir, loc, cmd, loc.adjRoads.get(dir));
	}

	public void notifyPolice(int cmd, boolean fromUser) {
        Police.add(this, dir, loc, cmd, loc.adjRoads.get(dir), fromUser);
    }

	public void notifyPolice(int cmd, Road requested) {
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
        loc = sensor.nextRoad;
        if(dir == TrafficMap.Direction.UNKNOWN) {
            dir = sensor.getNextRoadDir();
            PkgHandler.send(new AppPkg().setDir(name, dir));
        }
        sensor.nextRoad.cars.add(this);
        sensor.nextRoad.allRealCars.add(this);
        timeout = loc.timeouts.get(dir).get(name);
        sensor.nextRoad.iconPanel.repaint();
        Middleware.addInitialContext(name, dir, Car.MOVING, "movement", "enter",
                sensor.prevRoad.name, sensor.nextRoad.name, sensor.nextSensor.nextRoad.name,
                System.currentTimeMillis(), this, sensor);
        PkgHandler.send(new AppPkg().setCar(name, dir, loc.name));
    }

	public void init() {
		if(loc != null && firstEntry){
            Sensor sensor = null;
            for(Sensor s : loc.adjSensors.values())
                if(s.nextRoad == loc) {
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
        write(Command.RIGHT);

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

	void write(int cmd) {
        byte[] code = Command.codes.get(cmd);
        if(isConnected() && code != null) {
			try {
                dos.write(code);
                lastCmdTime = System.currentTimeMillis();
                switch (cmd) {
                    case Command.MOVE_FORWARD:
                        trend = Car.MOVING; break;
                    case Command.STOP:
                        trend = Car.STOPPED; break;
                    case Command.HORN_ON:
                        isHornOn = true; break;
                    case Command.HORN_OFF:
                        isHornOn = false; break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
	
	public void enter(Road road, TrafficMap.Direction dir){
		if(road == null || road == loc)
			return;
		leave(loc, true);
        notifyPolice(Police.BEFORE_ENTRY, road);
        loc = road;
		loc.cars.add(this);
        this.dir = dir;
        if (!hasPhantom()) {
            loc.allRealCars.add(this);
            timeout = loc.timeouts.get(dir).get(name); //setting remaining time to phantoms is meaningless
        }
        if(getState() != MOVING) {
            setState(MOVING);
            //trigger move event
            if(EventManager.hasListener(Event.Type.CAR_MOVE))
                EventManager.trigger(new Event(Event.Type.CAR_MOVE, name, loc.name));
        }

        setLoading(false);
		notifyPolice(Police.AFTER_ENTRY, road);
        if (hasPhantom() && loc == realLoc && dir == realDir) {
            resetRealInfo();
        }
		road.iconPanel.repaint();
        road.checkRealCrash();

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

//	public void leave(Road road){
//		leave(road, true);
//	}

	public void leave(Road road, boolean withEntry){
		if(road == null)
			return;
		notifyPolice(withEntry ? Police.BEFORE_LEAVE : Police.BEFORE_VANISH, road.adjRoads.get(dir));
		road.cars.remove(this);
        if (!hasPhantom())
            road.allRealCars.remove(this);
		notifyPolice(withEntry ? Police.AFTER_LEAVE : Police.AFTER_VANISH, road);
		road.iconPanel.repaint();
        road.checkRealCrash();

		//trigger leaving event
		if(EventManager.hasListener(Event.Type.CAR_LEAVE))
			EventManager.trigger(new Event(Event.Type.CAR_LEAVE, name, road.name));
	}
	
	public void setLoading(boolean loading) {
	    if (getLoading() != loading) {
            isLoading = loading;
            if (loc == null)
                return;
            loc.iconPanel.repaint();
        }
	}

	public boolean getLoading() {
        return isLoading;
    }

	public void saveRealInfo(){
        loc.realCars.add(this);
        realLoc = loc;
        realDir = dir;
    }

    public void setRealInfo(Road loc, TrafficMap.Direction dir) {
        realLoc.realCars.remove(this);
        realLoc.allRealCars.remove(this);
        realLoc.iconPanel.repaint();
        realLoc.checkRealCrash();
        if (this.loc == loc && this.dir == dir) {
            resetRealInfo();
        }
        else {
            loc.realCars.add(this);
            realLoc = loc;
            realDir = dir;
        }
        loc.allRealCars.add(this);
        loc.iconPanel.repaint();
        loc.checkRealCrash();
        timeout = loc.timeouts.get(dir).get(name);
    }

    public void resetRealInfo() {
        if(realLoc != null) {
            realLoc.realCars.remove(this);
            realLoc = null;
        }
        realDir = TrafficMap.Direction.UNKNOWN;
    }

    public void setAvailCmd(int cmd) {
        if (cmd != Command.MOVE_FORWARD && cmd != Command.STOP)
            return;
        availCmd = cmd;
        boolean isMoving = cmd == Command.MOVE_FORWARD;
        if (this == Dashboard.getSelectedCar()) {
            Dashboard.enableStartCarButton(isMoving);
            Dashboard.enableStopCarButton(!isMoving);
        }

        if (isMoving) {
            Dashboard.enableStartAllCarsButton(true);
            if (Dashboard.isStopAllCarsButtonEnabled()) {
                boolean allStart = true;
                for(Car car : Resource.getConnectedCars())
                    if(car.availCmd == Command.STOP) {
                        allStart = false;
                        break;
                    }
                Dashboard.enableStopAllCarsButton(!allStart);
            }
        }
        else {
            Dashboard.enableStopAllCarsButton(true);
            if (Dashboard.isStartAllCarsButtonEnabled()) {
                boolean allStop = true;
                for(Car car : Resource.getConnectedCars())
                    if(car.availCmd == Command.MOVE_FORWARD) {
                        allStop = false;
                        break;
                    }
                Dashboard.enableStartAllCarsButton(!allStop);
            }
        }
    }

    public int getAvailCmd() {
        return availCmd;
    }

	public String getDirStr(){
		return TrafficMap.dirOf(dir);
	}
	
	public String getRealDirStr(){
		return TrafficMap.dirOf(getRealDir());
	}
	
	public TrafficMap.Direction getRealDir(){
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
                return Resource.SILVER;
            default:
                return null;
        }
    }

    public Road getRealLoc(){
		return !hasPhantom() ? loc : realLoc;
	}

	private static Random random = new Random();
	public static String getACarName(){
        return allCarNames[random.nextInt(allCarNames.length)];
    }

	public static class CarIcon extends JLabel{
		private static final long serialVersionUID = 1L;
		private final String name;
        public static final int INSET = TrafficMap.CW / 13; // INSET : SIZE = 1 : 6
		public static final int SIZE = (TrafficMap.CW-INSET)/2 - 2;
        public static final int INSET2 = TrafficMap.CW / 23; // INSET2 : SIZE2 = 1 : 5
        public static final int SIZE2 = (TrafficMap.CW-3*INSET2)/4 - 2;
        public final Color color;
        private ImageIcon imageIcon;
		CarIcon(String name, String iconFile) {
			setOpaque(false);
//			setContentAreaFilled(false);
			setPreferredSize(new Dimension(SIZE, SIZE));
            imageIcon = Resource.loadImage(iconFile, SIZE, SIZE);
            setIcon(imageIcon);
//			setBorderPainted(false);
			this.name = name;
            color = colorOf(name);
		}
	}
}

