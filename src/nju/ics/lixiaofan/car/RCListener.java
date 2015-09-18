package nju.ics.lixiaofan.car;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

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
					if(RCServer.cars.containsKey(key) && !RCServer.cars.get(key).isConnected){
//						CarRC rc = new CarRC(3, name, strs[i+1], socket, in, out);
						Car car = RCServer.cars.get(key);//new Car(3, key);
						car.isConnected = true;
//						rc.key = key;
//						synchronized (RCServer.cars) {
//							RCServer.cars.put(key, car);
//						}
						Dashboard.addCar(car);
						synchronized (RCServer.rc) {
//							RCServer.rcs.put(key, rc);
							Dashboard.updateRCConn();
						}
						//calibrate
						if(car.name.equals(Car.BLACK) || car.name.equals(Car.RED)){
							CmdSender.send(car, 3);
						}
						//trigger add car event
						if(EventManager.hasListener(Event.Type.ADD_CAR))
							EventManager.trigger(new Event(Event.Type.ADD_CAR, car.name, car.loc.name));
					}
					i += 2;
				}
			}
		}
		
//		while(!exit){
//			try {
//				in.readUTF();
//			} catch (IOException e) {
//				e.printStackTrace();
//				exit = true;
//				break;
//			}
//		}
	}
}
