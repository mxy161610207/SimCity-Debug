package nju.xiaofanli.device.car;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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
	public int status = STOPPED;//0: stopped	1: moving	-1: uncertain
	public int trend = 0;//0: tend to stop	1: tend to move	-1: none
	public int finalState = 0;
	public int dir = -1;//0: N	1: S	2: W	3: E
	public Section loc = null;
	public Delivery.DeliveryTask dt = null;
	public Section dest = null;
	public boolean isLoading = false;//loading or unloading
	long lastInstrTime = System.currentTimeMillis();//used for waking cars
	public long stopTime = System.currentTimeMillis();//used for delivery
//	public int lastInstr = -1;
	public CarIcon icon = null;
	public Set<Citizen> passengers = new HashSet<>();
	
	public Section realLoc = null;//if this car become a phantom, then this variable stores it's real location 
	public int realDir, realStatus;

    private final String url;
    private StreamConnection conn = null;
    private DataOutputStream dos = null;
	public boolean tried = false;//whether tried to connect to this car
	public final Object TRIED_OBJ = new Object();
	
	public static final int STOPPED = 0;
	public static final int MOVING = 1;
	public static final int UNCERTAIN = -1;
	
	public static final String SILVER = "Silver SUV";//A
	public static final String GREEN = "Green Car";//B
	public static final String RED = "Red Car";//C
	public static final String WHITE = "White Car";//D
	public static final String BLACK = "Black Car";//E
	public static final String ORANGE = "Orange Car";//F
	
	public Car(String name, Section loc, String url) {
//		this.type = type;
		this.name = name;
		this.loc = loc;
        this.url = url;
		this.icon = new CarIcon(name);
	}
	
	public void reset(){
		status = STOPPED;
		trend = 0;
		finalState = 0;
		dir = -1;
		loc = null;
		dt = null;
		dest = null;
		isLoading = false;
		passengers.clear();
		realLoc = null;
		realDir = -1;
		realStatus = 0;
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
        if(name.equals(Car.BLACK) || name.equals(Car.RED))
            CmdSender.send(this, Command.LEFT);

        synchronized (Delivery.searchTasks) {
            if(Delivery.allBusy){
                Delivery.allBusy = false;
                Delivery.searchTasks.notify();
            }
        }

        //trigger add car event
        if(EventManager.hasListener(Event.Type.ADD_CAR))
            EventManager.trigger(new Event(Event.Type.ADD_CAR, name, loc.name));
    }

	public boolean isConnected(){
        return conn != null;
    }

	public void connect(){
        if(isConnected())
            return;
        System.out.println("connecting " + name);
        try {
            conn = (StreamConnection) Connector.open(url);
            dos = conn.openDataOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            conn = null;
            dos = null;
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
        notifySelfCheck();
    }

	void write(byte[] instr){
        if(isConnected() && instr != null){
			try {
                dos.write(instr);
                lastInstrTime = System.currentTimeMillis();
            } catch (IOException e) {
                e.printStackTrace();
                disconnect();
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
			if(c.isReal())
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
	
	public String getDirStr(){
		return getDirStr(dir);
	}
	
	public String getRealDirStr(){
		return getDirStr(getRealDir());
	}
	
	public int getRealDir(){
		return isReal() ? dir : realDir;
	}
	
	private String getDirStr(int dir){
		switch(dir){
		case 0:
			return "N";
		case 1:
			return "S";
		case 2:
			return "W";
		case 3:
			return "E";
		}
		return null;
	}
	
	public boolean isReal(){
		return realLoc == null;
	}
	
	public static Car carOf(String name){
		return TrafficMap.cars.get(name);
	}
	
	public String getStatusStr(){
		return getStatusStr(status);
	}
	
	public String getRealStatusStr(){
		return getStatusStr(getRealStatus());
	}
	
	private String getStatusStr(int status){
		switch(status){
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
	
	public int getRealStatus(){
		return isReal() ? status : realStatus;
	}
	
	public Section getRealLoc(){
		return isReal() ? loc : realLoc;
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

