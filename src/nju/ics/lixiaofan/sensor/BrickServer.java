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
	private static final int PORT = 9999;
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
	
	public static long recvTime[] = new long[10];
	private Thread brickListener = new Thread("Brick Listener"){
//		int[][] pre = new int[10][4];
		public void run() {
//			for(int i = 0;i < pre.length;i++)
//				for(int j = 0;j < pre[0].length;j++)
//					pre[i][j] = -1;
			try {
				server = new DatagramSocket(PORT);
			} catch (SocketException e) {
				e.printStackTrace();
			}
			new BrickHandler("Brick Handler").start();
			byte[] buf = new byte[4];//new byte[1024];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			while(true){
				try {
					server.receive(packet);
					String data = new String(packet.getData());
//					byte[] b = packet.getData();
					int bid = data.charAt(0) - '0';//byte2int(b, 0);
					int sid = data.charAt(1) - '0';//byte2int(b, 4);
//					System.out.println(data.substring(2));
					int d = Integer.parseInt(data.substring(2));//byte2int(b, 8);
//					if(((pre[bid][sid] + 1) % 100) != d)
//						System.err.println("B"+bid+"S"+(sid+1)+":"+pre[bid][sid]+" "+d);
//					pre[bid][sid] = d;
					recvTime[bid] = System.currentTimeMillis();
//					for(int i = 0;i < 10;i++)
//						System.out.print(i+":"+(System.currentTimeMillis() - recvTime[i])/1000+"\t");
//					System.out.println();
					if(sensor2print > 0 && sensor2print/10 == bid && sensor2print%10 == sid)
						System.out.println("B"+bid+"S"+(sid+1)+":"+d);
					if(d != Integer.MAX_VALUE && d != 99)
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