package nju.ics.lixiaofan.monitor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.car.RCServer;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.control.Delivery.DeliveryTask;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.data.DataProvider;

public class PkgHandler implements Runnable{
	private static Queue<AppPkg> queue = new LinkedList<AppPkg>();
	private static Sender sender = new Sender();
	private static List<Socket> sockets = null;
	private static HashMap<Socket, ObjectInputStream> in = null;
	private static HashMap<Socket, ObjectOutputStream> out = null;
	public PkgHandler(List<Socket> sockets, HashMap<Socket, ObjectInputStream> in, HashMap<Socket, ObjectOutputStream> out) {
		PkgHandler.sockets = sockets;
		PkgHandler.in = in;
		PkgHandler.out = out;
		new Thread(sender).start();
	}
	
	public void run() {
		while(true){
			while(queue.isEmpty()){
				synchronized (queue) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			AppPkg p = null;
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
					car.dir = p.dir;
				break;
			}
			case 2:{
				Car car = Car.carOf(p.car);
				if(car != null){
					if(p.cmd == 1){
						car.finalState = 1;
						car.sendRequest(1);
					}
					else if(p.cmd == 0){
						car.finalState = 0;
						Command.send(car, 0);
						car.sendRequest(0);
					}
				}
				break;
			}
			case 3:{
				Section sect = Section.sectionOf(p.loc);
				if(sect != null){
					Car car = Car.carOf(p.car);
					if(car != null){
						if(sect.cars.contains(car)){
							car.dir = -1;
							Dashboard.carLeave(car, sect);
						}
						else{
							car.dir = p.dir;
							Dashboard.carEnter(car, sect);
						}
					}
				}
				break;
			}
			case 8:{
				Section src = Section.sectionOf(p.src);
				Section dst = Section.sectionOf(p.dst);
				System.out.println(src.name+" "+dst.name);
				Delivery.add(src, dst);
				break;
			}
			}
		}
	}
	
	public void add(AppPkg p){
		synchronized (queue) {
			queue.add(p);
			queue.notify();
		}		
	}

	public static void send(AppPkg p){
		sender.add(p);
	}
	
	public void sendInitInfo(ObjectOutputStream oos){
		try {
			for(Car car : RCServer.cars.values()){
				if(car.loc == null)
					oos.writeObject(new AppPkg(car.name, (byte)-1, null));
				else
					oos.writeObject(new AppPkg(car.name, car.dir, car.loc.name));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(DeliveryTask dtask : DataProvider.getDelivTasks()){
			AppPkg p = new AppPkg();
			if(dtask.car == null)
				p.setDelivery((byte)dtask.id, null, dtask.srcSect.name, dtask.dstSect.name, (byte)dtask.phase);
			else
				p.setDelivery((byte)dtask.id, dtask.car.name, dtask.srcSect.name, dtask.dstSect.name, (byte)dtask.phase);
			try {
				oos.writeObject(p);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static class Sender implements Runnable {
		Queue<AppPkg> queue = new LinkedList<AppPkg>();
		public void run() {
			while(true){
				while(sockets.isEmpty()){
					synchronized (queue) {
						queue.clear();
					}
					synchronized (sockets) {
						try {
							sockets.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				while(queue.isEmpty()){
					synchronized (queue) {
						try {
							queue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				if(sockets.isEmpty())
					continue;
				AppPkg p = null;
				synchronized (queue) {
					p = queue.poll();
					if(p == null)
						continue;
				}
				for(int i = 0;i < sockets.size();i++){
					Socket socket = sockets.get(i);
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
			if(!sockets.isEmpty())
				synchronized (queue) {
					queue.add(p);
					queue.notify();
				}
		}
	};
	
	public static class  Receiver implements Runnable {
		Socket socket = null;
		public Receiver(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			ObjectInputStream ois = in.get(socket);
			AppPkg p = null;
			while(true){
				try {
					p = (AppPkg) ois.readObject();
					synchronized (queue) {
						queue.add(p);
						queue.notify();
					}
				} catch (OptionalDataException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
					synchronized (sockets) {
						sockets.remove(socket);
						in.remove(socket);
						out.remove(socket);
					}
					break;
				}
			}
		}
	};
}