package nju.xiaofanli.application;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.schedule.Police;

public class CarPusher implements Runnable {

    public CarPusher() {
        new Thread(this, "Car Pusher").start();
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            if (StateSwitcher.isNormal()) {
                Resource.getConnectedCars().forEach(car -> {
                    if (car.hasPhantom() && car.getState() == Car.STOPPED
                            && System.currentTimeMillis() - car.stopTime > 4000) {
                        car.notifyPolice(Police.REQUEST2ENTER);
                    }

                });
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
