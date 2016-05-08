package nju.ics.lixiaofan.car;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;


public class RCServer{
	private static ServerSocket server = null;
	public static CarRC rc = null;
	private static Runnable listener = new Runnable() {
		public void run() {
			try {
				server = new ServerSocket(8888);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			while(true){
				try {
					Socket socket = server.accept();
					rc = new CarRC(0, 0, socket, new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
					new Thread(new RCListener(socket), "RC Listener").start();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	};
	
	public RCServer() {
		new Thread(listener, "RC Server").start();
		new Thread(new CmdSender(), "Command Sender").start();
		new Thread(new Remediation(), "Remedy Thread").start();
	}
	
	public static void addCar(String name){
		RCListener.addCar(name);
	}
	
	private static class RCListener implements Runnable{
		private Socket socket;
		DataInputStream in = null;
		private boolean exit = false;
		private String str;
		public RCListener(Socket socket) {
			this.socket = socket;
			try {
				this.socket.setTcpNoDelay(true);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
//				new DataOutputStream(socket.getOutputStream());
				in = new DataInputStream(socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
				exit = true;
			}
			while(!exit){
				try {
					str = in.readUTF();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				String strs[] = str.split("_");
				if(strs[0].equals("zenwheels")){
					int i = 1;
					while(i < strs.length){
						addCar(strs[i]);
						i += 2;
					}
				}
			}
		}
		
		private static void addCar(String name){
			if(TrafficMap.cars.containsKey(name) && !TrafficMap.cars.get(name).isConnected){
				Car car = TrafficMap.cars.get(name);
//				System.out.println(car.name +" "+car.loc);
				car.isConnected = true;
				Dashboard.addCar(car);
//				synchronized (RCServer.rc) {
//					Dashboard.updateRCConn();
//				}
				//calibrate
				if(car.name.equals(Car.BLACK) || car.name.equals(Car.RED)){
					CmdSender.send(car, 3);
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
		}
	}
}
