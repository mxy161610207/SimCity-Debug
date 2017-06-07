package nju.xiaofanli.device.car;

import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.Resource;

import java.util.*;

public class CmdSender implements Runnable{
    private static final List<Command> queue = new ArrayList<>();

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

        class DelayedTask implements Runnable {
            private Car car;
            private int cmd;
            private long deadline;

            private DelayedTask(Car car, int cmd, long deadline) {
                this.car = car;
                this.cmd = cmd;
                this.deadline = deadline;
            }

            @Override
            public void run() {
                if (car == null || !StateSwitcher.isNormal())
                    return;

                long time = System.currentTimeMillis(), delay = deadline - time;
                if (delay > 0)
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                if (StateSwitcher.isNormal() && car.lastStartCmdTime < time) {
                    car.write(cmd);
//                    System.err.println("car: " + car.name + "\tcmd: " + (cmd == Command.STOP ? "STOP" : "START") + "\tdelay: " + delay);
                }
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
                cmd = queue.remove(0);
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
                    if (System.currentTimeMillis() >= cmd.deadline)
                        cmd.car.write(cmd.cmd);
                    else
                        Resource.execute(new DelayedTask(cmd.car, cmd.cmd, cmd.deadline));
                    break;
            }

//            synchronized (queue) {
//                while (!queue.isEmpty()) {
//                    Command cmd = queue.get(0);
//                    if (cmd.deadline > System.currentTimeMillis())
//                        break;
//                    queue.remove(0);
//                    switch (cmd.cmd) {
//                        case Command.URGE:
//                            Resource.execute(new WhistleTask(cmd.car, 300, 0, 1));
//                            break;
//                        case Command.WHISTLE:
//                            Resource.execute(new WhistleTask(cmd.car, 200, 0, 1));
//                            break;
//                        case Command.WHISTLE2:
//                            Resource.execute(new WhistleTask(cmd.car, 200, 100, 2));
//                            break;
//                        case Command.WHISTLE3:
//                            Resource.execute(new WhistleTask(cmd.car, 200, 100, 3));
//                        default:
//                            cmd.car.write(cmd.cmd);
//                            break;
//                    }
//                }
//            }
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                if (StateSwitcher.isResetting())
//                    thread.interrupt();
//            }
        }
    }

    private static Comparator<Command> comparator = (o1, o2) -> (int) (o1.deadline - o2.deadline);
    public static void send(Car car, int cmd, int delay) {
        if(StateSwitcher.isResetting())
            return;
        synchronized (queue) {
//            Command command = new Command(car, cmd, delay ? Math.max(System.currentTimeMillis(), car.lastDetectedTime+100) : System.currentTimeMillis());
//            boolean addition = true;
//            for(Iterator<Command> it = queue.iterator(); it.hasNext();){
//                Command cmd2 = it.next();
//                if(cmd2.car == car){
//                    if(cmd2.cmd != cmd || cmd2.deadline > command.deadline)
//                        it.remove();
//                    else
//                        addition = false;
//                    break;
//                }
//            }
//            if (addition) {
//                int pos = Collections.binarySearch(queue, command, comparator);
//                if(pos < 0)
//                    pos = -pos - 1;
//                queue.add(pos, command);
//                queue.notify();
//            }
            queue.add(new Command(car, cmd, System.currentTimeMillis()+delay));
            queue.notify();
        }
    }

    public static void clear(){
        synchronized (queue) {
            queue.clear();
        }
    }
}
