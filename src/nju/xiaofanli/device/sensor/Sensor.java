package nju.xiaofanli.device.sensor;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.city.Section;
import nju.xiaofanli.city.Section.Crossing;
import nju.xiaofanli.city.Section.Street;

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
	
	boolean entryDetected(int reading){
		return reading <= entryThreshold;
	}
	
	boolean leaveDetected(int reading){
		return reading >= leaveThreshold;
	}

	public void reset() {
		if(entryDetected(reading))
			state = DETECTED;
		else if(leaveDetected(reading))
			state = UNDETECTED;
		else
//			state = Sensor.INITIAL;
			state = UNDETECTED;
	}
	
	public static class SensorIconListener extends MouseAdapter{
		private Sensor sensor;
		public SensorIconListener(Sensor sensor) {
			this.sensor = sensor;
		}
		public void mousePressed(MouseEvent e) {
			switch (e.getButton()) {
			case MouseEvent.BUTTON1:
//				System.out.println("left click");
				BrickHandler.add(sensor.bid, sensor.sid, 0, System.currentTimeMillis());
				break;
			case MouseEvent.BUTTON3:
//				System.out.println("right click");
				BrickHandler.add(sensor.bid, sensor.sid, 20, System.currentTimeMillis());
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
