package nju.ics.lixiaofan.car;

import nju.ics.lixiaofan.control.StateSwitcher;
import nju.ics.lixiaofan.resource.Resource;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by Leslie on 2016/9/14.
 */
public class CmdSender implements Runnable{
    private static final Queue<Command> queue = new LinkedList<>();


    public CmdSender(){
        new Thread(this, "Command Sender").start();
        new Thread(new Remedy(), "Remedy Thread").start();
    }

    public void run() {
        class HornTask implements Runnable{
            private Car car;
            private int time;

            HornTask(Car car, int time) {
                this.car = car;
                this.time = time;
            }

            public void run() {
                if(car == null)
                    return;
                car.write(Command.codes.get(Command.HORN_ON));
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                car.write(Command.codes.get(Command.HORN_OFF));
            }
        }

        Thread thread = Thread.currentThread();
        StateSwitcher.register(thread);
        while(true){
            while(queue.isEmpty() || !StateSwitcher.isNormal()){
                synchronized (queue) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
//						e.printStackTrace();
                        if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
                            clear();
                    }
                }
            }
//			if(StateSwitcher.isResetting()){
//				if(!StateSwitcher.isThreadReset(thread))
//					clear();
//				continue;
//			}

            Command cmd;
            synchronized (queue) {
                cmd = queue.poll();
            }
            if(cmd.car == null)
                continue;
            switch(cmd.cmd){
                case Command.HORN:
                    Resource.execute(new HornTask(cmd.car, 1000));
                    break;
                default:
                    cmd.car.write(Command.codes.get(cmd.cmd));
                    break;
            }
        }
    }

    public static void send(Car car, int cmd){
        send(car, cmd, 0);
    }

    public static void send(Car car, int cmd, int type){
        if(StateSwitcher.isResetting())
            return;
        synchronized (queue) {
            queue.add(new Command(car, cmd, type));
            queue.notify();
        }
    }

    public static void clear(){
        synchronized (queue) {
            queue.clear();
        }
    }
}
