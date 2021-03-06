package nju.xiaofanli.device.sensor;

import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.dashboard.TrafficMap;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import java.io.*;

public class SensorManager {
	private static ConcurrentHashMap<Sensor, Set<SensorListener>> listeners = new ConcurrentHashMap<>();
	
	//sensors' id: "01" ~ "92" or "ALL"
	public synchronized static boolean register(SensorListener listener, String sensor){
		if(!worker.isAlive())
			worker.start();
		if(sensor.equals("ALL") || sensor.equals("all")){
			for(Sensor[] array : TrafficMap.sensors)
				for(Sensor s : array){
					if(!listeners.containsKey(s))
						listeners.put(s, new HashSet<>());
					listeners.get(s).add(listener);
				}
			return true;
		}
		else{
			int bid = sensor.charAt(0) - '0';
			int sid = sensor.charAt(1) - '1';
			if(bid >= 0 && bid <= 9){
				if(sid >= 0 && sid < TrafficMap.sensors[bid].length){
					Sensor s = TrafficMap.sensors[bid][sid];
					if(!listeners.containsKey(s))
						listeners.put(s, new HashSet<>());
					listeners.get(s).add(listener);
					return true;
				}
			}
		}
		return false;
	}
	
	public synchronized static boolean unregister(SensorListener listener){
		for(Set<SensorListener> set : listeners.values())
			set.remove(listener);
		return true;
	}
	
	public synchronized static void trigger(Sensor sensor, int value){
		if(StateSwitcher.isResetting())
			return;
		if(listeners.containsKey(sensor))
			synchronized (queue) {
				queue.add(new SensorValue(sensor, value));
				//mxy_edit-------------Sensor.txt--------------------------
				File f= new File("mxy_temp\\Sensor.txt");
				try (FileOutputStream fop = new FileOutputStream(f,true)){
					if(!f.exists()){
						f.createNewFile();
					}

					String s=new String("["+sensor.name+"]  "+"\ttime: "+value+"\n");
					byte[] content = s.getBytes();

					fop.write(content);
					fop.flush();
					fop.close();
				}catch (IOException e){
					e.printStackTrace();
				}
				// == EDIT END ==

				queue.notify();
			}
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
	
	private static final Queue<SensorValue> queue = new LinkedList<>();
	private static Thread worker = new Thread("SensorManager Worker"){
		public void run() {
			Thread thread = Thread.currentThread();
			StateSwitcher.register(thread);
            //noinspection InfiniteLoopStatement
            while(true){
				synchronized (queue) {
					while(queue.isEmpty() || !StateSwitcher.isNormal()) {
						try {
							queue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
								clear();
						}
					}
				}
				
				SensorValue sv;
				synchronized (queue) {
					sv = queue.poll();
				}
				if(sv == null)
					continue;
				
				if(listeners.containsKey(sv.sensor))
					for(SensorListener listener : listeners.get(sv.sensor))
						listener.sensorChanged(sv.value);
			}
		}
	};
	
	private static class SensorValue{
		Sensor sensor;
		int value;
		SensorValue(Sensor sensor, int value) {
			this.sensor = sensor;
			this.value = value;
		}
	} 
}
