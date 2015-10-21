package nju.ics.lixiaofan.city;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.sensor.Sensor;

public class Section extends Location{
	public Map<Integer, Section> adjs = new HashMap<Integer, Section>(); //physical adjacency
	public Map<Section, Section> access = new HashMap<Section, Section>(); //entrance & exit
	public int[] dir = {-1, -1};
	public Queue<Car> cars = new LinkedList<Car>();
	public Car[] permitted = {null};
	public boolean isCombined = false;
	public Set<Section> combined = null;
	public Object mutex = new Object();
	public Queue<Car> waiting = new LinkedList<Car>();
	public Set<Sensor> sensors = new HashSet<Sensor>();
	public SectionIcon icon = null;
	
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
	
	public boolean isOccupied(){
		return !cars.isEmpty();
	}
	
	public static class Crossing extends Section{
	}
	
	public static class Street extends Section{
	}
}