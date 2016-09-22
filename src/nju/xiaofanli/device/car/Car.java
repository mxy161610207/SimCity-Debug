package nju.xiaofanli.device.car;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.swing.JButton;

import nju.xiaofanli.city.Citizen;
import nju.xiaofanli.city.Section;
import nju.xiaofanli.city.TrafficMap;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

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
	
	public Section realLoc = null;//if this car become a phantom, then this variable stores it's real location 
	public int realDir, realState;

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
	
	public Car(String name, Section loc, String url) {
//		this.type = type;
		this.name = name;
		this.loc = loc;
        this.url = url;
		this.icon = new CarIcon(name);
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
		realLoc = null;
		realDir = TrafficMap.UNKNOWN_DIR;
		realState = STOPPED;
	}
	
	public void notifyPolice(int cmd) {
		if(loc == null)
			return;
		Police.add(this, dir, loc, cmd);
	}
	
	public void notifyPolice(Section next) {
		if(loc == null)
			return;
		Police.add(this, dir, loc, Police.ALREADY_ENTERED, next);
	}
	
	private void notifySelfCheck(){
		if(!tried){
			tried = true;
			synchronized (TRIED_OBJ) {
				TRIED_OBJ.notify();
			}
		}
	}

	public void init(){
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

	void write(byte[] code){
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
		Section prev = loc;
		loc = section;
		leave(prev);
		section.cars.add(this);
		
		int real = section.realCars.size();
		for(Car c : section.cars)
			if(!c.hasPhantom())
				real++;
		
		if(real > 1){
//			System.out.println("REAL CRASH");
			Dashboard.playCrashSound();
		}
		
		section.icon.repaint();
		if(section.isCombined()){
//			dir = section.dir[0];//only combined sections can change a car's direction
			for(Section s : section.combined)
				s.icon.repaint();
		}
		//trigger move event
		if(EventManager.hasListener(Event.Type.CAR_MOVE))
			EventManager.trigger(new Event(Event.Type.CAR_MOVE, name, loc.name));
	}
	
	public void leave(Section section){
		if(section == null)
			return;
		section.cars.remove(this);
		if(loc == section)
			loc = null;
		section.icon.repaint();
		if(section.isCombined()){
			for(Section s : section.combined)
				s.icon.repaint();
		}
		//trigger leaving event
		if(EventManager.hasListener(Event.Type.CAR_LEAVE))
			EventManager.trigger(new Event(Event.Type.CAR_LEAVE, name, section.name));
	}
	
	public void setLoading(boolean loading){
		isLoading = loading;
		if(loc == null)
			return;
		loc.icon.repaint();
		for(Section s : loc.combined)
			s.icon.repaint();
	}

	public void saveRealInfo(){
        loc.realCars.add(this);
        realLoc = loc;
        realDir = dir;
        realState = state;
    }

    public void loadRealInfo(){
        Section fakeLoc = loc;
        loc = realLoc;
        leave(fakeLoc);
        realLoc.realCars.remove(this);
        dir = realDir;
        state = realState;
        realLoc = null;
        realDir = TrafficMap.UNKNOWN_DIR;
        realState = STOPPED;
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
	
	public String getRealStateStr(){
		return stateOf(getRealState());
	}
	
	private String stateOf(int state){
		switch(state){
		case 0:
			return "Stopped";
		case 1:
			return "Moving";
		case -1:
			return "Uncertain";
		default:
			return null;	
		}
	}
	
	public int getRealState(){
		return !hasPhantom() ? state : realState;
	}

    public void setRealState(int realState){
        this.realState = realState;
    }

	public Section getRealLoc(){
		return !hasPhantom() ? loc : realLoc;
	}
	
	public static class CarIcon extends JButton{
		private static final long serialVersionUID = 1L;
		private final String name;
		public static final int SIZE = (int) (0.8*TrafficMap.SH);
		public static final int INSET = (int) (0.2*TrafficMap.SH);
		public static final Color SILVER = new Color(192, 192, 192);
		CarIcon(String name) {
			setOpaque(false);
			setContentAreaFilled(false);
			setPreferredSize(new Dimension(SIZE, SIZE));
//			setBorderPainted(false);
			this.name = name;
//			setSize(size, size);
//			setMinimumSize(new Dimension(size, size));
		}
		
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			switch(name){
			case Car.ORANGE:
				g.setColor(Color.ORANGE); break;
			case Car.BLACK:
				g.setColor(Color.BLACK); break;
			case Car.WHITE:
				g.setColor(Color.WHITE); break;
			case Car.RED:
				g.setColor(Color.RED); break;
			case Car.GREEN:
				g.setColor(Color.GREEN); break;
			case Car.SILVER:
				g.setColor(SILVER); break;
			default:
				return;
			}
			g.fillRect(0, 0, SIZE, SIZE);
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, SIZE, SIZE);
		}
	}
}

