package nju.ics.lixiaofan.control;

import java.util.concurrent.ConcurrentHashMap;

import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.dashboard.Dashboard;

public class Reset {
	private static boolean resetting = false;
	public static ConcurrentHashMap<Thread, Boolean> status = new ConcurrentHashMap<>();
	public static Object obj = new Object();
	
	public static boolean isResetting(){
		return resetting;
	}
	
	public static void setReset(boolean b){
		resetting = b;
	}
	
	public static void setThread(Thread thread){
		status.put(thread, true);
	}
	
	public static boolean getThread(Thread thread){
		return status.get(thread);
	}
	
	public static void addThread(Thread thread){
		status.put(thread, false);
	}
	
	public static boolean checkThread(Thread thread){
		if(!getThread(thread)){
			setThread(thread);
			if(isAllReset())
				synchronized (obj) {
					obj.notify();
				}
			return true;
		}
		return false;
	}
	
	private static boolean isAllReset(){
		for(Boolean b : status.values())
			if(!b)
				return false;
		return true;
	}
	
	public static class ResetTask implements Runnable {
		private boolean isFalse;//false inconsistency or not
		
		public ResetTask(boolean b) {
			isFalse = b;
		}
		
		public void run() {
			Dashboard.enableResetButton(false);
			resetting = true;
			//first step: stop the world
			Command.resetAllCars();
			
			synchronized (Reset.obj) {
				try {
					Reset.obj.wait();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			
			
			if(isFalse) {
				//left click for false inconsistency
			}
			else{
				//right click for real inconsistency
			}
			
			
			
			resetting = false;
			Dashboard.enableResetButton(true);
		}
	}
}
