package nju.xiaofanli.device.sensor;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.city.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.device.car.Car;

import java.util.Random;

/**
 * generate random sensor data
 * @author leslie
 *
 */
public class RandomDataGenerator implements Runnable{
	private Random random = new Random();
	public RandomDataGenerator(){
        new Thread(this, "Random Data Generator").start();
    }
	public void run() {
		//noinspection InfiniteLoopStatement
		while(true){
			if(StateSwitcher.isNormal() && (Middleware.isDetectionEnabled() || Middleware.isResolutionEnabled())) {
				int idx = random.nextInt(Resource.getConnectedCars().size());
				int count = 0;
				for (Car car : Resource.getConnectedCars()) {
					if (count == idx) {
						//send out random data to cause FP
						if (car.loc != null && car.dir != TrafficMap.UNKNOWN_DIR) {
							Sensor sensor = car.loc.adjSensors.get(car.dir);
							if (sensor != null) {
								int reading = 0;
								BrickHandler.add(sensor.bid, sensor.sid, reading, System.currentTimeMillis());
//							System.err.println(sensor.name + ": reading: " + reading);
							}
						}
						break;
					} else
						count++;
				}
			}
			
			try {
				Thread.sleep(random.nextInt(10)*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
