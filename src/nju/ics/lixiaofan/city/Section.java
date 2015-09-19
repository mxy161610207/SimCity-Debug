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

public class Section {
	public int id;
	public String name;
	public int region = 0;	//north-west north-east south-west south-east
	public int[] dir = {-1, -1};	//dir[1] only for crossings
	public Map<Integer, Section> adjs = new HashMap<Integer, Section>();// adjacency
	public boolean isOccupied = false;
	public boolean isCombined = false;
	public Queue<Car> cars = new LinkedList<Car>();
	public Car admittedCar = null;
	public Set<Section> combined = null;
	public Object mutex = new Object();
//	private Object mutex4comb = new Object();
	public Queue<Car> waitingCars = new LinkedList<Car>();
	public List<Sensor> sensors = new ArrayList<Sensor>();
	public SectionIcon icon = null;
	
	public void setAdmittedCar(Car car){
		admittedCar = car;
		if(isCombined)
			for(Section s : combined)
				s.admittedCar = car;
	}
	
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
		synchronized (waitingCars) {
			if(!waitingCars.contains(car)){
				waitingCars.add(car);
			}
		}
	}
	
	public void removeWaitingCar(Car car){
		synchronized (waitingCars) {
			if(waitingCars.contains(car)){
				waitingCars.remove(car);
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