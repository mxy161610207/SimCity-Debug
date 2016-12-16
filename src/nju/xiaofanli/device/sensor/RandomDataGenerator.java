package nju.xiaofanli.device.sensor;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.util.StyledText;

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
        Set<Sensor> enabled = new HashSet<>();
        Map<Sensor, Car> disabled = new HashMap<>();
		//noinspection InfiniteLoopStatement
		while(true){
			if(StateSwitcher.isNormal() && (Middleware.isDetectionEnabled() || Middleware.isResolutionEnabled()) && !TrafficMap.crashOccurred) {
                enabled.clear();
                disabled.clear();
                Resource.getConnectedCars().stream().filter(car -> car.loc != null && car.dir != TrafficMap.Direction.UNKNOWN).forEach(car -> {
                    if (car.hasPhantom()) {
                        enabled.add(car.loc.adjSensors.get(car.dir));
                        if (car.getState() != Car.STOPPED)
                            disabled.put(car.getRealLoc().adjSensors.get(car.getRealDir()), car);
//                        else
//                            enabled.add(car.getRealLoc().adjSensors.get(car.getRealDir()));
                    }
                    else {
                        if (car.getState() == Car.STOPPED)
                            enabled.add(car.loc.adjSensors.get(car.dir));
                        else
                            disabled.put(car.loc.adjSensors.get(car.dir), car);
                    }
                });
                enabled.removeAll(disabled.keySet());
                if (disabled.isEmpty() || !Middleware.isResolutionEnabled() || random.nextInt(enabled.size() + disabled.size()) < enabled.size()) {
                    if (!enabled.isEmpty()) {
                        Sensor sensor = (Sensor) enabled.toArray()[random.nextInt(enabled.size())];
                        BrickHandler.insert(sensor, 0, System.currentTimeMillis());
                        BrickHandler.insert(sensor, 30, System.currentTimeMillis()+100);
                    }
                }
                else {
                    Sensor sensor = (Sensor) disabled.keySet().toArray()[random.nextInt(disabled.size())];
                    Car car = disabled.get(sensor);
                    sensor.showBalloon(Context.FP, car.name, Middleware.isResolutionEnabled());
                    if (Middleware.isResolutionEnabled()) {
                        StyledText text = new StyledText();
                        text.append("Fixed a sensor error (sensor ").append(sensor.name, Resource.LIGHT_SKY_BLUE).append(" detected ")
                                .append(car.name, car.icon.color).append(").\n");
                        Dashboard.log(text);
                    }
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
