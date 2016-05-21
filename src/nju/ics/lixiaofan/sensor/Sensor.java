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
	public int reading;
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
	public JButton icon = null;
	
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
	
	//TODO
	public void reset() {
		if(entryDetected(reading))
			state = DETECTED;
		else if(leaveDetected(reading))
			state = UNDETECTED;
		else
			state = Sensor.INITIAL;
	}
	
	public static class SensorIcon extends MouseAdapter{
		private Sensor sensor;
		public SensorIcon(Sensor sensor) {
			this.sensor = sensor;
		}
		public void mousePressed(MouseEvent e) {
			switch (e.getButton()) {
			case MouseEvent.BUTTON1:
//				System.out.println("left click");
				BrickHandler.add(sensor.bid, sensor.sid, 0);
				break;
			case MouseEvent.BUTTON3:
//				System.out.println("right click");
				BrickHandler.add(sensor.bid, sensor.sid, 20);
				break;
			case MouseEvent.BUTTON2:
				System.out.print(sensor.name + " ");
				switch (sensor.state) {
				case INITIAL:
					System.out.println("INITIAL");
					break;
				case DETECTED:
					System.out.println("DETECTED");
					break;
				case UNDETECTED:
					System.out.println("UNDETECTED");
					break;
				}
				break;
			}		
		}
	}
}
