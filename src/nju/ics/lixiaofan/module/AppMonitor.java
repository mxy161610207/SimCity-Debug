package nju.ics.lixiaofan.module;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventListener;
import nju.ics.lixiaofan.monitor.AppPkg;
import nju.ics.lixiaofan.monitor.PkgHandler;

public class AppMonitor implements EventListener{
	public void eventTriggered(Event event) {
		switch (event.type) {
		case CAR_ENTER:{
			Car car = Car.carOf(event.car);
			PkgHandler.send(new AppPkg(car.name, car.dir, event.location));
			break;
		}
//		case Event.Type.CAR_LEAVE:{
//			Car car = Car.carOf(event.car);
//			PkgHandler.send(new AppPkg(car.uid, car.dir, Section.sectionOf(event.location).uid));
//			break;
//		}
//		case Event.Type.CAR_MOVE:
//			System.out.println("EventListener\tType:Move Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
//		case Event.Type.CAR_STOP:
//			System.out.println("EventListener\tType:Stop Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
//		case Event.Type.CAR_CRASH:
//			String cars = "";
//			for(String car:event.crashedCars)
//				cars += car+" ";
//			System.out.println("EventListener\tType:Crash Cars:"+cars+"Loc:"+event.location+" Time:"+event.time);
//			break;
		case DELIVERY_RELEASED:{
//			System.out.println("EventListener\tType:Deliv rls Src:"+event.dtask.srcSect+" Dst:"+event.dtask.dstSect+" Time:"+event.time);
			AppPkg p = new AppPkg();
			String src = event.dtask.srcSect != null ? event.dtask.srcSect.name : event.dtask.srcB.name;
			String dst = event.dtask.dstSect != null ? event.dtask.dstSect.name : event.dtask.dstB.name;
			p.setDelivery((byte)event.dtask.id, null, src, dst, (byte)event.dtask.phase);
			PkgHandler.send(p);
			break;
		}
		case DELIVERY_COMPLETED:{
//			System.out.println("EventListener\tType:Deliv cpt Src:"+event.dtask.srcSect+" Dst:"+event.dtask.dstSect+" Time:"+event.time);
			AppPkg p = new AppPkg();
			String src = event.dtask.srcSect != null ? event.dtask.srcSect.name : event.dtask.srcB.name;
			String dst = event.dtask.dstSect != null ? event.dtask.dstSect.name : event.dtask.dstB.name;
			p.setDelivery((byte)event.dtask.id, event.dtask.car.name, src, dst, (byte)event.dtask.phase);
			PkgHandler.send(p);
			break;
		}
		case CAR_START_LOADING:{
//			System.out.println("EventListener\tType:Start load Car:"+event.car+" Loc:"+event.location+" Time:"+event.time);
			AppPkg p = new AppPkg();
			p.setLoading(event.car, true);
			PkgHandler.send(p);
			break;
		}
		case CAR_END_LOADING:{
//			System.out.println("EventListener\tType:End load Car:"+event.car+" Loc:"+event.location+" Time:"+event.time);
			AppPkg p = new AppPkg();
			p.setLoading(event.car, false);
			PkgHandler.send(p);
			break;
		}
		case CAR_START_UNLOADING:{
//			System.out.println("EventListener\tType:Start unload Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); 
			AppPkg p = new AppPkg();
			p.setUnloading(event.car, true);
			PkgHandler.send(p);
			break;
		}
		case CAR_END_UNLOADING:{
//			System.out.println("EventListener\tType:End unload Car:"+event.car+" Loc:"+event.location+" Time:"+event.time);
			AppPkg p = new AppPkg();
			p.setUnloading(event.car, false);
			PkgHandler.send(p);
			break;
		}
//		case Event.Type.CAR_SEND_REQUEST:
//			System.out.println("EventListener\tType:Send req Car:"+event.car+" Loc:"+event.location+" Cmd:"+event.cmd+" Time:"+event.time); break;
//		case Event.Type.CAR_RECV_RESPONSE:
//			System.out.println("EventListener\tType:Recv req Car:"+event.car+" Loc:"+event.location+" Cmd:"+event.cmd+" Time:"+event.time); break;
//		case Event.Type.CAR_REACH_DEST:
//			System.out.println("EventListener\tType:Reach dst Car:"+event.car+" Loc:"+event.location+" Time:"+event.time); break;
//		default:
//			System.out.println("EventListener\tUnkown event type"); break;
		default:
			break;
		}
	}
}
