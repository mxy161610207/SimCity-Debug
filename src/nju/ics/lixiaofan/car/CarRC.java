package nju.ics.lixiaofan.car;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

//car remote control
public class CarRC {
//	public int id;
	public int type; // 0: battletank	1: tankbot	2: carbot 3: zenwheels
	public int location; // 0: north-west	1: north-east	2: south-west	3: south-east
	public int key;
	public String name, address;
	public long lastInstrTime = System.currentTimeMillis();
	Socket socket;
	DataInputStream in;
	DataOutputStream out;
	
	public CarRC(int type, int loc, Socket socket, DataInputStream in, DataOutputStream out) {
		this.type = type;
		this.location = loc;
		this.socket = socket;
		this.in = in;
		this.out = out;
	}
	public CarRC(int type, String name, String address, Socket socket, DataInputStream in, DataOutputStream out) {
		this.type = type;
		this.name = name;
		this.address = address;
		this.socket = socket;
		this.in = in;
		this.out = out;
	}
	
	public static String locationOf(int i){
		switch(i){
		case 0:
			return "North-west";
		case 1:
			return "North-east";
		case 2:
			return "South-west";
		case 3:
			return "South-east";
		}
		return null;
	}
}
