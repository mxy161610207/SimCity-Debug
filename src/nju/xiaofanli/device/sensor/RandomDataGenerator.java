package nju.xiaofanli.device.sensor;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.city.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.device.car.Car;

import java.util.*;

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
        List<Sensor> enabled = new ArrayList<>();
        Set<Sensor> disabled = new HashSet<>();
		//noinspection InfiniteLoopStatement
		while(true){
			if(StateSwitcher.isNormal() && (Middleware.isDetectionEnabled() || Middleware.isResolutionEnabled())) {
                enabled.clear();
                disabled.clear();
                Resource.getConnectedCars().stream().filter(car -> car.loc != null && car.dir != TrafficMap.UNKNOWN_DIR).forEach(car -> {
                    if (car.hasPhantom()) {
                        enabled.add(car.loc.adjSensors.get(car.dir));
                        if (car.getState() != Car.STOPPED)
                            disabled.add(car.getRealLoc().adjSensors.get(car.getRealDir()));
                    }
                    else {
                        if (car.getState() == Car.STOPPED)
                            enabled.add(car.loc.adjSensors.get(car.dir));
                        else
                            disabled.add(car.loc.adjSensors.get(car.dir));
                    }
                });
                enabled.removeAll(disabled);

                if(!enabled.isEmpty()) {
                    Sensor sensor = enabled.get(random.nextInt(enabled.size()));
                    BrickHandler.add(sensor.bid, sensor.sid, 0, System.currentTimeMillis());
//                System.err.println(sensor.name + ": reading: " + reading);
                }
            }
			try {
				Thread.sleep(random.nextInt(5000)+300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
