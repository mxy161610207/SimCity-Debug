package nju.ics.lixiaofan.sensor;

import java.util.Random;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.StateSwitcher;
import nju.ics.lixiaofan.resource.Resource;

/**
 * generate random sensor data
 * @author leslie
 *
 */
public class RandomDataGenerator implements Runnable{
	Random random = new Random();
	public void run() {
		Thread curThread = Thread.currentThread();
		StateSwitcher.register(curThread);
		while(true){
			int idx = random.nextInt(Resource.getConnectedCars().size());
			int count = 0;
			for(Car car : Resource.getConnectedCars()){
				if(count == idx){
					//send out random data to cause FP
					if(car.loc != null && car.dir != TrafficMap.UNKNOWN_DIR){
						Sensor sensor = car.loc.adjSensors.get(car.dir);
						if(sensor != null){
							int reading = 0;
							BrickHandler.add(sensor.bid, sensor.sid, reading);
							System.err.println(sensor.name + ": reading: " + reading);
						}
					}
					break;
				}
				else
					count++;
			}
			
			
			try {
				Thread.sleep(random.nextInt(10)*1000);
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}
		}
	}

}
