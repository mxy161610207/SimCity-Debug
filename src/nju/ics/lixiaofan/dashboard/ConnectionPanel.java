package nju.ics.lixiaofan.dashboard;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import nju.ics.lixiaofan.car.RCServer;

public class ConnectionPanel extends JPanel{
	private static final long serialVersionUID = 1L;
	private JTextArea ctrlta = new JTextArea(), rcta = new JTextArea();
	private JScrollPane ctrltaScroll = new JScrollPane(ctrlta), rctaScroll = new JScrollPane(rcta);
	public ConnectionPanel() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		setLayout(gbl);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(2, 5, 2, 5);
//		gbc.weightx = 1;
		
		JLabel ctrllabel = new JLabel("EV3 Controller");
		gbc.gridy = 0;
		gbl.setConstraints(ctrllabel, gbc);
		add(ctrllabel);
		
		gbc.gridy = 1;
		gbc.weighty = 1;
		gbl.setConstraints(ctrltaScroll, gbc);
		add(ctrltaScroll);
		ctrlta.setEditable(false);
		ctrlta.setLineWrap(true);
		ctrlta.setWrapStyleWord(true);
		updateBrickConn();
		
		JLabel rclabel = new JLabel("Car Remote Control");
		gbc.gridy = 3;
		gbc.weighty = 0;
		gbl.setConstraints(rclabel, gbc);
		add(rclabel);
		
		gbc.gridy = 4;
		gbc.weighty = 1;
		gbl.setConstraints(rctaScroll, gbc);
		add(rctaScroll);
		rcta.setEditable(false);
		rcta.setLineWrap(true);
		rcta.setWrapStyleWord(true);
		updateRCConn();
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		JLabel restLabel = new JLabel("");
		add(restLabel);
		gbl.setConstraints(restLabel, gbc);
	}
	
	public void updateBrickConn(){
		ctrlta.setText("deprecated");
//		ctrlta.setText("Connected: "+BrickServer.bhi.size());
//		for(Map.Entry<Integer, BrickHandlerInfo> entry : BrickServer.bhi.entrySet()){
//			ctrlta.append("\nC"+entry.getKey()+" "+entry.getValue().ip);
//		}
	}
	
	public void updateRCConn(){
//		rcta.setText("Connected: "+RCServer.rcs.size());
		if(RCServer.rc != null)
			synchronized (RCServer.rc) {
				rcta.setText(RCServer.rc.name+"\n"+RCServer.rc.address);
			}
	}
}
