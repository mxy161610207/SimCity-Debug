package nju.xiaofanli.application.monitor;

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
	private static final int PORT = 11111;
	private static final List<Socket> sockets = new ArrayList<>();
	private static HashMap<Socket, ObjectInputStream> in = new HashMap<>();
	private static HashMap<Socket, ObjectOutputStream> out = new HashMap<>();
	private static Runnable listener = () -> {
        try {
            server = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PkgHandler handler = new PkgHandler(sockets, in, out);
        new Thread(handler, "PkgHandler").start();
		//noinspection InfiniteLoopStatement
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
    };
	
	public AppServer() {
		new Thread(listener, "App Server").start();
	}
}
