package nju.ics.lixiaofan.car;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class CmdSender implements Runnable{
	public static Queue<Command> queue = new LinkedList<Command>();
	
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
			
			CarRC rc = RCServer.rc;//cmd.car.rc;
			if(rc == null)
				continue;
			try {
				switch(cmd.cmd){
				case Command.LEFT:case Command.RIGHT:
					rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_3");
					rc.out.flush();
					break;
				case Command.FORWARD:case Command.STOP:case Command.BACKWARD:
					rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_30");
					rc.out.flush();
					rc.lastInstrTime = System.currentTimeMillis();
					break;
				case Command.HORN:
					rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_3000");
					rc.out.flush();
					break;
				default:
					System.out.println("!!!UNKNOWN COMMAND!!!");
					break;
				}
//					if(cmd.cmd == 3 || cmd.cmd == 4){
//						rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_3");
//						rc.out.flush();
//					}
//					else{
//						rc.out.writeUTF(cmd.car.name+"_"+cmd.cmd+"_30");
//						rc.out.flush();
//						rc.lastInstrTime = System.currentTimeMillis();
//					}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

//	public static void send(Car car, int cmd, int type) {
//		synchronized (queue) {
//			queue.add(new Command(car, cmd, type));
//			queue.notify();
//		}
//	}
	
	public static void send(Car car, int cmd){
		synchronized (queue) {
			queue.add(new Command(car, cmd));
			queue.notify();
		}
	}
	
}
