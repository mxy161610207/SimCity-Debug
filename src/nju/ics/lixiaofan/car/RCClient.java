package nju.ics.lixiaofan.car;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;

/**
 * Rooting is not required in wireless ADB. With USB cable <B>connected</B>, port 5555 opened across
 * all involved firewalls and debug mode enabled
 * <p>
 * <B>adb tcpip 5555</B>
 * <p>
 * then look into wireless properties of your device and the
 * network you use, to see which IP address have been granted to device. Then
 * <p>
 * <B>adb connect 192.168.1.133</B> (192.168.1.133 is a sample IP address).
 * <p>
 * This is all. You can now use adb with USB cable <B>plugged out</B>.
 * <p>
 * To switch back to USB mode,
 * <p>
 * <B>adb usb</B>
 * <p>
 * The device may also revert back to USB mode after reboot.
 */

public class RCClient implements Runnable{
//	private static ServerSocket server = null;
	private static Socket socket = null;
	public static boolean tried = false;
	public static final Object TRIED_LOCK = new Object();
	public static CarRC rc = null;
	public final static String name = "Remote Control";
	private static final int PORT = 8888;
	public void run() {
		try {
//			server = new ServerSocket(PORT);
			startRCApk();
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
	
	private void startRCApk(){
		boolean started = false;
		try {
			Runtime.getRuntime().exec("cmd.exe /c adb forward tcp:" + PORT + " tcp:" + PORT);
			Process p = Runtime.getRuntime().exec("cmd.exe /c adb shell ps ample.zenwheels");
			Scanner in = new Scanner(p.getInputStream());
			while(in.hasNext())
				if(in.nextLine().contains("com.example.zenwheels")){
					started = true;
					break;
				}
			in.close();
			if(!started){
				Runtime.getRuntime().exec("cmd.exe /c adb shell am start -n com.example.zenwheels/com.example.car.MainActivity");
				try {
					Thread.sleep(3000);//wait for the android app to start
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
//				System.out.println(str);
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
//			System.out.println(name);
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
