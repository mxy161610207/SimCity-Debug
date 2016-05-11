package nju.ics.lixiaofan.brick;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.robotics.SampleProvider;

public class Client {
//	static Object obj = new Object();
//	static Socket server = null;
//	static DataOutputStream dos = null;
//	static DataInputStream dis = null;
	
	static DatagramSocket server = null;
	static int sensorNum;
	static int bid = -1;
	
	public static void main(String[] args) {
		System.out.println("Running...");
		try {
//			System.out.println("Connecting...");
//			server = new Socket("192.168.1.100",9999);
//			server.setTcpNoDelay(true);
//			dos = new DataOutputStream(server.getOutputStream());
//			dis = new DataInputStream(server.getInputStream());
			server = new DatagramSocket();
//			System.out.println("Connected");
//			sensorNum = dis.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
			try {
				InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				bid = Integer.parseInt(e.getMessage().substring(1, 2));
//				e.printStackTrace();
			}
		switch(bid){
		case 0:case 9:
			sensorNum = 2; break;
		case 1:case 4:case 5:case 8:
			sensorNum = 3; break;
		default:
			sensorNum = 4;
		}
			
		new SensorHandler().start();
//		Port port[] = new Port[sensorNum];
//		for(int i = 0;i < sensorNum;i++){
//			port[i] = LocalEV3.get().getPort("S" + (i+1));
//			IRSensorThread sensor = new IRSensorThread(i, port[i]);
//			sensor.start();
//		}
	}
}

class SensorHandler extends Thread{
	int sensorNum;
	EV3IRSensor[] ir;
	SampleProvider[] sp;
	boolean exit = false;
	int[] dis, preDis;
	long[] lastSentTime;
	public SensorHandler() {
		sensorNum = Client.sensorNum;
		ir = new EV3IRSensor[sensorNum];
		sp = new SampleProvider[sensorNum];
		dis = new int[sensorNum];
		preDis = new int[sensorNum];
		lastSentTime = new long[sensorNum];
		try{
			for(int i = 0;i < sensorNum;i++){
				ir[i] = new EV3IRSensor(LocalEV3.get().getPort("S" + (i+1)));
				sp[i] = ir[i].getDistanceMode();
				dis[i] = 255;
				preDis[i] = -1;
				lastSentTime[i] = 0;
			}
		}
		catch(IllegalArgumentException e){
    		exit = true;
    	}
		
	}
	public void run() {
    	float [] sample = null;
    	byte[] buf = new byte[12];
    	int2byte(Client.bid, buf, 0);
    	DatagramPacket packet = null;
    	long curTime;
    	try {
			packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("192.168.1.100"),9999);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
    	while(!exit){
    		sample = new float[sp[0].sampleSize()];
    		for(int i = 0;i < preDis.length;i++)
    			preDis[i] = -1;
    		
	        while (!exit){
	        	for(int i = 0;i < sensorNum;i++){
		            sp[i].fetchSample(sample, 0);
		            dis[i] = (int)sample[0];
		            curTime = System.currentTimeMillis();
		            if(preDis[i] != dis[i] || curTime - lastSentTime[i] >= 50){
			            try {
//			            	Client.dos.writeUTF((i+1) + "_" + dis[i]);
//			            	Client.dos.flush();
			            	int2byte(i, buf, 4);
			            	int2byte(dis[i], buf, 8);
			            	Client.server.send(packet);
//			            	if(i == 3)
//			            		System.out.println((i+1) + "_" + dis[i]);
						} catch (IOException e) {
		//					e.printStackTrace();
							System.out.println("Connection is broken");
							exit = true;
							break;
						}
			            preDis[i] = dis[i];
			            lastSentTime[i] = curTime;
		            }
	        	}
	        	try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
//					e.printStackTrace();
				}
	        }
    	}
	}
	
	public static void int2byte(int res, byte[] b, int offset) {  
		b[offset] = (byte) (res & 0xff);// 最低位   
		b[offset+1] = (byte) ((res >> 8) & 0xff);// 次低位   
		b[offset+2] = (byte) ((res >> 16) & 0xff);// 次高位   
		b[offset+3] = (byte) (res >>> 24);// 最高位,无符号右移。   
	}
}
/*
class IRSensorThread extends Thread{
    EV3IRSensor ir;
    SampleProvider sp;
    int id;
    int distance = 255;
    boolean exit = false;
//    static double[] enteringValue, leavingValue;
//    DataOutputStream dos = null;
   
    IRSensorThread(int id, Port port){
    	this.id = id;
    	try{
    		ir = new EV3IRSensor(port);
    		sp = ir.getDistanceMode();  		
//    		this.dos = dos;
//    		this.dos = Client.dos;
    	}
    	catch(IllegalArgumentException e){
    		exit = true;
//    		System.out.println("id: "+id+" IllegalArgumentException");
    	}
//		enteringValue = Client.inValue[Client.cid];
//		leavingValue = Client.outValue[Client.cid];
    }
    
    public void run(){
    	float [] sample = null;
//    	if(!exit)
//    		sample = new float[sp.sampleSize()];
//    	if(!exit)
//    		 System.out.println("id: "+id+" doesn't exit");
//    	else
//    		System.out.println("id: "+id+" exit");
    	while(!exit){
    		int prevDis = -1;
    		sample = new float[sp.sampleSize()];
	        while (true){         
//	        	sample = new float[sp.sampleSize()];
	            sp.fetchSample(sample, 0);
	            distance = (int)sample[0];
	            if(prevDis != distance){
		            try {
		            	synchronized (Client.dos) {
		            		Client.dos.writeUTF((id+1) + "_" + distance);
		            		Client.dos.flush();
						}
					} catch (IOException e) {
	//					e.printStackTrace();
	//					System.out.println(id+": Connection is broken");
						System.out.println("Connection is broken");
						break;
					}
		            prevDis = distance;
	            }
	        }
	        if(id == 0){
	        	while(true){
		        	try {
		        		System.out.println("Reconnecting...");
		    			Client.server = new Socket("192.168.1.100",9999);
		    			Client.server.setTcpNoDelay(true);
		    			Client.dos = new DataOutputStream(Client.server.getOutputStream());
		    			Client.dis = new DataInputStream(Client.server.getInputStream());
		    			System.out.println("Reconnected");
		    			synchronized (Client.obj) {
		    				Client.obj.notifyAll();
		    			}
		    			break;
		    		} catch (IOException e) {
		    			e.printStackTrace();
		    		}
		        	try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	        	}
	        }
	        else{
	        	synchronized (Client.obj) {
		        	try {
						Client.obj.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
	        }
    	}
    }
}
*/