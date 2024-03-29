package nju.xiaofanli.device.sensor;

import nju.xiaofanli.Resource;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.io.*;

public class BrickServer implements Runnable{
	private static DatagramSocket server = null;
	private static final int PORT = 9999;
	//	private static long recvTime[] = new long[10];
	public static Sensor showingSensor = null;
	// mxy_edit: for each sensor, record latest recv timestamp
	private static final long[][] sensorLatestRecvTimestamp = new long[10][4];
	private static final int[][] sensorLatestTermId = new int[10][4];
	static{
		for (int i = 0; i < 10; i++) {
			for(int j=0;j<4;j++){
				sensorLatestRecvTimestamp[i][j]=-1;
				sensorLatestTermId[i][j]=-1;
			}
		}
	}


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
		byte[] buf = new byte[100];//new byte[1024];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		int previousTermId = -1;
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
					System.out.println("["+showingSensor.name+"] reading: "+d+"\ttime: "+Long.parseLong(data.substring(4, 17)));
//				if(((pre[bid][sid] + 1) % 100) != d)
//					System.err.println("B"+bid+"S"+(sid+1)+":"+pre[bid][sid]+" "+d);
//				pre[bid][sid] = d;
//				recvTime[bid] = System.currentTimeMillis();
//				for(int i = 0;i < 10;i++)
//					System.out.print(i+":"+(System.currentTimeMillis() - recvTime[i])/1000+"\t");
				if (d == 99) { // clock synchronization
//					System.out.println("clock sync");

					byte[] buffer = data.substring(0, 2).concat(String.valueOf(System.currentTimeMillis())).getBytes();
//                    System.err.println(packet.getAddress()+"\t"+packet.getPort()+"\t"+System.currentTimeMillis());

					// mxy_edit: log for each brick
					String FileName = "B" + (data.charAt(4) - '0') + ".txt";
					File f= new File("mxy_temp\\Sensor\\"+FileName);
					try (FileOutputStream fop = new FileOutputStream(f,true)){
						if(!f.exists()){
							f.createNewFile();
						}
						// mxy_edit: modify the timestamp format
						SimpleDateFormat sDateFormat=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
						String time_str=sDateFormat.format(System.currentTimeMillis());

						String s = time_str
								+ "  sync_id =" + Integer.parseInt(data.substring(0, 2))
								+ "\n";
						byte[] content=s.getBytes();
						fop.write(content);
						fop.flush();
					}catch (IOException e) {
						e.printStackTrace();
					}

					server.send(new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort()));
				}
				else if(d != Integer.MAX_VALUE && d != 98) {
//					System.out.println("infrared sensor data coming");
					int termId = Integer.parseInt(data.substring(4, 8))-1000;
					int lastTermId = sensorLatestTermId[bid][sid];
					sensorLatestTermId[bid][sid]=termId;


					long time = Long.parseLong(data.substring(8, 21));
					// mxy_edit: temporary remove delay log
					// if (Math.abs(System.currentTimeMillis()-time) >= 500)
					// 	System.err.println("[B"+bid+"S"+(sid+1)+"] delay: "+(System.currentTimeMillis()-time)+"ms");
					// == EDIT END ==
					BrickHandler.insert(bid, sid, d, time);

					// mxy_edit: log for each sensor
					String FileName = "B" + bid + "S" + (sid + 1) + ".txt";
					File f= new File("mxy_temp\\Sensor\\"+FileName);
					try (FileOutputStream fop = new FileOutputStream(f,true)){
						if(!f.exists()){
							f.createNewFile();
						}
						// mxy_edit: modify the timestamp format
						SimpleDateFormat sDateFormat=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
						long pcTime = System.currentTimeMillis();
						String time_str=sDateFormat.format(time);
						long time_abs = Math.abs(pcTime-time);

						long last_recv = sensorLatestRecvTimestamp[bid][sid];
						sensorLatestRecvTimestamp[bid][sid] = time;
						double time_gap = (double) (time - last_recv) / 1000.0;
						if (last_recv == -1) time_gap = -1.0;
//									if (Math.abs(time_gap - 3.0)>=0.1 || time_gap<3.0){
//										System.err.println("Error time gap: ");
//									}

						String s = "[B" + bid + "S" + (sid + 1) + "] : dis=" + String.format("%2d", d)
								+ "  " + time_str
								+ "  net_delay =" + String.format("%3d", time_abs) + "ms"
								+ "  time_gap =" + String.format("%.3f", time_gap) + "s"
								+ "  termId = " + termId
								+ "\n";

						if ((lastTermId+1)%1000!=termId && lastTermId!=-1) {
							System.err.print("[Pkg loss "+lastTermId+"-"+termId+"]" + s);
						}
						if (Math.abs(System.currentTimeMillis()-time) >= 500) {
							System.err.print("[time delay]" + s);
						}


						byte[] content=s.getBytes();

						fop.write(content);
						fop.flush();
					}catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}