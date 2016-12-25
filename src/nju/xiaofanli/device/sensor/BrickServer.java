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
		byte[] buf = new byte[20];//new byte[1024];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		//noinspection InfiniteLoopStatement
		while(true){
			try {
				server.receive(packet);
				String data = new String(packet.getData());
//				System.out.println(data);
				int bid = data.charAt(0) - '0';//byte2int(b, 0);
				int sid = data.charAt(1) - '0';//byte2int(b, 4);
				int d = Integer.parseInt(data.substring(2, 4));//byte2int(b, 8);
                if(showingSensor != null && Resource.getSensor(bid, sid) == showingSensor)
                    System.out.println("["+showingSensor.name+"] reading: "+d);
//				if(((pre[bid][sid] + 1) % 100) != d)
//					System.err.println("B"+bid+"S"+(sid+1)+":"+pre[bid][sid]+" "+d);
//				pre[bid][sid] = d;
//				recvTime[bid] = System.currentTimeMillis();
//				for(int i = 0;i < 10;i++)
//					System.out.print(i+":"+(System.currentTimeMillis() - recvTime[i])/1000+"\t");
				if (d == 99) { // clock synchronization
                    byte[] buffer = data.substring(0, 2).concat(String.valueOf(System.currentTimeMillis())).getBytes();
//                    System.out.println(packet.getAddress()+"\t"+packet.getPort()+"\t"+System.currentTimeMillis());
                    server.send(new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort()));
				}
				else if(d != Integer.MAX_VALUE && d != 98) {
                    long time = Long.parseLong(data.substring(4, 17));
//                    System.out.println("[B"+bid+"S"+(sid+1)+"] "+(System.currentTimeMillis()-time)*0.001);
                    BrickHandler.insert(bid, sid, d, time);
                }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}
}