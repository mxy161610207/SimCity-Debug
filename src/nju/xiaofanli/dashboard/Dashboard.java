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
import nju.xiaofanli.consistency.middleware.Middleware;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.CmdSender;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.car.Remedy;
import nju.xiaofanli.device.sensor.Sensor;
import nju.xiaofanli.util.Pair;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
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
    private static final JComboBox<String> carbox = new JComboBox<>();
    private static final JButton startCarButton = new JButton("Start");
    private static final JButton stopCarButton = new JButton("Stop");
    private static final JButton startAllCarsButton = new JButton("Start all");
    private static final JButton stopAllCarsButton = new JButton("Stop all");
    private static final JLabel deliveryCountLabel = new JLabel();
    private static final JTextPane deliveryPane = new JTextPane();
    private static final JScrollPane deliveryPaneScroll = new JScrollPane(deliveryPane);
    private static final JTextArea remedyta = new JTextArea();
    private static final JScrollPane remedytaScroll = new JScrollPane(remedyta);
    private static final JTextPane logPane = new JTextPane();
    private static final JScrollPane logPaneScroll = new JScrollPane(logPane);
    //    private static final JLabel completedUserDeliveryCountLabel = new JLabel();
//    private static final JTextPane completedUserDeliveryPane = new JTextPane();
//    private static final JScrollPane completedUserDeliveryPaneScroll = new JScrollPane(completedUserDeliveryPane);
//    private static final JLabel completedSysDeliveryCountLabel = new JLabel();
//    private static final JTextPane completedSysDeliveryPane = new JTextPane();
//    private static final JScrollPane completedSysDeliveryPaneScroll = new JScrollPane(completedSysDeliveryPane);
    private static final VehicleConditionPanel VCPanel = new VehicleConditionPanel();
    private static final JButton resetButton = new JButton("Reset");
    private static final JButton deviceButton = new JButton("Device");
    private static final JButton startdButton = new JButton("Enable selection mode");
    private static final JButton deliverButton = new JButton("Deliver");
    private static final JButton canceldButton = new JButton("Cancel");
    private static final JCheckBox jchkSensor = new JCheckBox("Sensor number");
    private static final JCheckBox jchkRoad = new JCheckBox("Road number");
    private static final JCheckBox jchkBalloon = new JCheckBox("Inconsistent context");
    private static final JCheckBox jchkCrash = new JCheckBox("Crash sound");
    private static final JCheckBox jchkError = new JCheckBox("Error sound");
    private static final JCheckBox jchkDetection = new JCheckBox("Detection");
    private static final JCheckBox jchkResolution = new JCheckBox("Resolution");
    private static final JCheckBox jchkAutoGen = new JCheckBox("Automatically generate task(s)");
    public static boolean showSensor = false, showRoad = false, showBalloon = false,
            playCrashSound = false,	playErrorSound = false;

    private static final JTextField srctf = new JTextField();
    private static final JTextField desttf = new JTextField();
    private static final JTextField console  = new JTextField("Console");
    private static Location src = null, dest = null;
    private static boolean delivSelModeOn = false;
    private static final TrafficMap trafficMap = TrafficMap.getInstance();
    private static final JPanel leftPanel = new JPanel(), rightPanel = new JPanel(), bottomPanel = new JPanel();

    public static boolean blink = false;
    private static final Runnable blinkThread = new Runnable() {
        private final int duration = 500;
        private int count = 0;
        public void run() {
            //noinspection InfiniteLoopStatement
            while(true){
                blink = !blink;
                count++;
                for(Sensor[] array : Resource.getSensors())
                    for(Sensor sensor : array) {
                        if(sensor.balloon.duration > 0){
                            if(!sensor.balloon.isVisible())
                                sensor.balloon.setVisible(true);
                            sensor.balloon.duration -= duration;
                        }
                        else if(sensor.balloon.isVisible())
                            sensor.balloon.setVisible(false);
                    }

                for(Road road : Resource.getRoads().values()){
                    if (!road.cars.isEmpty() && road.cars.peek().isLoading)
                        road.icon.repaint();
                }

//                if (count % 4 == 0) {
//                    switch (TrafficMap.fakeCarIconLabel.getText()){
//                        case "Fake car":
//                            TrafficMap.fakeCarIconLabel.setText("(caused by"); break;
//                        case "(caused by":
//                            TrafficMap.fakeCarIconLabel.setText("inconsistent data)"); break;
//                        case "inconsistent data)":
//                            TrafficMap.fakeCarIconLabel.setText("Fake car"); break;
//                    }
//
//                    switch (TrafficMap.realCarIconLabel.getText()){
//                        case "Real car":
//                            TrafficMap.realCarIconLabel.setText("(invisible to"); break;
//                        case "(invisible to":
//                            TrafficMap.realCarIconLabel.setText("other cars)"); break;
//                        case "other cars)":
//                            TrafficMap.realCarIconLabel.setText("Real car"); break;
//                    }
//                }
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    static {
        carbox.setFont(Resource.bold16dialog);
        startCarButton.setFont(Resource.bold16dialog);
        startCarButton.setMargin(new Insets(0, 0, 0, 0));
        stopCarButton.setFont(Resource.bold16dialog);
        stopCarButton.setMargin(new Insets(0, 0, 0, 0));
        startAllCarsButton.setFont(Resource.bold16dialog);
        startAllCarsButton.setMargin(new Insets(0, 0, 0, 0));
        stopAllCarsButton.setFont(Resource.bold16dialog);
        stopAllCarsButton.setMargin(new Insets(0, 0, 0, 0));
        resetButton.setFont(Resource.bold16dialog);
        resetButton.setMargin(new Insets(0, 0, 0, 0));
        deviceButton.setFont(Resource.bold16dialog);
        deviceButton.setMargin(new Insets(0, 0, 0, 0));
        startdButton.setFont(Resource.bold16dialog);
        startdButton.setMargin(new Insets(0, 0, 0, 0));
        deliverButton.setFont(Resource.bold16dialog);
        deliverButton.setMargin(new Insets(0, 0, 0, 0));
        canceldButton.setFont(Resource.bold16dialog);
        canceldButton.setMargin(new Insets(0, 0, 0, 0));
        jchkSensor.setFont(Resource.bold16dialog);
        jchkRoad.setFont(Resource.bold16dialog);
        jchkBalloon.setFont(Resource.bold16dialog);
        jchkCrash.setFont(Resource.bold16dialog);
        jchkError.setFont(Resource.bold16dialog);
        jchkDetection.setFont(Resource.bold16dialog);
        jchkResolution.setFont(Resource.bold16dialog);
        jchkAutoGen.setFont(Resource.bold16dialog);
        srctf.setFont(Resource.bold16dialog);
        srctf.setEditable(false);
        desttf.setFont(Resource.bold16dialog);
        desttf.setEditable(false);
        console.setFont(Resource.bold16dialog);
        deliveryCountLabel.setFont(Resource.bold16dialog);
        deliveryCountLabel.setBackground(null);
        deliveryPane.setEditable(false);
        deliveryPane.setBackground(null);
//        deliveryPane.setContentType("text/html");
        deliveryPane.setFont(Resource.plain17dialog);
        deliveryPaneScroll.setBorder(BorderFactory.createEmptyBorder());
//        deliveryPaneScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        deliveryPaneScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        remedytaScroll.setBorder(BorderFactory.createEmptyBorder());
//        roadPaneScroll.setBorder(BorderFactory.createEmptyBorder());
        logPaneScroll.setBorder(BorderFactory.createEmptyBorder());
        logPaneScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        logPaneScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        logPane.setFont(Resource.plain17dialog);
        logPane.setEditable(false);
        logPane.setBackground(null);
        ((DefaultCaret) logPane.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        remedyta.setFont(Resource.plain17dialog);
        remedyta.setEditable(false);
        remedyta.setBackground(null);
//		updateRemedyCommandPanel();
    }

    private Dashboard() {
//		setEnabled(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        setResizable(false);
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
            ((TitledBorder) brickPanel.getBorder()).setTitleFont(Resource.bold16dialog);
            brickPanel.setLayout(new GridBagLayout());
            GridBagConstraints bgbc = new GridBagConstraints();
//		bgbc.fill = GridBagConstraints.BOTH;
            bgbc.gridx = bgbc.gridy = 0;
            bgbc.weightx = bgbc.weighty = 1;
            bgbc.insets = new Insets(1, 2, 1, 2);
            for (String name : Resource.getBricks()) {
                bgbc.anchor = GridBagConstraints.WEST;
                JLabel nameLabel = new JLabel(name);
                nameLabel.setFont(Resource.plain17dialog); //TODO consistent font size
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
            shutDownBtn.setFont(Resource.bold16dialog);
            shutDownBtn.setMargin(new Insets(0, 0, 0, 0));
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
            ((TitledBorder) carPanel.getBorder()).setTitleFont(Resource.bold16dialog);
            carPanel.setLayout(new GridBagLayout());
            GridBagConstraints cgbc = new GridBagConstraints();
//		cgbc.fill = GridBagConstraints.BOTH;
            cgbc.gridx = cgbc.gridy = 0;
            cgbc.weightx = cgbc.weighty = 1;
            cgbc.insets = new Insets(1, 2, 1, 2);
            for (Car car : Resource.getCars()) {
                cgbc.anchor = GridBagConstraints.WEST;
                JLabel nameLabel = new JLabel(car.name);
                nameLabel.setFont(Resource.plain17dialog); //TODO consistent font size
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
    private static int controlPanelWidth = 1280, controlPanelHeight = 720;
    public void loadCtrlUI(){
        if(controlPanel != null){
            setTitle("Dashboard");
            setContentPane(controlPanel);
//			pack();
            setLocationRelativeTo(null);
            return;
        }
        controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setPreferredSize(new Dimension(controlPanelWidth, controlPanelHeight));
        controlPanel.setSize(controlPanelWidth, controlPanelHeight);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
//		gbc.anchor = GridBagConstraints.CENTER;

        for(Road road : TrafficMap.roads.values())
            road.icon.addMouseListener(new RoadIconListener(road));
        for(Building building : TrafficMap.buildings.values())
            building.icon.addMouseListener(new BuildingIconListener(building));

        gbc.gridx = gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = gbc.weighty = 0;
        controlPanel.add(trafficMap, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
		leftPanel.setPreferredSize(new Dimension(230, 0));
        controlPanel.add(leftPanel, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = gbc.weighty = 0;
//        rightPanel.setPreferredSize(new Dimension(330, 0));
        controlPanel.add(rightPanel, gbc);

        gbc.gridx = gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.BOTH;
        bottomPanel.setPreferredSize(new Dimension(560, 0));
        controlPanel.add(bottomPanel, gbc);

        //left panel settings
        leftPanel.setLayout(new GridBagLayout());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(1, 2, 1, 2);
        gbc.gridx = gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = gbc.weighty = 1;

        VCPanel.setBorder(BorderFactory.createTitledBorder("Vehicle info"));
        ((TitledBorder) VCPanel.getBorder()).setTitleFont(Resource.bold16dialog);
        leftPanel.add(VCPanel, gbc);

        //right panel settings
        rightPanel.setLayout(new GridBagLayout());
        gbc.gridx = gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        gbc.weightx = gbc.weighty = 0;

        JPanel tmpPanel = new JPanel(new GridLayout(1, 3, 4, 0));
        rightPanel.add(tmpPanel, gbc);
        tmpPanel.add(resetButton);
        resetButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if(!resetButton.isEnabled())
                    return;
                StateSwitcher.setInconsistencyType(e.getButton() == MouseEvent.BUTTON1);
                StateSwitcher.startResetting();
            }
        });

        tmpPanel.add(deviceButton);
        deviceButton.addActionListener(e -> showDeviceDialog(true));

        tmpPanel.add(console, gbc);
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
            else if(cmd.startsWith("add dt ")){
                String s = cmd.substring("add dt ".length()).toLowerCase();
                Delivery.DeliveryTask dt = new Delivery.DeliveryTask(TrafficMap.getALocation(), TrafficMap.getALocation(),
                        TrafficMap.getACitizen(), s.equals("u"));
                Delivery.add(dt);
//                if(dt.createdByUser)
//                    Delivery.completedUserDelivNum++;
//                else
//                    Delivery.completedSysDelivNum++;
            }
            else if(cmd.equals("all busy")){
                Dashboard.log("All cars are busy!\n", Color.RED);
            }
            else if(cmd.equals("pick") || cmd.equals("drop")) {
                String carName = Car.getACarName();
                Citizen citizen = TrafficMap.getACitizen();
                Location loc = TrafficMap.getALocation();
                List<Pair<String, Color>> strings = new ArrayList<>();
                strings.add(new Pair<>(carName, Car.colorOf(carName)));
                strings.add(new Pair<>(cmd.equals("pick") ? " picks up " : " drops off ", Color.BLACK));
                strings.add(new Pair<>(citizen.name, citizen.icon.color));
                strings.add(new Pair<>(" at ", Color.BLACK));
                strings.add(new Pair<>(loc.name, Resource.DEEP_SKY_BLUE));
                strings.add(new Pair<>("\n", Color.BLACK));
                Dashboard.log(strings);
            }
            else if(cmd.equals("wander")) {
                for(Citizen citizen : Resource.getCitizens()) {
                    citizen.setAction(Citizen.Action.Wander);
                    citizen.startAction();
                }
            }
        });

        gbc.gridy += gbc.gridheight;
        JPanel miscPanel = new JPanel();
        rightPanel.add(miscPanel, gbc);
        miscPanel.setBorder(BorderFactory.createTitledBorder("Display & Sound Options"));
        ((TitledBorder) miscPanel.getBorder()).setTitleFont(Resource.bold16dialog);
        miscPanel.setLayout(new GridLayout(2, 1));
        JPanel topMiscPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)),
                bottomMiscPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        miscPanel.add(topMiscPanel);
        miscPanel.add(bottomMiscPanel);

        topMiscPanel.add(jchkRoad);
        topMiscPanel.add(jchkSensor);
        bottomMiscPanel.add(jchkBalloon);
        bottomMiscPanel.add(jchkCrash);

        jchkRoad.addActionListener(e -> {
            showRoad = jchkRoad.isSelected();
            trafficMap.repaint();
        });
        jchkRoad.doClick();

        jchkSensor.addActionListener(e -> {
            showSensor = jchkSensor.isSelected();
            for(Sensor[] array : TrafficMap.sensors)
                for(Sensor s : array)
                    s.icon.setVisible(showSensor);
        });

        jchkBalloon.addActionListener(e -> {
            showBalloon = jchkBalloon.isSelected();
            trafficMap.repaint();
        });

        jchkCrash.addActionListener(e -> playCrashSound = jchkCrash.isSelected());
        jchkError.addActionListener(e -> playErrorSound = jchkError.isSelected());

        gbc.gridy += gbc.gridheight;
        JPanel CCPanel = new JPanel();
        rightPanel.add(CCPanel, gbc);
        CCPanel.setBorder(BorderFactory.createTitledBorder("Consistency Checking"));
        ((TitledBorder) CCPanel.getBorder()).setTitleFont(Resource.bold16dialog);
        CCPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
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
        rightPanel.add(carbox, gbc);

        gbc.gridy += gbc.gridheight;
        JPanel cmdPanel = new JPanel(new GridLayout(1, 4, 4, 4));
        rightPanel.add(cmdPanel, gbc);
        cmdPanel.add(startCarButton);
        cmdPanel.add(stopCarButton);
        cmdPanel.add(startAllCarsButton);
        cmdPanel.add(stopAllCarsButton);
        startCarButton.addActionListener(e -> {
            Car car = getSelectedCar();
            if(car != null){
                car.notifyPolice(Police.REQUEST2ENTER, true);
            }
        });
        stopCarButton.addActionListener(e -> {
            Car car = Dashboard.getSelectedCar();
            if(car != null){
                car.notifyPolice(Police.REQUEST2STOP, true);
            }
        });
        startAllCarsButton.addActionListener(e -> {
            for (Car car : Resource.getConnectedCars())
                car.notifyPolice(Police.REQUEST2ENTER, true);
        });
        stopAllCarsButton.addActionListener(e -> {
            for (Car car : Resource.getConnectedCars())
                car.notifyPolice(Police.REQUEST2STOP, true);
        });

        gbc.gridy += gbc.gridheight;
        JPanel deliveryPanel = new JPanel();
        rightPanel.add(deliveryPanel, gbc);
        deliveryPanel.setBorder(BorderFactory.createTitledBorder("Taxi Service"));
        ((TitledBorder) deliveryPanel.getBorder()).setTitleFont(Resource.bold16dialog);
        deliveryPanel.setLayout(new GridBagLayout());

        GridBagConstraints dgbc = new GridBagConstraints();
        dgbc.insets = new Insets(1, 2, 1, 2);
        dgbc.fill = GridBagConstraints.BOTH;
        dgbc.gridx = dgbc.gridy = 0;
        dgbc.gridwidth = GridBagConstraints.REMAINDER;
        deliveryPanel.add(jchkAutoGen, dgbc);
        jchkAutoGen.addActionListener(e -> {
            Delivery.autoGenTasks = jchkAutoGen.isSelected();
            if (Delivery.autoGenTasks)
                Delivery.autoGenTasks();
        });
        dgbc.gridy += dgbc.gridheight;
        JPanel srcAndDestPanel = new JPanel(new GridLayout(1, 2));
        deliveryPanel.add(srcAndDestPanel, dgbc);
        JPanel srcPanel = new JPanel(new GridBagLayout()), destPanel = new JPanel(new GridBagLayout());
        srcAndDestPanel.add(srcPanel);
        srcAndDestPanel.add(destPanel);

        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.insets = new Insets(1, 2, 1, 2);
        sgbc.fill = GridBagConstraints.BOTH;
        sgbc.gridx = sgbc.gridy = 0;
        sgbc.weightx = sgbc.weighty = 0;
        JLabel srcLabel = new JLabel("Src");
        srcLabel.setFont(Resource.bold16dialog);
        srcPanel.add(srcLabel, sgbc);
        sgbc.gridx += sgbc.gridwidth;
        sgbc.weightx = 1;
        srcPanel.add(srctf, sgbc);

        sgbc.gridx = sgbc.gridy = 0;
        sgbc.weightx = sgbc.weighty = 0;
        JLabel destLabel = new JLabel("Dest");
        destLabel.setFont(Resource.bold16dialog);
        destPanel.add(destLabel, sgbc);
        sgbc.gridx += sgbc.gridwidth;
        sgbc.weightx = 1;
        destPanel.add(desttf, sgbc);

//        dgbc.gridy += dgbc.gridheight;
//        dgbc.gridwidth = 1;
//        JLabel srcLabel = new JLabel("Src");
//        srcLabel.setFont(Resource.bold16dialog);
//		deliveryPanel.add(srcLabel, dgbc);
//		dgbc.gridx += dgbc.gridwidth;
//		dgbc.weightx = 1;
//		deliveryPanel.add(srctf, dgbc);
//
//		dgbc.gridx += dgbc.gridwidth;
//		dgbc.weightx = 0;
//        JLabel destLabel = new JLabel("Dest");
//        destLabel.setFont(Resource.bold16dialog);
//		deliveryPanel.add(destLabel, dgbc);
//		dgbc.gridx += dgbc.gridwidth;
//		dgbc.weightx = 1;
//		deliveryPanel.add(desttf, dgbc);

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
            delivSelModeOn = true;
            src = dest = null;
            updateDeliverySrcPanel("Click any loc");
            updateDeliveryDestPanel("");

            startdButton.setVisible(false);
            deliverButton.setVisible(true);
            deliverButton.setEnabled(false);
            canceldButton.setVisible(true);
        });
        deliverButton.addActionListener(e -> {
            if(src != null && dest != null)
                Delivery.add(src, dest, true);

            delivSelModeOn = false;
            src = dest = null;
            updateDeliverySrcPanel("");
            updateDeliveryDestPanel("");
            deliverButton.setVisible(false);
            canceldButton.setVisible(false);
            startdButton.setVisible(true);
        });

        canceldButton.addActionListener(e -> {
            delivSelModeOn = false;
            src = dest = null;
            updateDeliverySrcPanel("");
            updateDeliveryDestPanel("");
            deliverButton.setVisible(false);
            startdButton.setVisible(true);
            canceldButton.setVisible(false);
        });
        deliverButton.setVisible(false);
        canceldButton.setVisible(false);

        //bottom panel settings
        bottomPanel.setLayout(new GridBagLayout());
        gbc.gridx = gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;

        JPanel ongoingDTPanel = new JPanel(new BorderLayout());
        bottomPanel.add(ongoingDTPanel, gbc);
        ongoingDTPanel.setBorder(BorderFactory.createTitledBorder("Tasks"));
        ((TitledBorder) ongoingDTPanel.getBorder()).setTitleFont(Resource.bold16dialog);
        ongoingDTPanel.add(deliveryCountLabel, BorderLayout.NORTH);
        ongoingDTPanel.add(deliveryPane, BorderLayout.CENTER);
        updateDeliveryTaskPanel();

        gbc.gridy += gbc.gridheight;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weighty = 1;
        logPaneScroll.setBorder(BorderFactory.createTitledBorder("Logs"));
        ((TitledBorder) logPaneScroll.getBorder()).setTitleFont(Resource.bold16dialog);
        bottomPanel.add(logPaneScroll, gbc);
        new Thread(blinkThread, "Blink Thread").start();

//		jchkResolution.doClick();
//		jchkBalloon.doClick();
//		jchkCrash.doClick();
//		jchkError.doClick();
        reset();

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
        TrafficMap.enableSensorIcons(true);
    }

    private static final JDialog deviceDialog = new JDialog(getInstance(), "Device");
    public static void showDeviceDialog(boolean closable){
        if (closable && deviceDialog.isVisible()) {
            deviceDialog.setVisible(false);
            return;
        }
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

    public static Road getNearestRoad(int x, int y){
        if(x < 0 || x >= trafficMap.getWidth() || y < 0 || y >= trafficMap.getHeight())
            return null;
        int min = Integer.MAX_VALUE, tmp;
        Road road = null;
//		System.out.println(x+", "+y);
        for(Road r : TrafficMap.roads.values()){
//			System.out.println(s.name+"\t"+s.icon.coord.centerX+", "+s.icon.coord.centerY);
            tmp = (int) (Math.pow(x-r.icon.coord.centerX, 2) + Math.pow(y-r.icon.coord.centerY, 2));
            if(tmp < min){
                road = r;
                min = tmp;
            }
        }
        return road;
    }

    private static void updateRoadInfoPane(Location loc){
        TrafficMap.updateRoadInfoPane(loc);
    }

    private static void updateDeliverySrcPanel(){
        srctf.setText(src != null ? src.name : "");
    }

    private static void updateDeliverySrcPanel(String text){
        srctf.setText(text);
    }

    private static void updateDeliveryDestPanel(){
        desttf.setText(dest != null ? dest.name : "");
    }

    private static void updateDeliveryDestPanel(String text) {
        desttf.setText(text);
    }

    public static void updateDeliveryTaskPanel(){
        Queue<Delivery.DeliveryTask> queue = new LinkedList<>();
        queue.addAll(Delivery.searchTasks);
        queue.addAll(Delivery.deliveryTasks);
        synchronized (deliveryCountLabel) {
            deliveryCountLabel.setText("Ongoing: " + queue.size()
                    + "    Completed: " + (Delivery.completedSysDelivNum + Delivery.completedUserDelivNum));
        }
        boolean firstLine = true;
        synchronized (deliveryPane) {
            deliveryPane.setText("");
            List<Pair<String, Color>> strings = new ArrayList<>();
            for (Delivery.DeliveryTask dt : queue) {
                if(firstLine)
                    firstLine = false;
                else
                    strings.add(new Pair<>("\n", null));
                strings.add(new Pair<>(dt.citizen.name, dt.citizen.icon.color));
                switch (dt.phase) {
                    case Delivery.DeliveryTask.SEARCH_CAR:
                        strings.add(new Pair<>(" at ", Color.BLACK));
                        strings.add(new Pair<>(dt.src.name, Resource.DEEP_SKY_BLUE));
                        strings.add(new Pair<>(" needs a taxi to ", Color.BLACK));
                        strings.add(new Pair<>(dt.dest.name, Resource.DEEP_SKY_BLUE));
                        break;
                    case Delivery.DeliveryTask.HEAD4SRC:
                        strings.add(new Pair<>(" at ", Color.BLACK));
                        strings.add(new Pair<>(dt.src.name, Resource.DEEP_SKY_BLUE));
                        strings.add(new Pair<>(" waits for ", Color.BLACK));
                        strings.add(new Pair<>(dt.car.name, dt.car.icon.color));
                        break;
                    case Delivery.DeliveryTask.HEAD4DEST:
                        strings.add(new Pair<>(" gets on ", Color.BLACK));
                        strings.add(new Pair<>(dt.car.name, dt.car.icon.color));
                        strings.add(new Pair<>(" at ", Color.BLACK));
                        strings.add(new Pair<>(dt.car.loc.name, Resource.DEEP_SKY_BLUE));
                        strings.add(new Pair<>(" and heads for ", Color.BLACK));
                        strings.add(new Pair<>(dt.dest.name, Resource.DEEP_SKY_BLUE));
                        break;
                    case Delivery.DeliveryTask.COMPLETED:
                        strings.add(new Pair<>(" gets off ", Color.BLACK));
                        strings.add(new Pair<>(dt.car.name, dt.car.icon.color));
                        strings.add(new Pair<>(" at ", Color.BLACK));
                        strings.add(new Pair<>(dt.car.loc.name, Resource.DEEP_SKY_BLUE));
                        break;
                }
            }
            append2pane(strings, deliveryPane);
        }
    }

    public static void updateRemedyCommandPanel(){
        synchronized (remedyta) {
            remedyta.setText("Nums: " + Remedy.getQueue().size());
            for (Command cmd : Remedy.getQueue()) {
                remedyta.append("\n" + cmd.car.name + " " + ((cmd.cmd == 0) ? "S" : "F") + " " + cmd.deadline);
            }
        }
    }

    public static void updateVehicleConditionPanel(Car car){
        synchronized (VCPanel) {
            VCPanel.updateVC(car);
        }
    }

    public static void updateVehicleConditionPanel(){
        synchronized (VCPanel) {
            Resource.getConnectedCars().forEach(Dashboard::updateVehicleConditionPanel);
        }
    }

    public static void append2pane(List<Pair<String, Color>> strings, JTextPane pane) {
        if(strings == null || strings.isEmpty())
            return;
        synchronized (pane) {
            for (Pair<String, Color> string : strings)
                append2pane(string.first, string.second, pane);
        }
    }

    public static void append2pane(String str, Color color, JTextPane pane) {
        if (color == null)
            color = Color.BLACK;
        synchronized (pane) {
            StyleContext sc = StyleContext.getDefaultStyleContext();
            Style style = sc.getStyle(color.toString());
            if (style == null) {
                style = sc.addStyle(color.toString(), null);
                StyleConstants.setForeground(style, color);
            }
            StyledDocument doc = pane.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), str, style);
                pane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    public static void log(List<Pair<String, Color>> strings) {
        append2pane(strings, logPane);
    }

    public static void log(String str, Color color) {
        append2pane(str, color, logPane);
    }

    public static synchronized void addCar(Car car){
        for(int i = 0;i < carbox.getItemCount();i++)
            if(carbox.getItemAt(i).equals(car.name))
                return;
        carbox.addItem(car.name);
        VCPanel.addCar(car);
        PkgHandler.send(new AppPkg().setCar(car.name, -1, null));
    }

    public static synchronized void removeCar(Car car){
        carbox.removeItem(car.name);
        VCPanel.removeCar(car);
    }

    public static void setSelectedCar(Car car){
        carbox.setSelectedItem(car.name);
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

    public static void reset(){
        src = dest = null;
        delivSelModeOn = false;
        deliverButton.setVisible(false);
        startdButton.setEnabled(Delivery.MAX_USER_DELIV_NUM > 0);
        startdButton.setVisible(true);
        canceldButton.setVisible(false);
        trafficMap.repaint();
        updateDeliverySrcPanel("");
        updateDeliveryDestPanel("");
        updateDeliveryTaskPanel();
        updateRemedyCommandPanel();
        updateVehicleConditionPanel();
        logPane.setText("");
        if(getSelectedCar() == null) {
            startCarButton.setEnabled(false);
            stopCarButton.setEnabled(false);
            startAllCarsButton.setEnabled(false);
            stopAllCarsButton.setEnabled(false);
        }
        else {
//            stopCarButton.setEnabled(false);
//            stopAllCarsButton.setEnabled(false);
        }
    }

    public static void enableDeliveryButton(boolean b){
        startdButton.setEnabled(b);
    }

    public static void enableStartCarButton(boolean b) {
        startCarButton.setEnabled(b);
    }

    public static void enableStopCarButton(boolean b) {
        stopCarButton.setEnabled(b);
    }

    public static void enableStartAllCarsButton(boolean b) {
        startAllCarsButton.setEnabled(b);
    }

    public static void enableStopAllCarsButton(boolean b) {
        stopAllCarsButton.setEnabled(b);
    }

    private class RoadIconListener extends MouseAdapter{
        Road road = null;

        RoadIconListener(Road road) {
            this.road = road;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (road == null || !road.icon.isEnabled())
                return;
//			System.out.println(road.name);
            // for delivery tasks
            if (delivSelModeOn) {
                if (src == null) {
                    src = road;
                    updateDeliverySrcPanel(src.name);
                    updateDeliveryDestPanel("Click any loc");
                }
                else if (dest == null) {
                    if (src instanceof Road && road.sameAs((Road) src))
                        return;
                    dest = road;
                    updateDeliveryDestPanel(dest.name);
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
//					if (road.cars.contains(car)) {
//						car.dir = -1;
//						car.leave(road);
//					} else {
//						dirButtons[road.dir[0]].setEnabled(true);
//						if (road.dir[1] >= 0)
//							dirButtons[road.dir[1]].setEnabled(true);
//
//						if (car.dir >= 0 && dirButtons[car.dir].isEnabled()) {
//							dirButtons[car.dir].setSelected(true);
//							// dirButtons[selectedCar.dir].doClick();
//						} else {
//							dirButtons[road.dir[0]].setSelected(true);
//							car.dir = road.dir[0];
//							// dirButtons[road.dir[0]].doClick();
//						}
//						car.enter(road);
//					}
//					PkgHandler.send(new AppPkg().setCar(car.name, car.dir, road.name));
//				}
//			}
//			else if (e.getButton() == MouseEvent.BUTTON3) {
            // right click
            else {
                if(TrafficMap.roadPaneScroll.isVisible()) {
                    TrafficMap.roadPaneScroll.setVisible(false);
                    return;
                }

                if (road instanceof Road.Crossroad) {
                    TrafficMap.roadPaneScroll.setLocation(road.icon.getX()+road.icon.getWidth(),
                            road.icon.getY()+(road.icon.getHeight()- TrafficMap.roadPaneScroll.getHeight())/2);
                }
                else if (((Road.Street.StreetIcon) road.icon).isVertical) {
                    int rightmost = road.icon.getX() + road.icon.getWidth() + TrafficMap.roadPaneScroll.getWidth();
                    if(rightmost < trafficMap.getWidth())
                        TrafficMap.roadPaneScroll.setLocation(road.icon.getX()+road.icon.getWidth(),
                                road.icon.getY()+(road.icon.getHeight()- TrafficMap.roadPaneScroll.getHeight())/2);
                    else
                        TrafficMap.roadPaneScroll.setLocation(road.icon.getX()- TrafficMap.roadPaneScroll.getWidth(),
                                road.icon.getY()+(road.icon.getHeight()- TrafficMap.roadPaneScroll.getHeight())/2);
                }
                else {
                    int topmost = road.icon.getY() - TrafficMap.roadPaneScroll.getHeight();
                    if (topmost >= 0)
                        TrafficMap.roadPaneScroll.setLocation(road.icon.getX()+(road.icon.getWidth()- TrafficMap.roadPaneScroll.getWidth())/2, topmost);
                    else
                        TrafficMap.roadPaneScroll.setLocation(road.icon.getX()+(road.icon.getWidth()- TrafficMap.roadPaneScroll.getWidth())/2,
                                road.icon.getY()+road.icon.getHeight());
                }
                updateRoadInfoPane(road);
                TrafficMap.roadPaneScroll.setVisible(true);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
//            super.mouseExited(e);
            TrafficMap.roadPaneScroll.setVisible(false);
        }
    }

    private class BuildingIconListener extends MouseAdapter{
        Building building = null;

        BuildingIconListener(Building building) {
            this.building = building;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (building == null || !building.icon.isEnabled())
                return;
//			System.out.println(building.name);
            // for delivery tasks
            if (delivSelModeOn) {
                if (src == null) {
                    src = building;
                    updateDeliverySrcPanel(src.name);
                    updateDeliveryDestPanel("Click any loc");
                }
                else if (dest == null) {
                    if (building == src)
                        return;
                    dest = building;
                    updateDeliveryDestPanel(dest.name);
                    deliverButton.setEnabled(true);
                }
            }
            else {
                if(TrafficMap.roadPaneScroll.isVisible()) {
                    TrafficMap.roadPaneScroll.setVisible(false);
                    return;
                }

                TrafficMap.roadPaneScroll.setLocation(building.icon.getX()+building.icon.getWidth(),
                        building.icon.getY()+(building.icon.getHeight()- TrafficMap.roadPaneScroll.getHeight())/2);
                updateRoadInfoPane(building);
                TrafficMap.roadPaneScroll.setVisible(true);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
//            super.mouseExited(e);
            TrafficMap.roadPaneScroll.setVisible(false);
        }
    }
}
