package nju.xiaofanli.application;

import nju.xiaofanli.city.Citizen;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventListener;

import java.awt.*;
import java.util.Arrays;

public class CarLoadingMonitor implements EventListener{
    public void eventTriggered(Event event) {
        switch (event.type) {
            case CAR_START_LOADING:{
                Car car = Car.carOf(event.car);
                if(car.dt != null && car.dt.citizen != null && car.dt.citizen.state == Citizen.Action.HailATaxi){
                    Citizen citizen = car.dt.citizen;
                    car.passengers.add(citizen);
                    citizen.car = car;
                    citizen.setAction(Citizen.Action.TakeATaxi);
                    Dashboard.log(Arrays.asList(car.name, " picks up ", citizen.name, " at ",  car.loc.name, "\n"),
                            Arrays.asList(car.icon.color, Color.BLACK, citizen.icon.color, Color.BLACK, Color.GRAY));
                }
                break;
            }
            case CAR_START_UNLOADING:{
                Car car = Car.carOf(event.car);
                if(car.dt != null && car.dt.citizen != null && car.dt.citizen.state == Citizen.Action.TakeATaxi){
                    Citizen citizen = car.dt.citizen;
                    car.passengers.remove(citizen);
                    citizen.car = null;
                    citizen.loc = car.loc;
                    citizen.setAction(Citizen.Action.GetOff);
                    Dashboard.log(Arrays.asList(car.name, " drops off ", citizen.name, " at ",  car.loc.name, "\n"),
                            Arrays.asList(car.icon.color, Color.BLACK, citizen.icon.color, Color.BLACK, Color.GRAY));
                }
                break;
            }
        }
    }
}
