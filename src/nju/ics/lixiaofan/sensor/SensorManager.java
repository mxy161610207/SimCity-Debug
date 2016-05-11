package nju.ics.lixiaofan.sensor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Reset;

public class SensorManager {
	private static ConcurrentHashMap<Sensor, Set<SensorListerner>> listeners = new ConcurrentHashMap<Sensor, Set<SensorListerner>>();
	
	//sensors' id: "01" ~ "92" or "ALL"
	public synchronized static boolean register(SensorListerner listener, String sensor){
		if(!worker.isAlive())
			worker.start();
		if(sensor.equals("ALL") || sensor.equals("all")){
			for(List<Sensor> list :TrafficMap.sensors)
				for(Sensor s : list){
					if(!listeners.containsKey(s))
						listeners.put(s, new HashSet<SensorListerner>());
					listeners.get(s).add(listener);
				}
			return true;
		}
		else{
			int bid = sensor.charAt(0)-'0';
			int sid = sensor.charAt(1)-'0';
			if(bid >= 0 && bid <= 9){
				if(sid >= 1 && sid <= TrafficMap.sensors.get(bid).size()){
					Sensor s = TrafficMap.sensors.get(bid).get(sid);
					if(!listeners.containsKey(s))
						listeners.put(s, new HashSet<SensorListerner>());
					listeners.get(s).add(listener);
					return true;
				}
			}
		}
		return false;
	}
	
	public synchronized static boolean unregister(SensorListerner listener){
		for(Set<SensorListerner> set : listeners.values())
			set.remove(listener);
		return true;
	}
	
	public synchronized static void trigger(Sensor sensor, int value){
		if(Reset.isResetting())
			return;
		if(listeners.containsKey(sensor))
			synchronized (queue) {
				queue.add(new SensorValue(sensor, value));
				queue.notify();
			}
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
	
	private static Queue<SensorValue> queue = new LinkedList<SensorValue>();
	private static Thread worker = new Thread("SensorManager Worker"){
		public void run() {
			Thread curThread = Thread.currentThread();
			Reset.addThread(curThread);
			while(true){
				while(queue.isEmpty()){
					synchronized (queue) {
						try {
							queue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							if(Reset.isResetting() && Reset.isUnchecked(curThread))
								clear();
						}
					}
				}
				if(Reset.isResetting()){
					if(Reset.isUnchecked(curThread))
						clear();
					continue;
				}
				
				SensorValue sv = null;
				synchronized (queue) {
					sv = queue.poll();
				}
				if(sv == null)
					continue;
				
				if(listeners.containsKey(sv.sensor))
					for(SensorListerner listener : listeners.get(sv.sensor))
						listener.sensorChanged(sv.value);
			}
		}
	};
	
	private static class SensorValue{
		Sensor sensor;
		int value;
		public SensorValue(Sensor sensor, int value) {
			this.sensor = sensor;
			this.value = value;
		}
	} 
}
