package nju.xiaofanli.device.sensor;

import nju.xiaofanli.city.Section;
import nju.xiaofanli.city.Section.Crossing;
import nju.xiaofanli.city.Section.Street;
import nju.xiaofanli.device.car.Car;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Sensor {
	public int bid;
	public int sid;
	public String name;
	public int state;
	public int reading = 30;
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
	public int showPos = -1;//4 types in total
	public int px, py;
	public JButton icon = null;

	public final static int INITIAL = 0;
	public final static int DETECTED = 1;
	public final static int UNDETECTED = 2;

    public Sensor(int bid, int sid) {
        this.bid = bid;
        this.sid = sid;
        name = "B" + bid + "S" + (sid+1);
        state = Sensor.UNDETECTED;
        entryThreshold = (int) (undetectedReading[bid][sid] * 0.75); //10;
        leaveThreshold = entryThreshold + 1; //11;
    }

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
				BrickHandler.add(sensor.bid, sensor.sid, 30, System.currentTimeMillis());
				break;
			case MouseEvent.BUTTON2: {
                BrickServer.showingSensor = BrickServer.showingSensor != sensor ? sensor : null;
                String str = "[" + sensor.name + "] state=";
                switch (sensor.state) {
                    case INITIAL:
                        str += "INITIAL";
                        break;
                    case DETECTED:
                        str += "DETECTED";
                        break;
                    case UNDETECTED:
                        str += "UNDETECTED";
                        break;
                }
                str += ", entryThreshold=" + sensor.entryThreshold + ", leaveThreshold=" + sensor.leaveThreshold;
                System.out.println(str);
            }
				break;
			}		
		}
	}

	private static final int[][] undetectedReading = new int[10][4];
	static {
        undetectedReading[0][0] = 24;
        undetectedReading[0][1] = 30;
        undetectedReading[1][0] = 24;
        undetectedReading[1][1] = 16;
        undetectedReading[1][2] = 18;
        undetectedReading[2][0] = 22;
        undetectedReading[2][1] = 18;
        undetectedReading[2][2] = 25;
        undetectedReading[2][3] = 19;
        undetectedReading[3][0] = 17;
        undetectedReading[3][1] = 22;
        undetectedReading[3][2] = 18;
        undetectedReading[3][3] = 17;
        undetectedReading[4][0] = 16;
        undetectedReading[4][1] = 20;
        undetectedReading[4][2] = 15;
        undetectedReading[5][0] = 20;
        undetectedReading[5][1] = 28;
        undetectedReading[5][2] = 17;
        undetectedReading[6][0] = 17;
        undetectedReading[6][1] = 19;
        undetectedReading[6][2] = 18;
        undetectedReading[6][3] = 21;
        undetectedReading[7][0] = 17;
        undetectedReading[7][1] = 19;
        undetectedReading[7][2] = 19;
        undetectedReading[7][3] = 23;
        undetectedReading[8][0] = 19;
        undetectedReading[8][1] = 27;
        undetectedReading[8][2] = 26;
        undetectedReading[9][0] = 23;
        undetectedReading[9][1] = 21;
    }
}
