package nju.ics.lixiaofan.car;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;

public class RCListener implements Runnable{
	private Socket socket;
	DataOutputStream out = null;
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
			out = new DataOutputStream(socket.getOutputStream());
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
					String key = strs[i];
					if(TrafficMap.cars.containsKey(key) && !TrafficMap.cars.get(key).isConnected){
						Car car = TrafficMap.cars.get(key);
						car.isConnected = true;
						Dashboard.addCar(car);
//						synchronized (RCServer.rc) {
//							Dashboard.updateRCConn();
//						}
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
					i += 2;
				}
			}
		}
	}
}
