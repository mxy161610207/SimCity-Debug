package nju.xiaofanli;

import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.SelfCheck;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.sensor.BrickHandler;
import nju.xiaofanli.device.sensor.Sensor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * NORMAL <-> RESET, RELOCATE, SUSPEND
 * <p>
 * RESET, RELOCATE <-> SUSPEND
 * @author leslie
 *
 */
public class StateSwitcher {
    private static volatile State state = State.NORMAL;
    private static ConcurrentHashMap<Thread, Boolean> threadStatus = new ConcurrentHashMap<>();//reset or not

    private enum State {NORMAL, RESET, SUSPEND, RELOCATE}

    private StateSwitcher(){}

    public static boolean isNormal(){
        return state == State.NORMAL;
    }

    public static boolean isResetting(){
        return state == State.RESET;
    }

    public static boolean isSuspending(){
        return state == State.SUSPEND;
    }

    public static boolean isRelocating() {
        return state == State.RELOCATE;
    }

    private static void setState(State s){
        //noinspection SynchronizeOnNonFinalField
        synchronized (state) {
            state = s;
        }
    }

    public static void register(Thread thread){
        threadStatus.put(thread, false);
    }

    public static void unregister(Thread thread){
        threadStatus.remove(thread);
        if(isResetting() && allReset())
            wakeUp(ResetTask.OBJ);
    }

    public static boolean isThreadReset(Thread thread){
        if(threadStatus.get(thread))
            return true;
        threadStatus.put(thread, true);
        if(isResetting() && allReset())
            wakeUp(ResetTask.OBJ);
        return false;
    }

    private static boolean allReset(){
        for(Boolean b : threadStatus.values()){
            if(!b)
                return false;
        }
        return true;
    }

    private static void resetThreadStatus(){
        for(Thread t : threadStatus.keySet())
            threadStatus.put(t, false);
    }

    private static void interruptAll(){
        threadStatus.keySet().forEach(Thread::interrupt);
    }

    private static void wakeUp(final Object obj){
        synchronized (obj) {
            obj.notify();
        }
    }

    public static void startResetting(){
        Resource.execute(resetTask);
    }

    public static void setInconsistencyType(boolean isReal){
        resetTask.isRealInconsistency = isReal;
    }

    public static void setLastStopCmdTime(long time){
        resetTask.lastStopCmdTime = time;
    }

    public static void detectedBy(Sensor sensor) {
        if (isResetting())
            ResetTask.detectedBy(sensor);
        else if (isRelocating())
            Relocation.detectedBy(sensor);
    }

    private static ResetTask resetTask = new ResetTask();
    private static class ResetTask implements Runnable {
        private static final Object OBJ = new Object();
        private final long maxWaitingTime = 1000;
        private Set<Car> cars2locate = new HashSet<>();
        private Map<Car, CarInfo> carInfo = new HashMap<>();
        boolean isRealInconsistency;//real inconsistency
        private Car car2locate = null;
        private long lastStopCmdTime;
        private Set<Car> locatedCars = new HashSet<>();

        private ResetTask() {
        }

        public void run() {
            checkIfSuspended();
            setState(State.RESET);
            Dashboard.enableCtrlUI(false);
            //first step: stop the world
            interruptAll();
            Command.stopAllCars();
            Command.silenceAllCars();
            if(isRealInconsistency){//all cars need to be located
                cars2locate.addAll(Resource.getConnectedCars());
            }
            else{//Only moving cars and crashed cars need to be located
                for(Car car :Resource.getConnectedCars()){
                    if(car.getState() != Car.STOPPED)
                        cars2locate.add(car);
                    if(car.getRealLoc() != null){
                        Set<Car> crashedCars = new HashSet<>(car.getRealLoc().realCars);
                        crashedCars.addAll(car.getRealLoc().cars.stream().filter(x -> !x.hasPhantom()).collect(Collectors.toList()));

                        if(crashedCars.size() > 1)
                            cars2locate.addAll(crashedCars);
                    }
                }
            }
            //store the info of cars that have no need to locate
            for(Car car :Resource.getConnectedCars()){
                if(cars2locate.contains(car))
                    continue;
                carInfo.put(car, new CarInfo(car.getRealLoc(), car.getRealDir()));
            }

            while(!allReset()){
                try {
                    synchronized (OBJ) {
                        OBJ.wait();//wait for all threads reaching their safe points
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    checkIfSuspended();
                }
            }

            long duration = maxWaitingTime - (System.currentTimeMillis() - lastStopCmdTime);
            while(duration > 0){
                long startTime = System.currentTimeMillis();
                try {
                    Thread.sleep(duration);//wait for all cars to stop
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    checkIfSuspended();
                }
                finally{
                    duration -= System.currentTimeMillis() - startTime;
                }
            }
            //second step: clear all statuses
            checkIfSuspended();
            TrafficMap.reset();
            Middleware.reset();
            Delivery.reset();

            //third step: resolve the inconsistency
            checkIfSuspended();
            //for false inconsistency, just restore its loc and dir
            for(Map.Entry<Car, CarInfo> e : carInfo.entrySet())
                e.getValue().restore(e.getKey());
            //for real inconsistency, locate cars one by one
            for(Car car : cars2locate){
                car2locate = car;
                Command.drive(car);
                while(!locatedCars.contains(car)){
                    try {
                        synchronized (OBJ) {
                            OBJ.wait();// wait for any readings from sensors
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        checkIfSuspended();
                    }
                }

                duration = maxWaitingTime - (System.currentTimeMillis() - lastStopCmdTime);
                while(duration > 0){
                    long startTime = System.currentTimeMillis();
                    try {
                        Thread.sleep(duration);//wait for the car to stop
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        checkIfSuspended();
                    }
                    finally{
                        duration -= System.currentTimeMillis() - startTime;
                    }
                }
            }
            //fourth step: recover the world
            checkIfSuspended();
            resetThreadStatus();
            car2locate = null;
            cars2locate.clear();
            locatedCars.clear();
            carInfo.clear();

            Dashboard.enableCtrlUI(true);
            Dashboard.reset();
            setState(State.NORMAL);

            TrafficMap.checkRealCrash();
        }

        public static void detectedBy(Sensor sensor) {
            if (isResetting()) {
                Car car = resetTask.car2locate;
                if (car != null && car.loc == null) {//still not located, then locate it
                    resetTask.car2locate = null;
                    Command.stop(car);
                    car.initLocAndDir(sensor);
                    resetTask.locatedCars.add(car);
                    wakeUp(ResetTask.OBJ);
                }
            }
        }

        private class CarInfo{
            private Sensor sensor;
            private CarInfo(Road loc, TrafficMap.Direction dir){
                sensor = loc.adjSensors.get(dir).prevSensor;
            }

            private void restore(Car car){
                car.initLocAndDir(sensor);
            }
        }
    }

    private static State prevState = null;
    private static final Lock SUSPEND_LOCK = new ReentrantLock();
    private static final Object SUSPEND_OBJ = new Object();
    private static Set<Car> movingCars = new HashSet<>(), whistlingCars = new HashSet<>();
    private static boolean isSuspended = false;
    public static void suspend(){
        if(!isSuspended && !SelfCheck.allReady()) {
            SUSPEND_LOCK.lock();
            if (!isSuspended && !SelfCheck.allReady()) {
                System.out.println("*** SUSPEND ***");
                isSuspended = true;
                prevState = state;
                setState(State.SUSPEND);
//                try {
//                    Thread.sleep(1000); //wait for working threads to reach their safe points
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                for (Car car : Resource.getConnectedCars()) {
                    if (car.trend == Car.MOVING)
                        movingCars.add(car);
                    Command.stop(car);
                    if (car.lastHornCmd == Command.HORN_ON && car.isHornOn)
                        whistlingCars.add(car);
                    Command.silence(car);
                }
                if (prevState == State.NORMAL)
                    Dashboard.enableCtrlUI(false);
                Dashboard.showDeviceDialog(false);
            }
            SUSPEND_LOCK.unlock();
        }
    }

    public static void resume(){
        if(isSuspended && SelfCheck.allReady()) {
            SUSPEND_LOCK.lock();
            if (isSuspended && SelfCheck.allReady()) {
                System.out.println("*** RESUME ***");
                isSuspended = false;
                Dashboard.closeDeviceDialog();
                if (prevState == State.NORMAL)
                    Dashboard.enableCtrlUI(true);

                movingCars.forEach(Command::drive);
                whistlingCars.forEach(Command::whistle);
                setState(prevState);
                interruptAll(); //must invoked after state changed back, drive all threads away from safe points
                if (prevState == State.RESET || prevState == State.RELOCATE) {
                    synchronized (SUSPEND_OBJ) {
                        SUSPEND_OBJ.notifyAll();
                    }
                }
                movingCars.clear();
                whistlingCars.clear();
                prevState = null;
            }
            SUSPEND_LOCK.unlock();
        }
    }

    private static void checkIfSuspended(){
        while(isSuspending()) {
            try {
                synchronized (SUSPEND_OBJ) {
                    SUSPEND_OBJ.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param car2relocate the car needed to relocate
     * @param sensor the sensor in font of the car's known location
     * @param detected whether car2relocate is detected by the next one of sensor
     */
    public static void startRelocating(Car car2relocate, Sensor sensor, boolean detected) {
        if (isResetting())
            return;
        Relocation.add(car2relocate, sensor, detected);
    }

    public static void startRelocationThread() {
        if (!relocation.isAlive())
            relocation.start();
    }


    private static Relocation relocation = new Relocation();
    public static class Relocation extends Thread {
        private static final Object OBJ = new Object();
        private static final Queue<Request> queue = new LinkedList<>();
        private static final Set<Car> cars2relocate = new HashSet<>();
        private static Car car2relocate = null;
        private static Sensor locatedSensor = null;
        private static final Set<Car> movingCars = new HashSet<>(), whistlingCars = new HashSet<>();
        private static boolean isPreserved = false, isInterested = false, areAllCarsStopped = false;
        private static final Set<Sensor> interestedSensors = new HashSet<>();

        private Relocation(){}
        @Override
        public void run() {
            setName("Relocation Thread");
            //noinspection InfiniteLoopStatement
            while (true) {
                while (queue.isEmpty()) {
                    if (isPreserved) {
                        checkIfSuspended();
                        isPreserved = false;
                        cars2relocate.clear();
                        movingCars.forEach(Command::drive);
                        whistlingCars.forEach(Command::whistle);
                        Dashboard.closeRelocationDialog();
                        Dashboard.enableCtrlUI(true);
                        movingCars.clear();
                        whistlingCars.clear();
                        setState(StateSwitcher.State.NORMAL);
                        System.out.println("switch state to normal");
                        interruptAll();
                    }

                    try {
                        synchronized (queue) {
                            if (queue.isEmpty())
                                queue.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                checkIfSuspended();

//                if (!isPreserved) {
//                    isPreserved = true;
//                    setState(StateSwitcher.State.RELOCATE);
//                    System.out.println("switch state to relocation");
//                    Dashboard.enableCtrlUI(false);
//                    try {
//                        Thread.sleep(1000); //wait for working threads to reach their safe points
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    checkIfSuspended();
//
//                    for (Car car : Resource.getConnectedCars()) {
//                        if (car.trend == Car.MOVING)
//                            movingCars.add(car);
//                        Command.stop(car);
//                        if (car.lastHornCmd == Command.HORN_ON && car.isHornOn)
//                            whistlingCars.add(car);
//                        Command.silence(car);
//                    }
//
//                    try {
//                        Thread.sleep(1000); //wait for all cars to stop
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    checkIfSuspended();
//                }
                if (!areAllCarsStopped) {
                    try {
                        Thread.sleep(1000); // wait for all cars to stop
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    areAllCarsStopped = true;
                    checkIfSuspended();
                }

                Request r;
                synchronized (queue) {
                    r = queue.poll();
                }
                if (r == null || r.car2relocate == null || r.sensor == null)
                    continue;

                if (r.sensor.nextRoad.isStraight.get(r.sensor.getNextRoadDir())
                        && r.sensor.prevRoad.isStraight.get(r.sensor.prevSensor.getNextRoadDir())) { //if both roads are straight, use backward relocation
                    backwardRelocate(r.car2relocate, r.sensor);
                }
                else { //if either one is curved, use forward relocation
                    forwardRelocate(r.car2relocate, r.sensor, true, r.detected);
                }
            }
        }

        private static void forwardRelocate(Car car, Sensor sensor, boolean knownLost, boolean detectedByNextNextSensor) {
            Map<Car, Sensor> relocatedCars = new HashMap<>();
            forwardRelocateRecursively(car, sensor, knownLost, detectedByNextNextSensor, relocatedCars);
            relocatedCars.forEach(BrickHandler::triggerEventAfterEntering); //each car should trigger events when it's detected by the LAST sensor
        }

        private static void forwardRelocateRecursively(Car car, Sensor sensor, boolean knownLost, boolean detectedByNextNextSensor, final Map<Car, Sensor> relocatedCars) {
            checkIfSuspended();
            Road nextRoad = sensor.nextRoad;
            while (!nextRoad.allRealCars.isEmpty()) {
                Car car1 = nextRoad.allRealCars.peek();
                forwardRelocateRecursively(car1, nextRoad.adjSensors.get(car1.getRealDir()), relocatedCars);
                if (!nextRoad.allRealCars.isEmpty())
                    System.err.println("There still is/are car(s) at " + nextRoad.name + " in front of the relocated car " + car.name);
            }

            synchronized (queue) {
                synchronized (cars2relocate) {
                    cars2relocate.add(car);
                }
                for (Iterator<Request> iter = queue.iterator();iter.hasNext();) {
                    Request r = iter.next();
                    if (r.car2relocate == car) {
                        iter.remove();
                        knownLost = true;
                    }
                }
            }

            Sensor nextNextSensor = sensor.nextSensor;
            Sensor nextNextNextSensor = sensor.nextSensor.nextSensor;
            Road nextNextRoad = nextNextSensor.nextRoad;
            Road nextNextNextRoad = nextNextNextSensor.nextRoad;

            if (knownLost) {
                while (!nextNextRoad.allRealCars.isEmpty()) {
                    Car car1 = nextNextRoad.allRealCars.peek();
                    forwardRelocateRecursively(car1, nextNextRoad.adjSensors.get(car1.getRealDir()), relocatedCars);
                    if (!nextNextRoad.allRealCars.isEmpty())
                        System.err.println("There still is/are car(s) at " + nextNextRoad.name);
                }
            }

            if (detectedByNextNextSensor) {
                while (!nextNextNextRoad.allRealCars.isEmpty()) {
                    Car car1 = nextNextNextRoad.allRealCars.peek();
                    forwardRelocateRecursively(car1, nextNextNextRoad.adjSensors.get(car1.getRealDir()), relocatedCars);
                    if (!nextNextNextRoad.allRealCars.isEmpty())
                        System.err.println("2There still is/are car(s) at " + nextNextNextRoad.name);
                }
            }

            car2relocate = car;
            long prevTimeout = sensor.prevRoad.timeouts.get(sensor.prevSensor.getNextRoadDir()).get(car.name);
            long nextTimeout = nextRoad.timeouts.get(sensor.getNextRoadDir()).get(car.name);
            long timeout = knownLost ? Math.max(prevTimeout, nextTimeout) : prevTimeout;
            Dashboard.clearRelocationDialog();
            Dashboard.showRelocationDialog(car);
            //relocate car using sensor or both sensor and sensor.nextSensor if already in relocation
            interestedSensors.clear();
            interestedSensors.add(sensor);
            if (knownLost)
                interestedSensors.add(nextNextSensor);
            if (detectedByNextNextSensor)
                interestedSensors.add(nextNextNextSensor);
            isInterested = true;
            Command.drive(car);
            if (locatedSensor == null) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
            Command.stop(car);
            isInterested = false;
            interestedSensors.clear();
            checkIfSuspended();

            if (locatedSensor == null) { // timeout reached, relocation failed
                Dashboard.showRelocationDialog(car, false, sensor.prevRoad);
                synchronized (OBJ) {
                    try {
                        OBJ.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                checkIfSuspended();
                // the car is manually relocated
                car.timeout = sensor.prevRoad.timeouts.get(sensor.prevSensor.getNextRoadDir()).get(car.name); // reset its timeout
            }
            else {
                Dashboard.showRelocationDialog(car, true, null);
                if (locatedSensor == sensor) {
                    //enter next road
                    sensor.state = Sensor.UNDETECTED;
//                    BrickHandler.switchState(locatedSensor, 0, System.currentTimeMillis());
                    Middleware.checkConsistency(car.name, car.getRealDir(), Car.MOVING, "movement", "enter",
                            sensor.prevRoad.name, nextRoad.name, nextNextRoad.name,
                            System.currentTimeMillis(), car, sensor, car.hasPhantom(), false);

                    relocatedCars.put(car, sensor);
                }
                else if (locatedSensor == sensor.nextSensor) {
                    //enter next road
                    sensor.state = Sensor.UNDETECTED;
                    Middleware.checkConsistency(car.name, car.getRealDir(), Car.MOVING, "movement", "enter",
                            sensor.prevRoad.name, nextRoad.name, nextNextRoad.name,
                            System.currentTimeMillis()-400, car, sensor, car.hasPhantom(), false);
                    //enter next next road
                    sensor.nextSensor.state = Sensor.UNDETECTED;
                    Middleware.checkConsistency(car.name, car.getRealDir(), Car.MOVING, "movement", "enter",
                            nextRoad.name, nextNextRoad.name, nextNextNextRoad.name,
                            System.currentTimeMillis(), car, nextNextSensor, car.hasPhantom(), false);

                    relocatedCars.put(car, nextNextSensor);
                }
                else if (locatedSensor == sensor.nextSensor.nextSensor) {
                    //enter next road
                    sensor.state = Sensor.UNDETECTED;
                    Middleware.checkConsistency(car.name, car.getRealDir(), Car.MOVING, "movement", "enter",
                            sensor.prevRoad.name, nextRoad.name, nextNextRoad.name,
                            System.currentTimeMillis()-800, car, sensor, car.hasPhantom(), false);
                    //enter next next road
                    sensor.nextSensor.state = Sensor.UNDETECTED;
                    Middleware.checkConsistency(car.name, car.getRealDir(), Car.MOVING, "movement", "enter",
                            nextRoad.name, nextNextRoad.name, nextNextNextRoad.name,
                            System.currentTimeMillis()-400, car, nextNextSensor, car.hasPhantom(), false);
                    //enter next next next road
                    sensor.nextSensor.nextSensor.state = Sensor.UNDETECTED;
                    Middleware.checkConsistency(car.name, car.getRealDir(), Car.MOVING, "movement", "enter",
                            nextNextRoad.name, nextNextNextRoad.name, nextNextNextSensor.nextSensor.nextRoad.name,
                            System.currentTimeMillis(), car, nextNextNextSensor, car.hasPhantom(), false);

                    relocatedCars.put(car, nextNextNextSensor);
                }
            }
            car2relocate = null;
            locatedSensor = null;
        }

        private static void forwardRelocateRecursively(Car car, Sensor sensor, final Map<Car, Sensor> relocatedCars) {
            forwardRelocateRecursively(car, sensor, false, false, relocatedCars);
        }

        private static void backwardRelocate(Car car, Sensor sensor) {
            checkIfSuspended();
            car2relocate = car;
            long timeout = (long) (sensor.prevRoad.timeouts.get(sensor.prevSensor.getNextRoadDir()).get(car.name) * 1.2);// car moves slower when moving backward, so better multiply by a factor
            Dashboard.clearRelocationDialog();
            Dashboard.showRelocationDialog(car);
            interestedSensors.clear();
            interestedSensors.add(sensor.prevSensor);
            interestedSensors.add(sensor);
            isInterested = true;
            Command.back(car);
            if (locatedSensor == null) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
//                                e.printStackTrace();
                }
            }
            Command.stop(car);
            isInterested = false;
            interestedSensors.clear();
            checkIfSuspended();

            if (locatedSensor == null) { // timeout reached, relocation failed
                Dashboard.showRelocationDialog(car, false, sensor.prevRoad);
                synchronized (OBJ) {
                    try {
                        OBJ.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                checkIfSuspended();
                // the car is manually relocated
                car.timeout = sensor.prevRoad.timeouts.get(sensor.prevSensor.getNextRoadDir()).get(car.name); // reset its timeout
            }
            else {
                Dashboard.showRelocationDialog(car, true, null);
                if (locatedSensor == sensor) {
                    sensor.state = Sensor.UNDETECTED;
//                    BrickHandler.switchState(locatedSensor, 0, System.currentTimeMillis());
                    Middleware.checkConsistency(car.name, car.getRealDir(), Car.MOVING, "movement", "enter",
                            sensor.prevRoad.name, sensor.nextRoad.name, sensor.nextSensor.nextRoad.name,
                            System.currentTimeMillis(), car, sensor, car.hasPhantom(), true);
                }
                else {
                    car.timeout = sensor.prevRoad.timeouts.get(sensor.prevSensor.getNextRoadDir()).get(car.name); // reset its timeout
                }
            }
            car2relocate = null;
            locatedSensor = null;
        }

        public static void manuallyRelocated() {
            synchronized (OBJ) {
                OBJ.notify();
            }
        }

        public static void detectedBy(Sensor sensor) {
            if (isInterestedSensor(sensor)) {
                isInterested = false;
                interestedSensors.clear();
                Command.stop(Relocation.car2relocate);
                Relocation.locatedSensor = sensor;
                relocation.interrupt();
            }
        }

        public static boolean isInterestedSensor(Sensor sensor) {
            return isRelocating() && isInterested && interestedSensors.contains(sensor);
        }

        public static void add(Car car2relocate, Sensor sensor, boolean detected) {
            synchronized (queue) {
                synchronized (cars2relocate) {
                    if (cars2relocate.contains(car2relocate))
                        return;
                    cars2relocate.add(car2relocate);
                }

//                setState(StateSwitcher.State.RELOCATE);
//                System.out.println("switch state to relocation");
                if (!isPreserved) {
                    isPreserved = true;
                    setState(StateSwitcher.State.RELOCATE);
                    System.out.println("switch state to relocation");
                    for (Car car : Resource.getConnectedCars()) {
                        if (car.trend == Car.MOVING)
                            movingCars.add(car);
                        Command.stop(car);
                        if (car.lastHornCmd == Command.HORN_ON && car.isHornOn)
                            whistlingCars.add(car);
                        Command.silence(car);
                    }
                    areAllCarsStopped = false;
                    Dashboard.enableCtrlUI(false);
                }

                queue.add(new Request(car2relocate, sensor, detected));
                queue.notifyAll();
            }
        }

        private static class Request {
            private Car car2relocate;
            private Sensor sensor;
            private boolean detected;
            Request(Car car2relocate, Sensor sensor, boolean detected) {
                this.car2relocate = car2relocate;
                this.sensor = sensor;
                this.detected = detected;
            }
        }
    }
}
