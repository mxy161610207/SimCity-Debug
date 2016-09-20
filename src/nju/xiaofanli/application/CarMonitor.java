package nju.xiaofanli.application;

import nju.xiaofanli.city.Citizen;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventListener;

public class CarMonitor implements EventListener{
    public void eventTriggered(Event event) {
        switch (event.type) {
            case CAR_START_LOADING:{
                Car car = Car.carOf(event.car);
                if(car.dt != null && car.dt.citizen != null && car.dt.citizen.state == Citizen.Activity.HailATaxi){
                    Citizen citizen = car.dt.citizen;
                    car.passengers.add(citizen);
                    citizen.car = car;
                    citizen.setActivity(Citizen.Activity.TakeATaxi);
                    Dashboard.appendLog(car.name + " picks up passenger "+ citizen.name);
                }
                break;
            }
            case CAR_START_UNLOADING:{
                Car car = Car.carOf(event.car);
                if(car.dt != null && car.dt.citizen != null && car.dt.citizen.state == Citizen.Activity.TakeATaxi){
                    Citizen citizen = car.dt.citizen;
                    car.passengers.remove(citizen);
                    citizen.car = null;
                    citizen.loc = car.loc;
                    citizen.setActivity(Citizen.Activity.GetOff);
                    Dashboard.appendLog(car.name + " drops off passenger "+ citizen.name);
                }
                break;
            }
        }
    }
}
