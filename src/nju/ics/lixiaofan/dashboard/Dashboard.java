package nju.ics.lixiaofan.dashboard;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.car.DPad;
import nju.ics.lixiaofan.car.RCServer;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.control.Remediation;
import nju.ics.lixiaofan.control.Delivery.DeliveryTask;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;
import nju.ics.lixiaofan.monitor.AppPkg;
import nju.ics.lixiaofan.monitor.PkgHandler;
import nju.ics.lixiaofan.sensor.BrickHandler;

public class Dashboard extends JFrame{
	private static final long serialVersionUID = 1L;
	private static JTabbedPane tabbedpane = new JTabbedPane();
	private static JPanel p1 = new JPanel();
	private static ConnectionPanel connPanel = new ConnectionPanel();
	private static TrafficMap mapPanel = new TrafficMap();
//	private static P testp = new P();
	private static JPanel leftPanel = new JPanel();
	private static JPanel rightPanel = new JPanel();
	private static JComboBox<String> carbox =  new JComboBox<String>();
	private static JTextArea //cmdta = new JTextArea(), 
			delivta = new JTextArea(),
			remedyta = new JTextArea(),
//			carta = new JTextArea(),
			roadta = new JTextArea(),
			logta = new JTextArea();
	private static JScrollPane //cmdtaScroll = new JScrollPane(cmdta),
			delivtaScroll = new JScrollPane(delivta),
			remedytaScroll = new JScrollPane(remedyta),
//			cartaScroll = new JScrollPane(carta),
			roadtaScroll = new JScrollPane(roadta),
			logtaScroll = new JScrollPane(logta);
	private static VehicleConditionPanel VCPanel = new VehicleConditionPanel();
	private static ButtonGroup radioButtonGroup = new ButtonGroup();
	private static JRadioButton[] dirButtons = { new JRadioButton("North"),
			new JRadioButton("South"), new JRadioButton("West"),
			new JRadioButton("East") };
	private static JCheckBox jchkSensor = new JCheckBox("Sensors"), jchkSection = new JCheckBox("Sections"); 
	private static JTextArea srcta = new JTextArea(), dstta = new JTextArea();
//	private static Shop srcShop = null, dstShop = null;
	private static Section srcSect = null, dstSect = null;
	private static JPanel deliveryPanel = new JPanel();
	private static boolean isDeliveryStarted = false;
	public static boolean blink = false;
	private static Runnable blinkThread = new Runnable() {
		public void run() {
			blink = !blink;
			while(true){
				for(Section s : TrafficMap.sections){
					if(s.cars.isEmpty())
						continue;
					if (s.cars.size() > 1 || s.cars.peek().isLoading)
						s.btn.repaint();
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
//	public static void main(String[] args) throws IOException {
//	}

	public Dashboard() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		add(tabbedpane);
		tabbedpane.add("Map&Control", p1);
		tabbedpane.add("Connection", connPanel);
		
		gbc.fill = GridBagConstraints.BOTH;
		p1.setLayout(gbl);
		p1.add(mapPanel);
		p1.add(leftPanel);
		p1.add(rightPanel);
		
		String mapLen = "";
		for(int i = 0;i < 19;i++)
			mapLen = mapLen.concat("          ");
		JLabel maplenLabel = new JLabel(mapLen);
		p1.add(maplenLabel);
		
		for(Section s : TrafficMap.sections)
			s.btn.addMouseListener(new SectionIconListener(s));
		
		gbc.gridx = 0;
		gbc.weightx = gbc.weighty = 1;
		gbl.setConstraints(leftPanel, gbc);
		gbc.gridx = 1;
		gbc.weightx = 0;
		gbl.setConstraints(mapPanel, gbc);
		gbc.gridy = 0;
		gbc.weighty = 0;
		gbl.setConstraints(maplenLabel, gbc);
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.weighty = 1;
		gbl.setConstraints(rightPanel, gbc);
		
		mapPanel.setBackground(Color.WHITE);

		gbc.insets = new Insets(1, 5, 1, 5);
		gbc.gridx = gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		leftPanel.setLayout(gbl);
		
		JButton resetButton = new JButton("Reset");
		gbl.setConstraints(resetButton, gbc);
		leftPanel.add(resetButton);
		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(int i = 0;i < TrafficMap.crossings.length;i++){
						TrafficMap.crossings[i].isOccupied = false;
						TrafficMap.crossings[i].cars.clear();
						TrafficMap.crossings[i].waitingCars.clear();
						TrafficMap.crossings[i].admittedCar = null;
				}
				for(int i = 0;i < TrafficMap.streets.length;i++){
						TrafficMap.streets[i].isOccupied = false;
						TrafficMap.streets[i].cars.clear();
						TrafficMap.streets[i].waitingCars.clear();
						TrafficMap.streets[i].admittedCar = null;
				}
			
				for(Car car : RCServer.cars.values()){
					car.loc = null;
					car.dir = -1;
					car.state = 0;
					car.expectation = 0;
					car.finalState = 0;
					car.dest = null;
					car.isLoading = false;
				}
				
				BrickHandler.resetState();
				mapPanel.repaint();
			}
		});
		
		JLabel delivlabel = new JLabel("Delivery Tasks                ");
		gbc.gridy = 1;
		gbl.setConstraints(delivlabel, gbc);
		leftPanel.add(delivlabel);
		
		gbc.gridy = 2;
		gbc.weighty = 1;
		gbl.setConstraints(delivtaScroll, gbc);
		leftPanel.add(delivtaScroll);
		delivta.setLineWrap(true);
		delivta.setWrapStyleWord(true);
		delivta.setEditable(false);
		updateDelivQ();
		
		JLabel remedylabel = new JLabel("Remediation Cmds");
		gbc.gridy = 3;
		gbc.gridheight = 1;
		gbc.weighty = 0;
		gbl.setConstraints(remedylabel, gbc);
		leftPanel.add(remedylabel);
		
		gbc.gridy = 4;
		gbc.weighty = 1;
		gbl.setConstraints(remedytaScroll, gbc);
		leftPanel.add(remedytaScroll);
		remedyta.setLineWrap(true);
		remedyta.setWrapStyleWord(true);
		remedyta.setEditable(false);
		updateRemedyQ();
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weighty = 0;
		JLabel vcLabel = new JLabel("Vehicle Condition                     ");
		gbl.setConstraints(vcLabel, gbc);
		leftPanel.add(vcLabel);
		
		gbc.gridy = 1;
		gbc.gridheight = 2;
		gbc.weighty = 0;
		gbl.setConstraints(VCPanel, gbc);
		leftPanel.add(VCPanel);
//		carta.setLineWrap(true);
//		carta.setWrapStyleWord(true);
//		carta.setEditable(false);
		gbc.gridheight = 1;
		
		gbc.gridy = 3;
		JLabel rcLabel = new JLabel("Road Condition");
		gbl.setConstraints(rcLabel, gbc);
		leftPanel.add(rcLabel);
		
		gbc.gridy = 4;
//		gbc.gridheight = 2;
//		gbc.weighty = 0;
		gbl.setConstraints(roadtaScroll, gbc);
		leftPanel.add(roadtaScroll);
		roadta.setLineWrap(true);
		roadta.setWrapStyleWord(true);
		roadta.setEditable(false);
		
		//right panel settings
		gbc.gridx = gbc.gridy = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.weightx = gbc.weighty = 0;
		rightPanel.setLayout(gbl);	
		
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(carbox,gbc);	
		rightPanel.add(carbox);
		carbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Car selectedCar = getSelectedCar();
				if(selectedCar != null){
					for(int i = 0;i < 4;i++){
						dirButtons[i].setEnabled(false);
//						dirButtons[i].setSelected(false);
					}
					radioButtonGroup.clearSelection();
					
					Section sect = selectedCar.loc;
					if(sect != null){
						dirButtons[sect.dir[0]].setEnabled(true);
						if(sect.dir[1] > 0)
							dirButtons[sect.dir[1]].setEnabled(true);
						if(selectedCar.dir >= 0 && dirButtons[selectedCar.dir].isEnabled()){
							dirButtons[selectedCar.dir].setSelected(true);
						}
						else{
							dirButtons[sect.dir[0]].setSelected(true);
							selectedCar.dir = (byte) sect.dir[0];
						}
					}
					else
						selectedCar.dir = -1;
				}
			}
		});
		
		gbc.gridy = 1;
		JPanel radioButtonsPanel = new JPanel();
		gbl.setConstraints(radioButtonsPanel, gbc);
		rightPanel.add(radioButtonsPanel);
		radioButtonsPanel.setLayout(new GridLayout(1, 4));
		
		for(int i = 0;i < 4;i++){
			dirButtons[i].setEnabled(false);
			radioButtonsPanel.add(dirButtons[i]);
			radioButtonGroup.add(dirButtons[i]);
		}
		dirButtons[0].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Car car = getSelectedCar();
				if(car != null){
					car.dir = 0;
					AppPkg p = new AppPkg();
					p.setDir(car.name, car.dir);
					PkgHandler.send(p);
				}
			}
		});
		dirButtons[1].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Car car = getSelectedCar();
				if(car != null){
					car.dir = 1;
					AppPkg p = new AppPkg();
					p.setDir(car.name, car.dir);
					PkgHandler.send(p);
				}
			}
		});
		dirButtons[2].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Car car = getSelectedCar();
				if(car != null){
					car.dir = 2;
					AppPkg p = new AppPkg();
					p.setDir(car.name, car.dir);
					PkgHandler.send(p);
				}
			}
		});
		dirButtons[3].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Car car = getSelectedCar();
				if(car != null){
					car.dir = 3;
					AppPkg p = new AppPkg();
					p.setDir(car.name, car.dir);
					PkgHandler.send(p);
				}
			}
		});
		
		gbc.gridy = 2;
		DPad cframe = new DPad();
		gbl.setConstraints(cframe, gbc);
		rightPanel.add(cframe);
		
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		jchkSection.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionevent) {
				TrafficMap.showSections = jchkSection.isSelected();
				mapPanel.repaint();
			}
		});
		gbl.setConstraints(jchkSection, gbc);
		rightPanel.add(jchkSection);
		
		gbc.gridx = 1;
		jchkSensor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				TrafficMap.showSensors = jchkSensor.isSelected();
				mapPanel.repaint();
			}
		});
		gbl.setConstraints(jchkSensor, gbc);
		rightPanel.add(jchkSensor);
		
		JLabel srclabel = new JLabel("Src:");
		srcta.setEditable(false);
		JLabel dstlabel = new JLabel("Dst:");
		dstta.setEditable(false);
		
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(deliveryPanel, gbc);
		rightPanel.add(deliveryPanel);
		deliveryPanel.setBorder(BorderFactory.createTitledBorder("Delivery"));
		deliveryPanel.add(srclabel);
		deliveryPanel.add(srcta);
		deliveryPanel.add(dstlabel);
		deliveryPanel.add(dstta);
		final JButton deliverButton = new JButton("Deliver");
		final JButton startdButton = new JButton("Start");
		final JButton canceldButton = new JButton("Cancel");
		deliveryPanel.add(deliverButton);
		deliveryPanel.add(startdButton);
		deliveryPanel.add(canceldButton);
		deliverButton.setVisible(false);
		canceldButton.setVisible(false);
		
		GridBagLayout dgbl = new GridBagLayout();
		GridBagConstraints dgbc = new GridBagConstraints();
		dgbc.insets = new Insets(5, 5, 5, 5);
		dgbc.fill = GridBagConstraints.BOTH;
		deliveryPanel.setLayout(dgbl);
		dgbl.setConstraints(srclabel, dgbc);
		dgbc.gridx = 1;
		dgbc.weightx = 1;
		dgbc.gridwidth = GridBagConstraints.REMAINDER;
		dgbl.setConstraints(srcta, dgbc);
		dgbc.gridx = 0;
		dgbc.gridy = 1;
		dgbc.weightx = 0;
		dgbl.setConstraints(dstlabel, dgbc);
		dgbc.gridx = 1;
		dgbc.weightx = 1;
		dgbl.setConstraints(dstta, dgbc);
		dgbc.gridx = 0;
		dgbc.gridy = 2;
		dgbc.gridwidth = GridBagConstraints.REMAINDER;
		dgbc.weightx = 1;
		dgbl.setConstraints(startdButton, dgbc);
		dgbc.gridwidth = 2;
		dgbl.setConstraints(deliverButton, dgbc);
		dgbc.gridx = 2;
		dgbl.setConstraints(canceldButton, dgbc);
		startdButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				srcSect = dstSect = null;
//				srcShop = dstShop = null;
				updateDeliverySrc();
				updateDeliveryDst();
				isDeliveryStarted = true;
				startdButton.setVisible(false);
				deliverButton.setVisible(true);
				canceldButton.setVisible(true);
			}
		});
		deliverButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				isDeliveryStarted = false;
				deliverButton.setVisible(false);
				startdButton.setVisible(true);
				canceldButton.setVisible(false);
				
				Delivery.add(srcSect, dstSect);
			}
		});
		
		canceldButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isDeliveryStarted = false;
				deliverButton.setVisible(false);
				startdButton.setVisible(true);
				canceldButton.setVisible(false);
			}
		});
		
		gbc.gridy = 5;
		JLabel logLabel = new JLabel("Log");
		gbl.setConstraints(logLabel, gbc);
		rightPanel.add(logLabel);
		
		gbc.gridy = 6;
		gbc.weighty = 1;
		gbl.setConstraints(logtaScroll, gbc);
		rightPanel.add(logtaScroll);
		logta.setEditable(false);
		logta.setLineWrap(true);
		logta.setWrapStyleWord(true);
		
		if(!RCServer.cars.isEmpty())
			for(Car car : RCServer.cars.values())
				if(car.isConnected)
					addCar(car);

		new Thread(blinkThread).start();
		
		setTitle("Dashboard");
		setSize(1200,700);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	public static void updateRoadCondition(Section s){
		String str = s.name+"\n";
		if(!s.cars.isEmpty()){
			str += "Cars:\n";
			for(Iterator<Car> it = s.cars.iterator();it.hasNext();){
				Car one = it.next();
				str += one.name + " (" + one.getState() + ") "+one.getDir();
				if(one.dest != null)
					str += " Dest:" + one.dest.name;
				str += "\n";
			}
		}
		if(!s.waitingCars.isEmpty()){
			str += "Waiting Cars:\n";
			for(Iterator<Car> it = s.waitingCars.iterator();it.hasNext();){
				Car one = it.next();
				str += one.name + " (" + one.getState() + ") "+one.getDir()+"\n";
			}
		}
		roadta.setText(str);
	}
	
	public static void updateDeliverySrc(){
		if(srcSect != null)
			srcta.setText(srcSect.name);
//		else if(srcShop != null)
//			srcta.setText(srcShop.name);
		else
			srcta.setText("");
	}
	
	public static void updateDeliveryDst(){
		if(dstSect != null)
			dstta.setText(dstSect.name);
//		else if(dstShop != null)
//			dstta.setText(dstShop.name);
		else
			dstta.setText("");
	}
	
//	public void updateCmdQ(){
//		synchronized (CmdSender.queue) {
//			cmdta.setText("Nums: "+CmdSender.queue.size());
//		}
//	}
	
	public static synchronized void updateBrickConn(){
		connPanel.updateBrickConn();
	}
	
	public static synchronized void updateRCConn(){
		connPanel.updateRCConn();
	}
	
	public static synchronized void updateDelivQ(){
		delivta.setText("Nums: "+(Delivery.searchTasks.size()+Delivery.deliveryTasks.size()));
		for(DeliveryTask dt:Delivery.searchTasks){
			delivta.append("\nPhase: "+dt.phase+" Src: ");
//			if(dt.srcSect == null)
//				delivta.append(dt.srcShop.name+" Dst: ");
//			else
				delivta.append(dt.srcSect.name+" Dst: ");
//			if(dt.dstSect == null)
//				delivta.append(dt.dstShop.name);
//			else
				delivta.append(dt.dstSect.name);
		}
		
		for(DeliveryTask dt:Delivery.deliveryTasks){
			delivta.append("\nPhase: "+dt.phase+" Car: "+dt.car.name+" Src: ");
//			if(dt.srcSect == null)
//				delivta.append(dt.srcShop.name+" Dst: ");
//			else
				delivta.append(dt.srcSect.name+" Dst: ");
//			if(dt.dstSect == null)
//				delivta.append(dt.dstShop.name);
//			else
				delivta.append(dt.dstSect.name);
		}
	}
	
	public static synchronized void updateRemedyQ(){
		remedyta.setText("Nums: "+Remediation.queue.size());
		for(Command cmd:Remediation.queue){
			remedyta.append("\n"+cmd.car.name+" "+((cmd.cmd==0)?"S":"F")+" "+cmd.level+" "+cmd.deadline);
		}
	}
	
	public static synchronized void updateVehicleCondition(Car car){
		VCPanel.updateVC(car);
	}
	
	public static synchronized void appendLog(String str){
		logta.append(str+"\n");
	}
	
	public static synchronized void addCar(Car car){
//		System.out.println(car.name);
		carbox.addItem(car.name);
		PkgHandler.send(new AppPkg(car.name, (byte)-1, null));
		
		if(car.loc != null){
			if(car.dir < 0){
				car.dir = (byte) car.loc.dir[0];
				AppPkg p = new AppPkg();
				p.setDir(car.name, car.dir);
				PkgHandler.send(p);
			}
			carEnter(car, car.loc);
			PkgHandler.send(new AppPkg(car.name, car.dir, car.loc.name));
		}
		VCPanel.addCar(car);
	}
	
	public static void addCar(Collection<Car> cars){
		for(Car car : cars)
			addCar(car);
	}
	
	public static synchronized void removeCar(Car car){
		carbox.removeItem(car.name);
		VCPanel.removeCar(car);
	}
	
	public static Car getSelectedCar(){
		if(carbox.getItemCount() == 0)
			return null;
		return Car.carOf((String) carbox.getSelectedItem());
	}
	
//	public static void setPushed(byte car, byte dir){
//		for(int i = 0;i < carbox.getItemCount();i++){
//			String name = carbox.getItemAt(i);
//			if(Car.carOf(name).uid == car){
//				carbox.setSelectedIndex(i);
//				dirButtons[dir].doClick();
//				break;
//			}
//		}
//	}
	
	public static void carEnter(Car car, Section section){
		if(car == null || section == null)
			return;
		carLeave(car, car.loc);
		
		section.isOccupied = true;
		section.cars.add(car);
		car.loc = section;
		section.btn.repaint();
		if(section.isCombined){
			for(Section s : section.combined){
				s.isOccupied = true;
				s.btn.repaint();
//				s.cars.add(car);
			}
		}
		//trigger move event
		if(EventManager.hasListener(Event.Type.CAR_MOVE))
			EventManager.trigger(new Event(Event.Type.CAR_MOVE, car.name, car.loc.name));
	}
	
	public static void carLeave(Car car, Section section){
		if(car == null || section == null)
			return;
		section.cars.remove(car);
		section.isOccupied = !section.cars.isEmpty();
		if(car.loc == section)
			car.loc = null;
		section.btn.repaint();
		if(section.isCombined){
			for(Section s : section.combined){
//				s.cars.remove(car);
				s.isOccupied = !s.cars.isEmpty();
				s.btn.repaint();
			}
		}
		//trigger leaving event
		if(EventManager.hasListener(Event.Type.CAR_LEAVE))
			EventManager.trigger(new Event(Event.Type.CAR_LEAVE, car.name, section.name));
//		mapPanel.repaint();
	}
	
//	public static void mapRepaint(){
//		mapPanel.repaint();
//	}
	
	private class SectionIconListener implements MouseListener{
		Section section = null;
		
		public SectionIconListener(Section section) {
			this.section = section;
		}
		public void mousePressed(MouseEvent e) {
			if (section == null)
				return;
			// for delivery tasks
			else if (isDeliveryStarted) {
				if (srcSect == null) {// && srcShop == null){
					srcSect = section;
					updateDeliverySrc();
				} else if (dstSect == null) {// && dstShop == null){
					if (section == srcSect)
						return;
					else if (section.isCombined
							&& section.combined.contains(srcSect))
						return;
					dstSect = section;
					updateDeliveryDst();
				}
			} else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
				// left click
				Car selectedCar = getSelectedCar();
				if (selectedCar != null) {
					radioButtonGroup.clearSelection();
					for (int i = 0; i < 4; i++)
						dirButtons[i].setEnabled(false);
					if (section.cars.contains(selectedCar)) {
						selectedCar.dir = -1;
						carLeave(selectedCar, section);
					} else {
						dirButtons[section.dir[0]].setEnabled(true);
						if (section.dir[1] >= 0)
							dirButtons[section.dir[1]].setEnabled(true);

						if (selectedCar.dir >= 0
								&& dirButtons[selectedCar.dir].isEnabled()) {
							dirButtons[selectedCar.dir].setSelected(true);
							// dirButtons[selectedCar.dir].doClick();
						} else {
							dirButtons[section.dir[0]].setSelected(true);
							selectedCar.dir = (byte) section.dir[0];
							// dirButtons[section.dir[0]].doClick();
						}
						carEnter(selectedCar, section);
					}
					PkgHandler.send(new AppPkg(selectedCar.name,
							selectedCar.dir, section.name));
				}
			} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
				// right click
				updateRoadCondition(section);
			} else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
				// middle click
				System.out.println("middle clicked");
			}
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

	} 
}
