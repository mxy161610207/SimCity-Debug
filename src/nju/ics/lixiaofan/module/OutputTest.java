package nju.ics.lixiaofan.module;

import nju.ics.lixiaofan.context.Context;
import nju.ics.lixiaofan.context.ContextListener;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventListener;

public class OutputTest{
	public EventListener el = new EventListener() {
		public void eventTriggered(Event event) {
			switch (event.type) {
			case CAR_ENTER:
				System.out.println("EventListener\tType:Enter Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
			case CAR_LEAVE:
				System.out.println("EventListener\tType:Leave Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
			case CAR_MOVE:
				System.out.println("EventListener\tType:Move Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
			case CAR_STOP:
				System.out.println("EventListener\tType:Stop Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
			case CAR_CRASH:
				String cars = "";
				for(String car:event.crashedCars)
					cars += car+" ";
				System.out.println("EventListener\tType:Crash Cars:"+cars+"Loc:"+event.location+" Time:"+event.time);
				break;
			case DELIVERY_RELEASED:
				System.out.println("EventListener\tType:Deliv rls Src:"+event.dtask.srcSect+" Dst:"+event.dtask.dstSect+" Time:"+event.time); break;
			case DELIVERY_COMPLETED:
				System.out.println("EventListener\tType:Deliv cpt Src:"+event.dtask.srcSect+" Dst:"+event.dtask.dstSect+" Time:"+event.time); break;
			case CAR_START_LOADING:
				System.out.println("EventListener\tType:Start load Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
			case CAR_END_LOADING:
				System.out.println("EventListener\tType:End load Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
			case CAR_START_UNLOADING:
				System.out.println("EventListener\tType:Start unload Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
			case CAR_END_UNLOADING:
				System.out.println("EventListener\tType:End unload Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
			case CAR_SEND_REQUEST:
				System.out.println("EventListener\tType:Send req Car:"+event.car+" Loc:"+event.location+" Cmd:"+event.cmd+" Time:"+event.time); break;
			case CAR_RECV_RESPONSE:
				System.out.println("EventListener\tType:Recv req Car:"+event.car+" Loc:"+event.location+" Cmd:"+event.cmd+" Time:"+event.time); break;
			case CAR_REACH_DEST:
				System.out.println("EventListener\tType:Reach dst Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
			default:
				System.out.println("EventListener\tUnkown event type"); break;
			}
		}
	};
	
	public ContextListener cl = new ContextListener() {
		public void contextChanged(Context context) {
			System.out.println("ContextListener\tSensor:"+context.sensor+" Car:"+context.car+" Dir:"+context.direction+" Time:"+context.time);
		}
	};
}
