package nju.ics.lixiaofan.car;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;

import nju.ics.lixiaofan.city.Citizen;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery.DeliveryTask;
import nju.ics.lixiaofan.control.TrafficPolice;

public class Car {
//	public int id;
	public int type;//0: battletank	1: tankbot	2: carbot 3: zenwheels
	public boolean isConnected = false;
	public String name = null;//only for zenwheels
//	public int frequency;//0: A|grey	1: B|orange	2: C|green	3: blue
	public int status = 0;//0: still	1: moving	-1: uncertain
	public int expectation = 0;//0: wanna stop	1: wanna move	-1: none
	public int finalState = 0;//the same as expectation
	public int dir = -1;//0: N	1: S	2: W	3: E
	public Section loc = null;
	public Section realLoc = null;//if this car become a phantom, then this variable stores it's real location 
	public DeliveryTask dt = null;
	public Section dest = null;
	public boolean isLoading = false;//loading or unloading
	public long lastDetectedTime = 0;//the time when the car was last detected
	public long lastInstrTime = System.currentTimeMillis();
	public long lastStopInstrTime = 0;//the time when a stop instruction was last sent to this car
	public long stopTime = System.currentTimeMillis();
	public int lastInstr = -1;
	public CarIcon icon = null;
	public Set<Citizen> passengers = new HashSet<Citizen>();
	
	public static final int STILL = 0;
	public static final int MOVING = 1;
	public static final int UNCERTAIN = -1;
	
	public static final String ORANGE = "Orange Car";
	public static final String GREEN = "Green Car";
	public static final String BLACK = "Black Car";
	public static final String WHITE = "White Car";
	public static final String SILVER = "Silver SUV";
	public static final String RED = "Red Car";
	
	public Car(int type, String name, Section loc) {
		this.type = type;
		this.name = name;
		this.loc = loc;
		this.icon = new CarIcon(name);
	}
	
	public void sendRequest(int cmd) {
		if(loc == null)
			return;
		TrafficPolice.sendRequest(this, loc, cmd);
	}
	
	public String getDir(){
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
		return "U";
	}
	
	public boolean isReal(){
		return realLoc == null;
	}
	
	public static Car carOf(String name){
		return TrafficMap.cars.get(name);
	}
	
	public String getState(){
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
	
	public static class CarIcon extends JButton{
		private static final long serialVersionUID = 1L;
		private String name = null;
		public static final int SIZE = (int) (0.8*TrafficMap.sh);
		public static final int INSET = (int) (0.2*TrafficMap.sh);
		public static final Color SILVER = new Color(192, 192, 192);
		public CarIcon(String name) {
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

