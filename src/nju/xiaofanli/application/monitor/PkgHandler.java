package nju.xiaofanli.application.monitor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import nju.xiaofanli.control.Police;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.dashboard.Building;
import nju.xiaofanli.dashboard.Citizen;
import nju.xiaofanli.dashboard.Location;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.Resource;

public class PkgHandler implements Runnable{
	private static final Queue<AppPkg> queue = new LinkedList<>();
	private static Sender sender = new Sender();
	private static List<Socket> sockets = null;
	private static HashMap<Socket, ObjectInputStream> in = null;
	private static HashMap<Socket, ObjectOutputStream> out = null;
	PkgHandler(List<Socket> sockets, HashMap<Socket, ObjectInputStream> in, HashMap<Socket, ObjectOutputStream> out) {
		PkgHandler.sockets = sockets;
		PkgHandler.in = in;
		PkgHandler.out = out;
		new Thread(sender, "PkgHandler Sender").start();
	}
	
	public void run() {
		Thread thread = Thread.currentThread();
		StateSwitcher.register(thread);
		//noinspection InfiniteLoopStatement
		while(true){
			while(queue.isEmpty() || !StateSwitcher.isNormal()){
				synchronized (queue) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
//						e.printStackTrace();
						if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
							clear();
					}
				}
			}
//			if(StateSwitcher.isResetting()){
//				if(!StateSwitcher.isThreadReset(thread))
//					clear();
//				continue;
//			}
			AppPkg p;
			synchronized (queue) {
				p = queue.poll();
				if(p == null)
					continue;
			}
			
			switch(p.type){
			case 1:{
//				Dashboard.setPushed(p.car, p.dir);
				Car car = Car.carOf(p.car);
				if(car != null)
					car.dir = TrafficMap.dirOf(p.dir);
				break;
			}
			case 2:{
				Car car = Car.carOf(p.car);
				if(car != null){
					if(p.cmd == Command.MOVE_FORWARD){
//						car.finalState = Car.MOVING;
						car.notifyPolice(Police.REQUEST2ENTER);
					}
					else if(p.cmd == Command.STOP){
//						car.finalState = Car.STOPPED;
						car.notifyPolice(Police.REQUEST2STOP);
					}
				}
				break;
			}
			case 3:{
				Road road = Road.roadOf(p.loc);
				if(road != null){
					Car car = Car.carOf(p.car);
					if(car != null){
						if(road.cars.contains(car)){
							car.dir = TrafficMap.Direction.UNKNOWN;
							car.leave(road, false);
						}
						else{
							car.enter(road, TrafficMap.dirOf(p.dir));
						}
					}
				}
				break;
			}
			case 8:
				System.out.println(p.src+" "+p.dest);
				Delivery.add(Location.LocOf(p.src), Location.LocOf(p.dest), true);
				break;
			}
		}
	}
	
	public static void add(AppPkg p){
		if(StateSwitcher.isResetting())
			return;
		synchronized (queue) {
			queue.add(p);
			queue.notify();
		}		
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}

	public static void send(AppPkg p){
		sender.add(p);
	}
	
	void sendInitialInfo(ObjectOutputStream oos) throws IOException{
		for(Car car : TrafficMap.cars.values()){
			if(!car.isConnected())
				continue;
			if(car.loc == null)
				oos.writeObject(new AppPkg().setCar(car.name, TrafficMap.Direction.UNKNOWN, null));
			else
				oos.writeObject(new AppPkg().setCar(car.name, car.dir, car.loc.name));
		} 
		
		for(Delivery.DeliveryTask dtask : Resource.getDelivTasks()){
			if(dtask.car == null)
				oos.writeObject(new AppPkg().setDelivery(dtask.id, null, dtask.src.name, dtask.dest.name, dtask.phase));
			else
				oos.writeObject(new AppPkg().setDelivery(dtask.id, dtask.car.name, dtask.src.name, dtask.dest.name, dtask.phase));
		}
		
		for(Building building : TrafficMap.buildings.values())
			oos.writeObject(new AppPkg().setBuilding(building.name, building.type.toString(), building.block));
		
		for(Citizen citizen : TrafficMap.citizens){
			oos.writeObject(new AppPkg().setCitizen(citizen.name, citizen.gender.toString(), citizen.job.toString(), citizen.icon.color.getRGB()));
			if(citizen.icon.isVisible()){
				oos.writeObject(new AppPkg().setCitizen(citizen.name, (double) citizen.icon.getX()/TrafficMap.SIZE, (double) citizen.icon.getY()/TrafficMap.SIZE));
				oos.writeObject(new AppPkg().setCitizen(citizen.name, true));
			}
		}
		oos.flush();
	}
	
	private static class Sender implements Runnable {
		private final Queue<AppPkg> queue = new LinkedList<>();
		public void run() {
			Thread thread = Thread.currentThread();
			StateSwitcher.register(thread);
			//noinspection InfiniteLoopStatement
			while(true){
				while(sockets.isEmpty()){
					clear();
					//noinspection SynchronizeOnNonFinalField
					synchronized (sockets) {
						try {
							sockets.wait();
						} catch (InterruptedException e) {
//							e.printStackTrace();
							if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
								clear();
						}
					}
				}
				while(queue.isEmpty() || !StateSwitcher.isNormal()){
					synchronized (queue) {
						try {
							queue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
								clear();
						}
					}
				}
//				if(StateSwitcher.isResetting()){
//					if(!StateSwitcher.isThreadReset(thread))
//						clear();
//					continue;
//				}
				AppPkg p;
				synchronized (queue) {
					p = queue.poll();
				}
				for (Socket socket : sockets) {
					try {
						out.get(socket).writeObject(p);
					} catch (IOException e) {
						e.printStackTrace();
//						synchronized (sockets) {
//							sockets.remove(socket);
//							in.remove(socket);
//							out.remove(socket);
//						}
					}
				}
			}			
		}
		
		public void add(AppPkg p){
			if(sockets != null && !sockets.isEmpty())
				synchronized (queue) {
					queue.add(p);
					queue.notify();
				}
		}
		
		public void clear(){
			synchronized (queue) {
				queue.clear();
			}
		}
	}

	static class Receiver implements Runnable {
		Socket socket = null;
		Receiver(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			ObjectInputStream ois = in.get(socket);
			while(true){
				try {
					add((AppPkg) ois.readObject());
				} catch (OptionalDataException | ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
					//noinspection SynchronizeOnNonFinalField
					synchronized (sockets) {
						sockets.remove(socket);
						in.remove(socket);
						out.remove(socket);
					}
					break;
				}
			}
		}
	}
}
