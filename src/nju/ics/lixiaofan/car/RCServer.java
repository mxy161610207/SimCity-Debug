package nju.ics.lixiaofan.car;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class RCServer{
	private static ServerSocket server = null;
	public static CarRC rc = null;
	private static Runnable listener = new Runnable() {
		public void run() {
			try {
				server = new ServerSocket(8888);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			while(true){
				try {
					Socket socket = server.accept();
					rc = new CarRC(0, 0, socket, new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
					Thread t = new Thread(new RCListener(socket));
					t.setDaemon(true);
					t.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		}
	};
	
	public RCServer() {
		new Thread(listener).start();
		new Thread(new CmdSender()).start();
		new Thread(new Remediation()).start();
	}
	
}
