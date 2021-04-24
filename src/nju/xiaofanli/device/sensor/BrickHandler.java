package nju.xiaofanli.device.sensor;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.context.Context;
import nju.xiaofanli.context.ContextManager;
import nju.xiaofanli.schedule.Police;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.car.Remedy;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

import java.io.*;
import java.util.*;

public class BrickHandler extends Thread{
    private static final List<RawData> rawData = new LinkedList<>();
    private static final Map<Sensor, int[]> sensors2handle = new HashMap<>();

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
                                car.timeout = car.getRealLoc().timeouts.get(car.getRealDir()).get(car.url); // reset timeout
                            }
                            else if (!sensors2handle.containsKey(nextSensor) && !sensors2handle.containsKey(nextSensor.nextSensor)) {
                                //if there are unhandled raw data about interested sensor, then no hurry to relocate
                                car.timeout = Integer.MAX_VALUE; //avoid relocating this repeatedly

                                //mxy_edit: output relocate log to file
                                String s= new String("[" + nextSensor.name + "] Timeout relocate " + car.name + "\t" + start + "\n");
                                byte[] content = s.getBytes();

                                //-------------Car Name.txt--------------------------
                                File f= new File("mxy_temp\\"+car.name+".txt");
                                try (FileOutputStream fop = new FileOutputStream(f,true)){
                                    if(!f.exists()){
                                        f.createNewFile();
                                    }
                                    fop.write(content);
                                    fop.flush();
                                    fop.close();
                                }catch (IOException e){
                                    e.printStackTrace();
                                }
                                //-------------Sensor.txt--------------------------
//                                f= new File("mxy_temp\\Sensor.txt");
//                                try (FileOutputStream fop = new FileOutputStream(f,true)){
//                                    if(!f.exists()){
//                                        f.createNewFile();
//                                    }
//                                    fop.write(content);
//                                    fop.flush();
//                                    fop.close();
//                                }catch (IOException e){
//                                    e.printStackTrace();
//                                }
                                // == EDIT END ==

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
            synchronized (sensors2handle) {
                if (sensors2handle.containsKey(data.sensor)) {
                    sensors2handle.get(data.sensor)[0]--;
                    if (sensors2handle.get(data.sensor)[0] == 0)
                        sensors2handle.remove(data.sensor);
                }
            }
            SensorManager.trigger(data.sensor, data.reading);
        }
    }

    public static void switchState(Car car, Sensor sensor, long time, boolean isRealCar, boolean isTrueCtx, boolean triggerEvent) {
        switch(sensor.state){
            case Sensor.UNDETECTED:{
                if(isTrueCtx){
                    sensor.state = Sensor.DETECTED;
                    sensor.car = car;
                    car.lastDetectedTime = time;
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

                System.out.println("["+sensor.name+"] DETECT "+car.name+"\ttime: "+time);
                car.enter(sensor.nextRoad, sensor.getNextRoadDir());
                Remedy.updateRemedyQWhenDetect(car);
                if (triggerEvent) //do triggered stuff
                    triggerEventAfterEntering(car, sensor);
            }
            break;
        }
    }

    public static void triggerEventAfterEntering(Car car, Sensor sensor) {
        if (car.dest == car.loc) {
            car.notifyPolice(Police.REQUEST2STOP, Dashboard.isNoisyScenarioEnabled() ? 100 : 0);
            //trigger reach dest event
            if (EventManager.hasListener(Event.Type.CAR_REACH_DEST))
                EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
        }
        else {
//            car.notifyPolice(car.lastCmd == Command.MOVE_FORWARD ? Police.REQUEST2ENTER : Police.REQUEST2STOP);
            if (car.lastCmd == Command.MOVE_FORWARD)
                car.notifyPolice(Police.REQUEST2ENTER);
            else
                car.notifyPolice(Police.REQUEST2STOP, Dashboard.isNoisyScenarioEnabled() ? 150 : 0);
        }

        //trigger context
        if (ContextManager.hasListener())
            ContextManager.trigger(new Context(sensor.name, car.name, car.getDirStr()));
    }

    private static void switchState(Sensor sensor, int reading, long time){
        switch(sensor.state){
            case Sensor.DETECTED:
                if(sensor.leaveDetected(reading)){
                    if(sensor.car != null && sensor.car.getRealLoc() == sensor.nextRoad && sensor.car.getRealDir() == sensor.dir
                            && sensor.car.getState() == Car.STOPPED){ // just a simple condition to judge FP
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
                    boolean isRealCar = false;
                    for (Car car2 : sensor.prevRoad.carsWithoutFake) {
                        if(car2.getRealDir() == sensor.dir){
                            isRealCar = sensor.prevRoad.realCars.contains(car2);
                            car = car2;
                            break;
                        }
                    }

                    if (car == null) {
                        Set<Car> fakeCars = new HashSet<>(sensor.prevRoad.cars);
                        fakeCars.removeAll(sensor.prevRoad.carsWithoutFake);
                        for (Car car2 : fakeCars) {
                            if(car2.dir == sensor.dir){
                                isRealCar = false;
                                car = car2;
                                break;
                            }
                        }
                    }

                    if(car == null) {
                        Sensor prevSensor = sensor.prevSensor;
                        for (Car car2 : prevSensor.prevRoad.carsWithoutFake) {
                            if(car2.getRealDir() == prevSensor.dir){
                                isRealCar = prevSensor.prevRoad.realCars.contains(car2);
                                car = car2;
                                break;
                            }
                        }

                        if (car == null) {
                            Set<Car> fakeCars = new HashSet<>(prevSensor.prevRoad.cars);
                            fakeCars.removeAll(prevSensor.prevRoad.carsWithoutFake);
                            for (Car car2 : fakeCars) {
                                if(car2.dir == prevSensor.dir){
                                    isRealCar = false;
                                    car = car2;
                                    break;
                                }
                            }
                        }
                    }

                    if (car == null)  {
                        System.out.println("[" + sensor.name + "] Car not found!\treading: " + reading + "\t" + time);
                        sensor.state = Sensor.UNDETECTED;
                    }
                    else {
                        System.out.println("[" + sensor.name + "] ENTERING!!!" + "\treading: " + reading + "\t" + time);
                        Middleware.checkConsistency(car.name, car.getState(), sensor.prevRoad.name,
                                sensor.nextRoad.name, sensor.nextSensor.nextRoad.name, time, car, sensor, isRealCar, true);
                    }
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
     * In normal phase, all readings will be discarded if the scenario is disabled.
     * In suspension phase, all readings will be discarded.
     * In relocation phase, only handle readings from interested sensors.
     */
    public static void insert(Sensor sensor, int reading, long time) {
        if (sensor == null)
            return;
        sensor.reading = reading;
        if (StateSwitcher.isNormal()) {
            if (Dashboard.isScenarioEnabled()) {
                synchronized (sensors2handle) {
                    if (!sensors2handle.containsKey(sensor))
                        sensors2handle.put(sensor, new int[]{1});
                    else
                        sensors2handle.get(sensor)[0]++;
                }
                RawData datum = new RawData(sensor, reading, time);
                synchronized (rawData) {
                    int pos = Collections.binarySearch(rawData, datum, comparator);
                    if (pos < 0)
                        pos = -pos - 1;
                    rawData.add(pos, datum);
                    rawData.notify();
                }
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
        }
        synchronized (sensors2handle) {
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
