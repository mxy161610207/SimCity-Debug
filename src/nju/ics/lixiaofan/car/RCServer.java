package nju.ics.lixiaofan.car;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;


public class RCServer{
	private static ServerSocket server = null;
	static CarRC rc = null;
	private static final int PORT = 8888;
	private static Runnable listener = new Runnable() {
		public void run() {
			try {
				server = new ServerSocket(PORT);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			while(true){
				try {
					Socket socket = server.accept();
					socket.setTcpNoDelay(true);
					socket.setSoTimeout(0);
					rc = new CarRC(0, 0, socket, new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
					new Thread(new RCListener(), "RC Listener").start();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	};
	
	public RCServer() {
		new Thread(listener, "RC Server").start();
		new Thread(new CmdSender(), "Command Sender").start();
		new Thread(new Remedy(), "Remedy Thread").start();
	}
	
	public static void addCar(String name){
		RCListener.addCar(name);
	}
	
	private static class RCListener implements Runnable{
//		private Socket socket;
//		DataInputStream in = null;
		private boolean exit = false;
		
//		public RCListener(Socket socket) {
//			this.socket = socket;
//			try {
//				this.socket.setTcpNoDelay(true);
//			} catch (SocketException e) {
//				e.printStackTrace();
//			}
//		}

		@Override
		public void run() {
//			try {
////				new DataOutputStream(socket.getOutputStream());
//				in = new DataInputStream(socket.getInputStream());
////				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
////				br.re
//			} catch (IOException e) {
//				e.printStackTrace();
//				exit = true;
//			}
			String str;
			while(!exit){
				try {
					str = rc.in.readUTF();
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
			Car car = TrafficMap.cars.get(name);
			if(car != null && !car.isConnected){
//				System.out.println(car.name +" "+car.loc);
				car.isConnected = true;
				TrafficMap.connectedCars.add(car);
				Dashboard.addCar(car);
//				synchronized (RCServer.rc) {
//					Dashboard.updateRCConn();
//				}
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
		}
	}
}
