package nju.ics.lixiaofan.car;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

//car remote control
public class CarRC {
//	public int id;
//	public int type; // 0: battletank	1: tankbot	2: carbot 3: zenwheels
//	public int location; // 0: north-west	1: north-east	2: south-west	3: south-east
	public int key;
	public String name, address;
	public long lastInstrTime = System.currentTimeMillis();
	final Socket socket;
	public final DataInputStream in;
	private final DataOutputStream out;
	
	public CarRC(Socket socket) throws IOException {
//		this.type = type;
//		this.location = loc;
		this.socket = socket;
		this.in = new DataInputStream(socket.getInputStream());
		this.out = new DataOutputStream(socket.getOutputStream());
	}
	
	public void write(String str){
		if(out == null || str == null)
			return;
		synchronized (out) {
			try {
				out.writeUTF(str);
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static List<RemoteDevice> devices = new ArrayList<>();
	private static List<String> services = new ArrayList<>();
	public static void main(String[] args) {
		final Object lock = new Object();
//		DiscoveryListener  listener = new DiscoveryListener(){
//		    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
//		        String name;
//		        try {
//		            name = btDevice.getFriendlyName(false);
//		        } catch (Exception e) {
//		            name = btDevice.getBluetoothAddress();
//		        }
//		        System.out.println("device found: " + name);
//		        devices.add(btDevice);
//		    }
//
//		    public void inquiryCompleted(int discType) {
//		        synchronized(lock){
//		            lock.notify();
//		        }
//		    }
//
//		    public void serviceSearchCompleted(int transID, int respCode) {
//		    	synchronized(lock){
//		            lock.notify();
//		        }
//		    }
//
//		    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
//		    	for (int i = 0; i < servRecord.length; i++) {
//                    String url = servRecord[i].getConnectionURL(ServiceRecord.AUTHENTICATE_ENCRYPT, true);
//                    if (url == null)
//                        continue;
//                    DataElement serviceName = servRecord[i].getAttributeValue(0x0100);
//                    if (serviceName != null)
//                        System.out.println("service " + serviceName.getValue() + " found " + url);
//                    else
//                        System.out.println("service found " + url);
//                    services.add(url);
//                }
//		    }
//		};
//		
//		LocalDevice localDevice;
//		DiscoveryAgent agent = null;
//		try {
//			// 1
//			localDevice = LocalDevice.getLocalDevice();
//			// 2
//			agent = localDevice.getDiscoveryAgent();
//			// 3
//			agent.startInquiry(DiscoveryAgent.GIAC, listener);
//			try {
//				synchronized (lock) {
//					lock.wait();
//				}
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			System.out.println("Device Inquiry Completed. ");
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		UUID[] uuidSet = new UUID[] { new UUID(0x1101) };//SPP
//        int[] attrIDs =  new int[] {0x0100};//Service name
//        
//        for(RemoteDevice device : devices){
//        	if(!device.getBluetoothAddress().equals("00066661A901"))
//        		continue;
//        	try {
//				System.out.println("search services on " + device.getBluetoothAddress() + " " + device.getFriendlyName(false));
//				agent.searchServices(attrIDs, uuidSet, device, listener);
//            } catch (IOException e) {
//				e.printStackTrace();
//			}
//	        synchronized(lock) {
//	            try {
//	            	lock.wait();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//	        }
//        }
		
		
		class BTListener extends Thread{
			InputStream in;
			public BTListener(InputStream in) {
				this.in = in;
			}
			public void run() {
				while(true){
					try {
						System.out.println(readInt(in));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		    private int readAll(InputStream is, byte[] b) throws IOException {
		        int left = b.length;
		        while(left > 0) {
		            int ret = is.read(b, b.length - left, left);
		            if(ret <= 0)
		                 throw new IOException("read failed, socket might closed or timeout, read ret: " + ret);
		            left -= ret;
		        }
		        return b.length;
		    }

		    private int readInt(InputStream is) throws IOException {
		        byte[] ibytes = new byte[4];
		        int ret = readAll(is, ibytes);
		        ByteBuffer bb = ByteBuffer.wrap(ibytes);
		        bb.order(ByteOrder.nativeOrder());
		        return bb.getInt();
		    }
		}
		
        byte[] lightsOn = ByteBuffer.allocate(4).putInt(0x8502).array();
        byte[] lightsOff = ByteBuffer.allocate(4).putInt(0x8500).array();
        Map<String, String> map = new HashMap<>();
//        map.put(Car.RED, "btspp://000666619F38:1;authenticate=false;encrypt=false;master=false");//red
//        map.put(Car.BLACK, "btspp://00066649A8C4:1;authenticate=false;encrypt=false;master=false");//black
//        map.put(Car.WHITE, "btspp://00066661AA61:1;authenticate=false;encrypt=false;master=false");//white
        map.put(Car.SILVER, "btspp://00066661A901:1;authenticate=true;encrypt=true");//silver
//        map.put(Car.GREEN, "btspp://000666459D35:1;authenticate=false;encrypt=false;master=false");//green
//        map.put(Car.ORANGE, "btspp://00066649960C:1;authenticate=false;encrypt=false;master=false");//orange
        for(Map.Entry<String, String> entry : map.entrySet()){
        	try {
        		System.out.println("connect to " + entry.getKey());
        		StreamConnection streamConn = (StreamConnection) Connector.open(entry.getValue());
				new BTListener(streamConn.openInputStream()).start();
//        		DataOutputStream dos = streamConn.openDataOutputStream();
//        		boolean on = true;
//				while(true){
//					if(on)
//						dos.write(lightsOn);
//					else
//						dos.write(lightsOff);
//					dos.flush();
//					on = !on;
//					try {
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
        	} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        synchronized (lock) {
        	try {
				lock.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
