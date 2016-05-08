package nju.ics.lixiaofan.resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.Section.Crossing;
import nju.ics.lixiaofan.city.Section.Street;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.control.Delivery.DeliveryTask;
import nju.ics.lixiaofan.sensor.Sensor;

public class ResourceProvider {
	private static ExecutorService threadPool = Executors.newCachedThreadPool();
	
	public static void execute(Runnable command){
		threadPool.execute(command);
	}
	
	public static Collection<Car> getCars(){
		return TrafficMap.cars.values();
	}
	
	public static List<List<Sensor>> getSensors(){
		return TrafficMap.sensors;
	}
	
	public static Crossing[] getCrossings(){
		return TrafficMap.crossings;
	}
	
	public static Street[] getStreets(){
		return TrafficMap.streets;
	}
	
	public static Map<String, Section> getSections(){
		return TrafficMap.sections;
	}
	
	public static Section getSection(String name){
		return TrafficMap.sections.get(name);
	}
	
	public static Set<DeliveryTask> getDelivTasks(){
		Set<DeliveryTask> set = new HashSet<Delivery.DeliveryTask>();
		set.addAll(Delivery.searchTasks);
		set.addAll(Delivery.deliveryTasks);
		return set;
	}
}
