package nju.xiaofanli.device.sensor;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.city.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.context.Context;
import nju.xiaofanli.context.ContextManager;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.car.Remedy;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class BrickHandler extends Thread{
    private static final Queue<RawData> rawData = new LinkedList<>();

    BrickHandler(String name) {
        super(name);
    }

    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        StateSwitcher.register(thread);
        //noinspection InfiniteLoopStatement
        while(true){
            while(rawData.isEmpty() || !StateSwitcher.isNormal()){
                try {
                    synchronized (rawData) {
                        rawData.wait();
                    }
                } catch (InterruptedException e) {
//					e.printStackTrace();
                    if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
                        clearRawData();
                }
            }
//			if(StateSwitcher.isResetting()){
//				if(!StateSwitcher.isThreadReset(thread))
//					clearRawData();
//				continue;
//			}
            RawData data;
            synchronized (rawData) {
                data = rawData.poll();
            }
            SensorManager.trigger(data.sensor, data.reading);
            switchState(data.sensor, data.reading, data.time);
        }
    }

    public static void switchState(Car car, Sensor sensor, boolean isCtxTrue){
        switch(sensor.state){
            case Sensor.UNDETECTED:{
                if(isCtxTrue){
                    sensor.state = Sensor.DETECTED;
                    sensor.car = car;
                    if(sensor.prevSensor.state == Sensor.DETECTED && sensor.prevSensor.car == car){
                        sensor.prevSensor.state = Sensor.UNDETECTED;
                        sensor.prevSensor.car = null;
                    }

                    if(car.hasPhantom()){
                        car.loadRealInfo();
                        PkgHandler.send(new AppPkg().setCarRealLoc(car.name, null));//TODO bug here
                    }
                }
                else if(!car.hasPhantom()){
                    car.saveRealInfo();
                    PkgHandler.send(new AppPkg().setCarRealLoc(car.name, car.getRealLoc().name));
                }
//    			else if(car.loc.sameAs(sensor.nextSection)){
//    				//the phantom already in this section
//    				break;
//    			}

                System.out.println("B"+sensor.bid+"S"+(sensor.sid+1)+" detects car: "+car.name);

                car.enter(sensor.nextSection);
                car.dir = sensor.nextSection.dir[1] == TrafficMap.UNKNOWN_DIR ? sensor.nextSection.dir[0] : sensor.dir;
//                car.state = Car.MOVING;

                Remedy.updateRemedyQWhenDetect(car);

                //do triggered stuff
//			    System.out.println(TrafficMap.nameOf(car.location)+"\t"+TrafficMap.nameOf(car.dest));
                if(car.dest != null){
                    if(car.dest.sameAs(car.loc)){
                        car.finalState = Car.STOPPED;
                        car.notifyPolice(Police.REQUEST2STOP);
                        Dashboard.appendLog(car.name+" reached destination");
                        //trigger reach dest event
                        if(EventManager.hasListener(Event.Type.CAR_REACH_DEST))
                            EventManager.trigger(new Event(Event.Type.CAR_REACH_DEST, car.name, car.loc.name));
                    }
                    else if(car.finalState == Car.STOPPED){
                        car.finalState = Car.MOVING;
                        car.notifyPolice(Police.REQUEST2ENTER);
                        Dashboard.appendLog(car.name+" failed to stop at dest, keep going");
                    }
                    else
                        car.notifyPolice(car.lastCmd == Command.MOVE_FORWARD ? Police.REQUEST2ENTER : Police.REQUEST2STOP);
                }
                else
                    car.notifyPolice(car.lastCmd == Command.MOVE_FORWARD ? Police.REQUEST2ENTER : Police.REQUEST2STOP);

                //trigger context
                if(ContextManager.hasListener())
                    ContextManager.trigger(new Context(""+sensor.bid +(sensor.sid+1), car.name, car.getDirStr()));
            }
            break;
        }
    }

    private static void switchState(Sensor sensor, int reading, long time){
        switch(sensor.state){
//            case Sensor.INITIAL:
//                if(sensor.entryDetected(reading))
//                    sensor.state = Sensor.DETECTED;
//                else if(sensor.leaveDetected(reading))
//                    sensor.state = Sensor.UNDETECTED;
//                break;
            case Sensor.DETECTED:
                if(sensor.leaveDetected(reading)){
                    if(sensor.car != null && !sensor.car.hasPhantom()
                            && sensor.car.loc.sameAs(sensor.nextSection)
                            && sensor.car.state == Car.STOPPED){ // just a simple condition to judge FP
                        System.out.println(sensor.name + " !!!FALSE POSITIVE!!!" +"\treading: " + reading);
                        break;
                    }
                    sensor.state = Sensor.UNDETECTED;
                    sensor.car = null;
//                    System.out.println(sensor.name + " LEAVING!!!" + "\treading: " + reading);
                }
                break;
            case Sensor.UNDETECTED:
                if(sensor.entryDetected(reading)){
                    Car car = null;
                    int dir = TrafficMap.UNKNOWN_DIR, state = Car.STOPPED;
                    //check real cars first
                    for(Car realCar : sensor.prevSection.realCars){
                        if(realCar.realDir == sensor.dir){
                            car = realCar;
                            dir = realCar.realDir;
                            state = realCar.realState;
                            break;
                        }
                    }
                    if(car == null){
                        for(Car tmp : sensor.prevSection.cars){
                            if(tmp.dir == sensor.dir){
                                car = tmp;
                                dir = car.dir;
                                state = car.state;
                                break;
                            }
                        }
                    }
                    if(car == null){
                        System.out.println(sensor.name + ": Cannot find any car!\treading: "+reading);
                        sensor.state = Sensor.UNDETECTED;
                        break;
                    }
//                    System.out.println(sensor.name + " ENTERING!!!" + "\treading: " + reading);

                    Middleware.add(car.name, dir, state, "movement", "enter",
                            sensor.prevSection.name, sensor.nextSection.name, time, car, sensor);
                }
                break;
        }
    }

    /**
     * This method is only called in resetting phase and will locate cars
     */
    private static void switchStateWhenResetting(Sensor sensor, int reading){
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

    public static void add(int bid, int sid, int reading, long time){
        Sensor sensor = Resource.getSensors().get(bid).get(sid);
        sensor.reading = reading;
        if(StateSwitcher.isResetting()){
            switchStateWhenResetting(sensor, reading);
            return;
        }
        RawData datum = new RawData(sensor, reading, time);
        synchronized (rawData) {
            rawData.add(datum);
            rawData.notify();
        }
    }

    private static void clearRawData(){
        synchronized (rawData) {
            rawData.clear();
        }
    }

    private static class RawData{
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
