package nju.ics.lixiaofan.dashboard;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
import javax.swing.JTextField;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.car.DPad;
import nju.ics.lixiaofan.car.RCServer;
import nju.ics.lixiaofan.car.Remediation;
import nju.ics.lixiaofan.city.Building;
import nju.ics.lixiaofan.city.Location;
import nju.ics.lixiaofan.city.Section;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.consistency.middleware.Middleware;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.control.Delivery.DeliveryTask;
import nju.ics.lixiaofan.control.Reset;
import nju.ics.lixiaofan.monitor.AppPkg;
import nju.ics.lixiaofan.monitor.PkgHandler;
import nju.ics.lixiaofan.resource.ResourceProvider;
import nju.ics.lixiaofan.sensor.Sensor;

public class Dashboard extends JFrame{
	private static final long serialVersionUID = 1L;
	private static TrafficMap trafficMap = new TrafficMap();
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
	private static JButton resetButton = new JButton("Reset"),
			deliverButton = new JButton("Deliver"),
			startdButton = new JButton("Start"),
			canceldButton = new JButton("Cancel");
	private static JCheckBox jchkSensor = new JCheckBox("Sensor"), jchkSection = new JCheckBox("Section"),
			jchkBalloon = new JCheckBox("Balloon"), jchkCrash = new JCheckBox("Crash Sound"),
			jchkError = new JCheckBox("Error Sound"),
			jchkDetection = new JCheckBox("Detection"), jchkResolution = new JCheckBox("Resolution"); 
	public static boolean showSensor = false, showSection = false, showBalloon = false,
			playCrashSound = false,	playErrorSound = false;
	private static JTextField srctf = new JTextField(), desttf = new JTextField(), console  = new JTextField("Console");
	private static Location src = null, dest = null;
	private static JPanel deliveryPanel = new JPanel(), CCPanel = new JPanel(), miscPanel = new JPanel();
	private static boolean isDeliveryStarted = false;
	public static boolean blink = false;
	private static Runnable blinkThread = new Runnable() {
		private int duration = 500;
		public void run() {
			while(true){
				blink = !blink;
				for(Section s : TrafficMap.sections.values()){
					if(s.balloon.duration > 0){
						if(!s.balloon.isVisible())
							s.balloon.setVisible(true);
						s.balloon.duration -= duration;
					}
					else if(s.balloon.isVisible())
						s.balloon.setVisible(false);
					
//					if(s.cars.isEmpty())
//						continue;
					if (!s.cars.isEmpty() && s.cars.peek().isLoading)
						s.icon.repaint();
				}
				try {
					Thread.sleep(duration);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
//	public static void main(String[] args) {
//	}

	public Dashboard() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.fill = GridBagConstraints.BOTH;
		setLayout(gbl);
		add(trafficMap);
		add(leftPanel);
		add(rightPanel);
		
		for(Section s : TrafficMap.sections.values())
			s.icon.addMouseListener(new SectionIconListener(s));
		for(Building b : TrafficMap.buildings.values())
			b.icon.addMouseListener(new BuildingIconListener(b));
		
		gbc.gridx = 0;
		gbc.weightx = 0;//1;
		gbc.weighty = 1;
		gbl.setConstraints(leftPanel, gbc);
		gbc.gridx = 1;
		gbc.weightx = gbc.weighty = 0;
		gbl.setConstraints(trafficMap, gbc);
		gbc.gridx = 2;
		gbc.weighty = 1;
		gbl.setConstraints(rightPanel, gbc);
		gbc.insets = new Insets(1, 5, 1, 5);
		gbc.gridx = gbc.gridy = 0;
		gbc.weightx = 0;//1;
		gbc.weighty = 0;
		leftPanel.setLayout(gbl);
		
		gbl.setConstraints(resetButton, gbc);
		leftPanel.add(resetButton);
		
		resetButton.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				resetButton.setEnabled(false);
				Reset.isRealInc = e.getButton() == MouseEvent.BUTTON1;
				ResourceProvider.execute(Reset.resetTask);
			}
		});
		
		JLabel delivlabel = new JLabel("Delivery Tasks                ");
		gbc.gridy++;
		gbl.setConstraints(delivlabel, gbc);
		leftPanel.add(delivlabel);
		
		gbc.gridy++;
		gbc.weighty = 1;
		gbl.setConstraints(delivtaScroll, gbc);
		leftPanel.add(delivtaScroll);
		delivta.setLineWrap(true);
		delivta.setWrapStyleWord(true);
		delivta.setEditable(false);
		updateDelivQ();
		
		JLabel remedylabel = new JLabel("Remediation Cmds");
		gbc.gridy++;
		gbc.gridheight = 1;
		gbc.weighty = 0;
		gbl.setConstraints(remedylabel, gbc);
		leftPanel.add(remedylabel);
		
		gbc.gridy++;
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
		gbl.setConstraints(console, gbc);
		leftPanel.add(console);
		//console commands
		console.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmd = console.getText();
				if(cmd.startsWith("add car ")){
					String car = cmd.substring("add car ".length()).toLowerCase();
					switch(car){
					case "r":case "red":case "red car":
						RCServer.addCar(Car.RED);	break;
					case "b":case "black":case "black car":
						RCServer.addCar(Car.BLACK);	break;
					case "w":case "white":case "white car":
						RCServer.addCar(Car.WHITE);	break;
					case "o":case "orange":case "orange car":
						RCServer.addCar(Car.ORANGE);	break;
					case "g":case "green":case "green car":
						RCServer.addCar(Car.GREEN);	break;
					case "s":case "silver":case "suv":case "silver suv":
						RCServer.addCar(Car.SILVER);	break;
					}
				}
			}
		});
		
		gbc.gridx = 1;
		gbc.gridy++;
		JLabel vcLabel = new JLabel("Vehicle Condition                     ");
		gbl.setConstraints(vcLabel, gbc);
		leftPanel.add(vcLabel);
		
		gbc.gridy++;
		gbc.gridheight = 1;
		gbc.weighty = 0;
		gbl.setConstraints(VCPanel, gbc);
		leftPanel.add(VCPanel);
//		carta.setLineWrap(true);
//		carta.setWrapStyleWord(true);
//		carta.setEditable(false);
		
		gbc.gridy += gbc.gridheight;
		gbc.gridheight = 1;
		JLabel rcLabel = new JLabel("Road Condition");
		gbl.setConstraints(rcLabel, gbc);
		leftPanel.add(rcLabel);
		
		gbc.gridy++;
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
		
		gbc.gridy++;
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
		
		gbc.gridy++;
		DPad dpad = new DPad();
		gbl.setConstraints(dpad, gbc);
		rightPanel.add(dpad);
		
		gbc.gridy++;
		gbl.setConstraints(miscPanel, gbc);
		rightPanel.add(miscPanel);
		miscPanel.setBorder(BorderFactory.createTitledBorder("Display & Sound Options"));
//		miscPanel.setLayout(new GridLayout(2, 0));
//		temporarily hard coded
//		miscPanel.setPreferredSize(new Dimension(200, 90));
		miscPanel.add(jchkSection);
		miscPanel.add(jchkSensor);
		miscPanel.add(jchkBalloon);
		miscPanel.add(jchkCrash);
		miscPanel.add(jchkError);
		
		jchkSection.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionevent) {
				showSection = jchkSection.isSelected();
				trafficMap.repaint();
			}
		});
		
		jchkSensor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				showSensor = jchkSensor.isSelected();
				for(List<Sensor> list : TrafficMap.sensors)
					for(Sensor s : list)
						s.button.setVisible(showSensor);
			}
		});
		
		jchkBalloon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				showBalloon = jchkBalloon.isSelected();
				trafficMap.repaint();
			}
		});
		
		jchkCrash.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				playCrashSound = jchkCrash.isSelected();
			}
		});
		
		jchkError.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				playErrorSound = jchkError.isSelected();
			}
		});
		
		gbc.gridy++;
		gbl.setConstraints(CCPanel, gbc);
		rightPanel.add(CCPanel);
		CCPanel.setBorder(BorderFactory.createTitledBorder("Consistency Checking"));
		CCPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		CCPanel.add(jchkDetection);
		CCPanel.add(jchkResolution);
		
		jchkDetection.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Middleware.setDetectionFlag(jchkDetection.isSelected());
				if(!jchkDetection.isSelected()){
					if(jchkResolution.isSelected())
						jchkResolution.doClick();
					if(jchkBalloon.isSelected())
						jchkBalloon.doClick();
					if(jchkCrash.isSelected())
						jchkCrash.doClick();
					if(jchkError.isSelected())
						jchkError.doClick();
				}
				
				jchkBalloon.setEnabled(jchkDetection.isSelected());
				jchkCrash.setEnabled(jchkDetection.isSelected());
				jchkError.setEnabled(jchkDetection.isSelected());
			}
		});
		jchkBalloon.setEnabled(jchkDetection.isSelected());
		jchkCrash.setEnabled(jchkDetection.isSelected());
		jchkError.setEnabled(jchkDetection.isSelected());
		
		jchkResolution.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Middleware.setResolutionFlag(jchkResolution.isSelected());
				if(!jchkDetection.isSelected() && jchkResolution.isSelected())
					jchkDetection.doClick();
			}
		});
		
		JLabel srclabel = new JLabel("Src:");
		srctf.setEditable(false);
		JLabel dstlabel = new JLabel("Dst:");
		desttf.setEditable(false);
		
		gbc.gridy++;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbl.setConstraints(deliveryPanel, gbc);
		rightPanel.add(deliveryPanel);
		deliveryPanel.setBorder(BorderFactory.createTitledBorder("Delivery"));
		deliveryPanel.add(srclabel);
		deliveryPanel.add(srctf);
		deliveryPanel.add(dstlabel);
		deliveryPanel.add(desttf);
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
		dgbl.setConstraints(srctf, dgbc);
		dgbc.gridx = 0;
		dgbc.gridy = 1;
		dgbc.weightx = 0;
		dgbl.setConstraints(dstlabel, dgbc);
		dgbc.gridx = 1;
		dgbc.weightx = 1;
		dgbl.setConstraints(desttf, dgbc);
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
				src = dest = null;
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
				
				if(src != null && dest != null)
					Delivery.add(src, dest);
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
		
		gbc.gridy++;
		JLabel logLabel = new JLabel("Log");
		gbl.setConstraints(logLabel, gbc);
		rightPanel.add(logLabel);
		
		gbc.gridy++;
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

		new Thread(blinkThread, "Blink Thread").start();
		jchkResolution.doClick();
		jchkBalloon.doClick();
		jchkCrash.doClick();
		jchkError.doClick();
		
		setTitle("Dashboard");
//		setSize(1200,mapPanel);
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	public static Section getNearestSection(int x, int y){
		if(x < 0 || x >= trafficMap.getWidth() || y < 0 || y >= trafficMap.getHeight())
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
				str += car.name + " (" + car.getStatusStr() + ") "+car.getDirStr();
				if(car.dest != null)
					str += " Dest:" + car.dest.name;
				str += "\n";
			}
		}
		if(!s.waiting.isEmpty()){
			str += "Waiting Cars:\n";
			for(Car car : s.waiting)
				str += car.name + " (" + car.getStatusStr() + ") "+car.getDirStr()+"\n";
		}
		if(!s.realCars.isEmpty()){
			str += "Real Cars:\n";
			for(Car car : s.realCars){
				str += car.name + " (" + car.getRealStatusStr() + ") "+car.getRealDirStr()+"\n";
			}
		}
		roadta.setText(str);
	}
	
	public static void updateDeliverySrc(){
		if(src != null)
			srctf.setText(src.name);
		else
			srctf.setText("");
	}
	
	public static void updateDeliveryDst(){
		if(dest != null)
			desttf.setText(dest.name);
		else
			desttf.setText("");
	}
	
	public static synchronized void updateDelivQ(){
		Queue<DeliveryTask> queue = new LinkedList<Delivery.DeliveryTask>();
		queue.addAll(Delivery.searchTasks);
		queue.addAll(Delivery.deliveryTasks);
		delivta.setText("Nums: " + queue.size());
		for(DeliveryTask dt : queue)
			delivta.append("\nPhase: "+dt.phase+" Src: "+dt.src.name+" Dst: "+dt.dest.name);
	}
	
	public static synchronized void updateRemedyQ(){
		remedyta.setText("Nums: "+Remediation.getQueue().size());
		for(Command cmd : Remediation.getQueue()){
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
			Section loc = car.loc;
			car.loc = null;
			car.enter(loc);
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
	
	public static void playCrashSound(){
		if(playCrashSound){
			try {
				AudioPlayer.player.start(new AudioStream(new FileInputStream("res/crash.wav")));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void playErrorSound(){
		if(playErrorSound)
			try {
				AudioPlayer.player.start(new AudioStream(new FileInputStream("res/oh_no.wav")));
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public static void enableResetButton(boolean b){
		resetButton.setEnabled(b);
	}
	
	public static void repaintTrafficMap(){
		trafficMap.repaint();
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
				} else if (dest == null) {
					if (building == src)
						return;
					dest = building;
					updateDeliveryDst();
					deliverButton.setEnabled(true);
				}
			} else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
				// left click
			} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {
				// right click
			} else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
				// middle click
//				System.out.println("middle clicked");
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
				} 
				else if (dest == null) {
					if (src instanceof Section && section.sameAs((Section) src))
						return;
					dest = section;
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
						selectedCar.leave(section);
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
						selectedCar.enter(section);
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