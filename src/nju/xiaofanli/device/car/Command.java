package nju.xiaofanli.device.car;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.Resource;

public class Command {
	public Car car = null;
	public int cmd = -1;
	public int level = 1;
	public long deadline = 1000;
	public int type = 0;//0: normal 1: wake car 2: reset
	
	public final static int STOP = Car.STOPPED;
	public final static int FORWARD = Car.MOVING;
	public final static int BACKWARD = 2;
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
        codes.put(Command.FORWARD, ByteBuffer.allocate(4).putInt(Codes.SPEED_FRONT[30]).array());
        codes.put(Command.BACKWARD, ByteBuffer.allocate(4).putInt(Codes.SPEED_BACK[30]).array());
        codes.put(Command.LEFT, ByteBuffer.allocate(4).putInt(Codes.STEER_LEFT[3]).array());
        codes.put(Command.RIGHT, ByteBuffer.allocate(4).putInt(Codes.STEER_RIGHT[3]).array());
        codes.put(Command.NO_STEER, ByteBuffer.allocate(4).putInt(Codes.NO_STEER).array());
        codes.put(Command.HORN_ON, ByteBuffer.allocate(4).putInt(Codes.HORN_ON).array());
        codes.put(Command.HORN_OFF, ByteBuffer.allocate(4).putInt(Codes.HORN_OFF).array());
    }
	
	public Command(Car car, int cmd) {
		this.car = car;
		this.cmd = cmd;
		deadline = Remedy.getDeadline(cmd, 1);
	}
	
	public Command(Car car, int cmd, int type) {
		this(car, cmd);
		this.type = type;
	}

	//cmd:	0: stop	1: forward	2: backward	3: left	4: right
	public static void send(Car car, int cmd){
		send(car, cmd, 1, true);
	}	
	
	public static void send(Car car, int cmd, int level, boolean remedy){
		if(car == null)
			return;
		CmdSender.send(car, cmd);
		if(cmd == STOP || cmd == FORWARD){
            car.lastCmd = cmd;
			if(cmd == STOP && car.getState() != Car.STOPPED || cmd == FORWARD && car.getState() != Car.MOVING)
				car.setState(Car.UNCERTAIN);
			if(car.hasPhantom() && (cmd == STOP && car.getRealState() != Car.STOPPED || cmd == FORWARD && car.getRealState() != Car.MOVING))
				car.setRealState(Car.UNCERTAIN);
			
			if(remedy)
				Remedy.addRemedyCommand(car, cmd);
		}
	}
	
	public static void send(Command cmd, boolean remedy) {
		send(cmd.car, cmd.cmd, cmd.level, remedy);
	}
	
	static void wake(Car car){
		if(car == null || !car.isConnected())
			return;
		if(StateSwitcher.isNormal()){
			 if(car.getRealState() == Car.MOVING)
				 drive(car);
			 else if(car.getRealState() == Car.STOPPED)
				 stop(car);
		}
		else if(StateSwitcher.isSuspending())
			stop(car); // maintain its stopped state
	}
	
	/**
	 * Only called by Wake, Reset and Suspend 
	 */
	public static void drive(Car car){
        if(car != null && car.isConnected())
            car.write(codes.get(FORWARD));
	}
	
	/**
	 * Only called by Wake, Reset and Suspend
	 */
	public static void stop(Car car){
		if(car != null && car.isConnected()){
            car.write(codes.get(STOP));
			if(StateSwitcher.isResetting())
				StateSwitcher.setLastStopCmdTime(car.lastCmdTime);
		}
	}
	
	/**
	 * Only called by Reset Thread
	 */
	public static void stopAllCars(){
		Resource.getConnectedCars().forEach(Command::stop);
	}
}
