package nju.ics.lixiaofan.monitor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AppServer {
	private static ServerSocket server = null;
	private static List<Socket> sockets = new ArrayList<Socket>();
	private static HashMap<Socket, ObjectInputStream> in = new HashMap<Socket, ObjectInputStream>();
	private static HashMap<Socket, ObjectOutputStream> out = new HashMap<Socket, ObjectOutputStream>();
	private static Runnable listener = new Runnable() {
		public void run() {
			try {
				server = new ServerSocket(11111);
			} catch (IOException e) {
				e.printStackTrace();
			}
			PkgHandler handler = new PkgHandler(sockets, in, out);
			new Thread(handler, "PkgHandler").start();
			while(true){
				try {
					Socket socket = server.accept();
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					handler.sendInitialInfo(oos);
					synchronized (sockets) {
						in.put(socket, new ObjectInputStream(socket.getInputStream()));
						out.put(socket, oos);
						sockets.add(socket);
						sockets.notify();
					}
					new Thread(new PkgHandler.Receiver(socket), "PkgHandler Receiver").start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	public AppServer() {
		new Thread(listener, "App Server").start();
	}
}
