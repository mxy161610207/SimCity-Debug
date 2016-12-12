package nju.xiaofanli.device.car;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.event.Event;
import nju.xiaofanli.event.EventManager;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class Command {
	public Car car = null;
	public int cmd = -1;
	public long deadline = 1000;

	public final static int STOP = Car.STOPPED;
	public final static int MOVE_FORWARD = Car.MOVING;
	public final static int MOVE_BACKWARD = 2;
	public final static int LEFT = 3;
	public final static int RIGHT = 4;
	public final static int NO_STEER = 5;
    public final static int HORN_ON = 6;
    public final static int HORN_OFF = 7;

	public final static int URGE = 8;
    public final static int WHISTLE = 9;
    public final static int WHISTLE2 = 10;
    public final static int WHISTLE3 = 11;

//	public final static int CONNECT = 9;
//	public final static int DISCONNECT = 10;

    static final Map<Integer, byte[]> codes = new HashMap<>();

    static {
        codes.put(Command.STOP, ByteBuffer.allocate(4).putInt(Codes.NO_SPEED).array());
        codes.put(Command.MOVE_FORWARD, ByteBuffer.allocate(4).putInt(Codes.SPEED_FRONT[30]).array());
        codes.put(Command.MOVE_BACKWARD, ByteBuffer.allocate(4).putInt(Codes.SPEED_BACK[30]).array());
        codes.put(Command.LEFT, ByteBuffer.allocate(4).putInt(Codes.STEER_LEFT[5]).array());
        codes.put(Command.RIGHT, ByteBuffer.allocate(4).putInt(Codes.STEER_RIGHT[5]).array());
        codes.put(Command.NO_STEER, ByteBuffer.allocate(4).putInt(Codes.NO_STEER).array());
        codes.put(Command.HORN_ON, ByteBuffer.allocate(4).putInt(Codes.HORN_ON).array());
        codes.put(Command.HORN_OFF, ByteBuffer.allocate(4).putInt(Codes.HORN_OFF).array());
    }
	
	public Command(Car car, int cmd) {
		this.car = car;
		this.cmd = cmd;
		deadline = getDeadline();
	}

	long getDeadline(){
		switch (cmd) {
			case Command.MOVE_FORWARD: case Command.MOVE_BACKWARD:
				return System.currentTimeMillis();
			default:
				return System.currentTimeMillis() + 1000;
		}
	}

	//cmd:	0: stop	1: forward	2: backward	3: left	4: right
	public static void send(Car car, int cmd){
		send(car, cmd, true);
	}	
	
	public static void send(Car car, int cmd, boolean remedy){
		if(car == null)
			return;
		CmdSender.send(car, cmd);
		if(cmd == STOP || cmd == MOVE_FORWARD){
            car.lastCmd = cmd;
			if(remedy)
				Remedy.addRemedyCommand(car, cmd);
		}
		else if(cmd == HORN_ON || cmd == HORN_OFF)
			car.lastHornCmd = cmd;
	}

	static void wake(Car car){
		if(car == null || !car.isConnected())
			return;
//		if(StateSwitcher.isNormal()){
//			 if(car.getState() == Car.MOVING)
//				 drive(car);
//			 else if(car.getState() == Car.STOPPED)
//				 stop(car);
//		}
//		else if(StateSwitcher.isSuspending())
//			stop(car); // maintain its stopped state
		if(car.trend == Car.MOVING)
			drive(car);
		else if(car.trend == Car.STOPPED)
			stop(car);
	}
	
	/**
	 * Only called by Wake, Reset and Suspend 
	 */
	public static void drive(Car car){
        if(car != null && car.isConnected())
            car.write(MOVE_FORWARD);
	}
	
	/**
	 * Only called by Wake, Reset and Suspend
	 */
	public static void stop(Car car){
		if(car != null && car.isConnected()){
            car.write(STOP);
			if(StateSwitcher.isResetting())
				StateSwitcher.setLastStopCmdTime(car.lastCmdTime);
		}
	}

    /**
     * Only called by Wake, Reset and Suspend
     */
    public static void back(Car car){
        if(car != null && car.isConnected())
            car.write(MOVE_BACKWARD);
    }
	
	/**
	 * Only called by Reset and Suspend
	 */
	public static void stopAllCars(){
        Resource.getConnectedCars().forEach(Command::stop);
	}
    /**
     * Only called by Reset and Suspend
     */
	public static void silenceAllCars(){
        Resource.getConnectedCars().forEach(Command::silence);
    }

    /**
     * Only called by Reset and Suspend
     */
    public static void silence(Car car){
        if(car != null && car.isConnected())
            car.write(HORN_OFF);
    }

    /**
     * Only called by Reset and Suspend
     */
    public static void whistle(Car car){
        if(car != null && car.isConnected())
            car.write(HORN_ON);
    }
}
