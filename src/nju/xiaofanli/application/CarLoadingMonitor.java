package nju.xiaofanli.application;

import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventListener;

public class CarLoadingMonitor implements EventListener{
    public void eventTriggered(Event event) {
        switch (event.type) {
            case CAR_END_LOADING:{

            }
            break;
            case CAR_END_UNLOADING:{

            }
            break;
        }
    }
}
