package nju.ics.lixiaofan.car;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class CmdSender implements Runnable{
	public static Queue<Command> queue = new LinkedList<Command>();
//	private static Hashtable<Integer, CarRC> rcs = RCServer.rcs;
//	private long interval = 100;
	
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
			Command cmd = null;
			synchronized (queue) {
				cmd = queue.poll();
			}
			if(cmd == null)
				continue;
			
			//zenwheels
			if(cmd.car.type == 3){
				CarRC rc = RCServer.rc;//cmd.car.rc;
				if(rc != null){
					try {
//						if(cmd.type == 1)
//							System.out.println("wake");
						if(cmd.cmd == 3 || cmd.cmd == 4){
//							rc.out.writeUTF(cmd.cmd+"_3");
							rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_3");
							rc.out.flush();
//							System.out.println("steer "+cmd.cmd);
						}
						else{
//							rc.out.writeUTF(cmd.cmd+"_30");
							rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_30");
							rc.out.flush();
							rc.lastInstrTime = System.currentTimeMillis();
						}
						//assume zenwheels will recv the cmd definitely
//						cmd.car.state = cmd.cmd;
//						if(cmd.cmd == 0)
//							cmd.car.stopTime = System.currentTimeMillis();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				continue;
			}
		}
		
	}

//	public static void send(CarRC rc){
//		synchronized (queue) {
//			queue.add(new Command(rc));
//			queue.notify();
////			Server.dashboard.updateCmdQ();
//		}
//	}
	
	public static void send(Car car, int cmd, int type) {
		synchronized (queue) {
			queue.add(new Command(car, cmd, type));
			queue.notify();
//			Server.dashboard.updateCmdQ();
		}
	}
	
	public static void send(Car car, int cmd){
		synchronized (queue) {
			queue.add(new Command(car, cmd));
			queue.notify();
//			Server.dashboard.updateCmdQ();
		}
	}
	
}
