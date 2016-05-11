package nju.ics.lixiaofan.sensor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;

public class BrickServer {
//	private static ServerSocket server = null;
	private static DatagramSocket server = null;
	private int sensor2print = 0;
	private Thread keyListener = new Thread("Keyboard Listener") {
		@Override
		public void run() {
			@SuppressWarnings("resource")
			Scanner sc = new Scanner(System.in);
			int cmd;
			while(true){
				cmd = sc.nextInt();
				sensor2print = cmd - 1;
			}
		}
	};

	private Thread brickListener = new Thread("Brick Listener"){
		public void run() {
			try {
				server = new DatagramSocket(9999);
			} catch (SocketException e) {
				e.printStackTrace();
			}
			new BrickHandler("Brick Handler").start();
			byte[] buf = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			while(true){
				try {
					server.receive(packet);
					byte[] b = packet.getData();
					int bid = byte2int(b, 0);
					int sid = byte2int(b, 4);
					int d = byte2int(b, 8);
					if(sensor2print > 0 && sensor2print/10 == bid && sensor2print%10 == sid)
						System.out.println("B"+bid+"S"+(sid+1)+":"+d);
					if(d != Integer.MAX_VALUE)
						BrickHandler.add(bid, sid, d);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	public BrickServer() {
		keyListener.start();
		brickListener.start();
	}
	
	public static int byte2int(byte[] res, int offset) {   
		int targets = (res[offset] & 0xff) | ((res[offset+1] << 8) & 0xff00)
		| ((res[offset+2] << 24) >>> 8) | (res[offset+3] << 24);   
		return targets;   
	}
}