package nju.ics.lixiaofan.event;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nju.ics.lixiaofan.event.Event.Type;

public class EventManager {
	private static ConcurrentHashMap<Type, Set<EventListener>> listeners = new ConcurrentHashMap<Type, Set<EventListener>>();
	
	public synchronized static boolean register(EventListener listener, Type type){
		if(!worker.isAlive())
			worker.start();
		if(type.equals(Type.ALL)){
			for(Type t : Type.values()){
				if(!listeners.containsKey(t))
					listeners.put(t, new HashSet<EventListener>());
				listeners.get(t).add(listener);
//				System.out.println(t);
			}
			return true;
		}
		else{
			if(!listeners.containsKey(type))
				listeners.put(type, new HashSet<EventListener>());
			return listeners.get(type).add(listener);
		}
	}
	
	public synchronized static boolean unregister(EventListener listener){
		for(Set<EventListener> set : listeners.values())
			set.remove(listener);
		return true;
	}
	
	public synchronized static void trigger(Event event){
		if(hasListener(event.type))
			synchronized (queue) {
				queue.add(event);
				queue.notify();
			}
	}
	
	public static boolean hasListener(Type type){
//		System.out.println("type:"+type);
//		if(listeners.containsKey(type)){
//			System.out.println("contains "+type);
//			if(listeners.get(type).size() > 0)
//				System.out.println("size > 0");
//		}
		return listeners.containsKey(type) && listeners.get(type).size() > 0;
	}
	
	private static Queue<Event> queue = new LinkedList<Event>();
	private static Thread worker = new Thread(){
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
				
				Event event = null;
				synchronized (queue) {
					event = queue.poll();
				}
				if(event == null)
					continue;
				
				try {
					if(listeners.containsKey(event.type))
						for(EventListener listener : listeners.get(event.type))
							listener.eventTriggered(event.clone());
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
		}
	};
}
