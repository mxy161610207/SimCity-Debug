package nju.ics.lixiaofan.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.RCServer;
import nju.ics.lixiaofan.city.Section.Crossing;
import nju.ics.lixiaofan.city.Section.Street;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.control.Delivery.DeliveryTask;
import nju.ics.lixiaofan.sensor.Sensor;

public class DataProvider {
	public static Collection<Car> getCars(){
		return RCServer.cars.values();
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
	
	public static Set<DeliveryTask> getDelivTasks(){
		Set<DeliveryTask> set = new HashSet<Delivery.DeliveryTask>();
		set.addAll(Delivery.searchTasks);
		set.addAll(Delivery.deliveryTasks);
		return set;
	}
}
