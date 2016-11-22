package nju.xiaofanli.dashboard;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.Style;

import nju.xiaofanli.Resource;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.sensor.Sensor;
import nju.xiaofanli.util.Pair;

public class TrafficMap extends JPanel{
    private static final long serialVersionUID = 1L;
    private static TrafficMap instance = null;
    public static final boolean DIRECTION = true;
    public static final ConcurrentMap<String, Car> cars = new ConcurrentHashMap<>();
    public static final List<Car> carList = new ArrayList<>();
    public static final Set<Car> connectedCars = new HashSet<>();
    public static final Road.Crossroad[] crossroads = new Road.Crossroad[7];
    public static final Road.Street[] streets = new Road.Street[18];
    public static final Map<String, Road> roads = new HashMap<>();
    public static final Map<String, Location> locations = new HashMap<>();
    public static final List<Location> locationList = new ArrayList<>();
    public static final Sensor[][] sensors = new Sensor[10][];
    public static final List<Citizen> citizens = new ArrayList<>();
    public static final List<Citizen> freeCitizens = new ArrayList<>();
    public static final ConcurrentMap<Building.Type, Building> buildings = new ConcurrentHashMap<>();
    private static final JTextPane roadPane = new JTextPane();
    static final JScrollPane roadPaneScroll = new JScrollPane(roadPane);
    private static final JPanel crossroadIconPanel = createIconPanel(Resource.CROSSROAD_ICON, TrafficMap.SH/2, TrafficMap.SH/2, "Crossroad", Resource.bold17dialog),
            streetIconPanel = createIconPanel(Resource.STREET_ICON, TrafficMap.SH, TrafficMap.SH/2,  "Street", Resource.bold17dialog),
            carIconPanel = createIconPanel(Resource.getCarIcons(Car.ORANGE)[0], TrafficMap.SH/2, TrafficMap.SH/2,  "Car", Resource.bold17dialog),
            fakeCarIconPanel = createIconPanel(Resource.getCarIcons(Car.ORANGE)[1], TrafficMap.SH/2*15/17, TrafficMap.SH/2*15/17,  "Fake location", Resource.bold15dialog),
            realCarIconPanel = createIconPanel(Resource.getCarIcons(Car.ORANGE)[2], TrafficMap.SH/2*15/17, TrafficMap.SH/2*15/17,  "Real location", Resource.bold15dialog);
    private static final List<JPanel> iconPanels = Arrays.asList(crossroadIconPanel, streetIconPanel, carIconPanel, fakeCarIconPanel, realCarIconPanel);
    private static final Map<JLabel, Integer> crashLettersLabels = new HashMap<>();

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

    public static final int UNKNOWN_DIR = -1;
    public static final int NORTH = 0;
    public static final int SOUTH = 1;
    public static final int WEST = 2;
    public static final int EAST = 3;

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
        roadPane.setFont(Resource.plain17dialog);
        roadPaneScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Location info",
                TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
        ((TitledBorder) roadPaneScroll.getBorder()).setTitleFont(Resource.bold16dialog);
        roadPaneScroll.setBackground(Color.LIGHT_GRAY);

        fakeCarIconPanel.setVisible(false);
        fakeCarIconPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        realCarIconPanel.setVisible(false);
        realCarIconPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        for (int i = 0;i < 5;i++) {
            JLabel label = new JLabel(Resource.CRASH_LETTERS);
            crashLettersLabels.put(label, 0);
            label.setVisible(false);
            label.setSize(label.getPreferredSize());
        }
    }

    private TrafficMap() {
        setLayout(null);
        setSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setMaximumSize(new Dimension(SIZE, SIZE));
        setPreferredSize(new Dimension(SIZE, SIZE));
        initRoads();
        initSensors();
        initBuildings();

        for (Sensor[] array : sensors) {
            for (Sensor sensor : array) {
                add(sensor.icon);
                Resource.timeouts.get(sensor.name).forEach((car, time) -> {
                    int direction = sensor.nextRoad.dir[1] == UNKNOWN_DIR ? sensor.nextRoad.dir[0] : sensor.dir;
                    if (!sensor.nextRoad.timeouts.containsKey(direction))
                        sensor.nextRoad.timeouts.put(direction, new HashMap<>());
                    sensor.nextRoad.timeouts.get(direction).put(car, (int) (time * 1.5));
                });
            }
        }

        add(roadPaneScroll);
        crashLettersLabels.keySet().forEach(this::add);
        for (Sensor[] array : sensors)
            for (Sensor sensor : array)
                add(sensor.balloon);
        citizens.forEach(citizen -> add(citizen.icon));
        roads.values().forEach(road -> locations.put(road.name, road));
        buildings.values().forEach(building -> {
            add(building.icon);
            locations.put(building.name, building);
        });
        locationList.addAll(locations.values());
        roads.values().forEach(road -> {
//            if (road.id != 2 && road.id != 3)
                add(road.icon);
        });

        JPanel iconPanel = new JPanel(new GridLayout(5, 1));
        iconPanel.setBounds(5, 0, U3, U3);
        add(iconPanel);
        iconPanels.forEach(iconPanel::add);
    }

    public static void reset(){
        cars.values().forEach(Car::reset);
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
                    crossroads[i].icon.coord.x = U2 + SW;
                    crossroads[i].icon.coord.y = U2 + SW;
                    break;
                case 1:
                    crossroads[i].icon.coord.x = U2 + SW + U1;
                    crossroads[i].icon.coord.y = U2 + SW;
                    break;
                case 2:
                    crossroads[i].icon.coord.x = U2 + SW;
                    crossroads[i].icon.coord.y = U2 + SW + U1;
                    break;
                case 3:
                    crossroads[i].icon.coord.x = U2 + SW + U1;
                    crossroads[i].icon.coord.y = U2 + SW + U1;
                    break;
                case 4:
                    crossroads[i].icon.coord.x = U2 + SW + 2*U1;
                    crossroads[i].icon.coord.y = U2 + SW + U1;
                    break;
                case 5:
                    crossroads[i].icon.coord.x = U2 + SW + U1;
                    crossroads[i].icon.coord.y = U2 + SW + 2*U1;
                    break;
                case 6:
                    crossroads[i].icon.coord.x = U2 + SW + 2*U1;
                    crossroads[i].icon.coord.y = U2 + SW + 2*U1;
                    break;
            }

            crossroads[i].icon.coord.w = crossroads[i].icon.coord.h = CW;
            crossroads[i].icon.addCrossroadIcon(crossroads[i].icon.coord);
            crossroads[i].icon.coord.centerX = crossroads[i].icon.coord.x + crossroads[i].icon.coord.w/2;
            crossroads[i].icon.coord.centerY = crossroads[i].icon.coord.y + crossroads[i].icon.coord.h/2;
            crossroads[i].icon.setBounds(crossroads[i].icon.coord.x, crossroads[i].icon.coord.y, crossroads[i].icon.coord.w, crossroads[i].icon.coord.h);
        }

        for(int i = 0;i < streets.length;i++){
            streets[i].id = i;
            streets[i].name = "Street " + i;
            roads.put(streets[i].name, streets[i]);
            streets[i].icon.coord.arcw = AW;
            streets[i].icon.coord.arch = AW;
            switch (i) {
                case 0:
                    streets[i].icon.coord.x = U1;
                    streets[i].icon.coord.y = 0;
                    streets[i].icon.coord.w = U4;
                    streets[i].icon.coord.h = U3;
                    streets[i].icon.addStreetIcon(0, 0, U4, SH, AW, AW, false);
                    streets[i].icon.addStreetIcon(0, 0, SH, U3, AW, AW, true);
                    streets[i].icon.addStreetIcon(U1, 0, SH, U3, AW, AW, true);
                    break;
                case 1:
                    streets[i].icon.coord.x = U3 + 2*U1;
                    streets[i].icon.coord.y = 0;
                    streets[i].icon.coord.w = U5;
                    streets[i].icon.coord.h = U5;
                    streets[i].icon.addStreetIcon(U5-U4, 0, U4, SH, AW, AW, false);
                    streets[i].icon.addStreetIcon(U5-U4, 0, SH, U5, AW, AW, true);
                    streets[i].icon.addStreetIcon(0, U1, U5, SH, AW, AW, false);
                    streets[i].icon.addStreetIcon(U5-SH, 0, SH, U4, AW, AW, true);
                    break;
                case 2:
                    streets[i].icon.coord.x = 0;
                    streets[i].icon.coord.y = U1;
                    streets[i].icon.coord.w = U3;
                    streets[i].icon.coord.h = U4;
                    streets[i].icon.addStreetIcon(0, 0, U3, SH, AW, AW, false);
                    streets[i].icon.addStreetIcon(0, 0, SH, U4, AW, AW, true);
                    streets[i].icon.addStreetIcon(0, U1, U3, SH, AW, AW, false);
                    break;
                case 3:
                    streets[i].icon.coord.x = U1+U2;
                    streets[i].icon.coord.y = U1;
                    streets[i].icon.coord.w = SW;
                    streets[i].icon.coord.h = SH;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, false);
                    break;
                case 4:
                    streets[i].icon.coord.x = 2*U1+U2;
                    streets[i].icon.coord.y = U1;
                    streets[i].icon.coord.w = SW;
                    streets[i].icon.coord.h = SH;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, false);
                    break;
                case 5:
                    streets[i].icon.coord.x = U1;
                    streets[i].icon.coord.y = U1 + U2;
                    streets[i].icon.coord.w = SH;
                    streets[i].icon.coord.h = SW;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, true);
                    break;
                case 6:
                    streets[i].icon.coord.x = 2 * U1;
                    streets[i].icon.coord.y = U1 + U2;
                    streets[i].icon.coord.w = SH;
                    streets[i].icon.coord.h = SW;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, true);
                    break;
                case 7:
                    streets[i].icon.coord.x = 3 * U1;
                    streets[i].icon.coord.y = U1 + U2;
                    streets[i].icon.coord.w = SH;
                    streets[i].icon.coord.h = SW;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, true);
                    break;
                case 8:
                    streets[i].icon.coord.x = U1 + U2;
                    streets[i].icon.coord.y = 2 * U1;
                    streets[i].icon.coord.w = SW;
                    streets[i].icon.coord.h = SH;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, false);
                    break;
                case 9:
                    streets[i].icon.coord.x = 2*U1 + U2;
                    streets[i].icon.coord.y = 2 * U1;
                    streets[i].icon.coord.w = SW;
                    streets[i].icon.coord.h = SH;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, false);
                    break;
                case 10:
                    streets[i].icon.coord.x = 3*U1 + U2;
                    streets[i].icon.coord.y = 2 * U1;
                    streets[i].icon.coord.w = U3;
                    streets[i].icon.coord.h = U4;
                    streets[i].icon.addStreetIcon(0, 0, U3, SH, AW, AW, false);
                    streets[i].icon.addStreetIcon(0, U1, U3, SH, AW, AW, false);
                    streets[i].icon.addStreetIcon(U3-SH, 0, SH, U4, AW, AW, true);
                    break;
                case 11:
                    streets[i].icon.coord.x = U1;
                    streets[i].icon.coord.y = 2*U1+(SH+CW)/2;
                    streets[i].icon.coord.w = SH;
                    streets[i].icon.coord.h = SW;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, true);
                    break;
                case 12:
                    streets[i].icon.coord.x = 2 * U1;
                    streets[i].icon.coord.y = 2*U1+(SH+CW)/2;
                    streets[i].icon.coord.w = SH;
                    streets[i].icon.coord.h = SW;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, true);
                    break;
                case 13:
                    streets[i].icon.coord.x = 3 * U1;
                    streets[i].icon.coord.y = 2*U1+(SH+CW)/2;
                    streets[i].icon.coord.w = SH;
                    streets[i].icon.coord.h = SW;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, true);
                    break;
                case 14:
                    streets[i].icon.coord.x = 0;
                    streets[i].icon.coord.y = U3 + 2*U1;
                    streets[i].icon.coord.w = U5;
                    streets[i].icon.coord.h = U5;
                    streets[i].icon.addStreetIcon(0, U5-U4, U5, SH, AW, AW, false);
                    streets[i].icon.addStreetIcon(0, U5-U4, SH, U4, AW, AW, true);
                    streets[i].icon.addStreetIcon(0, U5-SH, U4, SH, AW, AW, false);
                    streets[i].icon.addStreetIcon(U1, 0, SH, U5, AW, AW, true);
                    break;
                case 15:
                    streets[i].icon.coord.x = U1+(SH+CW)/2;
                    streets[i].icon.coord.y = 3 * U1;
                    streets[i].icon.coord.w = SW;
                    streets[i].icon.coord.h = SH;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, false);
                    break;
                case 16:
                    streets[i].icon.coord.x = 2*U1+(SH+CW)/2;
                    streets[i].icon.coord.y = 3 * U1;
                    streets[i].icon.coord.w = SW;
                    streets[i].icon.coord.h = SH;
                    streets[i].icon.addStreetIcon(streets[i].icon.coord, false);
                    break;
                case 17:
                    streets[i].icon.coord.x = 2 * U1;
                    streets[i].icon.coord.y = 3*U1+U2;
                    streets[i].icon.coord.w = U4;
                    streets[i].icon.coord.h = U3;
                    streets[i].icon.addStreetIcon(0, 0, SH, U3, AW, AW, true);
                    streets[i].icon.addStreetIcon(0, U3-SH, U4, SH, AW, AW, false);
                    streets[i].icon.addStreetIcon(U1, 0, SH, U3, AW, AW, true);
                    break;
            }

            streets[i].icon.coord.centerX = streets[i].icon.coord.x + streets[i].icon.coord.w/2;
            streets[i].icon.coord.centerY = streets[i].icon.coord.y + streets[i].icon.coord.h/2;
            streets[i].icon.setBounds(streets[i].icon.coord.x, streets[i].icon.coord.y, streets[i].icon.coord.w, streets[i].icon.coord.h);
        }

        setAdjRoads();
    }

    private static void initSensors(){
        for(int i = 0;i < sensors.length;i++)
            for(int j = 0;j < sensors[i].length;j++)
                sensors[i][j] = new Sensor(i, j);

        setSensor(sensors[0][0], crossroads[0], streets[0], NORTH);
        setSensor(sensors[0][1], crossroads[0], streets[2], WEST);
        setSensor(sensors[1][0], streets[1], streets[4], WEST);
        setSensor(sensors[1][1], streets[0], crossroads[1], SOUTH);
        setSensor(sensors[1][2], streets[4], crossroads[1], WEST);
        setSensor(sensors[2][0], crossroads[1], streets[3], WEST);
        setSensor(sensors[2][1], crossroads[2], streets[5], NORTH);
        setSensor(sensors[2][2], streets[5], crossroads[0], NORTH);
        setSensor(sensors[2][3], streets[3], crossroads[0], WEST);
        setSensor(sensors[3][0], streets[9], crossroads[4], EAST);
        setSensor(sensors[3][1], crossroads[1], streets[6], SOUTH);
        setSensor(sensors[3][2], streets[6], crossroads[3], SOUTH);
        setSensor(sensors[3][3], crossroads[3], streets[9], EAST);
        setSensor(sensors[4][0], crossroads[4], streets[7], NORTH);
        setSensor(sensors[4][1], crossroads[4], streets[10], EAST);
        setSensor(sensors[4][2], streets[7], streets[1], NORTH);
        setSensor(sensors[5][0], streets[11], crossroads[2], NORTH);
        setSensor(sensors[5][1], streets[2], crossroads[2], EAST);
        setSensor(sensors[5][2], streets[14], streets[11], NORTH);
        setSensor(sensors[6][0], crossroads[2], streets[8], EAST);
        setSensor(sensors[6][1], streets[12], crossroads[5], SOUTH);
        setSensor(sensors[6][2], streets[8], crossroads[3], EAST);
        setSensor(sensors[6][3], crossroads[3], streets[12], SOUTH);
        setSensor(sensors[7][0], streets[16], crossroads[5], WEST);
        setSensor(sensors[7][1], crossroads[6], streets[13], NORTH);
        setSensor(sensors[7][2], streets[13], crossroads[4], NORTH);
        setSensor(sensors[7][3], crossroads[6], streets[16], WEST);
        setSensor(sensors[8][0], streets[15], streets[14], WEST);
        setSensor(sensors[8][1], crossroads[5], streets[17], SOUTH);
        setSensor(sensors[8][2], crossroads[5], streets[15], WEST);
        setSensor(sensors[9][0], streets[17], crossroads[6], NORTH);
        setSensor(sensors[9][1], streets[10], crossroads[6], WEST);

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
            if(TrafficMap.DIRECTION){
                prev.nextSensor = next;
                next.prevSensor = prev;
            }
            else{
                prev.prevSensor = next;
                next.nextSensor = prev;
            }
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

    private static void setSensor(Sensor sensor, Road prevRoad, Road nextRoad, int dir){
        sensor.dir = TrafficMap.DIRECTION ? dir : oppositeDirOf(dir);
        sensor.prevRoad = prevRoad;
        sensor.nextRoad = nextRoad;
        sensor.prevRoad.dir[sensor.prevRoad.dir[0] == UNKNOWN_DIR ? 0 : 1] = sensor.dir;

        sensor.prevRoad.adjSensors.put(sensor.dir, sensor);
        sensor.nextRoad.adjSensors.put(oppositeDirOf(sensor.nextRoad.numSections == 3 ? sensor.nextRoad.dir[0] : sensor.dir), sensor);

        if(sensor.nextRoad.icon.coord.x-sensor.prevRoad.icon.coord.x == sensor.prevRoad.icon.coord.w) {
            if (sensor.nextRoad.numSections == 1) {
                sensor.px = sensor.nextRoad.icon.coord.x;
                sensor.py = sensor.nextRoad.icon.coord.centerY;
            }
            else {
                sensor.px = sensor.nextRoad.icon.coord.x;
                sensor.py = sensor.prevRoad.icon.coord.centerY;
            }
        }
        else if(sensor.prevRoad.icon.coord.x-sensor.nextRoad.icon.coord.x == sensor.nextRoad.icon.coord.w) {
            if (sensor.nextRoad.numSections == 1) {
                sensor.px = sensor.prevRoad.icon.coord.x;
                sensor.py = sensor.nextRoad.icon.coord.centerY;
            }
            else {
                sensor.px = sensor.prevRoad.icon.coord.x;
                sensor.py = sensor.prevRoad.icon.coord.centerY;
            }
        }
        else if(sensor.nextRoad.icon.coord.y-sensor.prevRoad.icon.coord.y == sensor.prevRoad.icon.coord.h) {
            if (sensor.nextRoad.numSections == 1) {
                sensor.px = sensor.nextRoad.icon.coord.centerX;
                sensor.py = sensor.nextRoad.icon.coord.y;
            }
            else {
                sensor.px = sensor.prevRoad.icon.coord.centerX;
                sensor.py = sensor.nextRoad.icon.coord.y;
            }
        }
        else if(sensor.prevRoad.icon.coord.y-sensor.nextRoad.icon.coord.y == sensor.nextRoad.icon.coord.h) {
            if (sensor.nextRoad.numSections == 1) {
                sensor.px = sensor.nextRoad.icon.coord.centerX;
                sensor.py = sensor.prevRoad.icon.coord.y;
            }
            else {
                sensor.px = sensor.prevRoad.icon.coord.centerX;
                sensor.py = sensor.prevRoad.icon.coord.y;
            }
        }

        FontMetrics fm = sensor.icon.getFontMetrics(sensor.icon.getFont());
        int w = fm.stringWidth(sensor.icon.getText())+8, h = fm.getHeight();
        sensor.icon.setBounds(sensor.px-w/2, sensor.py-h/2, w, h);
    }

    private static void setAdjRoads(){
        crossroads[0].adjRoads.put(NORTH, streets[0]);
        crossroads[0].adjRoads.put(SOUTH, streets[5]);
        crossroads[0].adjRoads.put(WEST, streets[2]);
        crossroads[0].adjRoads.put(EAST, streets[3]);

        crossroads[1].adjRoads.put(NORTH, streets[0]);
        crossroads[1].adjRoads.put(SOUTH, streets[6]);
        crossroads[1].adjRoads.put(WEST, streets[3]);
        crossroads[1].adjRoads.put(EAST, streets[4]);

        crossroads[2].adjRoads.put(NORTH, streets[5]);
        crossroads[2].adjRoads.put(SOUTH, streets[11]);
        crossroads[2].adjRoads.put(WEST, streets[2]);
        crossroads[2].adjRoads.put(EAST, streets[8]);

        crossroads[3].adjRoads.put(NORTH, streets[6]);
        crossroads[3].adjRoads.put(SOUTH, streets[12]);
        crossroads[3].adjRoads.put(WEST, streets[8]);
        crossroads[3].adjRoads.put(EAST, streets[9]);

        crossroads[4].adjRoads.put(NORTH, streets[7]);
        crossroads[4].adjRoads.put(SOUTH, streets[13]);
        crossroads[4].adjRoads.put(WEST, streets[9]);
        crossroads[4].adjRoads.put(EAST, streets[10]);

        crossroads[5].adjRoads.put(NORTH, streets[12]);
        crossroads[5].adjRoads.put(SOUTH, streets[17]);
        crossroads[5].adjRoads.put(WEST, streets[15]);
        crossroads[5].adjRoads.put(EAST, streets[16]);

        crossroads[6].adjRoads.put(NORTH, streets[13]);
        crossroads[6].adjRoads.put(SOUTH, streets[17]);
        crossroads[6].adjRoads.put(WEST, streets[16]);
        crossroads[6].adjRoads.put(EAST, streets[10]);

        if(TrafficMap.DIRECTION){
            streets[0].adjRoads.put(SOUTH, crossroads[1]);
            streets[0].adjRoads.put(NORTH, crossroads[0]);
            streets[2].adjRoads.put(EAST, crossroads[2]);
            streets[2].adjRoads.put(WEST, crossroads[0]);
            streets[10].adjRoads.put(WEST, crossroads[6]);
            streets[10].adjRoads.put(EAST, crossroads[4]);
            streets[17].adjRoads.put(NORTH, crossroads[6]);
            streets[17].adjRoads.put(SOUTH, crossroads[5]);
        }
        else{
            streets[0].adjRoads.put(SOUTH, crossroads[0]);
            streets[0].adjRoads.put(NORTH, crossroads[1]);
            streets[2].adjRoads.put(EAST, crossroads[0]);
            streets[2].adjRoads.put(WEST, crossroads[2]);
            streets[10].adjRoads.put(WEST, crossroads[4]);
            streets[10].adjRoads.put(EAST, crossroads[6]);
            streets[17].adjRoads.put(NORTH, crossroads[5]);
            streets[17].adjRoads.put(SOUTH, crossroads[6]);
        }

        streets[1].adjRoads.put(WEST, streets[4]);
        streets[1].adjRoads.put(SOUTH, streets[7]);
        streets[3].adjRoads.put(WEST, crossroads[0]);
        streets[3].adjRoads.put(EAST, crossroads[1]);
        streets[4].adjRoads.put(WEST, crossroads[1]);
        streets[4].adjRoads.put(EAST, streets[1]);
        streets[5].adjRoads.put(NORTH, crossroads[0]);
        streets[5].adjRoads.put(SOUTH, crossroads[2]);
        streets[6].adjRoads.put(NORTH, crossroads[1]);
        streets[6].adjRoads.put(SOUTH, crossroads[3]);
        streets[7].adjRoads.put(NORTH, streets[1]);
        streets[7].adjRoads.put(SOUTH, crossroads[4]);
        streets[8].adjRoads.put(WEST, crossroads[2]);
        streets[8].adjRoads.put(EAST, crossroads[3]);
        streets[9].adjRoads.put(WEST, crossroads[3]);
        streets[9].adjRoads.put(EAST, crossroads[4]);
        streets[11].adjRoads.put(NORTH, crossroads[2]);
        streets[11].adjRoads.put(SOUTH, streets[14]);
        streets[12].adjRoads.put(NORTH, crossroads[3]);
        streets[12].adjRoads.put(SOUTH, crossroads[5]);
        streets[13].adjRoads.put(NORTH, crossroads[4]);
        streets[13].adjRoads.put(SOUTH, crossroads[6]);
        streets[14].adjRoads.put(NORTH, streets[11]);
        streets[14].adjRoads.put(EAST, streets[15]);
        streets[15].adjRoads.put(WEST, streets[14]);
        streets[15].adjRoads.put(EAST, crossroads[5]);
        streets[16].adjRoads.put(WEST, crossroads[5]);
        streets[16].adjRoads.put(EAST, crossroads[6]);

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
        if(TrafficMap.DIRECTION){
            road.entrance2exit.put(entry, exit);
            road.exit2entrance.put(exit, entry);
        }
        else{
            road.entrance2exit.put(exit, entry);
            road.exit2entrance.put(entry, exit);
        }
    }

    public static void checkRealCrash() {
        roads.values().forEach(Road::checkRealCrash);
    }

    void updateRoadInfoPane(Location loc) {
        synchronized (roadPane) {
            roadPane.setText("");
            if (loc == null)
                return;

            if (loc instanceof Building)
                Dashboard.append2pane(loc.name, null, roadPane);
            else if (loc instanceof Road) {
                List<Pair<String, Style>> strings = new ArrayList<>();
                strings.add(new Pair<>(loc.name, null));

                Road road = (Road) loc;
                Set<Car> allCars = new HashSet<>();
                allCars.addAll(road.cars);
                allCars.addAll(road.realCars);
                allCars.addAll(road.waiting);
                if (road.getPermitted() != null)
                    allCars.add(road.getPermitted());

                if(!allCars.isEmpty())
                    strings.add(new Pair<>("\n", null));
                allCars.forEach(car -> {
                    boolean hasBracket = false;
                    strings.add(new Pair<>(car.name, Resource.getTextStyle(car.icon.color)));
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
                    if (car == road.getPermitted()) {
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
                    strings.add(new Pair<>(sb.toString(), null));
                });

                Dashboard.append2pane(strings, roadPane);
//
//                StringBuilder sb = new StringBuilder();
//                sb.append(dirOf(road.dir[0]) + " " + road.timeouts.get(road.dir[0]).get(Car.RED));
//                if (road.dir[1] != UNKNOWN_DIR)
//                    sb.append("\n" + dirOf(road.dir[1]) + " " + road.timeouts.get(road.dir[1]).get(Car.RED));
//                roadPane.setText(sb.toString());
            }
        }
    }

    void showCrashEffect(Road road) {
        synchronized (crashLettersLabels) {
            for (Map.Entry<JLabel, Integer> entry : crashLettersLabels.entrySet()) {
                JLabel label = entry.getKey();
                if (!label.isVisible()) {
                    int x = road.icon.getX() + road.icon.getWidth() / 2 - label.getWidth() / 2;
                    int y = road.icon.getY() + road.icon.getHeight() / 2 - label.getHeight() / 2;
                    label.setLocation(x, y);
                    entry.setValue(2000);
                    label.setVisible(true);
                    break;
                }
            }
        }
    }

    static void checkCrashEffectExpiration(int elapsed) {
        synchronized (crashLettersLabels) {
            for (Map.Entry<JLabel, Integer> entry : crashLettersLabels.entrySet()) {
                JLabel label = entry.getKey();
                if (label.isVisible()) {
                    entry.setValue(entry.getValue() - elapsed);
                    if (entry.getValue() <= 0)
                        label.setVisible(false);
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

    private static JPanel createIconPanel(ImageIcon icon, int width, int height, String text, Font textFont) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(Resource.loadImage(icon, width, height), JLabel.LEADING), BorderLayout.WEST);
        JLabel textLabel = new JLabel(text, JLabel.TRAILING);
        textLabel.setFont(textFont);
        p.add(textLabel, BorderLayout.EAST);
        return p;
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

    public static int oppositeDirOf(int dir){
        switch (dir) {
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            case EAST:
                return WEST;
            default:
                return UNKNOWN_DIR;
        }
    }

    public static String dirOf(int dir){
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

    public static class Coord{
        public int x, y, w, h;
        public int arcw ,arch;
        public int centerX, centerY;
    }
}