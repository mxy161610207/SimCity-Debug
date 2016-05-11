package nju.ics.lixiaofan.context;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import nju.ics.lixiaofan.control.Reset;

public class ContextManager {
	private static Set<ContextListener> listeners = new HashSet<ContextListener>();
	
	public synchronized static boolean register(ContextListener listener){
		if(!worker.isAlive())
			worker.start();
		return listeners.add(listener);
	}
	
	public synchronized static boolean unregister(ContextListener listener){
		return listeners.remove(listener);
	}
	
	public synchronized static void trigger(Context context){
		if(Reset.isResetting())
			return;
		if(!listeners.isEmpty())
			synchronized (queue) {
				queue.add(context);
				queue.notify();
			}
	}
	
	public static boolean hasListener(){
		return !listeners.isEmpty();
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
	
	private static Queue<Context> queue = new LinkedList<Context>();
	private static Thread worker = new Thread("ContextManager Worker"){
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
				Context context = null;
				synchronized (queue) {
					context = queue.poll();
				}
				if(context == null)
					continue;
				
				for(ContextListener listener : listeners)
					try {
						listener.contextChanged(context.clone());
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
			}
		}
	};
}
