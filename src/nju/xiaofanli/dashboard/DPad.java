package nju.xiaofanli.dashboard;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.device.car.Car;

import javax.swing.*;
import java.awt.*;

class DPad extends JPanel{
	private static final long serialVersionUID = 1L;
    //	private JButton startB = new JButton("Beep");
	
//	private boolean startBeep = false;
//	private Object beepObj = new Object();
//	private Runnable beepThread = new Runnable() {
//		boolean moving = false;
//		@Override
//		public void run() {
//			try {
//				while(true){
//					while(!startBeep)
//						synchronized (beepObj) {
//							beepObj.wait();
//						}
//	
//					if(moving){
//						moving = false;
//						Command.send(Dashboard.getSelectedCar(), 0);
//					}
//					else{
//						moving = true;
//						Command.send(Dashboard.getSelectedCar(), 1);
//					}
//					Thread.sleep(250);
//				} 
//			}
//			catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//	};
	
	DPad() {
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
		JButton jbf = new JButton("Drive");
        jbf.setFont(Dashboard.bold14dialog);
		add(jbf);
        JButton jbs = new JButton("Stop");
        jbs.setFont(Dashboard.bold14dialog);
        add(jbs);
        JButton jbl = new JButton("Left");
        jbl.setEnabled(false);
        JButton jbr = new JButton("Right");
        jbr.setEnabled(false);
        JButton jbb = new JButton("Backward");
        jbb.setEnabled(false);
//		startB.setEnabled(false);
		jbs.addActionListener(arg -> {
			Car car = Dashboard.getSelectedCar();
            if(car != null){
                car.finalState = Car.STOPPED;
                car.notifyPolice(Police.REQUEST2STOP);
            }
        });
		jbf.addActionListener(arg -> {
            Car car = Dashboard.getSelectedCar();
            if(car != null){
                car.finalState = Car.MOVING;
                car.notifyPolice(Police.REQUEST2ENTER);
            }
        });
//		jbb.addActionListener(arg -> {
//            if(Dashboard.getSelectedCar() != null)
//                Command.send(Dashboard.getSelectedCar(), 2);
//        });
//		jbl.addActionListener(arg -> {
//            if(Dashboard.getSelectedCar() != null)
//                Command.send(Dashboard.getSelectedCar(), 3);
//        });
//		jbr.addActionListener(arg -> {
//            if(Dashboard.getSelectedCar() != null)
//                Command.send(Dashboard.getSelectedCar(), 4);
//        });
//		startB.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				if(startBeep){
//					startBeep = false;
//				}
//				else{
//					startBeep = true;
//					synchronized (beepObj) {
//						beepObj.notify();
//					}		
//				}
//			}
//		});	
//		new Thread(beepThread).start();
	}
}
