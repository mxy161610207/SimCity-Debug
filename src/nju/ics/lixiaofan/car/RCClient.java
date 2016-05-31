package nju.ics.lixiaofan.car;
import java.io.IOException;
import java.net.Socket;

import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;


public class RCClient implements Runnable{
//	private static ServerSocket server = null;
	private static Socket socket = null;
	public static boolean tried = false;
	public static final Object TRIED_LOCK = new Object();
	public static CarRC rc = null;
	public final static String NAME = "Remote Control";
	private static final int PORT = 8888;
	public void run() {
		try {
//			server = new ServerSocket(PORT);
			Runtime.getRuntime().exec("cmd.exe /c adb shell am start -n com.example.zenwheels/com.example.car.MainActivity");
			Runtime.getRuntime().exec("cmd.exe /c adb forward tcp:" + PORT + " tcp:" + PORT);//(");
			try {
				Thread.sleep(3000);//wait for the android app to start
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			socket = new Socket("localhost", PORT);
		} catch (IOException e) {
			e.printStackTrace();
			socket = null;
		}
//		System.out.println(socket);
		if(socket != null)
		try {
//			Socket socket = server.accept();
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(0);
			rc = new CarRC(socket);
			new Thread(new RCListener(), "RC Listener").start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		tried = true;
		synchronized (TRIED_LOCK) {
			TRIED_LOCK.notify();
		}	
	}
	
	public RCClient() {
		new Thread(this, "RC Client").start();
		new Thread(new CmdSender(), "Command Sender").start();
		new Thread(new Remedy(), "Remedy Thread").start();
	}
	
	public static boolean isConnected(){
		return socket != null && !socket.isClosed() && socket.isConnected();
	}
	
	public static void addCar(String name){
		RCListener.addCar(name);
	}
	
	private static class RCListener implements Runnable{
		public void run() {
			String str;
			while(true){
				try {
					str = rc.in.readUTF();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				String strs[] = str.split("_");
				if(strs[0].equals("ADD")){
					for(int i = 1;i < strs.length;i++)
						addCar(strs[i]);
				}
				else if(strs[0].equals("REMOVE"))
					removeCar(strs[1]);
			}
		}
		
		private static void addCar(String name){
			Car car = Car.carOf(name);
			if(car == null)
				return;
			if(!car.isConnected){
//				System.out.println(car.name +" "+car.loc);
				car.isConnected = true;
				TrafficMap.connectedCars.add(car);
				Dashboard.addCar(car);
				//calibrate
				if(car.name.equals(Car.BLACK) || car.name.equals(Car.RED)){
					CmdSender.send(car, Command.LEFT);
				}
				synchronized (Delivery.searchTasks) {
					if(Delivery.allBusy){
						Delivery.allBusy = false;
						Delivery.searchTasks.notify();
					}
				}
				//trigger add car event
				if(EventManager.hasListener(Event.Type.ADD_CAR))
					EventManager.trigger(new Event(Event.Type.ADD_CAR, car.name, car.loc.name));
			}
			if(!car.tried){
				car.tried = true;
				synchronized (car.TRIED_LOCK) {
					car.TRIED_LOCK.notify();
				}
			}
		}
	
		
		private static void removeCar(String name){
			Car car = Car.carOf(name);
			if(car == null)
				return;
			if(car.isConnected){
				car.isConnected = false;
				//TODO
			}
			if(!car.tried){
				car.tried = true;
				synchronized (car.TRIED_LOCK) {
					car.TRIED_LOCK.notify();
				}
			}
		}
	}
}
