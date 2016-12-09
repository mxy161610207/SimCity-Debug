package nju.xiaofanli.device.sensor;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.context.Context;
import nju.xiaofanli.context.ContextManager;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.car.Remedy;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

import java.util.*;

public class BrickHandler extends Thread{
    private static final List<RawData> rawData = new LinkedList<>();
    private static final Set<Sensor> sensors2handle = new HashSet<>();

    BrickHandler(String name) {
        super(name);

        Runnable countdownThread = () -> {
            long start = System.currentTimeMillis();
            //noinspection InfiniteLoopStatement
            while (true) {
                if (StateSwitcher.isNormal()) {
                    int elapsed = (int) (System.currentTimeMillis() - start);
                    start = System.currentTimeMillis();
                    for (Car car : Resource.getConnectedCars()) {
                        if (car.trend == Car.STOPPED)
                            continue;
//						System.out.println(car.name + " " + car.timeout);
                        car.timeout -= elapsed;
                        if (car.timeout < 0) {
                            Sensor nextSensor = car.getRealLoc().adjSensors.get(car.getRealDir());
                            Sensor prevSensor = nextSensor.prevSensor;

                            if (prevSensor.state == Sensor.DETECTED && prevSensor.car == car) {
                                car.timeout = car.getRealLoc().timeouts.get(car.getRealDir()).get(car.name); // reset timeout
                            }
                            else if (!sensors2handle.contains(nextSensor) && !sensors2handle.contains(nextSensor.nextSensor)) {
                                //if there are unhandled raw data about interested sensor, then no hurry to relocate
                                car.timeout = Integer.MAX_VALUE; //avoid relocating this repeatedly
                                System.out.println("[" + nextSensor.name + "] Timeout relocate " + car.name + "\t" + start);
                                StateSwitcher.startRelocating(car, nextSensor, false);
                            }
                        }

                    }
                }
                else
                    start = System.currentTimeMillis();

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
//					e.printStackTrace();
                }
            }
        };
        new Thread(countdownThread, "Countdown Thread").start(); //TODO enable timeout
    }

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        StateSwitcher.register(thread);
        //noinspection InfiniteLoopStatement
        while(true){
            synchronized (rawData) { // in suspension and relocation phase, this handler still runs to exhaust rawdata queue
                while(rawData.isEmpty() || StateSwitcher.isResetting()) {
                    try {
                        rawData.wait();
                    } catch (InterruptedException e) {
//					e.printStackTrace();
                        if (StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread)) {
                            clearRawData();
                        }
                    }
                }
            }

            RawData data;
            synchronized (rawData) {
                data = rawData.remove(0);
            }
            switchState(data.sensor, data.reading, data.time);
            sensors2handle.remove(data.sensor);
            SensorManager.trigger(data.sensor, data.reading);
        }
    }

    public static void switchState(Car car, Sensor sensor, boolean isRealCar, boolean isTrueCtx, boolean triggerSth) {
        switch(sensor.state){
            case Sensor.UNDETECTED:{
                if(isTrueCtx){
                    sensor.state = Sensor.DETECTED;
                    sensor.car = car;
                    if(sensor.prevSensor.state == Sensor.DETECTED && sensor.prevSensor.car == car){
                        sensor.prevSensor.state = Sensor.UNDETECTED;
                        sensor.prevSensor.car = null;
                    }

                    if (isRealCar) { //real car entered
                        car.setRealInfo(sensor.nextRoad, sensor.getNextRoadDir());
                        break;
                    }
                }
                else {
                    if (isRealCar) //abandon false context triggered by real (invisible) car
                        break;
                    else if(!car.hasPhantom()) {
                        car.saveRealInfo();
                        PkgHandler.send(new AppPkg().setCarRealLoc(car.name, car.getRealLoc().name));
                    }
                }

                System.out.println("["+sensor.name+"] DETECT "+car.name);
                car.enter(sensor.nextRoad, sensor.getNextRoadDir());
                Remedy.updateRemedyQWhenDetect(car);
                if (triggerSth) { //do triggered stuff
                    if (car.dest == car.loc) {
                        car.notifyPolice(Police.REQUEST2STOP);
                        //trigger reach dest event
                        if (EventManager.hasListener(Event.Type.CAR_REACH_DEST))
                            EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
                    }
                    else
                        car.notifyPolice(car.lastCmd == Command.MOVE_FORWARD ? Police.REQUEST2ENTER : Police.REQUEST2STOP);

                    //trigger context
                    if (ContextManager.hasListener())
                        ContextManager.trigger(new Context(sensor.name, car.name, car.getDirStr()));
                }
            }
            break;
        }
    }

    private static void switchState(Sensor sensor, int reading, long time){
        switch(sensor.state){
            case Sensor.DETECTED:
                if(sensor.leaveDetected(reading)){
                    if(sensor.car != null && !sensor.car.hasPhantom() && sensor.car.loc == sensor.nextRoad && sensor.car.state == Car.STOPPED){ // just a simple condition to judge FP
                        System.out.println("[" + sensor.name + "] !!!FALSE POSITIVE!!!" +"\treading: " + reading + "\t" + time);
                        break;
                    }
                    sensor.state = Sensor.UNDETECTED;
                    sensor.car = null;
                    System.out.println("[" + sensor.name + "] LEAVING!!!" + "\treading: " + reading + "\t" + time);
                }
                break;
            case Sensor.UNDETECTED:
                if(sensor.entryDetected(reading)){
                    Car car = null;
                    TrafficMap.Direction dir = TrafficMap.Direction.UNKNOWN;
                    boolean isRealCar = false;
                    //check real cars first
                    for(Car realCar : sensor.prevRoad.realCars){
                        if(realCar.getRealDir() == sensor.dir){
                            isRealCar = true;
                            car = realCar;
                            dir = realCar.getRealDir();
                            break;
                        }
                    }
                    if(car == null){
                        for(Car tmp : sensor.prevRoad.cars){
                            if(tmp.dir == sensor.dir){
                                isRealCar = false;
                                car = tmp;
                                dir = car.dir;
                                break;
                            }
                        }
                    }
                    if(car == null) {
                        //checking if it's FN
                        Sensor prevSensor = sensor.prevSensor;
                        Sensor prevPrevSensor = prevSensor.prevSensor;
                        for(Car realCar : prevSensor.prevRoad.realCars) {
                            if(realCar.getRealDir() == prevSensor.dir && realCar.getState() == Car.MOVING
                                    && (prevPrevSensor.state != Sensor.DETECTED || prevPrevSensor.car != realCar)) {
                                car = realCar;
                                break;
                            }
                        }
                        if (car == null) {
                            for(Car tmp : prevSensor.prevRoad.cars) {
                                if(!tmp.hasPhantom() && tmp.dir == prevSensor.dir && tmp.getState() == Car.MOVING
                                        && (prevPrevSensor.state != Sensor.DETECTED || prevPrevSensor.car != tmp)) {
                                    car = tmp;
                                    break;
                                }
                            }
                        }

                        if (car == null)  {
                            System.out.println("[" + sensor.name + "] Cannot find any car!\treading: " + reading + "\t" + time);
                            sensor.state = Sensor.UNDETECTED;
                            break;
                        }
                        else {
                            System.out.println("[" + sensor.name + "] Relocate " + car.name + "\t" + time);
                            synchronized (rawData) {
                                for (ListIterator<RawData> iter = rawData.listIterator();iter.hasNext();) {
                                    RawData data = iter.next();
                                    if (data.sensor == sensor)
                                        iter.remove(); // remove all raw data of the same sensor, make sure not trigger relocation repeatedly
                                }
                            }
                            StateSwitcher.startRelocating(car, prevSensor, true);
                            break;
                        }
                    }
                    System.out.println("[" + sensor.name + "] ENTERING!!!" + "\treading: " + reading + "\t" + time);

                    Middleware.checkConsistency(car.name, dir, car.state, "movement", "enter", sensor.prevRoad.name,
                            sensor.nextRoad.name, sensor.nextSensor.nextRoad.name, time, car, sensor, isRealCar, true);
                }
                break;
        }
    }

    /**
     * This method is only called in resetting or relocation phase and will locate cars
     */
    private static void switchStateWhenLocating(Sensor sensor, int reading){
        switch(sensor.state){
            case Sensor.DETECTED:
                if(sensor.leaveDetected(reading))
                    sensor.state = Sensor.UNDETECTED;
                break;
            case Sensor.UNDETECTED:
                if(sensor.entryDetected(reading)){
                    sensor.state = Sensor.DETECTED;
                    StateSwitcher.detectedBy(sensor);
                }
                break;
        }
    }

    private static Comparator<RawData> comparator = (o1, o2) -> (int) (o1.time - o2.time);
    public static void insert(int bid, int sid, int reading, long time){
        insert(Resource.getSensor(bid, sid), reading, time);
    }

    /**
     * In suspension phase, all readings will be discarded.
     * In relocation phase, all readings except those from interested sensors will be discarded.
     */
    public static void insert(Sensor sensor, int reading, long time) {
        if (sensor == null)
            return;
        sensor.reading = reading;
        if (StateSwitcher.isNormal()) {
            RawData datum = new RawData(sensor, reading, time);
            synchronized (rawData) {
                sensors2handle.add(sensor);
                int pos = Collections.binarySearch(rawData, datum, comparator);
                if(pos < 0)
                    pos = -pos - 1;
                rawData.add(pos, datum);
                rawData.notify();
            }
        }
        else if(StateSwitcher.isResetting()) {
            switchStateWhenLocating(sensor, reading);
        }
        else if (StateSwitcher.isRelocating()) {
            if (StateSwitcher.Relocation.isInterestedSensor(sensor))
                switchStateWhenLocating(sensor, reading);
        }
    }

    private static void clearRawData() {
        synchronized (rawData) {
            rawData.clear();
            sensors2handle.clear();
        }
    }

    private static class RawData {
        Sensor sensor;
        int reading;
        long time;
        RawData(Sensor sensor, int reading, long time) {
            this.sensor = sensor;
            this.reading = reading;
            this.time = time;
        }
    }
}
