package nju.xiaofanli.device.car;

import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.Resource;

import java.util.LinkedList;
import java.util.Queue;

public class CmdSender implements Runnable{
    private static final Queue<Command> queue = new LinkedList<>();

    public CmdSender(){
        new Thread(this, "Command Sender").start();
        new Thread(new Remedy(), "Remedy Thread").start();
    }

    public void run() {
        class WhistleTask implements Runnable{
            private Car car;
            private int duration, interval, count;

            private WhistleTask(Car car, int duration, int interval, int count) {
                this.car = car;
                this.duration = duration;
                this.interval = interval;
                this.count = count;
            }

            public void run() {
                if(car == null)
                    return;
                whistle();
                for(int i = count - 1;i > 0;i--){
                    try {
                        Thread.sleep(interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    whistle();
                }
            }

            private void whistle(){
                car.write(Command.HORN_ON);
                car.write(Command.LEFT_HEADLIGHT_ON);
                car.write(Command.LEFT_TAILLIGHT_ON);
                car.write(Command.RIGHT_HEADLIGHT_ON);
                car.write(Command.RIGHT_TAILLIGHT_ON);
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(car.lastHornCmd != Command.HORN_ON) //if the car crashed and keeps whistling, do not silence it
                    car.write(Command.HORN_OFF);
                car.write(Command.LEFT_LIGHTS_OFF);
                car.write(Command.RIGHT_LIGHTS_OFF);
            }
        }

        Thread thread = Thread.currentThread();
        StateSwitcher.register(thread);
        //noinspection InfiniteLoopStatement
        while(true){
            synchronized (queue) {
                while(queue.isEmpty() || !StateSwitcher.isNormal()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
//						e.printStackTrace();
                        if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
                            clear();
                    }
                }
            }

            Command cmd;
            synchronized (queue) {
                cmd = queue.poll();
            }
            if(cmd.car == null)
                continue;
            switch(cmd.cmd){
                case Command.URGE:
                    Resource.execute(new WhistleTask(cmd.car, 300, 0, 1));
                    break;
                case Command.WHISTLE:
                    Resource.execute(new WhistleTask(cmd.car, 200, 0, 1));
                    break;
                case Command.WHISTLE2:
                    Resource.execute(new WhistleTask(cmd.car, 200, 100, 2));
                    break;
                case Command.WHISTLE3:
                    Resource.execute(new WhistleTask(cmd.car, 200, 100, 3));
                default:
                    cmd.car.write(cmd.cmd);
                    break;
            }
        }
    }

    public static void send(Car car, int cmd){
        if(StateSwitcher.isResetting())
            return;
        synchronized (queue) {
            queue.add(new Command(car, cmd));
            queue.notify();
        }
    }

    public static void clear(){
        synchronized (queue) {
            queue.clear();
        }
    }
}
