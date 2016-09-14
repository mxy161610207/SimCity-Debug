package nju.ics.lixiaofan.car;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import nju.ics.lixiaofan.control.StateSwitcher;
import nju.ics.lixiaofan.resource.Resource;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

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
	public final static int HORN = 6;
    public final static int HORN_ON = 7;
    public final static int HORN_OFF = 8;
	public final static int CONNECT = 9;
	public final static int DISCONNECT = 10;

    static final Map<Integer, byte[]> codes = new HashMap<>();

    static {
        codes.put(Command.STOP, ByteBuffer.allocate(4).putInt(CarCodes.NO_SPEED).array());
        codes.put(Command.FORWARD, ByteBuffer.allocate(4).putInt(CarCodes.SPEED_FRONT[30]).array());
        codes.put(Command.BACKWARD, ByteBuffer.allocate(4).putInt(CarCodes.SPEED_BACK[30]).array());
        codes.put(Command.LEFT, ByteBuffer.allocate(4).putInt(CarCodes.STEER_LEFT[3]).array());
        codes.put(Command.RIGHT, ByteBuffer.allocate(4).putInt(CarCodes.STEER_RIGHT[3]).array());
        codes.put(Command.NO_STEER, ByteBuffer.allocate(4).putInt(CarCodes.NO_STEER).array());
        codes.put(Command.HORN_ON, ByteBuffer.allocate(4).putInt(CarCodes.HORN_ON).array());
        codes.put(Command.HORN_OFF, ByteBuffer.allocate(4).putInt(CarCodes.HORN_OFF).array());
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
			car.trend = cmd;
			if(cmd != car.status)
				car.status = Car.UNCERTAIN;
			if(!car.isReal() && cmd != car.realStatus)
				car.realStatus = Car.UNCERTAIN;
//			car.lastInstr = cmd;
			
			if(remedy)
				Remedy.addRemedyCommand(car, cmd);
		}
	}
	
	public static void send(Command cmd, boolean remedy) {
		send(cmd.car, cmd.cmd, cmd.level, remedy);
	}
	
	public static void wake(Car car){
		if(car == null || !car.isConnected())
			return;
		if(StateSwitcher.isNormal()){
			 if(car.getRealStatus() == Car.MOVING)
				 drive(car);
			 else if(car.getRealStatus() == Car.STOPPED)
				 stop(car);
		}
		else if(StateSwitcher.isSuspending())
			stop(car); // maintain its stopped status
	}
	
	public static void connect(Car car) {
        if (car != null)
           car.connect();
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
				StateSwitcher.resetTask.lastStopInstrTime = car.lastInstrTime;
		}
	}
	
	/**
	 * Only called by Reset Thread
	 */
	public static void stopAllCars(){
		for(Car car : Resource.getConnectedCars())
			if(car.getRealStatus() != Car.STOPPED)
				stop(car);
	}
}
