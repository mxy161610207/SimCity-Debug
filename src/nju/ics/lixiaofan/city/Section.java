package nju.ics.lixiaofan.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.sensor.Sensor;

public class Section extends Location{
	public int region = 0;	//north-west north-east south-west south-east
	public int[] dir = {-1, -1};	//dir[1] only for crossings
	public Map<Integer, Section> adjs = new HashMap<Integer, Section>();// adjacency
	public boolean isOccupied = false;
	public boolean isCombined = false;
	public Queue<Car> cars = new LinkedList<Car>();
	public Car[] permitted = {null};
	public Set<Section> combined = null;
	public Object mutex = new Object();
//	private Object mutex4comb = new Object();
	public Queue<Car> waiting = new LinkedList<Car>();
	public List<Sensor> sensors = new ArrayList<Sensor>();
	public SectionIcon icon = null;
	
//	public void setPermitted(Car car){
//		permitted[0] = car;
////		if(isCombined)
////			for(Section s : combined)
////				s.admittedCar = car;
//	}
	
	public static Section sectionOf(String name){
		if(name == null)
			return null;
		String[] strs = name.split(" ");
		int id = Integer.parseInt(strs[1]);
		if(strs[0].equals("Crossing"))
			return TrafficMap.crossings[id];
		else if(strs[0].equals("Street"))
			return TrafficMap.streets[id];
		else
			return null;
	}
	
	public void addWaitingCar(Car car){
		synchronized (waiting) {
			if(!waiting.contains(car)){
				waiting.add(car);
			}
		}
	}
	
	public void removeWaitingCar(Car car){
		synchronized (waiting) {
			if(waiting.contains(car)){
				waiting.remove(car);
			}
		}
	}
	
	public static class Crossing extends Section{
		public Street[] adj = new Street[4];//sequence: N->S->W->E
	}
	
	public static class Street extends Section{
		public Crossing[] adj = new Crossing[2];//sequence: same as the sequence cars pass by (positive)
	}
}