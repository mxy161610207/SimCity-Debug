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
import java.awt.event.ItemEvent;
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
    private static final VehicleConditionPanel VCPanel = new VehicleConditionPanel();
    private static final JButton resetButton = new JButton("Reset");
    private static final JButton deviceButton = new JButton("Device");
    private static final JButton startdButton = new JButton("Manually create a task");
    private static final JButton deliverButton = new JButton("Deliver");
    private static final JButton canceldButton = new JButton("Cancel");
    private static final JCheckBox jchkSensor = new JCheckBox("Show sensors");
    private static final JCheckBox jchkRoad = new JCheckBox("Show roads");
    private static final JCheckBox jchkBalloon = new JCheckBox("Show error");
    private static final JCheckBox jchkCrash = new JCheckBox("Play crash");
    private static final JRadioButton idealRadioButton = new JRadioButton("Ideal");
    private static final JRadioButton noisyRadioButton = new JRadioButton("Noisy");
    private static final JRadioButton fixedRadioButton = new JRadioButton("Fixed");
    private static JRadioButton selectedScenario = null;
    private static final JCheckBox jchkAutoGen = new JCheckBox("Automatically generate tasks");
    public static boolean showSensor = false;
    public static boolean showRoad = false;
    public static boolean showError = false;
    public static boolean playCrashSound = false;

    private static final JTextField srctf = new JTextField();
    private static final JTextField desttf = new JTextField();
    private static final JTextField console  = new JTextField("Console");
    private static Location src = null, dest = null;
    private static boolean delivSelModeOn = false;
    private static final TrafficMap trafficMap = TrafficMap.getInstance();
    private static final JPanel leftPanel = new JPanel(), rightPanel = new JPanel(), bottomPanel = new JPanel();

    public static boolean blink = false;
    private static final Runnable blinkThread = () -> {
        long start = System.currentTimeMillis();
        //noinspection InfiniteLoopStatement
        while(true){
            blink = !blink;
            int elapsed = (int) (System.currentTimeMillis() - start);
            start = System.currentTimeMillis();

            for(Sensor[] array : Resource.getSensors()) {
                for (Sensor sensor : array) {
                    if (sensor.balloon.isVisible()) {
                        sensor.balloon.duration -= elapsed;
                        if (sensor.balloon.duration <= 0)
                            sensor.balloon.setVisible(false);
                    }
                }
            }

            TrafficMap.checkCrashEffectExpiration(elapsed);

            for(Road road : Resource.getRoads().values()){
                boolean isLoading = false;
                for (Car car : road.cars) {
                    if (car.isLoading) {
                        isLoading = true;
                        break;
                    }
                }
                if (isLoading)
                    road.iconPanel.repaint();
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    static {
        carbox.setEditable(false);
        carbox.setFont(Resource.bold16dialog);
        carbox.addItemListener(e -> {
            Car car = Car.carOf((String) e.getItem());
            if (car == null)
                return;
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                    car.setAvailCmd(car.getAvailCmd());
//                    enableStartCarButton(car.getAvailCmd() == Command.MOVE_FORWARD);
//                    enableStopCarButton(car.getAvailCmd() == Command.STOP);
                    break;
                case ItemEvent.DESELECTED:
                    enableStartCarButton(false);
                    enableStopCarButton(false);
                    break;
            }
        });
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
        idealRadioButton.setFont(Resource.bold16dialog);
        noisyRadioButton.setFont(Resource.bold16dialog);
        fixedRadioButton.setFont(Resource.bold16dialog);
        jchkAutoGen.setFont(Resource.bold16dialog);
        srctf.setFont(Resource.bold16dialog);
        srctf.setEditable(false);
        desttf.setFont(Resource.bold16dialog);
        desttf.setEditable(false);
        console.setFont(Resource.bold16dialog);
        deliveryCountLabel.setFont(Resource.bold16dialog);
        deliveryCountLabel.setBackground(null);
        deliveryPane.setEditable(false);
        deliveryPane.setBackground(Resource.SNOW4);
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
//        logPaneScroll.setBackground(Resource.SNOW4);
        logPane.setFont(Resource.plain17dialog);
        logPane.setEditable(false);
        logPane.setBackground(Resource.SNOW4);
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
    public static void loadCheckUI(){
        if(checkingPanel != null){
            getInstance().setTitle("Self Checking");
            getInstance().setContentPane(checkingPanel);
            getInstance().pack();
            getInstance().setLocationRelativeTo(null);
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


        getInstance().setTitle("Self Checking");
        getInstance().setContentPane(checkingPanel);
        getInstance().pack();
        getInstance().setLocationRelativeTo(null);
    }

    private static JPanel controlPanel = null;
    private static int controlPanelWidth = 1280, controlPanelHeight = 720;
    public static void loadCtrlUI(){
        if(controlPanel != null){
            getInstance().setTitle("Dashboard");
            getInstance().setContentPane(controlPanel);
            getInstance().setLocationRelativeTo(null);
            return;
        }
        controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setPreferredSize(new Dimension(controlPanelWidth, controlPanelHeight));
        controlPanel.setSize(controlPanelWidth, controlPanelHeight);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
//		gbc.anchor = GridBagConstraints.CENTER;

        for (Road road : TrafficMap.roads.values()) {
            for (Road.RoadIcon icon : road.iconPanel.icons)
                icon.addMouseListener(new RoadIconListener(icon));
        }
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
                if(!resetButton.isEnabled() || e.getButton() != MouseEvent.BUTTON3) // only focus on right click
                    return;
//                System.out.println("mouse listener");
                StateSwitcher.setInconsistencyType(false);
                StateSwitcher.startResetting();
            }
        });
        resetButton.addActionListener(e -> {
            if(!resetButton.isEnabled())
                return;
//            System.out.println("action listener");
            StateSwitcher.setInconsistencyType(true);
            StateSwitcher.startResetting();
        });

        tmpPanel.add(deviceButton);
        deviceButton.addActionListener(e -> showDeviceDialog(true));

        tmpPanel.add(console, gbc);
        console.addActionListener(e -> {
            String cmd = console.getText();
            if(cmd.startsWith("add car ")){
                String suffix = cmd.substring("add car ".length()).toLowerCase();
                String name = null;
                switch(suffix){
                    case "r":case "red":case "red car":
                        name = Car.RED; break;
                    case "b":case "black":case "black car":
                        name = Car.BLACK; break;
                    case "w":case "white":case "white car":
                        name = Car.WHITE; break;
                    case "o":case "orange":case "orange car":
                        name = Car.ORANGE; break;
                    case "g":case "green":case "green car":
                        name = Car.GREEN; break;
                    case "s":case "silver":case "suv":case "silver suv":
                        name = Car.SILVER; break;
                }
                if (name != null) {
                    Car car = Car.carOf(name);
                    if (car == null) {
                        switch (name) {
                            case Car.RED:
                                car = new Car(name, Road.roadOf("Street 5"), null, "res/red_car_icon.png"); break;
                            case Car.BLACK:
                                car = new Car(name, Road.roadOf("Street 16"), null, "res/black_car_icon.png"); break;
                            case Car.WHITE:
                                car = new Car(name, Road.roadOf("Street 8"), null, "res/white_car_icon.png"); break;
                            case Car.ORANGE:
                                car = new Car(name, Road.roadOf("Street 17"), null, "res/orange_car_icon.png"); break;
                            case Car.GREEN:
                                car = new Car(name, Road.roadOf("Street 15"), null, "res/green_car_icon.png"); break;
                            case Car.SILVER:
                                car = new Car(name, Road.roadOf("Street 11"), null, "res/silver_suv_icon.png"); break;
                        }
                        TrafficMap.cars.put(car.name, car);
                        TrafficMap.carList.add(car);
                    }
                    car.init();
                }
            }
            else if(cmd.startsWith("connect car ") || cmd.startsWith("disconnect car ")){
                String name1 = cmd.substring(
                        cmd.charAt(0) == 'c' ? "connect car ".length()
                                : "disconnect car ".length()).toLowerCase();
                String s;
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
                Dashboard.log("All cars are busy!\n", Resource.getTextStyle(Color.RED));
            }
            else if(cmd.equals("pick") || cmd.equals("drop")) {
                String carName = Car.getACarName();
                Citizen citizen = TrafficMap.getACitizen();
                Location loc = TrafficMap.getALocation();
                List<Pair<String, Style>> strings = new ArrayList<>();
                strings.add(new Pair<>(carName, Resource.getTextStyle(Car.colorOf(carName))));
                strings.add(new Pair<>(cmd.equals("pick") ? " picks up " : " drops off ", null));
                strings.add(new Pair<>(citizen.name, Resource.getTextStyle(citizen.icon.color)));
                strings.add(new Pair<>(" at ", null));
                strings.add(new Pair<>(loc.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
                strings.add(new Pair<>(".\n", null));
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
        miscPanel.setBorder(BorderFactory.createTitledBorder("Display & sound options"));
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
            for (Road road : Resource.getRoads().values()) {
                for (Road.RoadIcon icon : road.iconPanel.icons)
                    icon.showRoadNumber(showRoad);
            }
        });
        jchkRoad.doClick();

        jchkSensor.addActionListener(e -> {
            showSensor = jchkSensor.isSelected();
            for(Sensor[] array : TrafficMap.sensors)
                for(Sensor s : array)
                    s.icon.setVisible(showSensor);
        });
        jchkSensor.doClick();

        jchkBalloon.addActionListener(e -> {
            showError = jchkBalloon.isSelected();
            trafficMap.repaint();
        });

        jchkCrash.addActionListener(e -> {
            playCrashSound = jchkCrash.isSelected();
            if (playCrashSound) {
                Resource.getConnectedCars().forEach(car -> {
                    if (car.isInCrash)
                        Command.send(car, Command.HORN_ON);
                });
            }
            else {
                Resource.getConnectedCars().forEach(car -> {
                    if (car.isInCrash)
                        Command.send(car, Command.HORN_OFF);
                });
            }
        });

        gbc.gridy += gbc.gridheight;
        JPanel CCPanel = new JPanel();
        rightPanel.add(CCPanel, gbc);
        CCPanel.setBorder(BorderFactory.createTitledBorder("Scenario selection"));
        ((TitledBorder) CCPanel.getBorder()).setTitleFont(Resource.bold16dialog);
        CCPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ButtonGroup bg = new ButtonGroup();
        bg.add(idealRadioButton);
        bg.add(noisyRadioButton);
        bg.add(fixedRadioButton);
        CCPanel.add(idealRadioButton);
        CCPanel.add(noisyRadioButton);
        CCPanel.add(fixedRadioButton);

        idealRadioButton.addActionListener(e -> {
            if (selectedScenario == idealRadioButton)
                return;
            selectedScenario = idealRadioButton;
            Middleware.enableDetection(false);
            Middleware.enableResolution(false);

            if (jchkBalloon.isSelected()) {
                jchkBalloon.setEnabled(true);
                jchkBalloon.doClick();
            }

            if (jchkCrash.isSelected()) {
                jchkCrash.setEnabled(true);
                jchkCrash.doClick();
            }

            idealRadioButton.setEnabled(false);
            noisyRadioButton.setEnabled(false);
            fixedRadioButton.setEnabled(false);
            jchkBalloon.setEnabled(false);
            jchkCrash.setEnabled(false);
            TrafficMap.showFakeLocIconLabel(false);
            TrafficMap.showRealLocIconLabel(false);
        });
        idealRadioButton.doClick();
        enableScenarioSelection(true);

        noisyRadioButton.addActionListener(e -> {
            if (selectedScenario == noisyRadioButton)
                return;
            selectedScenario = noisyRadioButton;
            Middleware.enableDetection(true);
            Middleware.enableResolution(false);
            jchkBalloon.setEnabled(true);
            jchkCrash.setEnabled(true);

            if (!jchkBalloon.isSelected())
                jchkBalloon.doClick();

            if (!jchkCrash.isSelected())
                jchkCrash.doClick();

            idealRadioButton.setEnabled(false);
            noisyRadioButton.setEnabled(false);
            fixedRadioButton.setEnabled(false);
            TrafficMap.showFakeLocIconLabel(true);
            TrafficMap.showRealLocIconLabel(true);
        });

        fixedRadioButton.addActionListener(e -> {
            if (selectedScenario == fixedRadioButton)
                return;
            selectedScenario = fixedRadioButton;
            Middleware.enableDetection(true);
            Middleware.enableResolution(true);
            jchkBalloon.setEnabled(true);
            jchkCrash.setEnabled(true);

            if (!jchkBalloon.isSelected())
                jchkBalloon.doClick();

            if (!jchkCrash.isSelected())
                jchkCrash.doClick();

            idealRadioButton.setEnabled(false);
            noisyRadioButton.setEnabled(false);
            fixedRadioButton.setEnabled(false);
            TrafficMap.showFakeLocIconLabel(false);
            TrafficMap.showRealLocIconLabel(false);
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
                enableScenarioSelection(false);
            }
        });
        stopCarButton.addActionListener(e -> {
            Car car = Dashboard.getSelectedCar();
            if(car != null){
                car.notifyPolice(Police.REQUEST2STOP, true);
                enableScenarioSelection(false);
            }
        });
        startAllCarsButton.addActionListener(e -> {
            if (!Resource.getConnectedCars().isEmpty()) {
                for (Car car : Resource.getConnectedCars())
                    car.notifyPolice(Police.REQUEST2ENTER, true);
                enableScenarioSelection(false);
            }
        });
        stopAllCarsButton.addActionListener(e -> {
            if (!Resource.getConnectedCars().isEmpty()) {
                for (Car car : Resource.getConnectedCars())
                    car.notifyPolice(Police.REQUEST2STOP, true);
                enableScenarioSelection(false);
            }
        });

        gbc.gridy += gbc.gridheight;
        JPanel deliveryPanel = new JPanel();
        rightPanel.add(deliveryPanel, gbc);
        deliveryPanel.setBorder(BorderFactory.createTitledBorder("Taxi service"));
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
            if (Delivery.autoGenTasks) {
                Delivery.autoGenTasks();
                enableScenarioSelection(false);
            }
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

            enableScenarioSelection(false);
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
//        ongoingDTPanel.setBackground(Resource.SNOW4);
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

        reset();

        getInstance().setTitle("Dashboard");
        getInstance().setContentPane(controlPanel);
        getInstance().pack();
        getInstance().setLocationRelativeTo(null);
    }

    private static final Class ComponentView$Invalidator = ComponentView.class.getDeclaredClasses()[0];
    private static final Map<Component, Boolean> compoEnable = new HashMap<>();
    private static void setEnabledRecurse(final Component root, final boolean enabled){
        if(root == null)
            return;
        Queue<Component> queue = new LinkedList<>();
        queue.add(root);
        while(!queue.isEmpty()){
            Component compo = queue.poll();
            if(compo instanceof Container && !(compo instanceof JComboBox)) {
                for (Component c : ((Container) compo).getComponents())
                    if (!c.getClass().equals(ComponentView$Invalidator))
                        queue.add(c);
            }
            if(enabled){
                Boolean b = compoEnable.get(compo);
                if(b != null)
                    compo.setEnabled(b);
            }
            else if (compo.isVisible()) {
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

    private static final JDialog relocationDialog = new JDialog(getInstance(), "Relocation");
    private static final JTextPane relocationTextPane = new JTextPane();
    private static final JButton relocationDoneButton = new JButton("Done");
    static {
        relocationTextPane.setBackground(null);
        relocationTextPane.setEditable(false);
        relocationTextPane.setFont(Resource.plain17dialog);
        relocationDoneButton.setFont(Resource.bold16dialog);
        relocationDoneButton.setMargin(new Insets(2, 5, 2, 5));
        relocationDoneButton.setVisible(false);
        relocationDoneButton.addActionListener(e -> {
            if (StateSwitcher.isRelocating()) {
                relocationDoneButton.setVisible(false);
                relocationDialog.pack();
                StateSwitcher.Relocation.manuallyRelocated();
            }
        });
        relocationDialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = gbc.weighty = 1;
        relocationDialog.add(relocationTextPane, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.weightx = gbc.weighty = 0;
        relocationDialog.add(relocationDoneButton, gbc);
        relocationDialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        relocationDialog.setResizable(false);
//        relocationDialog.setMinimumSize(new Dimension(200, 0));
//        relocationDialog.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
        relocationDialog.setLocationRelativeTo(null);
    }

    public static void showRelocationDialog(Car car) {
        List<Pair<String, Style>> strings = new ArrayList<>();
        strings.add(new Pair<>("Relocating ", null));
        if (car != null)
            strings.add(new Pair<>(car.name, Resource.getTextStyle(car.icon.color)));
        strings.add(new Pair<>("...", null));
        append2pane(strings, relocationTextPane);
        relocationDialog.pack();
        relocationDialog.setVisible(true);
    }

    public static void showRelocationDialog(Car car, boolean successful, Road road) {
        List<Pair<String, Style>> strings = new ArrayList<>(), strings2log = new ArrayList<>();
        if (successful) {
            strings.add(new Pair<>("Successful", Resource.getTextStyle(Color.GREEN)));

            strings2log.add(new Pair<>(car.name, Resource.getTextStyle(car.icon.color)));
            strings2log.add(new Pair<>(" is relocated successfully.\n", null));
        }
        else {
            strings.add(new Pair<>("Failed\n", Resource.getTextStyle(Color.RED)));
            strings.add(new Pair<>("Please put ", null));
            strings.add(new Pair<>(car.name, Resource.getTextStyle(car.icon.color)));
            strings.add(new Pair<>(" at ", null));
            strings.add(new Pair<>(road.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
            strings.add(new Pair<>(".\n", null));
            strings.add(new Pair<>("After", Resource.getTextStyle(true)));
            strings.add(new Pair<>(" that, click ", null));
            strings.add(new Pair<>("Done", Resource.getTextStyle(true)));
            strings.add(new Pair<>(" button.", null));
            relocationDoneButton.setVisible(true);

            strings2log.add(new Pair<>("Fail", Resource.getTextStyle(Color.RED)));
            strings2log.add(new Pair<>(" to relocate ", null));
            strings2log.add(new Pair<>(car.name, Resource.getTextStyle(car.icon.color)));
            strings2log.add(new Pair<>(".\n", null));
        }
        append2pane(strings, relocationTextPane);
        relocationDialog.pack();
        relocationDialog.setVisible(true);
        log(strings2log);
    }

    public static void clearRelocationDialog() {
        synchronized (relocationTextPane) {
            relocationTextPane.setText("");
        }
    }

    public static void closeRelocationDialog(){
        relocationDialog.setVisible(false);
        synchronized (relocationTextPane) {
            relocationTextPane.setText("");
        }
    }

    public static void showInitDialog() {
        if (Resource.getConnectedCars().isEmpty())
            return;
        JDialog dialog = new JDialog(getInstance(), "Initialization");
        JButton button = new JButton("Reset");
        button.setFont(Resource.bold16dialog);
        button.setMargin(new Insets(2, 5, 2, 5));
        button.addActionListener(e -> {
            dialog.dispose();
            resetButton.doClick();
        });
        JTextPane pane = new JTextPane();
        pane.setBackground(null);
        pane.setEditable(false);
        pane.setFont(Resource.plain17dialog);
        List<Car> cars = new ArrayList<>(Resource.getConnectedCars());
        List<Pair<String, Style>> strings = new ArrayList<>();
        strings.add(new Pair<>("Please put ", null));
        strings.add(new Pair<>(cars.get(0).name, Resource.getTextStyle(cars.get(0).icon.color)));
        strings.add(new Pair<>(" at ", null));
        strings.add(new Pair<>(cars.get(0).loc.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
        for (int i = 1;i < cars.size();i++) {
            strings.add(new Pair<>(", ", null));
            strings.add(new Pair<>(cars.get(i).name, Resource.getTextStyle(cars.get(i).icon.color)));
            strings.add(new Pair<>(" at ", null));
            strings.add(new Pair<>(cars.get(i).loc.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
        }
        strings.add(new Pair<>(".\n", null));
        strings.add(new Pair<>("After", Resource.getTextStyle(true)));
        strings.add(new Pair<>(" that, click ", null));
        strings.add(new Pair<>("Reset", Resource.getTextStyle(true)));
        strings.add(new Pair<>(" button.", null));
        append2pane(strings, pane);

        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = gbc.weighty = 1;
        dialog.add(pane, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.weightx = gbc.weighty = 0;
        dialog.add(button, gbc);
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setVisible(true);
    }


    public static void showCrashDialog(List<Car> cars) {
        if (cars == null || cars.isEmpty())
            return;

        List<Pair<String, Style>> strings = new ArrayList<>();
        strings.add(new Pair<>("Please select ", null));
        strings.add(new Pair<>(cars.get(0).name, Resource.getTextStyle(cars.get(0).icon.color)));
        for (int i = 1;i < cars.size();i++) {
            strings.add(new Pair<>(", or ", null));
            strings.add(new Pair<>(cars.get(i).name, Resource.getTextStyle(cars.get(i).icon.color)));
        }
        strings.add(new Pair<>(", and click ", null));
        strings.add(new Pair<>("Start", Resource.getTextStyle(true)));
        strings.add(new Pair<>(" button to recover from the crash.\n", null));

//        JTextPane pane = new JTextPane();
//        pane.setBackground(null);
//        pane.setEditable(false);
//        pane.setFont(Resource.plain17dialog);
//        Dashboard.append2pane(strings, pane);
//        JOptionPane.showMessageDialog(Dashboard.getInstance(), pane, "Recover from the crash", JOptionPane.PLAIN_MESSAGE, null);
        Dashboard.log(strings);
    }

    public static Road getNearestRoad(int x, int y){
        if(x < 0 || x >= trafficMap.getWidth() || y < 0 || y >= trafficMap.getHeight())
            return null;
        int min = Integer.MAX_VALUE, tmp;
        Road road = null;
//		System.out.println(x+", "+y);
        for(Road r : TrafficMap.roads.values()){
//			System.out.println(s.name+"\t"+s.iconPanel.coord.centerX+", "+s.iconPanel.coord.centerY);
            tmp = (int) (Math.pow(x-r.iconPanel.coord.centerX, 2) + Math.pow(y-r.iconPanel.coord.centerY, 2));
            if(tmp < min){
                road = r;
                min = tmp;
            }
        }
        return road;
    }

    private static void updateRoadInfoPane(Location loc){
        trafficMap.updateRoadInfoPane(loc);
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
            List<Pair<String, Style>> strings = new ArrayList<>();
            for (Delivery.DeliveryTask dt : queue) {
                if(firstLine)
                    firstLine = false;
                else
                    strings.add(new Pair<>("\n", null));
                strings.add(new Pair<>(dt.citizen.name, Resource.getTextStyle(dt.citizen.icon.color)));
                switch (dt.phase) {
                    case Delivery.DeliveryTask.SEARCH_CAR:
                        strings.add(new Pair<>(" at ", null));
                        strings.add(new Pair<>(dt.src.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
                        strings.add(new Pair<>(" needs a taxi to ", null));
                        strings.add(new Pair<>(dt.dest.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
                        break;
                    case Delivery.DeliveryTask.HEAD4SRC:
                        strings.add(new Pair<>(" at ", null));
                        strings.add(new Pair<>(dt.src.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
                        strings.add(new Pair<>(" waits for ", null));
                        strings.add(new Pair<>(dt.car.name, Resource.getTextStyle(dt.car.icon.color)));
                        break;
                    case Delivery.DeliveryTask.HEAD4DEST:
                        strings.add(new Pair<>(" gets on ", null));
                        strings.add(new Pair<>(dt.car.name, Resource.getTextStyle(dt.car.icon.color)));
                        strings.add(new Pair<>(" at ", null));
                        strings.add(new Pair<>(dt.car.loc.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
                        strings.add(new Pair<>(" and heads for ", null));
                        strings.add(new Pair<>(dt.dest.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
                        break;
                    case Delivery.DeliveryTask.COMPLETED:
                        strings.add(new Pair<>(" gets off ", null));
                        strings.add(new Pair<>(dt.car.name, Resource.getTextStyle(dt.car.icon.color)));
                        strings.add(new Pair<>(" at ", null));
                        strings.add(new Pair<>(dt.car.loc.name, Resource.getTextStyle(Resource.DEEP_SKY_BLUE)));
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

    public static void append2pane(List<Pair<String, Style>> strings, JTextPane pane) {
        if(strings == null || strings.isEmpty())
            return;
        synchronized (pane) {
            for (Pair<String, Style> string : strings)
                append2pane(string.first, string.second, pane);
        }
    }

    public static void append2pane(String str, Style style, JTextPane pane) {
        if (style == null)
            style = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        synchronized (pane) {
            StyledDocument doc = pane.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), str, style);
//                pane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    public static void log(List<Pair<String, Style>> strings) {
        append2pane(strings, logPane);
    }

    public static void log(String str, Style style) {
        append2pane(str, style, logPane);
    }

    public static synchronized void addCar(Car car){
        for(int i = 0;i < carbox.getItemCount();i++)
            if(carbox.getItemAt(i).equals(car.name))
                return;
        carbox.addItem(car.name);
        VCPanel.addCar(car);
        PkgHandler.send(new AppPkg().setCar(car.name, TrafficMap.Direction.UNKNOWN, null));
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

    public static void showCrashEffect(Road road) {
        trafficMap.showCrashEffect(road);
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
        jchkAutoGen.setSelected(false);
        enableStartCarButton(getSelectedCar() != null);
        enableStopCarButton(false);
        enableStartAllCarsButton(!Resource.getConnectedCars().isEmpty());
        enableStopAllCarsButton(false);

        enableScenarioSelection(true);
        idealRadioButton.doClick();
        enableScenarioSelection(true);
//        jchkDetection.setEnabled(true);
//        jchkDetection.setSelected(false);
//        jchkResolution.setEnabled(true);
//        jchkResolution.setSelected(false);
//        jchkBalloon.setEnabled(true);
//        jchkBalloon.setSelected(false);
//        showError = false;
//        jchkBalloon.setEnabled(false);
//        jchkCrash.setEnabled(true);
//        jchkCrash.setSelected(false);
//        playCrashSound = false;
//        jchkCrash.setEnabled(false);
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

    public static boolean isStartAllCarsButtonEnabled() {
        return startAllCarsButton.isEnabled();
    }

    public static void enableStopAllCarsButton(boolean b) {
        stopAllCarsButton.setEnabled(b);
    }

    public static boolean isStopAllCarsButtonEnabled() {
        return stopAllCarsButton.isEnabled();
    }

    private static void enableScenarioSelection(boolean b) {
        idealRadioButton.setEnabled(b);
        noisyRadioButton.setEnabled(b);
        fixedRadioButton.setEnabled(b);
    }

    private static class RoadIconListener extends MouseAdapter {
        Road.RoadIcon icon = null;

        RoadIconListener(Road.RoadIcon icon) {
            this.icon = icon;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (icon.road == null || !icon.isEnabled())
                return;
            icon.isPressed = true;
            icon.repaint();
//			System.out.println(road.name);
            // for delivery tasks
            if (delivSelModeOn) {
                if (src == null) {
                    src = icon.road;
                    updateDeliverySrcPanel(src.name);
                    updateDeliveryDestPanel("Click any loc");
                }
                else if (dest == null) {
                    if (src instanceof Road && icon.road == src)
                        return;
                    dest = icon.road;
                    updateDeliveryDestPanel(dest.name);
                    deliverButton.setEnabled(true);
                }
            }
            else {
                if(TrafficMap.roadPaneScroll.isVisible()) {
                    TrafficMap.roadPaneScroll.setVisible(false);
                    return;
                }

                if (icon instanceof Road.Crossroad.CrossroadIcon) {
                    TrafficMap.roadPaneScroll.setLocation(icon.road.iconPanel.getX()+icon.getX()+icon.getWidth(),
                            icon.road.iconPanel.getY()+icon.getY()+(icon.getHeight()-TrafficMap.roadPaneScroll.getHeight())/2);
                    updateRoadInfoPane(icon.road);
                    TrafficMap.roadPaneScroll.setVisible(true);
                }
                else if (icon instanceof Road.Street.StreetIcon) {
                    if (((Road.Street.StreetIcon) icon).isVertical) {
                        int rightmost = icon.road.iconPanel.getX() + icon.getX() + icon.getWidth() + TrafficMap.roadPaneScroll.getWidth();
                        if (rightmost < trafficMap.getWidth())
                            TrafficMap.roadPaneScroll.setLocation(icon.road.iconPanel.getX()+icon.getX()+icon.getWidth(),
                                    icon.road.iconPanel.getY()+icon.getY()+(icon.getHeight()-TrafficMap.roadPaneScroll.getHeight())/2);
                        else
                            TrafficMap.roadPaneScroll.setLocation(icon.road.iconPanel.getX()+icon.getX()-TrafficMap.roadPaneScroll.getWidth(),
                                    icon.road.iconPanel.getY()+icon.getY()+(icon.getHeight()-TrafficMap.roadPaneScroll.getHeight())/2);
                    }
                    else {
                        int topmost = icon.road.iconPanel.getY() + icon.getY() - TrafficMap.roadPaneScroll.getHeight();
                        if (topmost >= 0)
                            TrafficMap.roadPaneScroll.setLocation(icon.road.iconPanel.getX()+icon.getX()+(icon.getWidth()-TrafficMap.roadPaneScroll.getWidth())/2, topmost);
                        else
                            TrafficMap.roadPaneScroll.setLocation(icon.road.iconPanel.getX()+icon.getX()+(icon.getWidth()-TrafficMap.roadPaneScroll.getWidth())/2,
                                    icon.road.iconPanel.getY()+icon.getY()+icon.getHeight());
                    }
                    updateRoadInfoPane(icon.road);
                    TrafficMap.roadPaneScroll.setVisible(true);
                }
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
//            super.mouseExited(e);
            if (icon.road == null || !icon.isEnabled())
                return;
            icon.isEntered = false;
            icon.repaint();
            TrafficMap.roadPaneScroll.setVisible(false);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
//            super.mouseEntered(e);
            if (icon.road == null || !icon.isEnabled())
                return;
            icon.isEntered = true;
            icon.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
//            super.mouseReleased(e);
            if (icon.road == null || !icon.isEnabled())
                return;
            icon.isPressed = false;
            icon.repaint();
        }
    }

    private static class BuildingIconListener extends MouseAdapter{
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
