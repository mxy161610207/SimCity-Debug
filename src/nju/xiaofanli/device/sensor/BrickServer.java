package nju.xiaofanli.device.sensor;

import nju.xiaofanli.Resource;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class BrickServer implements Runnable{
	private static DatagramSocket server = null;
	private static final int PORT = 9999;
//	private static long recvTime[] = new long[10];
	public static Sensor showingSensor = null;

	public BrickServer() {
		new Thread(this, "Brick Server").start();
//		keyListener.start();
//		brickListener.start();
	}

	public void run() {
		try {
			server = new DatagramSocket(PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		new BrickHandler("Brick Handler").start();
		byte[] buf = new byte[4];//new byte[1024];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		//noinspection InfiniteLoopStatement
		while(true){
			try {
				server.receive(packet);
				String data = new String(packet.getData());
//				byte[] b = packet.getData();
				int bid = data.charAt(0) - '0';//byte2int(b, 0);
				int sid = data.charAt(1) - '0';//byte2int(b, 4);
//				System.out.println(data.substring(2));
				int d = Integer.parseInt(data.substring(2));//byte2int(b, 8);
                if(showingSensor != null && Resource.getSensors()[bid][sid] == showingSensor)
                    System.out.println("["+showingSensor.name+"] reading: "+d);
//				if(((pre[bid][sid] + 1) % 100) != d)
//					System.err.println("B"+bid+"S"+(sid+1)+":"+pre[bid][sid]+" "+d);
//				pre[bid][sid] = d;
//				recvTime[bid] = System.currentTimeMillis();
//				for(int i = 0;i < 10;i++)
//					System.out.print(i+":"+(System.currentTimeMillis() - recvTime[i])/1000+"\t");
//				System.out.println();
//				if(sensor2print > 0 && sensor2print/10 == bid && sensor2print%10 == sid)
//					System.out.println("B"+bid+"S"+(sid+1)+":"+d);
				if(d != Integer.MAX_VALUE && d != 99)
					BrickHandler.add(bid, sid, d, System.currentTimeMillis());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
}