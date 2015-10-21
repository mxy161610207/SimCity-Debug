package nju.ics.lixiaofan.sensor;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.Section.Crossing;
import nju.ics.lixiaofan.city.Section.Street;

public class Sensor {
	public int bid;
	public int sid;
	public int state;
	public Crossing crossing;
	public Street street;
	public Car car = null;
	public Section nextSection = null;
	public Section prevSection = null;
	public Sensor nextSensor = null;
	public Sensor prevSensor = null;
	public int dir;
	public boolean isEntrance;
	public boolean isTriggered = false;
	public int showPos = -1;//4 types in total
	public int px, py;
}
