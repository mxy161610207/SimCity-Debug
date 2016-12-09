package nju.xiaofanli.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import nju.xiaofanli.StateSwitcher;

public class EventManager {
	private static ConcurrentHashMap<Event.Type, Set<EventListener>> listeners = new ConcurrentHashMap<>();
	
	public synchronized static void register(EventListener listener, Event.Type type){
		if(!worker.isAlive())
			worker.start();
		if(type.equals(Event.Type.ALL)){
			for(Event.Type t : Event.Type.values()){
				if(!listeners.containsKey(t))
					listeners.put(t, new HashSet<>());
				listeners.get(t).add(listener);
//				System.out.println(t);
			}
		}
		else{
			if(!listeners.containsKey(type))
				listeners.put(type, new HashSet<>());
			listeners.get(type).add(listener);
		}
	}

	public synchronized static void register(EventListener listener, List<Event.Type> types){
        for(Event.Type type : new HashSet<>(types))
            register(listener, type);
    }
	
	public synchronized static boolean unregister(EventListener listener){
		for(Set<EventListener> set : listeners.values())
			set.remove(listener);
		return true;
	}
	
	public synchronized static void trigger(Event event){
		if(StateSwitcher.isResetting())
			return;
		if(hasListener(event.type))
			synchronized (queue) {
				queue.add(event);
				queue.notify();
			}
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
	
	public static boolean hasListener(Event.Type type){
		return listeners.containsKey(type) && !listeners.get(type).isEmpty();
	}
	
	private static final Queue<Event> queue = new LinkedList<>();
	private static Thread worker = new Thread("EventManager Worker"){
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
//							e.printStackTrace();
							if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
								clear();
						}
					}
				}
				
				Event event;
				synchronized (queue) {
					event = queue.poll();
				}
				if(event == null)
					continue;

				if(listeners.containsKey(event.type))
                    for(EventListener listener : listeners.get(event.type))
                        listener.eventTriggered(event);
			}
		}
	};
}
