package nju.ics.lixiaofan.sensor;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.Section.Crossing;
import nju.ics.lixiaofan.city.Section.Street;

public class Sensor {
	public int bid;
	public int sid;
	public String name;
	public int state;
	public int entryThreshold, leaveThreshold;
	public Crossing crossing;
	public Street street;
	public Car car = null;
	public Section nextSection = null;
	public Section prevSection = null;
	public Sensor nextSensor = null;
	public Sensor prevSensor = null;
	public int dir;
	public boolean isEntrance;
//	public boolean isTriggered = false;
	public int showPos = -1;//4 types in total
	public int px, py;
	public JButton button = null;
	
	public final static int INITIAL = 0;
	public final static int DETECTED = 1;
	public final static int UNDETECTED = 2;
	
	public boolean entryDetected(int reading){
		return reading <= entryThreshold;
	}
	
	public boolean leaveDetected(int reading){
		return reading >= leaveThreshold;
	}
	
	public void setLeaveThreshold(int i){
		leaveThreshold = i;
	}
	
	public void reset() {
		state = Sensor.INITIAL;//UNDETECTED;
	}
	
	public static class ButtonListener extends MouseAdapter{
		private int bid, sid;
		public ButtonListener(int bid, int sid) {
			this.bid = bid;
			this.sid = sid;
		}
		public void mousePressed(MouseEvent e) {
			switch (e.getButton()) {
			case MouseEvent.BUTTON1:
//				System.out.println("left click");
				BrickHandler.add(bid, sid, 0);
				break;
			case MouseEvent.BUTTON3:
//				System.out.println("right click");
				BrickHandler.add(bid, sid, 20);
				break;
			}		
		}
	}
}
