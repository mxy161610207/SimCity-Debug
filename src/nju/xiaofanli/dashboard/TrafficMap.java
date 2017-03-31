package nju.xiaofanli.dashboard;

import nju.xiaofanli.Resource;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.sensor.Sensor;
import nju.xiaofanli.util.StyledText;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TrafficMap extends JPanel{
    private static final long serialVersionUID = 1L;
    private static TrafficMap instance = null;
    public static final Set<Car> allCars = new LinkedHashSet<>();
    public static final Set<Car> cars = new HashSet<>(); //select some cars in allCars to connect
    public static final List<Car> carList = new ArrayList<>();
    public static final Map<String, Car> connectedCars = new HashMap<>();
    public static final Road.Crossroad[] crossroads = new Road.Crossroad[7];
    public static final Road.Street[] streets = new Road.Street[18];
    public static final Map<String, Road> roads = new HashMap<>();
    public static final Map<String, Location> locations = new HashMap<>();
    public static final List<Location> locationList = new ArrayList<>();
    public static final Sensor[][] sensors = new Sensor[10][];
    public static final List<Citizen> citizens = new ArrayList<>();
    private static final List<Citizen> freeCitizens = new ArrayList<>();
    public static final ConcurrentMap<Building.Type, Building> buildings = new ConcurrentHashMap<>();
    private static final JTextPane roadPane = new JTextPane();
    static final JScrollPane roadPaneScroll = new JScrollPane(roadPane);
    private static final IdentifierPanel crossroadIconPanel = new IdentifierPanel(Resource.CROSSROAD_ICON, TrafficMap.SH/2, TrafficMap.SH/2, "Crossroad", Resource.en16bold),
            streetIconPanel = new IdentifierPanel(Resource.STREET_ICON, TrafficMap.SH, TrafficMap.SH/2,  "Street", Resource.en16bold),
            carIconPanel = new IdentifierPanel(Resource.NORMAL_CAR_ICON, TrafficMap.SH/2, TrafficMap.SH/2,  "Car", Resource.en16bold),
            fakeCarIconPanel = new IdentifierPanel(Resource.FAKE_CAR_ICON, TrafficMap.SH/2*15/17, TrafficMap.SH/2*15/17,  "Fake location", Resource.en15bold),
            realCarIconPanel = new IdentifierPanel(Resource.REAL_CAR_ICON, TrafficMap.SH/2*15/17, TrafficMap.SH/2*15/17,  "Real location", Resource.en15bold);
    private static final List<JPanel> iconPanels = Arrays.asList(crossroadIconPanel, streetIconPanel, carIconPanel, fakeCarIconPanel, realCarIconPanel);
    private static final Map<CrashLettersPanel, Integer> crashLettersPanels = new HashMap<>();
    private static final Map<Object, Object> roadAndCrashLettersPanel = new HashMap<>();
    public static final JLabel mLabel = new JLabel("M");
    public static final Set<JLabel> upArrows = new HashSet<>(), downArrows = new HashSet<>();
    public static boolean crashOccurred = false, allCarsStopped = true;

    public static final int SH = 48;//street height
    public static final int SW = SH * 2;//street width
    public static final int CW = (int) (SH * 1.5);//crossroad width
    private static final int AW = CW / 2;
    private static final int U1 = CW + SW;
    private static final int U2 = (CW+SH)/2;
    public static final int U3 = SW+(CW+SH)/2;
    private static final int U4 = U1+SH;
    private static final int U5 = U3 + CW;
    public static final int SIZE = U3 + 2*U1 + U5;

    public enum Direction {
        NORTH, SOUTH, WEST, EAST, UNKNOWN
    }

    static{
        for(int i = 0; i < crossroads.length; i++) {
            crossroads[i] = new Road.Crossroad();
        }
        for(int i = 0;i < streets.length;i++) {
            streets[i] = new Road.Street();
        }

        sensors[0] = new Sensor[2];
        sensors[1] = new Sensor[3];
        sensors[2] = new Sensor[4];
        sensors[3] = new Sensor[4];
        sensors[4] = new Sensor[3];
        sensors[5] = new Sensor[3];
        sensors[6] = new Sensor[4];
        sensors[7] = new Sensor[4];
        sensors[8] = new Sensor[3];
        sensors[9] = new Sensor[2];

        //      roadPane.setLineWrap(true);
//		roadPane.setWrapStyleWord(true);
        roadPaneScroll.setVisible(false);
        roadPaneScroll.setSize(U3, U3);
        roadPane.setEditable(false);
        roadPane.setBackground(Color.WHITE);
        roadPane.setFont(Resource.en17plain);
        roadPaneScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Location info",
                TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
        ((TitledBorder) roadPaneScroll.getBorder()).setTitleFont(Resource.en16bold);
        roadPaneScroll.setBackground(Color.LIGHT_GRAY);
        roadPane.setBackground(Resource.SNOW4);

        fakeCarIconPanel.setVisible(false);
        fakeCarIconPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        realCarIconPanel.setVisible(false);
        realCarIconPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        for (int i = 0;i < 5;i++) {
            CrashLettersPanel panel = new CrashLettersPanel(new JLabel(Resource.CRASH_LETTERS));
            crashLettersPanels.put(panel, 0);
            panel.setVisible(false);

            JLabel icon = new JLabel(Resource.loadImage(Resource.UP_ARROW_ICON, SH/2, SH/2));
            icon.setSize(icon.getPreferredSize());
            icon.setVisible(false);
            upArrows.add(icon);
            icon = new JLabel(Resource.loadImage(Resource.DOWN_ARROW_ICON, SH/2, SH/2));
            icon.setSize(icon.getPreferredSize());
            icon.setVisible(false);
            downArrows.add(icon);
        }

        mLabel.setFont(Resource.en20bold);
        mLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(-4, 0, -3, 0)));
//        mLabel.setBorder(BorderFactory.createEmptyBorder(-6, -1, -5, -1));
//        mLabel.setForeground(Color.WHITE);
        mLabel.setBackground(Color.WHITE);
        mLabel.setOpaque(true);
        mLabel.setSize(mLabel.getPreferredSize());
        mLabel.setVisible(false);
    }

    private TrafficMap() {
        setLayout(null);
        setSize(SIZE, SIZE);
        setPreferredSize(getSize());
        initRoads();
        initSensors();
        initBuildings();

        roads.values().forEach(road -> {
            road.isStraight.put(road.dir[0], true);
            if (road.dir[1] != Direction.UNKNOWN)
                road.isStraight.put(road.dir[1], true);
        });
        streets[0].isStraight.put(streets[0].dir[0], false);
        streets[1].isStraight.put(streets[1].dir[0], false);
        streets[10].isStraight.put(streets[10].dir[0], false);
        streets[14].isStraight.put(streets[14].dir[0], false);
        crossroads[0].isStraight.put(Direction.WEST, false); //horizontal
        crossroads[0].isStraight.put(Direction.EAST, false); //horizontal
        crossroads[2].isStraight.put(Direction.WEST, false); //horizontal
        crossroads[2].isStraight.put(Direction.EAST, false); //horizontal
        crossroads[5].isStraight.put(Direction.NORTH, false); //vertical
        crossroads[5].isStraight.put(Direction.SOUTH, false); //vertical
        crossroads[6].isStraight.put(Direction.NORTH, false); //vertical
        crossroads[6].isStraight.put(Direction.SOUTH, false); //vertical

        for (Sensor[] array : sensors) {
            for (Sensor sensor : array) {
                add(sensor.icon);
                if (Resource.timeouts.containsKey(sensor.name)) {
                    Resource.timeouts.get(sensor.name).forEach((url, time) -> {
                        Direction direction = sensor.getNextRoadDir();
                        if (!sensor.nextRoad.timeouts.containsKey(direction))
                            sensor.nextRoad.timeouts.put(direction, new HashMap<>());
                        sensor.nextRoad.timeouts.get(direction).put(url, time);
                    });
                }
            }
        }

        add(roadPaneScroll);
        crashLettersPanels.keySet().forEach(this::add);
        for (Sensor[] array : sensors)
            for (Sensor sensor : array)
                add(sensor.balloon);
        add(mLabel);
        upArrows.forEach(this::add);
        downArrows.forEach(this::add);
        citizens.forEach(citizen -> add(citizen.icon));
        roads.values().forEach(road -> locations.put(road.name, road));
        buildings.values().forEach(building -> {
            add(building.icon);
            locations.put(building.name, building);
        });
        locationList.addAll(locations.values());
        roads.values().forEach(road -> {
//            if (road.id != 2 && road.id != 3)
                add(road.iconPanel);
        });

        JPanel iconPanel = new JPanel(new GridLayout(5, 1));
        iconPanel.setBounds(5, 0, U3, U3);
        add(iconPanel);
        iconPanels.forEach(iconPanel::add);
    }

    public static void reset() {
        connectedCars.values().forEach(Car::reset);
        roads.values().forEach(Road::reset);
        for (Sensor[] array : sensors)
            for (Sensor sensor : array)
                sensor.reset();
        citizens.forEach(Citizen::reset);
        freeCitizens.clear();
        freeCitizens.addAll(citizens);
        roadPane.setText("");
        roadPaneScroll.setVisible(false);
        fakeCarIconPanel.setVisible(false);
        realCarIconPanel.setVisible(false);
        crashOccurred = false;
        allCarsStopped = true;
        for (Map.Entry<CrashLettersPanel, Integer> entry : crashLettersPanels.entrySet()) {
            entry.setValue(0);
            entry.getKey().setVisible(false);
        }
        roadAndCrashLettersPanel.clear();
        upArrows.forEach(jLabel -> jLabel.setVisible(false));
        downArrows.forEach(jLabel -> jLabel.setVisible(false));
    }

    private static Random random = new Random();
    public static Location getALocation(){
        return locationList.get(random.nextInt(locationList.size()));
    }

    public static Location getALocationExcept(Location location){
        if(location instanceof Building)
            return getALocation();

        Location res = getALocation();
        while(res instanceof Road && location == res)
            res = getALocation();
        return res;
    }

    public static Citizen removeAFreeCitizen(){
        synchronized (freeCitizens){
            if(freeCitizens.isEmpty()){
                System.out.println("Run out of free citizens!");
                return null;
            }
            return freeCitizens.remove(random.nextInt(freeCitizens.size()));
        }
    }

    public static void addAFreeCitizen(Citizen citizen) {
        synchronized (freeCitizens) {
            if (!freeCitizens.contains(citizen))
                freeCitizens.add(citizen);
        }
    }

    public static Citizen getACitizen(){
        return citizens.get(random.nextInt(citizens.size()));
    }

    public static Car getACar(){
        return carList.get(random.nextInt(carList.size()));
    }

    private static void initBuildings(){
        for(Building building : buildings.values()){
            if(building.block < 0 || building.block > 15)
                return;
            building.icon.coord.x = U2 + (building.block%4)*U1;
            building.icon.coord.y = U2 + (building.block/4)*U1;
            building.icon.coord.w = building.icon.coord.h = SW;
            building.icon.coord.centerX = building.icon.coord.x + building.icon.coord.w/2;
            building.icon.coord.centerY = building.icon.coord.y + building.icon.coord.h/2;
            building.icon.setBounds(building.icon.coord.x, building.icon.coord.y, building.icon.coord.w, building.icon.coord.h);
            building.icon.setIcon();

            switch (building.block) {
                case 0:
                    building.addrs.add(crossroads[0]);
                    break;
                case 1:
                    building.addrs.add(streets[0]);
                    building.addrs.add(streets[3]);
                    building.addrs.add(crossroads[0]);
                    building.addrs.add(crossroads[1]);
                    break;
                case 2:
                    building.addrs.add(streets[4]);
                    building.addrs.add(crossroads[1]);
                    break;
                case 3:
                    building.addrs.add(streets[1]);
                    break;
                case 4:
                    building.addrs.add(streets[2]);
                    building.addrs.add(streets[5]);
                    building.addrs.add(crossroads[0]);
                    building.addrs.add(crossroads[2]);
                    break;
                case 5:
                    building.addrs.add(streets[3]);
                    building.addrs.add(streets[5]);
                    building.addrs.add(streets[6]);
                    building.addrs.add(streets[8]);
                    building.addrs.add(crossroads[0]);
                    building.addrs.add(crossroads[1]);
                    building.addrs.add(crossroads[2]);
                    building.addrs.add(crossroads[3]);
                    break;
                case 6:
                    building.addrs.add(streets[4]);
                    building.addrs.add(streets[6]);
                    building.addrs.add(streets[7]);
                    building.addrs.add(streets[9]);
                    building.addrs.add(crossroads[1]);
                    building.addrs.add(crossroads[3]);
                    building.addrs.add(crossroads[4]);
                    break;
                case 7:
                    building.addrs.add(streets[7]);
                    building.addrs.add(crossroads[4]);
                    break;
                case 8:
                    building.addrs.add(streets[11]);
                    building.addrs.add(crossroads[2]);
                    break;
                case 9:
                    building.addrs.add(streets[8]);
                    building.addrs.add(streets[11]);
                    building.addrs.add(streets[12]);
                    building.addrs.add(streets[15]);
                    building.addrs.add(crossroads[2]);
                    building.addrs.add(crossroads[3]);
                    building.addrs.add(crossroads[5]);
                    break;
                case 10:
                    building.addrs.add(streets[9]);
                    building.addrs.add(streets[12]);
                    building.addrs.add(streets[13]);
                    building.addrs.add(streets[16]);
                    building.addrs.add(crossroads[3]);
                    building.addrs.add(crossroads[4]);
                    building.addrs.add(crossroads[5]);
                    building.addrs.add(crossroads[6]);
                    break;
                case 11:
                    building.addrs.add(streets[10]);
                    building.addrs.add(streets[13]);
                    building.addrs.add(crossroads[4]);
                    building.addrs.add(crossroads[6]);
                    break;
                case 12:
                    building.addrs.add(streets[14]);
                    break;
                case 13:
                    building.addrs.add(streets[15]);
                    building.addrs.add(crossroads[5]);
                    break;
                case 14:
                    building.addrs.add(streets[16]);
                    building.addrs.add(streets[17]);
                    building.addrs.add(crossroads[5]);
                    building.addrs.add(crossroads[6]);
                    break;
                case 15:
                    building.addrs.add(crossroads[6]);
                    break;
            }
        }
    }

    private static void initRoads() {
        for(int i = 0; i < crossroads.length; i++){
            crossroads[i].id = i;
            crossroads[i].name = "Crossroad " + i;
            roads.put(crossroads[i].name, crossroads[i]);
            switch (i) {
                case 0:
                    crossroads[i].iconPanel.coord.x = U2 + SW;
                    crossroads[i].iconPanel.coord.y = U2 + SW;
                    break;
                case 1:
                    crossroads[i].iconPanel.coord.x = U2 + SW + U1;
                    crossroads[i].iconPanel.coord.y = U2 + SW;
                    break;
                case 2:
                    crossroads[i].iconPanel.coord.x = U2 + SW;
                    crossroads[i].iconPanel.coord.y = U2 + SW + U1;
                    break;
                case 3:
                    crossroads[i].iconPanel.coord.x = U2 + SW + U1;
                    crossroads[i].iconPanel.coord.y = U2 + SW + U1;
                    break;
                case 4:
                    crossroads[i].iconPanel.coord.x = U2 + SW + 2*U1;
                    crossroads[i].iconPanel.coord.y = U2 + SW + U1;
                    break;
                case 5:
                    crossroads[i].iconPanel.coord.x = U2 + SW + U1;
                    crossroads[i].iconPanel.coord.y = U2 + SW + 2*U1;
                    break;
                case 6:
                    crossroads[i].iconPanel.coord.x = U2 + SW + 2*U1;
                    crossroads[i].iconPanel.coord.y = U2 + SW + 2*U1;
                    break;
            }

            crossroads[i].iconPanel.coord.w = crossroads[i].iconPanel.coord.h = CW;
            crossroads[i].iconPanel.addCrossroadIcon(crossroads[i].iconPanel.coord);
            crossroads[i].iconPanel.coord.centerX = crossroads[i].iconPanel.coord.x + crossroads[i].iconPanel.coord.w/2;
            crossroads[i].iconPanel.coord.centerY = crossroads[i].iconPanel.coord.y + crossroads[i].iconPanel.coord.h/2;
            crossroads[i].iconPanel.setBounds(crossroads[i].iconPanel.coord.x, crossroads[i].iconPanel.coord.y, crossroads[i].iconPanel.coord.w, crossroads[i].iconPanel.coord.h);
        }

        for(int i = 0;i < streets.length;i++){
            streets[i].id = i;
            streets[i].name = "Street " + i;
            roads.put(streets[i].name, streets[i]);
            streets[i].iconPanel.coord.arcw = AW;
            streets[i].iconPanel.coord.arch = AW;
            switch (i) {
                case 0:
                    streets[i].iconPanel.coord.x = U1;
                    streets[i].iconPanel.coord.y = 0;
                    streets[i].iconPanel.coord.w = U4;
                    streets[i].iconPanel.coord.h = U3;
                    streets[i].iconPanel.addStreetIcon(0, 0, U4, SH, AW, AW, false);
                    streets[i].iconPanel.addStreetIcon(0, 0, SH, U3, AW, AW, true);
                    streets[i].iconPanel.addStreetIcon(U1, 0, SH, U3, AW, AW, true);
                    break;
                case 1:
                    streets[i].iconPanel.coord.x = U3 + 2*U1;
                    streets[i].iconPanel.coord.y = 0;
                    streets[i].iconPanel.coord.w = U5;
                    streets[i].iconPanel.coord.h = U5;
                    streets[i].iconPanel.addStreetIcon(U5-U4, 0, U4, SH, AW, AW, false);
                    streets[i].iconPanel.addStreetIcon(U5-U4, 0, SH, U5, AW, AW, true);
                    streets[i].iconPanel.addStreetIcon(0, U1, U5, SH, AW, AW, false);
                    streets[i].iconPanel.addStreetIcon(U5-SH, 0, SH, U4, AW, AW, true);
                    break;
                case 2:
                    streets[i].iconPanel.coord.x = 0;
                    streets[i].iconPanel.coord.y = U1;
                    streets[i].iconPanel.coord.w = U3;
                    streets[i].iconPanel.coord.h = U4;
                    streets[i].iconPanel.addStreetIcon(0, 0, U3, SH, AW, AW, false);
                    streets[i].iconPanel.addStreetIcon(0, 0, SH, U4, AW, AW, true);
                    streets[i].iconPanel.addStreetIcon(0, U1, U3, SH, AW, AW, false);
                    break;
                case 3:
                    streets[i].iconPanel.coord.x = U1+U2;
                    streets[i].iconPanel.coord.y = U1;
                    streets[i].iconPanel.coord.w = SW;
                    streets[i].iconPanel.coord.h = SH;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, false);
                    break;
                case 4:
                    streets[i].iconPanel.coord.x = 2*U1+U2;
                    streets[i].iconPanel.coord.y = U1;
                    streets[i].iconPanel.coord.w = SW;
                    streets[i].iconPanel.coord.h = SH;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, false);
                    break;
                case 5:
                    streets[i].iconPanel.coord.x = U1;
                    streets[i].iconPanel.coord.y = U1 + U2;
                    streets[i].iconPanel.coord.w = SH;
                    streets[i].iconPanel.coord.h = SW;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, true);
                    break;
                case 6:
                    streets[i].iconPanel.coord.x = 2 * U1;
                    streets[i].iconPanel.coord.y = U1 + U2;
                    streets[i].iconPanel.coord.w = SH;
                    streets[i].iconPanel.coord.h = SW;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, true);
                    break;
                case 7:
                    streets[i].iconPanel.coord.x = 3 * U1;
                    streets[i].iconPanel.coord.y = U1 + U2;
                    streets[i].iconPanel.coord.w = SH;
                    streets[i].iconPanel.coord.h = SW;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, true);
                    break;
                case 8:
                    streets[i].iconPanel.coord.x = U1 + U2;
                    streets[i].iconPanel.coord.y = 2 * U1;
                    streets[i].iconPanel.coord.w = SW;
                    streets[i].iconPanel.coord.h = SH;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, false);
                    break;
                case 9:
                    streets[i].iconPanel.coord.x = 2*U1 + U2;
                    streets[i].iconPanel.coord.y = 2 * U1;
                    streets[i].iconPanel.coord.w = SW;
                    streets[i].iconPanel.coord.h = SH;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, false);
                    break;
                case 10:
                    streets[i].iconPanel.coord.x = 3*U1 + U2;
                    streets[i].iconPanel.coord.y = 2 * U1;
                    streets[i].iconPanel.coord.w = U3;
                    streets[i].iconPanel.coord.h = U4;
                    streets[i].iconPanel.addStreetIcon(0, 0, U3, SH, AW, AW, false);
                    streets[i].iconPanel.addStreetIcon(0, U1, U3, SH, AW, AW, false);
                    streets[i].iconPanel.addStreetIcon(U3-SH, 0, SH, U4, AW, AW, true);
                    break;
                case 11:
                    streets[i].iconPanel.coord.x = U1;
                    streets[i].iconPanel.coord.y = 2*U1+(SH+CW)/2;
                    streets[i].iconPanel.coord.w = SH;
                    streets[i].iconPanel.coord.h = SW;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, true);
                    break;
                case 12:
                    streets[i].iconPanel.coord.x = 2 * U1;
                    streets[i].iconPanel.coord.y = 2*U1+(SH+CW)/2;
                    streets[i].iconPanel.coord.w = SH;
                    streets[i].iconPanel.coord.h = SW;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, true);
                    break;
                case 13:
                    streets[i].iconPanel.coord.x = 3 * U1;
                    streets[i].iconPanel.coord.y = 2*U1+(SH+CW)/2;
                    streets[i].iconPanel.coord.w = SH;
                    streets[i].iconPanel.coord.h = SW;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, true);
                    break;
                case 14:
                    streets[i].iconPanel.coord.x = 0;
                    streets[i].iconPanel.coord.y = U3 + 2*U1;
                    streets[i].iconPanel.coord.w = U5;
                    streets[i].iconPanel.coord.h = U5;
                    streets[i].iconPanel.addStreetIcon(0, U5-U4, U5, SH, AW, AW, false);
                    streets[i].iconPanel.addStreetIcon(0, U5-U4, SH, U4, AW, AW, true);
                    streets[i].iconPanel.addStreetIcon(0, U5-SH, U4, SH, AW, AW, false);
                    streets[i].iconPanel.addStreetIcon(U1, 0, SH, U5, AW, AW, true);
                    break;
                case 15:
                    streets[i].iconPanel.coord.x = U1+(SH+CW)/2;
                    streets[i].iconPanel.coord.y = 3 * U1;
                    streets[i].iconPanel.coord.w = SW;
                    streets[i].iconPanel.coord.h = SH;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, false);
                    break;
                case 16:
                    streets[i].iconPanel.coord.x = 2*U1+(SH+CW)/2;
                    streets[i].iconPanel.coord.y = 3 * U1;
                    streets[i].iconPanel.coord.w = SW;
                    streets[i].iconPanel.coord.h = SH;
                    streets[i].iconPanel.addStreetIcon(streets[i].iconPanel.coord, false);
                    break;
                case 17:
                    streets[i].iconPanel.coord.x = 2 * U1;
                    streets[i].iconPanel.coord.y = 3*U1+U2;
                    streets[i].iconPanel.coord.w = U4;
                    streets[i].iconPanel.coord.h = U3;
                    streets[i].iconPanel.addStreetIcon(0, 0, SH, U3, AW, AW, true);
                    streets[i].iconPanel.addStreetIcon(0, U3-SH, U4, SH, AW, AW, false);
                    streets[i].iconPanel.addStreetIcon(U1, 0, SH, U3, AW, AW, true);
                    break;
            }

            streets[i].iconPanel.coord.centerX = streets[i].iconPanel.coord.x + streets[i].iconPanel.coord.w/2;
            streets[i].iconPanel.coord.centerY = streets[i].iconPanel.coord.y + streets[i].iconPanel.coord.h/2;
            streets[i].iconPanel.setBounds(streets[i].iconPanel.coord.x, streets[i].iconPanel.coord.y, streets[i].iconPanel.coord.w, streets[i].iconPanel.coord.h);
        }

        setAdjRoads();
    }

    private static void initSensors(){
        for(int i = 0;i < sensors.length;i++)
            for(int j = 0;j < sensors[i].length;j++)
                sensors[i][j] = new Sensor(i, j);

        setSensor(sensors[0][0], crossroads[0], streets[0], Direction.NORTH);
        setSensor(sensors[0][1], crossroads[0], streets[2], Direction.WEST);
        setSensor(sensors[1][0], streets[1], streets[4], Direction.WEST);
        setSensor(sensors[1][1], streets[0], crossroads[1], Direction.SOUTH);
        setSensor(sensors[1][2], streets[4], crossroads[1], Direction.WEST);
        setSensor(sensors[2][0], crossroads[1], streets[3], Direction.WEST);
        setSensor(sensors[2][1], crossroads[2], streets[5], Direction.NORTH);
        setSensor(sensors[2][2], streets[5], crossroads[0], Direction.NORTH);
        setSensor(sensors[2][3], streets[3], crossroads[0], Direction.WEST);
        setSensor(sensors[3][0], streets[9], crossroads[4], Direction.EAST);
        setSensor(sensors[3][1], crossroads[1], streets[6], Direction.SOUTH);
        setSensor(sensors[3][2], streets[6], crossroads[3], Direction.SOUTH);
        setSensor(sensors[3][3], crossroads[3], streets[9], Direction.EAST);
        setSensor(sensors[4][0], crossroads[4], streets[7], Direction.NORTH);
        setSensor(sensors[4][1], crossroads[4], streets[10], Direction.EAST);
        setSensor(sensors[4][2], streets[7], streets[1], Direction.NORTH);
        setSensor(sensors[5][0], streets[11], crossroads[2], Direction.NORTH);
        setSensor(sensors[5][1], streets[2], crossroads[2], Direction.EAST);
        setSensor(sensors[5][2], streets[14], streets[11], Direction.NORTH);
        setSensor(sensors[6][0], crossroads[2], streets[8], Direction.EAST);
        setSensor(sensors[6][1], streets[12], crossroads[5], Direction.SOUTH);
        setSensor(sensors[6][2], streets[8], crossroads[3], Direction.EAST);
        setSensor(sensors[6][3], crossroads[3], streets[12], Direction.SOUTH);
        setSensor(sensors[7][0], streets[16], crossroads[5], Direction.WEST);
        setSensor(sensors[7][1], crossroads[6], streets[13], Direction.NORTH);
        setSensor(sensors[7][2], streets[13], crossroads[4], Direction.NORTH);
        setSensor(sensors[7][3], crossroads[6], streets[16], Direction.WEST);
        setSensor(sensors[8][0], streets[15], streets[14], Direction.WEST);
        setSensor(sensors[8][1], crossroads[5], streets[17], Direction.SOUTH);
        setSensor(sensors[8][2], crossroads[5], streets[15], Direction.WEST);
        setSensor(sensors[9][0], streets[17], crossroads[6], Direction.NORTH);
        setSensor(sensors[9][1], streets[10], crossroads[6], Direction.WEST);

        Queue<Sensor> orders = new LinkedList<>();
        orders.add(sensors[0][0]);
        orders.add(sensors[1][1]);
        orders.add(sensors[3][1]);
        orders.add(sensors[3][2]);
        orders.add(sensors[6][3]);
        orders.add(sensors[6][1]);
        orders.add(sensors[8][1]);
        orders.add(sensors[9][0]);
        orders.add(sensors[7][1]);
        orders.add(sensors[7][2]);
        orders.add(sensors[4][0]);
        orders.add(sensors[4][2]);
        orders.add(sensors[1][0]);
        orders.add(sensors[1][2]);
        orders.add(sensors[2][0]);
        orders.add(sensors[2][3]);
        orders.add(sensors[0][1]);
        orders.add(sensors[5][1]);
        orders.add(sensors[6][0]);
        orders.add(sensors[6][2]);
        orders.add(sensors[3][3]);
        orders.add(sensors[3][0]);
        orders.add(sensors[4][1]);
        orders.add(sensors[9][1]);
        orders.add(sensors[7][3]);
        orders.add(sensors[7][0]);
        orders.add(sensors[8][2]);
        orders.add(sensors[8][0]);
        orders.add(sensors[5][2]);
        orders.add(sensors[5][0]);
        orders.add(sensors[2][1]);
        orders.add(sensors[2][2]);
        orders.add(sensors[0][0]);

        Sensor prev = orders.poll(), next;
        while(!orders.isEmpty()){
            next = orders.poll();
            prev.nextSensor = next;
            next.prevSensor = prev;
            prev = next;
        }

        for(Sensor[] array : sensors){
            for(Sensor sensor : array) {
                Rectangle bounds = sensor.icon.getBounds();
                sensor.balloon.setBounds(bounds.x + bounds.width/2 - Sensor.BalloonIcon.WIDTH/2,
                        bounds.y + bounds.height/2 - (sensor.balloon.useReversedIcon ? 0 : Sensor.BalloonIcon.HEIGHT),
                        Sensor.BalloonIcon.WIDTH, Sensor.BalloonIcon.HEIGHT);
            }
        }
    }

    private static void setSensor(Sensor sensor, Road prevRoad, Road nextRoad, Direction dir){
        sensor.dir = dir;
        sensor.prevRoad = prevRoad;
        sensor.nextRoad = nextRoad;
        sensor.prevRoad.dir[sensor.prevRoad.dir[0] == Direction.UNKNOWN ? 0 : 1] = sensor.dir;

        sensor.prevRoad.adjSensors.put(sensor.dir, sensor);
        sensor.nextRoad.adjSensors.put(oppositeDirOf(sensor.nextRoad.numSections == 3 ? sensor.nextRoad.dir[0] : sensor.dir), sensor);

        if(sensor.nextRoad.iconPanel.coord.x-sensor.prevRoad.iconPanel.coord.x == sensor.prevRoad.iconPanel.coord.w) {
            if (sensor.nextRoad.numSections == 1) {
                sensor.px = sensor.nextRoad.iconPanel.coord.x;
                sensor.py = sensor.nextRoad.iconPanel.coord.centerY;
            }
            else {
                sensor.px = sensor.nextRoad.iconPanel.coord.x;
                sensor.py = sensor.prevRoad.iconPanel.coord.centerY;
            }
        }
        else if(sensor.prevRoad.iconPanel.coord.x-sensor.nextRoad.iconPanel.coord.x == sensor.nextRoad.iconPanel.coord.w) {
            if (sensor.nextRoad.numSections == 1) {
                sensor.px = sensor.prevRoad.iconPanel.coord.x;
                sensor.py = sensor.nextRoad.iconPanel.coord.centerY;
            }
            else {
                sensor.px = sensor.prevRoad.iconPanel.coord.x;
                sensor.py = sensor.prevRoad.iconPanel.coord.centerY;
            }
        }
        else if(sensor.nextRoad.iconPanel.coord.y-sensor.prevRoad.iconPanel.coord.y == sensor.prevRoad.iconPanel.coord.h) {
            if (sensor.nextRoad.numSections == 1) {
                sensor.px = sensor.nextRoad.iconPanel.coord.centerX;
                sensor.py = sensor.nextRoad.iconPanel.coord.y;
            }
            else {
                sensor.px = sensor.prevRoad.iconPanel.coord.centerX;
                sensor.py = sensor.nextRoad.iconPanel.coord.y;
            }
        }
        else if(sensor.prevRoad.iconPanel.coord.y-sensor.nextRoad.iconPanel.coord.y == sensor.nextRoad.iconPanel.coord.h) {
            if (sensor.nextRoad.numSections == 1) {
                sensor.px = sensor.nextRoad.iconPanel.coord.centerX;
                sensor.py = sensor.prevRoad.iconPanel.coord.y;
            }
            else {
                sensor.px = sensor.prevRoad.iconPanel.coord.centerX;
                sensor.py = sensor.prevRoad.iconPanel.coord.y;
            }
        }

        FontMetrics fm = sensor.icon.getFontMetrics(sensor.icon.getFont());
        int w = fm.stringWidth(sensor.icon.getText())+8, h = fm.getHeight();
        sensor.icon.setBounds(sensor.px-w/2, sensor.py-h/2, w, h);
    }

    private static void setAdjRoads(){
        crossroads[0].adjRoads.put(Direction.NORTH, streets[0]);
        crossroads[0].adjRoads.put(Direction.SOUTH, streets[5]);
        crossroads[0].adjRoads.put(Direction.WEST, streets[2]);
        crossroads[0].adjRoads.put(Direction.EAST, streets[3]);

        crossroads[1].adjRoads.put(Direction.NORTH, streets[0]);
        crossroads[1].adjRoads.put(Direction.SOUTH, streets[6]);
        crossroads[1].adjRoads.put(Direction.WEST, streets[3]);
        crossroads[1].adjRoads.put(Direction.EAST, streets[4]);

        crossroads[2].adjRoads.put(Direction.NORTH, streets[5]);
        crossroads[2].adjRoads.put(Direction.SOUTH, streets[11]);
        crossroads[2].adjRoads.put(Direction.WEST, streets[2]);
        crossroads[2].adjRoads.put(Direction.EAST, streets[8]);

        crossroads[3].adjRoads.put(Direction.NORTH, streets[6]);
        crossroads[3].adjRoads.put(Direction.SOUTH, streets[12]);
        crossroads[3].adjRoads.put(Direction.WEST, streets[8]);
        crossroads[3].adjRoads.put(Direction.EAST, streets[9]);

        crossroads[4].adjRoads.put(Direction.NORTH, streets[7]);
        crossroads[4].adjRoads.put(Direction.SOUTH, streets[13]);
        crossroads[4].adjRoads.put(Direction.WEST, streets[9]);
        crossroads[4].adjRoads.put(Direction.EAST, streets[10]);

        crossroads[5].adjRoads.put(Direction.NORTH, streets[12]);
        crossroads[5].adjRoads.put(Direction.SOUTH, streets[17]);
        crossroads[5].adjRoads.put(Direction.WEST, streets[15]);
        crossroads[5].adjRoads.put(Direction.EAST, streets[16]);

        crossroads[6].adjRoads.put(Direction.NORTH, streets[13]);
        crossroads[6].adjRoads.put(Direction.SOUTH, streets[17]);
        crossroads[6].adjRoads.put(Direction.WEST, streets[16]);
        crossroads[6].adjRoads.put(Direction.EAST, streets[10]);

        streets[0].adjRoads.put(Direction.SOUTH, crossroads[1]);
        streets[0].adjRoads.put(Direction.NORTH, crossroads[0]);
        streets[2].adjRoads.put(Direction.EAST, crossroads[2]);
        streets[2].adjRoads.put(Direction.WEST, crossroads[0]);
        streets[10].adjRoads.put(Direction.WEST, crossroads[6]);
        streets[10].adjRoads.put(Direction.EAST, crossroads[4]);
        streets[17].adjRoads.put(Direction.NORTH, crossroads[6]);
        streets[17].adjRoads.put(Direction.SOUTH, crossroads[5]);

        streets[1].adjRoads.put(Direction.WEST, streets[4]);
        streets[1].adjRoads.put(Direction.SOUTH, streets[7]);
        streets[3].adjRoads.put(Direction.WEST, crossroads[0]);
        streets[3].adjRoads.put(Direction.EAST, crossroads[1]);
        streets[4].adjRoads.put(Direction.WEST, crossroads[1]);
        streets[4].adjRoads.put(Direction.EAST, streets[1]);
        streets[5].adjRoads.put(Direction.NORTH, crossroads[0]);
        streets[5].adjRoads.put(Direction.SOUTH, crossroads[2]);
        streets[6].adjRoads.put(Direction.NORTH, crossroads[1]);
        streets[6].adjRoads.put(Direction.SOUTH, crossroads[3]);
        streets[7].adjRoads.put(Direction.NORTH, streets[1]);
        streets[7].adjRoads.put(Direction.SOUTH, crossroads[4]);
        streets[8].adjRoads.put(Direction.WEST, crossroads[2]);
        streets[8].adjRoads.put(Direction.EAST, crossroads[3]);
        streets[9].adjRoads.put(Direction.WEST, crossroads[3]);
        streets[9].adjRoads.put(Direction.EAST, crossroads[4]);
        streets[11].adjRoads.put(Direction.NORTH, crossroads[2]);
        streets[11].adjRoads.put(Direction.SOUTH, streets[14]);
        streets[12].adjRoads.put(Direction.NORTH, crossroads[3]);
        streets[12].adjRoads.put(Direction.SOUTH, crossroads[5]);
        streets[13].adjRoads.put(Direction.NORTH, crossroads[4]);
        streets[13].adjRoads.put(Direction.SOUTH, crossroads[6]);
        streets[14].adjRoads.put(Direction.NORTH, streets[11]);
        streets[14].adjRoads.put(Direction.EAST, streets[15]);
        streets[15].adjRoads.put(Direction.WEST, streets[14]);
        streets[15].adjRoads.put(Direction.EAST, crossroads[5]);
        streets[16].adjRoads.put(Direction.WEST, crossroads[5]);
        streets[16].adjRoads.put(Direction.EAST, crossroads[6]);

        setAccess(crossroads[0], streets[3], streets[2], streets[5], streets[0]);
        setAccess(crossroads[1], streets[4], streets[3], streets[0], streets[6]);
        setAccess(crossroads[2], streets[11], streets[5], streets[2], streets[8]);
        setAccess(crossroads[3], streets[6], streets[12], streets[8], streets[9]);
        setAccess(crossroads[4], streets[13], streets[7], streets[9], streets[10]);
        setAccess(crossroads[5], streets[12], streets[17], streets[16], streets[15]);
        setAccess(crossroads[6], streets[17], streets[13], streets[10], streets[16]);
        setAccess(streets[0], crossroads[0], crossroads[1]);
        setAccess(streets[1], streets[7], streets[4]);
        setAccess(streets[2], crossroads[0], crossroads[2]);
        setAccess(streets[3], crossroads[1], crossroads[0]);
        setAccess(streets[4], streets[1], crossroads[1]);
        setAccess(streets[5], crossroads[2], crossroads[0]);
        setAccess(streets[6], crossroads[1], crossroads[3]);
        setAccess(streets[7], crossroads[4], streets[1]);
        setAccess(streets[8], crossroads[2], crossroads[3]);
        setAccess(streets[9], crossroads[3], crossroads[4]);
        setAccess(streets[10], crossroads[4], crossroads[6]);
        setAccess(streets[11], streets[14], crossroads[2]);
        setAccess(streets[12], crossroads[3], crossroads[5]);
        setAccess(streets[13], crossroads[6], crossroads[4]);
        setAccess(streets[14], streets[15], streets[11]);
        setAccess(streets[15], crossroads[5], streets[14]);
        setAccess(streets[16], crossroads[6], crossroads[5]);
        setAccess(streets[17], crossroads[5], crossroads[6]);
    }

    private static void setAccess(Road road, Road entry1, Road exit1, Road entry2, Road exit2){
        setAccess(road, entry1, exit1);
        setAccess(road, entry2, exit2);
    }

    private static void setAccess(Road road, Road entry, Road exit){
        road.entrance2exit.put(entry, exit);
        road.exit2entrance.put(exit, entry);
    }

    public static void checkCrash() {
        roads.values().forEach(Road::checkCrash);
    }

    void updateRoadInfoPane(Location loc) {
        synchronized (roadPane) {
            roadPane.setText("");
            if (loc == null)
                return;

            if (loc instanceof Building)
                Dashboard.append2pane(new StyledText(loc.name), roadPane);
            else if (loc instanceof Road) {
                StyledText text = new StyledText(loc.name);

                Road road = (Road) loc;
                Set<Car> allCars = new HashSet<>();
                allCars.addAll(road.cars);
                allCars.addAll(road.realCars);
                allCars.addAll(road.waiting);
                if (road.permitted != null)
                    allCars.add(road.permitted);

                if(!allCars.isEmpty())
                    text.append("\n");
                allCars.forEach(car -> {
                    boolean hasBracket = false;
                    text.append(car.name, car.icon.color);
                    StringBuilder sb = new StringBuilder();
                    if (car.hasPhantom()) {
                        if (road.cars.contains(car)) {
                            hasBracket = true;
                            sb.append(" (Fake");
                        }
                        if (road.realCars.contains(car)) {
                            if (!hasBracket) {
                                hasBracket = true;
                                sb.append(" (Real");
                            }
                            else
                                sb.append(", Real");
                        }
                    }
                    if (car == road.permitted) {
                        if (!hasBracket) {
                            hasBracket = true;
                            sb.append(" (Permitted");
                        }
                        else
                            sb.append(", Permitted");
                    }
                    if (road.waiting.contains(car)) {
                        if (!hasBracket) {
                            hasBracket = true;
                            sb.append(" (Waiting");
                        }
                        else
                            sb.append(", Waiting");
                    }
                    sb.append(hasBracket ? ")\n" : "\n");
                    text.append(sb.toString());
                });

                Dashboard.append2pane(text, roadPane);
            }
        }
    }

    static void showCrashEffect(Road road) {
        if (roadAndCrashLettersPanel.containsKey(road) && roadAndCrashLettersPanel.get(road) != null)
            return;
        String crashedCars = "";
        if (!road.carsWithoutFake.isEmpty()) {
            Iterator<Car> iter = road.carsWithoutFake.iterator();
            crashedCars = iter.next().name;
            while (iter.hasNext())
                crashedCars += ", " + iter.next().name;
        }

        synchronized (crashLettersPanels) {
            for (Map.Entry<CrashLettersPanel, Integer> entry : crashLettersPanels.entrySet()) {
                CrashLettersPanel panel = entry.getKey();
                if (!panel.isVisible()) {
                    panel.setText(crashedCars);
                    int x = road.iconPanel.getX() + road.iconPanel.getWidth() / 2 - panel.getWidth() / 2;
                    int y = road.iconPanel.getY() + road.iconPanel.getHeight() / 2 - panel.getHeight() / 2;
                    panel.setLocation(x, y);
//                    entry.setValue(500000);
                    panel.setVisible(true);
                    roadAndCrashLettersPanel.put(road, panel);
                    roadAndCrashLettersPanel.put(panel, road);
                    break;
                }
            }
        }
    }

    static void hideCrashEffect(Road road) {
        if (!roadAndCrashLettersPanel.containsKey(road) || roadAndCrashLettersPanel.get(road) == null)
            return;

        CrashLettersPanel panel = (CrashLettersPanel) roadAndCrashLettersPanel.get(road);
        synchronized (crashLettersPanels) {
            crashLettersPanels.put(panel, 0);
            panel.setVisible(false);
            roadAndCrashLettersPanel.remove(road);
            roadAndCrashLettersPanel.remove(panel);
        }
    }

    private static void hideCrashEffect(CrashLettersPanel panel) {
        if (!roadAndCrashLettersPanel.containsKey(panel) || roadAndCrashLettersPanel.get(panel) == null)
            return;
        hideCrashEffect((Road) roadAndCrashLettersPanel.get(panel));
    }

    static void checkCrashEffectExpiration(int elapsed) {
        synchronized (crashLettersPanels) {
            for (Map.Entry<CrashLettersPanel, Integer> entry : crashLettersPanels.entrySet()) {
                CrashLettersPanel panel = entry.getKey();
                if (panel.isVisible()) {
                    entry.setValue(entry.getValue() - elapsed);
                    if (entry.getValue() <= 0)
                        panel.setVisible(false);
                }
            }
        }
    }

    public static TrafficMap getInstance(){
        if(instance == null)
            synchronized (TrafficMap.class) {
                if(instance == null)
                    instance = new TrafficMap();
            }
        return instance;
    }

    public static void showFakeLocIconLabel(boolean b) {
        fakeCarIconPanel.setVisible(b);
    }

    public static void showRealLocIconLabel(boolean b) {
        realCarIconPanel.setVisible(b);
    }

    public static void enableSensorIcons(boolean enable) {
        for (Sensor[] array : sensors)
            for(Sensor sensor : array)
                sensor.icon.setEnabled(enable);
    }

    static void switchLanguage() {
        crossroadIconPanel.setText(Dashboard.useEnglish() ? "Crossroad" : "十字路口");
        streetIconPanel.setText(Dashboard.useEnglish() ? "Street" : "单行道");
        carIconPanel.setText(Dashboard.useEnglish() ? "Car" : "车辆");
        fakeCarIconPanel.setText(Dashboard.useEnglish() ? "Fake location" : "虚假位置");
        realCarIconPanel.setText(Dashboard.useEnglish() ? "Real location" : "真实位置");

        ((TitledBorder) roadPaneScroll.getBorder()).setTitle(Dashboard.useEnglish() ? "Location info" : "位置信息");
    }

    public static Direction oppositeDirOf(Direction dir){
        switch (dir) {
            case NORTH:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.NORTH;
            case WEST:
                return Direction.EAST;
            case EAST:
                return Direction.WEST;
            default:
                return Direction.UNKNOWN;
        }
    }

    public static String dirOf(Direction dir){
        switch(dir){
            case NORTH:
                return "North";
            case SOUTH:
                return "South";
            case WEST:
                return "West";
            case EAST:
                return "East";
            default:
                return "Unknown";
        }
    }

    public static Direction dirOf(int dir){
        switch(dir){
            case 0:
                return Direction.NORTH;
            case 1:
                return Direction.SOUTH;
            case 2:
                return Direction.WEST;
            case 3:
                return Direction.EAST;
            default:
                return Direction.UNKNOWN;
        }
    }

    private static class IdentifierPanel extends JPanel {
        private final JLabel iconLabel, textLabel;
        IdentifierPanel (ImageIcon icon, int width, int height, String text, Font textFont) {
            setLayout(new BorderLayout());
            iconLabel = new JLabel(Resource.loadImage(icon, width, height), JLabel.LEADING);
            add(iconLabel, BorderLayout.WEST);
            textLabel = new JLabel(text, JLabel.TRAILING);
            textLabel.setFont(textFont);
            add(textLabel, BorderLayout.EAST);
        }

        public void setText(String text) {
            textLabel.setText(text);
        }
    }

    private static class CrashLettersPanel extends JPanel {
        private final JLabel icon;
        private String text;
        private final FontMetrics fm;
        CrashLettersPanel(JLabel icon) {
            this.icon = icon;
            add(icon);
            setLayout(null);
            setOpaque(false);
            setBackground(null);
            setFont(Resource.en16bold);
            fm = getFontMetrics(getFont());
            icon.setSize(icon.getPreferredSize());
            CrashLettersPanel panel = this;
            icon.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    hideCrashEffect(panel);
                }
            });
        }

        @Override
        public void paintComponent(Graphics g) {
//            super.paintComponent(g);
            icon.setLocation((getWidth()-icon.getWidth())/2, 0);
//            System.out.println(icon.getX()+", "+icon.getY());
            int x = (getWidth()-fm.stringWidth(text))/2;
            int y = icon.getHeight() + fm.getAscent();
            g.setColor(Color.WHITE);
            g.drawString(text, x-1, y-1);
            g.drawString(text, x-1, y+1);
            g.drawString(text, x+1, y-1);
            g.drawString(text, x+1, y+1);
            g.setColor(Color.BLACK);
            g.drawString(text, x, y);
        }

        public void setText(String text) {
            if (text == null || text.equals(this.text))
                return;
            this.text = text;
            setSize(Math.max(fm.stringWidth(text), icon.getWidth()), fm.getHeight() + icon.getHeight());
            setPreferredSize(getSize());
//            repaint();
        }
    }

    public static class Coord{
        public int x, y, w, h;
        public int arcw ,arch;
        public int centerX, centerY;
    }
}