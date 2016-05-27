package nju.ics.lixiaofan.dashboard;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

import com.sun.xml.internal.ws.api.streaming.XMLStreamReaderFactory.Default;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Command;
import nju.ics.lixiaofan.car.DPad;
import nju.ics.lixiaofan.car.RCServer;
import nju.ics.lixiaofan.car.Remedy;
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
	
	public Dashboard() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
//		gbc.anchor = GridBagConstraints.CENTER;
		
		for(Section s : TrafficMap.sections.values())
			s.icon.addMouseListener(new SectionIconListener(s));
		for(Building b : TrafficMap.buildings.values())
			b.icon.addMouseListener(new BuildingIconListener(b));
		
		gbc.gridx = gbc.gridy = 0;
		gbc.weightx = gbc.weighty = 1;
		leftPanel.setPreferredSize(new Dimension(300, 0));
		add(leftPanel, gbc);
		gbc.gridx = 1;
		gbc.weightx = gbc.weighty = 0;
		add(trafficMap, gbc);
		gbc.gridx = 2;
		gbc.weightx = 1;
		gbc.weighty = 1;
		rightPanel.setPreferredSize(new Dimension(300, 0));
		add(rightPanel, gbc);
		
		//left panel settings
		leftPanel.setLayout(new GridBagLayout());
		gbc.insets = new Insets(1, 5, 1, 5);
		gbc.gridx = gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
//		gbc.gridheight = gbc.gridwidth = 1;
		leftPanel.add(resetButton, gbc);
		
		resetButton.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				//TODO
				if(resetButton.isEnabled()){
					resetButton.setEnabled(false);
					Reset.isRealInc = e.getButton() == MouseEvent.BUTTON1;
	//				System.out.println(Reset.isRealInc);
					ResourceProvider.execute(Reset.resetTask);
				}
			}
		});
		
		gbc.gridy++;
		leftPanel.add(new JLabel("Delivery Tasks"), gbc);
		
		gbc.gridy++;
		gbc.weighty = 1;
		leftPanel.add(delivtaScroll, gbc);
		delivta.setLineWrap(true);
		delivta.setWrapStyleWord(true);
		delivta.setEditable(false);
		updateDelivQ();
		
		gbc.gridy++;
//		gbc.gridheight = 1;
		gbc.weighty = 0;
		leftPanel.add(new JLabel("Remedy Commands"), gbc);
		
		gbc.gridy++;
		gbc.weighty = 1;
		leftPanel.add(remedytaScroll, gbc);
		remedyta.setLineWrap(true);
		remedyta.setWrapStyleWord(true);
		remedyta.setEditable(false);
		updateRemedyQ();
		
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weighty = 0;
		leftPanel.add(console, gbc);
		//TODO console commands
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
					default:
						return;
					}
				}
				else if(cmd.startsWith("connect car ") || cmd.startsWith("disconnect car ")){
					String car = cmd.substring(
							cmd.charAt(0) == 'c' ? "connect car ".length()
									: "disconnect car ".length()).toLowerCase();
					String s = "";
					switch(car){
					case "r":case "red":case "red car":
						s = Car.RED;	break;
					case "b":case "black":case "black car":
						s = Car.BLACK;	break;
					case "w":case "white":case "white car":
						s = Car.WHITE;	break;
					case "o":case "orange":case "orange car":
						s = Car.ORANGE;	break;
					case "g":case "green":case "green car":
						s = Car.GREEN;	break;
					case "s":case "silver":case "suv":case "silver suv":
						s = Car.SILVER;	break;
					default:
						return;
					}
					s += "_" + (cmd.charAt(0) == 'c' ? Command.CONNECT : Command.DISCONNECT) + "_0";
					try {
						RCServer.rc.out.writeUTF(s);
						RCServer.rc.out.flush();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		
		gbc.gridx = 1;
		gbc.gridy++;
		gbc.weighty = 0;
		leftPanel.add(new JLabel("Vehicle Condition"), gbc);
		
		gbc.gridy++;
//		gbc.gridheight = 1;
		gbc.weighty = 1;
		leftPanel.add(VCPanel, gbc);
		
		gbc.gridy += gbc.gridheight;
//		gbc.gridheight = 1;
		gbc.weighty = 0;
		leftPanel.add(new JLabel("Road Condition"), gbc);
		
		gbc.gridy++;
//		gbc.gridheight = 2;
//		gbc.weighty = 0;
		gbc.weighty = 1;
		leftPanel.add(roadtaScroll, gbc);
		roadta.setLineWrap(true);
		roadta.setWrapStyleWord(true);
		roadta.setEditable(false);
		
		//right panel settings
		rightPanel.setLayout(new GridBagLayout());
		gbc.gridx = gbc.gridy = 0;
		gbc.gridheight = gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
//		gbc.gridwidth = GridBagConstraints.REMAINDER;
		rightPanel.add(carbox, gbc);
		carbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg) {
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
		rightPanel.add(radioButtonsPanel, gbc);
		radioButtonsPanel.setLayout(new GridLayout(1, 4));
		
		for(final int[] i = {0};i[0] < 4;i[0]++){
			dirButtons[i[0]].setEnabled(false);
			radioButtonsPanel.add(dirButtons[i[0]]);
			radioButtonGroup.add(dirButtons[i[0]]);
			dirButtons[i[0]].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg) {
					Car car = getSelectedCar();
					if(car != null){
						car.dir = i[0];
						PkgHandler.send(new AppPkg().setDir(car.name, car.dir));
					}
				}
			});
		}
		
		gbc.gridy++;
		rightPanel.add(new DPad(), gbc);
		
		gbc.gridy++;
		rightPanel.add(miscPanel, gbc);
		miscPanel.setBorder(BorderFactory.createTitledBorder("Display & Sound Options"));
//		miscPanel.setLayout(new GridLayout(2, 0));
//		miscPanel.setPreferredSize(new Dimension(240, 90));
		miscPanel.setLayout(new GridBagLayout());
		GridBagConstraints mgbc = new GridBagConstraints();
		mgbc.fill = GridBagConstraints.BOTH;
		mgbc.gridx = mgbc.gridy = 0;
		mgbc.weightx = 1;
		mgbc.gridwidth = 2;
		miscPanel.add(jchkSection, mgbc);
		mgbc.gridx += mgbc.gridwidth;
		miscPanel.add(jchkSensor, mgbc);
		mgbc.gridx += mgbc.gridwidth;
		miscPanel.add(jchkBalloon, mgbc);
		mgbc.gridx = 0;
		mgbc.gridy++;
		mgbc.weightx = 1.5;
		mgbc.gridwidth = 3;
		miscPanel.add(jchkCrash, mgbc);
		mgbc.gridx += mgbc.gridwidth;
		miscPanel.add(jchkError, mgbc);
		
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
						s.icon.setVisible(showSensor);
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
		rightPanel.add(CCPanel, gbc);
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
		
		JLabel srclabel = new JLabel("Src");
		srctf.setEditable(false);
		JLabel dstlabel = new JLabel("Dst");
		desttf.setEditable(false);
		
		gbc.gridy++;
//		gbc.gridwidth = GridBagConstraints.REMAINDER;
		rightPanel.add(deliveryPanel, gbc);
		deliveryPanel.setBorder(BorderFactory.createTitledBorder("Delivery"));
		deliveryPanel.setLayout(new GridBagLayout());
		
		GridBagConstraints dgbc = new GridBagConstraints();
		dgbc.insets = new Insets(3, 5, 3, 5);
		dgbc.fill = GridBagConstraints.BOTH;
		dgbc.gridx = 0;
		dgbc.gridy = 0;
		deliveryPanel.add(srclabel, dgbc);
		dgbc.gridx++;
		dgbc.weightx = 1;
//		dgbc.gridwidth = GridBagConstraints.REMAINDER;
		deliveryPanel.add(srctf, dgbc);
		dgbc.gridx++;
//		dgbc.gridy++;
		dgbc.weightx = 0;
		deliveryPanel.add(dstlabel, dgbc);
		dgbc.gridx++;
		dgbc.weightx = 1;
		deliveryPanel.add(desttf, dgbc);
		dgbc.gridx = 0;
		dgbc.gridy++;
		dgbc.gridwidth = GridBagConstraints.REMAINDER;
		dgbc.weightx = 1;
		deliveryPanel.add(startdButton, dgbc);
		dgbc.gridwidth = 2;
		deliveryPanel.add(deliverButton, dgbc);
		dgbc.gridx = 2;
		deliveryPanel.add(canceldButton, dgbc);
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
		deliverButton.setVisible(false);
		canceldButton.setVisible(false);
		
		gbc.gridy++;
		JLabel logLabel = new JLabel("Logs");
		rightPanel.add(logLabel, gbc);
		
		gbc.gridy++;
		gbc.weighty = 1;
		rightPanel.add(logtaScroll, gbc);
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
		remedyta.setText("Nums: "+Remedy.getQueue().size());
		for(Command cmd : Remedy.getQueue()){
			remedyta.append("\n"+cmd.car.name+" "+((cmd.cmd==0)?"S":"F")+" "+cmd.level+" "+cmd.deadline);
		}
	}
	
	public static synchronized void updateVC(Car car){
		VCPanel.updateVC(car);
	}
	
	public static synchronized void updateVC(){
		for(Car car : ResourceProvider.getConnectedCars())
			updateVC(car);
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
	
	private class SectionIconListener extends MouseAdapter{
		Section section = null;
		
		public SectionIconListener(Section section) {
			this.section = section;
		}
		public void mousePressed(MouseEvent e) {
			if (section == null)
				return;
			// for delivery tasks
			if (isDeliveryStarted) {
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
			} 
			else if (e.getButton() == MouseEvent.BUTTON1) {
				// left click
				Car car = getSelectedCar();
				if (car != null) {
					radioButtonGroup.clearSelection();
					for (int i = 0; i < 4; i++)
						dirButtons[i].setEnabled(false);
					if (section.cars.contains(car)) {
						car.dir = -1;
						car.leave(section);
					} else {
						dirButtons[section.dir[0]].setEnabled(true);
						if (section.dir[1] >= 0)
							dirButtons[section.dir[1]].setEnabled(true);

						if (car.dir >= 0 && dirButtons[car.dir].isEnabled()) {
							dirButtons[car.dir].setSelected(true);
							// dirButtons[selectedCar.dir].doClick();
						} else {
							dirButtons[section.dir[0]].setSelected(true);
							car.dir = section.dir[0];
							// dirButtons[section.dir[0]].doClick();
						}
						car.enter(section);
					}
					PkgHandler.send(new AppPkg().setCar(car.name, car.dir, section.name));
				}
			} 
			else if (e.getButton() == MouseEvent.BUTTON3) {
				// right click
				updateRoadCondition(section);
			} 
		}
	}
	
	private class BuildingIconListener extends MouseAdapter{
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
			} 
		}
	}
}