package nju.ics.lixiaofan.dashboard;

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
import java.util.LinkedList;
import java.util.Queue;

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
import javax.swing.JTextArea;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.car.DPad;
import nju.ics.lixiaofan.car.Remediation;
import nju.ics.lixiaofan.city.Building;
import nju.ics.lixiaofan.city.Location;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.control.Delivery.DeliveryTask;
import nju.ics.lixiaofan.event.Event;
import nju.ics.lixiaofan.event.EventManager;
import nju.ics.lixiaofan.monitor.AppPkg;
import nju.ics.lixiaofan.monitor.PkgHandler;
import nju.ics.lixiaofan.sensor.BrickHandler;

public class Dashboard extends JFrame{
	private static final long serialVersionUID = 1L;
	private static TrafficMap mapPanel = new TrafficMap();
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
	private static JButton deliverButton = new JButton("Deliver"),
			startdButton = new JButton("Start"),
			canceldButton = new JButton("Cancel");
	private static JCheckBox jchkSensor = new JCheckBox("Sensors"), jchkSection = new JCheckBox("Sections"); 
	private static JTextArea srcta = new JTextArea(), dstta = new JTextArea();
	private static Location src = null, dst = null;
	private static JPanel deliveryPanel = new JPanel();
	private static boolean isDeliveryStarted = false;
	public static boolean blink = false;
	private static Runnable blinkThread = new Runnable() {
		public void run() {
			while(true){
				blink = !blink;
				for(Section s : TrafficMap.sections.values()){
					if(s.cars.isEmpty())
						continue;
					if (s.cars.size() > 1 || s.cars.peek().isLoading)
						s.icon.repaint();
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
		
		gbc.fill = GridBagConstraints.BOTH;
		setLayout(gbl);
		add(mapPanel);
		add(leftPanel);
		add(rightPanel);
		
//		String mapLen = "";
//		for(int i = 0;i < 19;i++)
//			mapLen = mapLen.concat("          ");
//		JLabel maplenLabel = new JLabel(mapLen);
//		add(maplenLabel);
		
		for(Section s : TrafficMap.sections.values())
			s.icon.addMouseListener(new SectionIconListener(s));
		for(Building b : TrafficMap.buildings.values())
			b.icon.addMouseListener(new BuildingIconListener(b));
		
		gbc.gridx = 0;
		gbc.weightx = gbc.weighty = 1;
		gbl.setConstraints(leftPanel, gbc);
		gbc.gridx = 1;
		gbc.weightx = 0;
		gbl.setConstraints(mapPanel, gbc);
//		gbc.gridy = 0;
//		gbc.weighty = 0;
//		gbl.setConstraints(maplenLabel, gbc);
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.weighty = 1;
		gbl.setConstraints(rightPanel, gbc);
		
//		mapPanel.setBackground(Color.WHITE);

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
						TrafficMap.crossings[i].cars.clear();
						TrafficMap.crossings[i].waiting.clear();
						TrafficMap.crossings[i].permitted[0] = null;
				}
				for(int i = 0;i < TrafficMap.streets.length;i++){
						TrafficMap.streets[i].cars.clear();
						TrafficMap.streets[i].waiting.clear();
						TrafficMap.streets[i].permitted[0] = null;
				}
			
				for(Car car : TrafficMap.cars.values()){
					car.loc = null;
					car.dir = -1;
					car.status = Car.STILL;
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
							selectedCar.dir = sect.dir[0];
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
					PkgHandler.send(new AppPkg().setDir(car.name, car.dir));
				}
			}
		});
		dirButtons[1].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Car car = getSelectedCar();
				if(car != null){
					car.dir = 1;
					PkgHandler.send(new AppPkg().setDir(car.name, car.dir));
				}
			}
		});
		dirButtons[2].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Car car = getSelectedCar();
				if(car != null){
					car.dir = 2;
					PkgHandler.send(new AppPkg().setDir(car.name, car.dir));
				}
			}
		});
		dirButtons[3].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Car car = getSelectedCar();
				if(car != null){
					car.dir = 3;
					PkgHandler.send(new AppPkg().setDir(car.name, car.dir));
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
				src = dst = null;
				updateDeliverySrc();
				updateDeliveryDst();
				isDeliveryStarted = true;
				startdButton.setVisible(false);
				deliverButton.setVisible(true);
				deliverButton.setEnabled(false);
				canceldButton.setVisible(true);
			}
		});
		deliverButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				isDeliveryStarted = false;
				deliverButton.setVisible(false);
				startdButton.setVisible(true);
				canceldButton.setVisible(false);
				
				if(src != null && dst != null)
					Delivery.add(src, dst);
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
		
		if(!TrafficMap.cars.isEmpty())
			for(Car car : TrafficMap.cars.values())
				if(car.isConnected)
					addCar(car);

		new Thread(blinkThread).start();
		
		setTitle("Dashboard");
//		setSize(1200,mapPanel);
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	public static Section getNearestSection(int x, int y){
		if(x < 0 || x >= mapPanel.getWidth() || y < 0 || y >= mapPanel.getHeight())
			return null;
		int min = Integer.MAX_VALUE, tmp;
		Section section = null;
//		System.out.println(x+", "+y);
		for(Section s : TrafficMap.sections.values()){
//			System.out.println(s.name+"\t"+s.icon.coord.centerX+", "+s.icon.coord.centerY);
			tmp = (int) (Math.pow(x-s.icon.coord.centerX, 2) + Math.pow(y-s.icon.coord.centerY, 2));
			if(tmp < min){
				section = s;
				min = tmp;
			}
		}
		return section;
	}
	
	public static void updateRoadCondition(Section s){
		String str = s.name+"\n";
		if(!s.cars.isEmpty()){
			str += "Cars:\n";
			for(Car car : s.cars){
				str += car.name + " (" + car.getState() + ") "+car.getDir();
				if(car.dest != null)
					str += " Dest:" + car.dest.name;
				str += "\n";
			}
		}
		if(!s.waiting.isEmpty()){
			str += "Waiting Cars:\n";
			for(Car car : s.waiting)
				str += car.name + " (" + car.getState() + ") "+car.getDir()+"\n";
		}
		roadta.setText(str);
	}
	
	public static void updateDeliverySrc(){
		if(src != null)
			srcta.setText(src.name);
		else
			srcta.setText("");
	}
	
	public static void updateDeliveryDst(){
		if(dst != null)
			dstta.setText(dst.name);
		else
			dstta.setText("");
	}
	
	public static synchronized void updateDelivQ(){
		Queue<DeliveryTask> queue = new LinkedList<Delivery.DeliveryTask>();
		queue.addAll(Delivery.searchTasks);
		queue.addAll(Delivery.deliveryTasks);
		delivta.setText("Nums: " + queue.size());
		for(DeliveryTask dt : queue)
			delivta.append("\nPhase: "+dt.phase+" Src: "+dt.src.name+" Dst: "+dt.dst.name);
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
		PkgHandler.send(new AppPkg().setCar(car.name, -1, null));
		
		if(car.loc != null){
			if(car.dir < 0){
				car.dir = car.loc.dir[0];
				PkgHandler.send(new AppPkg().setDir(car.name, car.dir));
			}
			carEnter(car, car.loc);
			PkgHandler.send(new AppPkg().setCar(car.name, car.dir, car.loc.name));
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
	
	public static void carEnter(Car car, Section section){
		if(car == null || section == null)
			return;
		carLeave(car, car.loc);
		
		section.cars.add(car);
		car.loc = section;
		section.icon.repaint();
		if(section.isCombined){
			//only combined sections can change a car's direction
			car.dir = section.dir[0];
			for(Section s : section.combined)
				s.icon.repaint();
		}
		//trigger move event
		if(EventManager.hasListener(Event.Type.CAR_MOVE))
			EventManager.trigger(new Event(Event.Type.CAR_MOVE, car.name, car.loc.name));
	}
	
	public static void carLeave(Car car, Section section){
		if(car == null || section == null)
			return;
		section.cars.remove(car);
		if(car.loc == section)
			car.loc = null;
		section.icon.repaint();
		if(section.isCombined){
			for(Section s : section.combined)
				s.icon.repaint();
		}
		//trigger leaving event
		if(EventManager.hasListener(Event.Type.CAR_LEAVE))
			EventManager.trigger(new Event(Event.Type.CAR_LEAVE, car.name, section.name));
	}
	
	public static void displayPopup(String text, TrafficMap.Coord coord){
		
	}
	
	private class BuildingIconListener implements MouseListener{
		Building building = null;
		
		public BuildingIconListener(Building building) {
			this.building = building;
		}
		
		public void mousePressed(MouseEvent e) {
			if (building == null)
				return;
			// for delivery tasks
			if (isDeliveryStarted) {
				if (src == null) {
					src = building;
					updateDeliverySrc();
				} else if (dst == null) {
					if (building == src)
						return;
					dst = building;
					updateDeliveryDst();
					deliverButton.setEnabled(true);
				}
			} else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
				// left click
			} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
				// right click
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
				if (src == null) {
					src = section;
					updateDeliverySrc();
				} else if (dst == null) {
					if (section == src || section.isCombined && section.combined.contains(src))
						return;
					dst = section;
					updateDeliveryDst();
					deliverButton.setEnabled(true);
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
							selectedCar.dir = section.dir[0];
							// dirButtons[section.dir[0]].doClick();
						}
						carEnter(selectedCar, section);
					}
					PkgHandler.send(new AppPkg().setCar(selectedCar.name, selectedCar.dir, section.name));
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

