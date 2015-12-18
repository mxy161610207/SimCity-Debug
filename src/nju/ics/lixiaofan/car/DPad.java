package nju.ics.lixiaofan.car;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;

import nju.ics.lixiaofan.dashboard.Dashboard;

public class DPad extends JPanel{
	private static final long serialVersionUID = 1L;
	private JButton jbf = new JButton("Forward");
	private JButton jbb = new JButton("Backward");
	private JButton jbl = new JButton("Left");
	private JButton jbr = new JButton("Right");
	private JButton jbs = new JButton("Stop");
	private JButton startB = new JButton("Beep");
	
	private boolean startBeep = false;
	private Object beepObj = new Object();
	private Runnable beepThread = new Runnable() {
		boolean moving = false;
		@Override
		public void run() {
			try {
				while(true){
					while(!startBeep)
						synchronized (beepObj) {
							beepObj.wait();
						}
	
					if(moving){
						moving = false;
						Command.send(Dashboard.getSelectedCar(), 0);
					}
					else{
						moving = true;
						Command.send(Dashboard.getSelectedCar(), 1);
					}
					Thread.sleep(250);
				} 
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	
	public DPad() {
//		setLayout(new GridLayout(3, 3, 5, 5));
//		add(new JPanel());
//		add(jbf);
//		add(new JPanel());
//		add(jbl);
//		add(jbs);
//		add(jbr);
//		add(new JPanel());
//		add(jbb);
//		add(startB);
		setLayout(new GridLayout(1, 2, 5, 5));
		add(jbf);
		add(jbs);
		jbl.setEnabled(false);
		jbr.setEnabled(false);
		jbb.setEnabled(false);
		startB.setEnabled(false);
		jbs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(Dashboard.getSelectedCar() != null){
					Dashboard.getSelectedCar().finalState = 0;
					Command.send(Dashboard.getSelectedCar(), 0);
					Dashboard.getSelectedCar().sendRequest(0);
				}
			}
		});
		jbf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(Dashboard.getSelectedCar() != null){
					Dashboard.getSelectedCar().finalState = 1;
					Dashboard.getSelectedCar().sendRequest(1);
				}
			}
		});
		jbb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(Dashboard.getSelectedCar() != null)
					Command.send(Dashboard.getSelectedCar(), 2);
			}
		});
		jbl.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(Dashboard.getSelectedCar() != null)
					Command.send(Dashboard.getSelectedCar(), 3);
			}
		});
		jbr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(Dashboard.getSelectedCar() != null)
					Command.send(Dashboard.getSelectedCar(), 4);
			}
		});
		startB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(startBeep){
					startBeep = false;
				}
				else{
					startBeep = true;
					synchronized (beepObj) {
						beepObj.notify();
					}		
				}
			}
		});	
		new Thread(beepThread).start();
	}
}
