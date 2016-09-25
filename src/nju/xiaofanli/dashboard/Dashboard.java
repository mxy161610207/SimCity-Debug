package nju.xiaofanli.dashboard;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.city.Building;
import nju.xiaofanli.city.Location;
import nju.xiaofanli.city.Section;
import nju.xiaofanli.city.TrafficMap;
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.CmdSender;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.car.Remedy;
import nju.xiaofanli.device.sensor.Sensor;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Queue;

public class Dashboard extends JFrame{
	private static final long serialVersionUID = 1L;
	private static Dashboard instance = null;
	private static final TrafficMap trafficMap = new TrafficMap();
	private static final JPanel leftPanel = new JPanel();
	private static final JPanel rightPanel = new JPanel();
	private static final JComboBox<String> carbox = new JComboBox<>();
	private static final JTextArea delivta = new JTextArea();
	private static final JTextArea remedyta = new JTextArea();
	private static final JTextArea roadta = new JTextArea();
	private static final JTextArea logta = new JTextArea();
	private static final JScrollPane delivtaScroll = new JScrollPane(delivta);
	private static final JScrollPane remedytaScroll = new JScrollPane(remedyta);
	private static final JScrollPane roadtaScroll = new JScrollPane(roadta);
	private static final JScrollPane logtaScroll = new JScrollPane(logta);
	private static final VehicleConditionPanel VCPanel = new VehicleConditionPanel();
    private static final JButton resetButton = new JButton("Reset");
	private static final JButton deliverButton = new JButton("Deliver");
	private static final JButton startdButton = new JButton("Start");
	private static final JButton canceldButton = new JButton("Cancel");
	private static final JCheckBox jchkSensor = new JCheckBox("Sensor");
	private static final JCheckBox jchkSection = new JCheckBox("Section");
	private static final JCheckBox jchkBalloon = new JCheckBox("Balloon");
	private static final JCheckBox jchkCrash = new JCheckBox("Crash Sound");
	private static final JCheckBox jchkError = new JCheckBox("Error Sound");
	private static final JCheckBox jchkDetection = new JCheckBox("Detection");
	private static final JCheckBox jchkResolution = new JCheckBox("Resolution");
	public static boolean showSensor = false, showSection = false, showBalloon = false,
			playCrashSound = false,	playErrorSound = false;
	private static final JTextField srctf = new JTextField();
	private static final JTextField desttf = new JTextField();
	private static final JTextField console  = new JTextField("Console");
	private static Location src = null, dest = null;
	private static final JPanel deliveryPanel = new JPanel();
	private static final JPanel CCPanel = new JPanel();
	private static final JPanel miscPanel = new JPanel();
	private static boolean isDeliveryStarted = false;
	public static boolean blink = false;
	private static final Runnable blinkThread = new Runnable() {
		private final int duration = 500;
		public void run() {
			//noinspection InfiniteLoopStatement
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

	private Dashboard() {
//		setEnabled(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setVisible(true);
	}

	public static Dashboard getInstance(){
		if(instance == null)
			synchronized (Dashboard.class) {
				if(instance == null)
					instance = new Dashboard();
			}
		return instance;
	}

	private static final Map<String, JLabel> deviceLabels = new HashMap<>();
	public static void setDeviceStatus(String device, boolean status){
		JLabel label = deviceLabels.get(device);
		if(label == null)
			return;
		if(status){
			if(device.endsWith("conn"))
				label.setIcon(Resource.getOrangeCheckImageIcon());
			else
				label.setIcon(Resource.getGreenCheckImageIcon());
		}
		else
			label.setIcon(Resource.getRedXImageIcon());
	}

	private static JPanel checkingPanel = null;
	public static final int MARK_SIZE = 30;
	public void loadCheckUI(){
		if(checkingPanel != null){
			setTitle("Self Checking");
			setContentPane(checkingPanel);
//			cards.show(getContentPane(), "Check");
			pack();
			setLocationRelativeTo(null);
			return;
		}
		checkingPanel = new JPanel(new GridBagLayout());
//		add(checkingPanel, "Check");
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = gbc.gridy = 0;
		gbc.weightx = gbc.weighty = 1;
		//brick panel
        if(Resource.getBricks().size() > 0) {
            JPanel brickPanel = new JPanel();
            checkingPanel.add(brickPanel, gbc);
            brickPanel.setBorder(BorderFactory.createTitledBorder("Bricks"));
            brickPanel.setLayout(new GridBagLayout());
            GridBagConstraints bgbc = new GridBagConstraints();
//		bgbc.fill = GridBagConstraints.BOTH;
            bgbc.gridx = bgbc.gridy = 0;
            bgbc.weightx = bgbc.weighty = 1;
            bgbc.insets = new Insets(1, 5, 1, 5);
            for (String name : Resource.getBricks()) {
                bgbc.anchor = GridBagConstraints.WEST;
                JLabel nameLabel = new JLabel(name);
                nameLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, MARK_SIZE));
                brickPanel.add(nameLabel, bgbc);
                bgbc.gridx += bgbc.gridwidth;
                bgbc.anchor = GridBagConstraints.EAST;
                JLabel status = new JLabel();
                brickPanel.add(status, bgbc);
                status.setIcon(Resource.getBlackQuestionImageIcon());
                deviceLabels.put(name + " conn", status);
                deviceLabels.put(name + " sample", status);
                bgbc.gridx = 0;
                bgbc.gridy += bgbc.gridheight;
            }
            //disconnect button
            gbc.gridx = 0;
            gbc.gridy += gbc.gridheight;
            gbc.weightx = gbc.weighty = 0;
            JButton shutDownBtn = new JButton("Shutdown");
            checkingPanel.add(shutDownBtn, gbc);
            shutDownBtn.addActionListener(e -> {
                shutDownBtn.setEnabled(false);
                int[] count = {Resource.getBricks().size()};
                for(String name : Resource.getBricks())
                    Resource.execute(()->{
                        Session session = Resource.getRootSession(name);
                        if(session != null) {
                            Channel channel = null;
                            try {
                                session.connect();
                                channel = session.openChannel("exec");
                                ((ChannelExec) channel).setCommand("poweroff");
                                channel.setInputStream(null);
                                ((ChannelExec) channel).setErrStream(System.err);
                                channel.connect();
                            } catch (JSchException e1) {
                                e1.printStackTrace();
                            }
                            finally {
                                if (channel != null)
                                    channel.disconnect();
                                session.disconnect();
                            }
                        }
                        synchronized (count){
                            if(--count[0] == 0)
                                shutDownBtn.setEnabled(true);
                        }
                    });
            });
        }
		//car panel
        if(Resource.getCars().size() > 0) {
            gbc.gridx += gbc.gridwidth;
            gbc.gridy = 0;
            gbc.gridheight = 2;
            gbc.weightx = gbc.weighty = 1;
            JPanel carPanel = new JPanel();
            checkingPanel.add(carPanel, gbc);
            carPanel.setBorder(BorderFactory.createTitledBorder("Cars"));
            carPanel.setLayout(new GridBagLayout());
            GridBagConstraints cgbc = new GridBagConstraints();
//		cgbc.fill = GridBagConstraints.BOTH;
            cgbc.gridx = cgbc.gridy = 0;
            cgbc.weightx = cgbc.weighty = 1;
            cgbc.insets = new Insets(1, 5, 1, 5);
            for (Car car : Resource.getCars()) {
                cgbc.anchor = GridBagConstraints.WEST;
                JLabel nameLabel = new JLabel(car.name);
                nameLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, MARK_SIZE));
                carPanel.add(nameLabel, cgbc);
                cgbc.gridx += cgbc.gridwidth;
                cgbc.anchor = GridBagConstraints.EAST;
                JLabel status = new JLabel();
                carPanel.add(status, cgbc);
                status.setIcon(Resource.getBlackQuestionImageIcon());
                deviceLabels.put(car.name, status);
                cgbc.gridx = 0;
                cgbc.gridy += cgbc.gridheight;
            }

        }


		setTitle("Self Checking");
//		cards.show(getContentPane(), "Check");
		setContentPane(checkingPanel);
		pack();
		setLocationRelativeTo(null);
	}

	private static JPanel controlPanel = null;
	public void loadCtrlUI(){
		if(controlPanel != null){
			setTitle("Dashboard");
			setContentPane(controlPanel);
//			cards.show(getContentPane(), "Control");
			pack();
			setLocationRelativeTo(null);
			return;
		}
		controlPanel = new JPanel(new GridBagLayout());
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
		controlPanel.add(leftPanel, gbc);
		gbc.gridx = 1;
		gbc.weightx = gbc.weighty = 0;
		controlPanel.add(trafficMap, gbc);
		gbc.gridx = 2;
		gbc.weightx = 1;
		gbc.weighty = 1;
		rightPanel.setPreferredSize(new Dimension(300, 0));
		controlPanel.add(rightPanel, gbc);

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
				if(!resetButton.isEnabled())
					return;
				StateSwitcher.setInconsistencyType(e.getButton() == MouseEvent.BUTTON1);
				StateSwitcher.startResetting();
			}
		});

		gbc.gridy += gbc.gridheight;
		leftPanel.add(new JLabel("Delivery Task"), gbc);

		gbc.gridy += gbc.gridheight;
		gbc.weighty = 1;
		leftPanel.add(delivtaScroll, gbc);
		delivta.setLineWrap(true);
		delivta.setWrapStyleWord(true);
		delivta.setEditable(false);
		updateDeliveryTaskPanel();

		gbc.gridy += gbc.gridheight;
//		gbc.gridheight = 1;
		gbc.weighty = 0;
		leftPanel.add(new JLabel("Remedy Command"), gbc);

		gbc.gridy += gbc.gridheight;
		gbc.weighty = 1;
		leftPanel.add(remedytaScroll, gbc);
		remedyta.setLineWrap(true);
		remedyta.setWrapStyleWord(true);
		remedyta.setEditable(false);
		updateRemedyCommandPanel();

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weighty = 0;
		JButton deviceButton = new JButton("Device");
		leftPanel.add(deviceButton, gbc);
		deviceButton.addActionListener(e -> showDeviceDialog(true));

		gbc.gridx += gbc.gridwidth;
		leftPanel.add(console, gbc);
		//TODO console commands
		console.addActionListener(e -> {
			String cmd = console.getText();
			if(cmd.startsWith("add car ")){
				String car = cmd.substring("add car ".length()).toLowerCase();
				switch(car){
					case "r":case "red":case "red car":
						Car.carOf(Car.RED).init(); break;
					case "b":case "black":case "black car":
						Car.carOf(Car.BLACK).init(); break;
					case "w":case "white":case "white car":
						Car.carOf(Car.WHITE).init(); break;
					case "o":case "orange":case "orange car":
						Car.carOf(Car.ORANGE).init(); break;
					case "g":case "green":case "green car":
						Car.carOf(Car.GREEN).init(); break;
					case "s":case "silver":case "suv":case "silver suv":
						Car.carOf(Car.SILVER).init(); break;
				}
			}
			else if(cmd.startsWith("connect car ") || cmd.startsWith("disconnect car ")){
				String name1 = cmd.substring(
						cmd.charAt(0) == 'c' ? "connect car ".length()
								: "disconnect car ".length()).toLowerCase();
				String s = "";
				switch(name1){
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
				Car car = Car.carOf(s);
				if(car != null){
					if(cmd.charAt(0) == 'c')
						car.connect();
					else
						car.disconnect();
				}
			}
			else if(cmd.equals("urge"))
			    CmdSender.send(getSelectedCar(), Command.URGE);
			else if(cmd.equals("whistle"))
                CmdSender.send(getSelectedCar(), Command.WHISTLE);
            else if(cmd.equals("whistle2"))
                CmdSender.send(getSelectedCar(), Command.WHISTLE2);
            else if(cmd.equals("whistle3"))
                CmdSender.send(getSelectedCar(), Command.WHISTLE3);
            else if(cmd.equals("left"))
                CmdSender.send(getSelectedCar(), Command.LEFT);
            else if(cmd.equals("right"))
                CmdSender.send(getSelectedCar(), Command.RIGHT);
		});

		gbc.gridx = 1;
		gbc.gridy += gbc.gridheight;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weighty = 0;
		leftPanel.add(new JLabel("Vehicle Condition"), gbc);

		gbc.gridy += gbc.gridheight;
//		gbc.gridheight = 1;
		gbc.weighty = 1;
		leftPanel.add(VCPanel, gbc);

		gbc.gridy += gbc.gridheight;
//		gbc.gridheight = 1;
		gbc.weighty = 0;
		leftPanel.add(new JLabel("Road Condition"), gbc);

		gbc.gridy += gbc.gridheight;
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

		gbc.gridy += gbc.gridheight;
		rightPanel.add(new DPad(), gbc);

		gbc.gridy += gbc.gridheight;
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
		mgbc.gridy += mgbc.gridheight;
		mgbc.weightx = 1.5;
		mgbc.gridwidth = 3;
		miscPanel.add(jchkCrash, mgbc);
		mgbc.gridx += mgbc.gridwidth;
		miscPanel.add(jchkError, mgbc);

		jchkSection.addActionListener(e -> {
            showSection = jchkSection.isSelected();
            trafficMap.repaint();
        });

		jchkSensor.addActionListener(e -> {
            showSensor = jchkSensor.isSelected();
            for(List<Sensor> list : TrafficMap.sensors)
                for(Sensor s : list)
                    s.icon.setVisible(showSensor);
        });

		jchkBalloon.addActionListener(e -> {
            showBalloon = jchkBalloon.isSelected();
            trafficMap.repaint();
        });

		jchkCrash.addActionListener(e -> playCrashSound = jchkCrash.isSelected());

		jchkError.addActionListener(e -> playErrorSound = jchkError.isSelected());

		gbc.gridy += gbc.gridheight;
		rightPanel.add(CCPanel, gbc);
		CCPanel.setBorder(BorderFactory.createTitledBorder("Consistency Checking"));
		CCPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		CCPanel.add(jchkDetection);
		CCPanel.add(jchkResolution);

		jchkDetection.addActionListener(e -> {
            Middleware.setDetectionEnabled(jchkDetection.isSelected());
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
        });
		jchkBalloon.setEnabled(jchkDetection.isSelected());
		jchkCrash.setEnabled(jchkDetection.isSelected());
		jchkError.setEnabled(jchkDetection.isSelected());

		jchkResolution.addActionListener(e -> {
            Middleware.setResolutionEnabled(jchkResolution.isSelected());
            if(!jchkDetection.isSelected() && jchkResolution.isSelected())
                jchkDetection.doClick();
        });

		gbc.gridy += gbc.gridheight;
//		gbc.gridwidth = GridBagConstraints.REMAINDER;
		rightPanel.add(deliveryPanel, gbc);
		deliveryPanel.setBorder(BorderFactory.createTitledBorder("Delivery"));
		deliveryPanel.setLayout(new GridBagLayout());

		GridBagConstraints dgbc = new GridBagConstraints();
		dgbc.insets = new Insets(3, 5, 3, 5);
		dgbc.fill = GridBagConstraints.BOTH;
		dgbc.gridx = 0;
		dgbc.gridy = 0;
		deliveryPanel.add(new JLabel("Src"), dgbc);
		dgbc.gridx += dgbc.gridwidth;
		dgbc.weightx = 1;
//		dgbc.gridwidth = GridBagConstraints.REMAINDER;
		deliveryPanel.add(srctf, dgbc);
        srctf.setEditable(false);
		dgbc.gridx += dgbc.gridwidth;
//		dgbc.gridy += dgbc.gridheight;
		dgbc.weightx = 0;
		deliveryPanel.add(new JLabel("Dst"), dgbc);
		dgbc.gridx += dgbc.gridwidth;
		dgbc.weightx = 1;
		deliveryPanel.add(desttf, dgbc);
        desttf.setEditable(false);
		dgbc.gridx = 0;
		dgbc.gridy += dgbc.gridheight;
		dgbc.gridwidth = GridBagConstraints.REMAINDER;
		dgbc.weightx = 1;
		deliveryPanel.add(startdButton, dgbc);
		dgbc.gridwidth = 2;
		deliveryPanel.add(deliverButton, dgbc);
		dgbc.gridx += dgbc.gridwidth;
		deliveryPanel.add(canceldButton, dgbc);
		startdButton.addActionListener(e -> {
            src = dest = null;
            updateDeliverySrcPanel();
            updateDeliveryDstPanel();
            isDeliveryStarted = true;
            startdButton.setVisible(false);
            deliverButton.setVisible(true);
            deliverButton.setEnabled(false);
            canceldButton.setVisible(true);
        });
		deliverButton.addActionListener(e -> {
            isDeliveryStarted = false;
            deliverButton.setVisible(false);
            startdButton.setVisible(true);
            canceldButton.setVisible(false);

            if(src != null && dest != null)
                Delivery.add(src, dest);
        });

		canceldButton.addActionListener(e -> {
            isDeliveryStarted = false;
            deliverButton.setVisible(false);
            startdButton.setVisible(true);
            canceldButton.setVisible(false);
        });
		deliverButton.setVisible(false);
		canceldButton.setVisible(false);

		gbc.gridy += gbc.gridheight;
		JLabel logLabel = new JLabel("Log");
		rightPanel.add(logLabel, gbc);

		gbc.gridy += gbc.gridheight;
		gbc.weighty = 1;
		rightPanel.add(logtaScroll, gbc);
		logta.setEditable(false);
		logta.setLineWrap(true);
		logta.setWrapStyleWord(true);

		new Thread(blinkThread, "Blink Thread").start();
		jchkResolution.doClick();
		jchkBalloon.doClick();
		jchkCrash.doClick();
		jchkError.doClick();

		setTitle("Dashboard");
		setContentPane(controlPanel);
		pack();
		setLocationRelativeTo(null);
	}

	private static final Map<Component, Boolean> compoEnable = new HashMap<>();
	private static void setEnabledRecurse(Component root, boolean enabled){
		if(root == null)
			return;
		Queue<Component> queue = new LinkedList<>();
		queue.add(root);
		while(!queue.isEmpty()){
			Component compo = queue.poll();
			if(compo instanceof Container)
				Collections.addAll(queue, ((Container) compo).getComponents());
			if(enabled){
				Boolean b = compoEnable.get(compo);
				if(b != null)
					compo.setEnabled(b);
			}
			else{
				compoEnable.put(compo, compo.isEnabled());
				compo.setEnabled(false);
			}
		}
	}

	public static void enableCtrlUI(boolean enabled){
		setEnabledRecurse(controlPanel, enabled);
	}

	private static final JDialog deviceDialog = new JDialog(getInstance(), "Device");
	public static void showDeviceDialog(boolean closable){
		deviceDialog.setDefaultCloseOperation(closable ? HIDE_ON_CLOSE : DO_NOTHING_ON_CLOSE);
		deviceDialog.setContentPane(checkingPanel);
		deviceDialog.pack();
//		deviceDialog.setLocationRelativeTo(null);
		deviceDialog.setVisible(true);
//		setEnabledRecurse(controlPanel, false);
	}

	public static void closeDeviceDialog(){
		deviceDialog.setVisible(false);
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

	private static void updateRoadConditionPane(Section s){
        if(s == null) {
            roadta.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
		sb.append(s.name).append("\n");
		if(!s.cars.isEmpty()){
            sb.append("Cars:\n");
			for(Car car : s.cars){
				sb.append(car.name).append(" (").append(car.getStateStr()).append(") ").append(car.getDirStr());
				if(car.dest != null)
					sb.append(" Dest:").append(car.dest.name);
                sb.append("\n");
			}
		}
		if(!s.waiting.isEmpty()){
            sb.append("Waiting Cars:\n");
			for(Car car : s.waiting)
                sb.append(car.name).append(" (").append(car.getStateStr()).append(") ").append(car.getDirStr()).append("\n");
		}
		if(!s.realCars.isEmpty()){
			sb.append("Real Cars:\n");
			for(Car car : s.realCars){
				sb.append(car.name).append(" (").append(car.getRealStateStr()).append(") ").append(car.getRealDirStr()).append("\n");
			}
		}
		roadta.setText(sb.toString());
	}

    private static void updateRoadConditionPane(Building b){
        if(b == null) {
            roadta.setText("");
            return;
        }
        roadta.setText(b.name);
    }

	private static void updateDeliverySrcPanel(){
        srctf.setText(src != null ? src.name : "");
	}

	private static void updateDeliveryDstPanel(){
        desttf.setText(dest != null ? dest.name : "");
	}

	public static synchronized void updateDeliveryTaskPanel(){
		Queue<Delivery.DeliveryTask> queue = new LinkedList<>();
		queue.addAll(Delivery.searchTasks);
		queue.addAll(Delivery.deliveryTasks);
		delivta.setText("Nums: " + queue.size());
		for(Delivery.DeliveryTask dt : queue)
			delivta.append("\nPhase: "+dt.phase+" Src: "+dt.src.name+" Dst: "+dt.dest.name);
	}

	public static synchronized void updateRemedyCommandPanel(){
		remedyta.setText("Nums: "+ Remedy.getQueue().size());
		for(Command cmd : Remedy.getQueue()){
			remedyta.append("\n"+cmd.car.name+" "+((cmd.cmd==0)?"S":"F")+" "+cmd.deadline);
		}
	}

	public static synchronized void updateVehicleConditionPanel(Car car){
		VCPanel.updateVC(car);
	}

	public static synchronized void updateVehicleConditionPanel(){
		Resource.getConnectedCars().forEach(Dashboard::updateVehicleConditionPanel);
	}

	public static synchronized void appendLog(String str){
		logta.append(str+"\n");
	}

	public static synchronized void addCar(Car car){
		for(int i = 0;i < carbox.getItemCount();i++)
			if(carbox.getItemAt(i).equals(car.name))
				return;
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

	public static synchronized void removeCar(Car car){
		carbox.removeItem(car.name);
		VCPanel.removeCar(car);
	}

	public static void setSelectedCar(Car car){
        carbox.setSelectedItem(car.name);
    }

	static Car getSelectedCar(){
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

	public static void updateAll(){
        trafficMap.repaint();
        updateDeliverySrcPanel();
        updateDeliveryDstPanel();
        updateDeliveryTaskPanel();
        updateRemedyCommandPanel();
        updateVehicleConditionPanel();
        roadta.setText("");
        logta.setText("");
    }

    private class SectionIconListener extends MouseAdapter{
		Section section = null;

		SectionIconListener(Section section) {
			this.section = section;
		}
		public void mousePressed(MouseEvent e) {
			if (section == null || !section.icon.isEnabled())
				return;
//			System.out.println(section.name);
			// for delivery tasks
			if (isDeliveryStarted) {
				if (src == null) {
					src = section;
					updateDeliverySrcPanel();
				}
				else if (dest == null) {
					if (src instanceof Section && section.sameAs((Section) src))
						return;
					dest = section;
					updateDeliveryDstPanel();
					deliverButton.setEnabled(true);
				}
			}
//			else if (e.getButton() == MouseEvent.BUTTON1) {
//				// left click
//				Car car = getSelectedCar();
//				if (car != null) {
//					radioButtonGroup.clearSelection();
//					for (int i = 0; i < 4; i++)
//						dirButtons[i].setEnabled(false);
//					if (section.cars.contains(car)) {
//						car.dir = -1;
//						car.leave(section);
//					} else {
//						dirButtons[section.dir[0]].setEnabled(true);
//						if (section.dir[1] >= 0)
//							dirButtons[section.dir[1]].setEnabled(true);
//
//						if (car.dir >= 0 && dirButtons[car.dir].isEnabled()) {
//							dirButtons[car.dir].setSelected(true);
//							// dirButtons[selectedCar.dir].doClick();
//						} else {
//							dirButtons[section.dir[0]].setSelected(true);
//							car.dir = section.dir[0];
//							// dirButtons[section.dir[0]].doClick();
//						}
//						car.enter(section);
//					}
//					PkgHandler.send(new AppPkg().setCar(car.name, car.dir, section.name));
//				}
//			}
//			else if (e.getButton() == MouseEvent.BUTTON3) {
                // right click
            else {
				updateRoadConditionPane(section);
			}
		}
	}

	private class BuildingIconListener extends MouseAdapter{
		Building building = null;

		BuildingIconListener(Building building) {
			this.building = building;
		}

		public void mousePressed(MouseEvent e) {
			if (building == null || !building.icon.isEnabled())
				return;
//			System.out.println(building.name);
			// for delivery tasks
			if (isDeliveryStarted) {
				if (src == null) {
					src = building;
					updateDeliverySrcPanel();
				} else if (dest == null) {
					if (building == src)
						return;
					dest = building;
					updateDeliveryDstPanel();
					deliverButton.setEnabled(true);
				}
			}
			else {
                updateRoadConditionPane(building);
            }
		}
	}
}
