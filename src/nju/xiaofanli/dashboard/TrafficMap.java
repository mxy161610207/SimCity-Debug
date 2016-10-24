package nju.xiaofanli.dashboard;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import nju.xiaofanli.Resource;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.sensor.Sensor;
import nju.xiaofanli.util.MarqueeLabel;
import nju.xiaofanli.util.Pair;

public class TrafficMap extends JPanel{
    private static final long serialVersionUID = 1L;
    private static TrafficMap instance = null;
    public static final boolean DIRECTION = true;
    public static final ConcurrentMap<String, Car> cars = new ConcurrentHashMap<>();
    public static final List<Car> carList = new ArrayList<>();
    public static final Set<Car> connectedCars = new HashSet<>();
    public static final Road.Crossroad[] crossroads = new Road.Crossroad[9];
    public static final Road.Street[] streets = new Road.Street[32];
    public static final Map<String, Road> roads = new HashMap<>();
    public static final Map<String, Location> locations = new HashMap<>();
    public static final List<Location> locationList = new ArrayList<>();
    public static final Sensor[][] sensors = new Sensor[10][];
    public static final List<Citizen> citizens = new ArrayList<>();
    public static final List<Citizen> freeCitizens = new ArrayList<>();
    public static final ConcurrentMap<Building.Type, Building> buildings = new ConcurrentHashMap<>();
    private static final JTextPane roadPane = new JTextPane();
    static final JScrollPane roadPaneScroll = new JScrollPane(roadPane);
    private static final JLabel crossroadIconLabel = new JLabel("Crossroad", Resource.CROSSROAD_ICON, SwingConstants.LEADING),
            streetIconLabel = new JLabel("Street", Resource.STREET_ICON, SwingConstants.LEADING),
            carIconLabel = new JLabel("Normal car", Resource.CAR_ICON, SwingConstants.LEADING),
            fakeCarIconLabel = new MarqueeLabel("Fake car (caused by inconsistent context)", Resource.FAKE_CAR_ICON, SwingConstants.LEADING, 26),
            realCarIconLabel = new MarqueeLabel("Real car (invisible to other cars)", Resource.REAL_CAR_ICON, SwingConstants.LEADING, 26);
    private static final List<JLabel> iconLabels = Arrays.asList(crossroadIconLabel, streetIconLabel, carIconLabel, fakeCarIconLabel, realCarIconLabel);

    public static final int SH = 48;//street height
    public static final int SW = SH * 2;//street width
    public static final int CW = (int) (SH * 1.5);//crossroad width
    private static final int AW = CW / 2;
    private static final int U1 = CW + SW;
    private static final int U2 = (CW-SH)/2;
    private static final int U3 = SW+(CW+SH)/2;
    private static final int U4 = U1+SH;
    public static final int SIZE = 4*(SW+CW)+SH;

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
        roadPane.setEditable(false);
        roadPane.setBackground(Color.WHITE);
        roadPane.setFont(Resource.plain17dialog);
        roadPaneScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Location info",
                TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
        ((TitledBorder) roadPaneScroll.getBorder()).setTitleFont(Resource.bold16dialog);
        roadPaneScroll.setBackground(Color.LIGHT_GRAY);

        iconLabels.forEach(label -> label.setFont(Resource.bold17dialog));
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

        for (Sensor[] array : sensors)
            for (Sensor sensor : array)
                add(sensor.icon);

        roadPaneScroll.setVisible(false);
        roadPaneScroll.setSize(U3, U3);
        add(roadPaneScroll);
        for (Sensor[] array : sensors)
            for (Sensor sensor : array)
                add(sensor.balloon);
        citizens.forEach(citizen -> add(citizen.icon));
        roads.values().forEach(road -> {
//            add(road.balloon);
            locations.put(road.name, road);
        });
        buildings.values().forEach(building -> {
            add(building.icon);
            locations.put(building.name, building);
        });
        locationList.addAll(locations.values());
        roads.values().forEach(road -> add(road.icon));

        final int[] hOffset = { 5 };
        iconLabels.forEach(label -> {
            FontMetrics fm = getFontMetrics(label.getFont());
            label.setBounds(5, hOffset[0], label.getIcon().getIconWidth()+160,
                    Math.max(label.getIcon().getIconHeight(), fm.getHeight()));
            add(label);
            hOffset[0] += label.getHeight() + 5;
        });
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
    }

    private static Random random = new Random();
    public static Location getALocation(){
        return locationList.get(random.nextInt(locationList.size()));
    }

    public static Location getALocationExcept(Location location){
        if(location instanceof Building)
            return getALocation();

        Location res = getALocation();
        while(res instanceof Road && ((Road) location).sameAs((Road) res))
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
            int size = streets[7].icon.coord.w;
            int x = crossroads[0].icon.coord.x - size;
            int y = crossroads[0].icon.coord.y - size;
            int u = size + crossroads[0].icon.coord.w;
            x += (building.block % 4) * u;
            y += (building.block / 4) * u;
            building.icon.coord.x = x;
            building.icon.coord.y = y;
            building.icon.coord.w = building.icon.coord.h = size;
            building.icon.coord.centerX = x + size/2;
            building.icon.coord.centerY = y + size/2;
            building.icon.setBounds(x, y, size, size);
            building.icon.setIcon();

            switch (building.block) {
                case 0:
                    building.addrs.add(crossroads[0]);
                    break;
                case 1:
                    building.addrs.add(streets[0]);
                    building.addrs.add(streets[7]);
                    building.addrs.add(crossroads[0]);
                    building.addrs.add(crossroads[1]);
                    break;
                case 2:
                    building.addrs.add(streets[8]);
                    building.addrs.add(crossroads[1]);
                    break;
                case 3:
                    building.addrs.add(streets[1]);
                    break;
                case 4:
                    building.addrs.add(streets[6]);
                    building.addrs.add(streets[11]);
                    building.addrs.add(crossroads[0]);
                    building.addrs.add(crossroads[3]);
                    break;
                case 5:
                    building.addrs.add(streets[7]);
                    building.addrs.add(streets[11]);
                    building.addrs.add(streets[12]);
                    building.addrs.add(streets[15]);
                    building.addrs.add(crossroads[0]);
                    building.addrs.add(crossroads[1]);
                    building.addrs.add(crossroads[3]);
                    building.addrs.add(crossroads[4]);
                    break;
                case 6:
                    building.addrs.add(streets[8]);
                    building.addrs.add(streets[12]);
                    building.addrs.add(streets[13]);
                    building.addrs.add(streets[16]);
                    building.addrs.add(crossroads[1]);
                    building.addrs.add(crossroads[4]);
                    building.addrs.add(crossroads[5]);
                    break;
                case 7:
                    building.addrs.add(streets[13]);
                    building.addrs.add(crossroads[5]);
                    break;
                case 8:
                    building.addrs.add(streets[18]);
                    building.addrs.add(crossroads[3]);
                    break;
                case 9:
                    building.addrs.add(streets[15]);
                    building.addrs.add(streets[18]);
                    building.addrs.add(streets[19]);
                    building.addrs.add(streets[23]);
                    building.addrs.add(crossroads[3]);
                    building.addrs.add(crossroads[4]);
                    building.addrs.add(crossroads[7]);
                    break;
                case 10:
                    building.addrs.add(streets[16]);
                    building.addrs.add(streets[19]);
                    building.addrs.add(streets[20]);
                    building.addrs.add(streets[24]);
                    building.addrs.add(crossroads[4]);
                    building.addrs.add(crossroads[5]);
                    building.addrs.add(crossroads[7]);
                    building.addrs.add(crossroads[8]);
                    break;
                case 11:
                    building.addrs.add(streets[17]);
                    building.addrs.add(streets[20]);
                    building.addrs.add(crossroads[5]);
                    building.addrs.add(crossroads[8]);
                    break;
                case 12:
                    building.addrs.add(streets[22]);
                    break;
                case 13:
                    building.addrs.add(streets[23]);
                    building.addrs.add(crossroads[7]);
                    break;
                case 14:
                    building.addrs.add(streets[28]);
                    building.addrs.add(streets[24]);
                    building.addrs.add(crossroads[7]);
                    building.addrs.add(crossroads[8]);
                    break;
                case 15:
                    building.addrs.add(crossroads[8]);
                    break;
            }
            Set<Road> rs = new HashSet<>();
            for(Road road : building.addrs)
                rs.addAll(road.combined);
            building.addrs.addAll(rs);
        }
    }

    private static void initRoads() {
        for(int i = 0; i < crossroads.length; i++){
            crossroads[i].id = i;
            crossroads[i].name = "Crossroad " + i;
            roads.put(crossroads[i].name, crossroads[i]);
            crossroads[i].icon = new Road.Crossroad.CrossroadIcon();
            crossroads[i].icon.id = i;
            crossroads[i].icon.road = crossroads[i];
//			crossroads[i].icon.coord.x = (i%3+1)*u;
//			crossroads[i].icon.coord.y = (i/3+1)*u;
            crossroads[i].icon.coord.x = (SH+CW)/2 + SW + (i%3) * U1;
            crossroads[i].icon.coord.y = (SH+CW)/2 + SW + (i/3) * U1;
            crossroads[i].icon.coord.w = CW;
            crossroads[i].icon.coord.h = CW;
            crossroads[i].icon.coord.centerX = crossroads[i].icon.coord.x + crossroads[i].icon.coord.w/2;
            crossroads[i].icon.coord.centerY = crossroads[i].icon.coord.y + crossroads[i].icon.coord.h/2;
            crossroads[i].icon.setBounds(crossroads[i].icon.coord.x, crossroads[i].icon.coord.y,
                    crossroads[i].icon.coord.w, crossroads[i].icon.coord.h);
        }
        for(int i = 0;i < streets.length;i++){
            streets[i].id = i;
            streets[i].name = "Street " + i;
            roads.put(streets[i].name, streets[i]);
            streets[i].icon = new Road.Street.StreetIcon();
            streets[i].icon.id = i;
            streets[i].icon.road = streets[i];
            int quotient = i / 8;
            int remainder = i % 8;
            //vertical streets
            if(remainder > 1 && remainder < 6){
                ((Road.Street.StreetIcon)streets[i].icon).isVertical = true;
                streets[i].icon.coord.w = SH;
                streets[i].icon.coord.arcw = AW;
                streets[i].icon.coord.arch = AW;
                switch (quotient) {
                    case 0:case 2:
                        streets[i].icon.coord.x = (remainder-1) * U1;
                        streets[i].icon.coord.y = (quotient==0) ? 0 : U1*2+(SH+CW)/2;
                        if(streets[i].id == 21)
                            streets[i].icon.coord.y -= (SH+CW)/2;
                        break;
                    case 1:case 3:
                        streets[i].icon.coord.x = (remainder-2) * U1;
                        streets[i].icon.coord.y = quotient*U1+(SH+CW)/2;
                        if(streets[i].id == 10 || streets[i].id == 26)
                            streets[i].icon.coord.y -=(SH+CW)/2;
                        break;
                }

                if(quotient == 0)
                    streets[i].icon.coord.h = remainder != 5 ? U3 : U4;
                else if(quotient == 3)
                    streets[i].icon.coord.h = remainder != 2 ? U3 : U4;
                else if(i == 10 || i ==21)
                    streets[i].icon.coord.h = U4;
                else
                    streets[i].icon.coord.h = SW;
            }
            //horizontal streets
            else{
                ((Road.Street.StreetIcon)streets[i].icon).isVertical = false;
                streets[i].icon.coord.h = SH;
                streets[i].icon.coord.arcw = AW;
                streets[i].icon.coord.arch = AW;
                switch(remainder){
                    case 6:
                        streets[i].icon.coord.x = 0;
                        streets[i].icon.coord.y = (quotient+1) * U1;
                        break;
                    case 7:
                        streets[i].icon.coord.x = (remainder-6)*U1+(SH+CW)/2;
                        streets[i].icon.coord.y = (quotient+1) * U1;
                        if(streets[i].id == 31)
                            streets[i].icon.coord.x += SW+(CW-SH)/2;
                        break;
                    case 0:case 1:
                        if(streets[i].id > 1){
                            streets[i].icon.coord.x = (remainder+2)*U1+(SH+CW)/2;
                            streets[i].icon.coord.y = quotient * U1;
                        }
                        else{
                            streets[i].icon.coord.x = (remainder*2+1)*U1;
                            streets[i].icon.coord.y = 0;
                        }
                        break;
                }

                switch(remainder){
                    case 6:
                        streets[i].icon.coord.w = quotient != 3 ? U1-U2 : U4;
                        break;
                    case 7:
                        streets[i].icon.coord.w = i != 31 ? SW : U4;
                        break;
                    case 0:
                        streets[i].icon.coord.w = i != 0 ? SW : U4;
                        break;
                    case 1:
                        streets[i].icon.coord.w = quotient != 0 ? U3 : U4;
                        break;
                }
            }

            streets[i].icon.coord.centerX = streets[i].icon.coord.x + streets[i].icon.coord.w/2;
            streets[i].icon.coord.centerY = streets[i].icon.coord.y + streets[i].icon.coord.h/2;
            streets[i].icon.setBounds(streets[i].icon.coord.x, streets[i].icon.coord.y,
                    streets[i].icon.coord.w, streets[i].icon.coord.h);
        }

        combineRoads();
        setAdjs();
    }

    private static void initSensors(){
        for(int i = 0;i < sensors.length;i++)
            for(int j = 0;j < sensors[i].length;j++)
                sensors[i][j] = new Sensor(i, j);

        setSensor(sensors[0][0], crossroads[0], streets[2], NORTH);
        setSensor(sensors[0][1], crossroads[0], streets[6], WEST);
        setSensor(sensors[1][0], crossroads[2], streets[8], WEST);
        setSensor(sensors[1][1], crossroads[1], streets[3], SOUTH);
        setSensor(sensors[1][2], crossroads[1], streets[8], WEST);
        setSensor(sensors[2][0], crossroads[1], streets[7], WEST);
        setSensor(sensors[2][1], crossroads[3], streets[11], NORTH);
        setSensor(sensors[2][2], crossroads[0], streets[11], NORTH);
        setSensor(sensors[2][3], crossroads[0], streets[7], WEST);
        setSensor(sensors[3][0], crossroads[5], streets[16], EAST);
        setSensor(sensors[3][1], crossroads[1], streets[12], SOUTH);
        setSensor(sensors[3][2], crossroads[4], streets[12], SOUTH);
        setSensor(sensors[3][3], crossroads[4], streets[16], EAST);
        setSensor(sensors[4][0], crossroads[5], streets[13], NORTH);
        setSensor(sensors[4][1], crossroads[5], streets[17], EAST);
        setSensor(sensors[4][2], crossroads[2], streets[13], NORTH);
        setSensor(sensors[5][0], crossroads[3], streets[18], NORTH);
        setSensor(sensors[5][1], crossroads[3], streets[14], EAST);
        setSensor(sensors[5][2], crossroads[6], streets[18], NORTH);
        setSensor(sensors[6][0], crossroads[3], streets[15], EAST);
        setSensor(sensors[6][1], crossroads[7], streets[19], SOUTH);
        setSensor(sensors[6][2], crossroads[4], streets[15], EAST);
        setSensor(sensors[6][3], crossroads[4], streets[19], SOUTH);
        setSensor(sensors[7][0], crossroads[7], streets[24], WEST);
        setSensor(sensors[7][1], crossroads[8], streets[20], NORTH);
        setSensor(sensors[7][2], crossroads[5], streets[20], NORTH);
        setSensor(sensors[7][3], crossroads[8], streets[24], WEST);
        setSensor(sensors[8][0], crossroads[6], streets[23], WEST);
        setSensor(sensors[8][1], crossroads[7], streets[28], SOUTH);
        setSensor(sensors[8][2], crossroads[7], streets[23], WEST);
        setSensor(sensors[9][0], crossroads[8], streets[29], NORTH);
        setSensor(sensors[9][1], crossroads[8], streets[25], WEST);

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

    private static void setSensor(Sensor sensor, Road.Crossroad crossroad, Road.Street street, int dir){
        sensor.dir = TrafficMap.DIRECTION ? dir : oppositeDirOf(dir);
        sensor.crossroad = crossroad;
        sensor.street = street;

        Road road = roadBehind(sensor);
        if(road instanceof Road.Crossroad){
            sensor.isEntrance = true;
            sensor.nextRoad = sensor.crossroad;
            sensor.prevRoad = sensor.street;
            sensor.street.dir[0] = sensor.dir;
        }
        else{
            sensor.isEntrance = false;
            sensor.nextRoad = sensor.street;
            sensor.prevRoad = sensor.crossroad;
            if(sensor.crossroad.dir[0] == UNKNOWN_DIR)
                sensor.crossroad.dir[0] = sensor.dir;
            else
                sensor.crossroad.dir[1] = sensor.dir;
        }

        sensor.prevRoad.adjSensors.put(sensor.dir, sensor);
        if(sensor.nextRoad.isCombined() && sensor.nextRoad instanceof Road.Street)
            sensor.nextRoad.adjSensors.put(oppositeDirOf(sensor.nextRoad.dir[0]), sensor);
        else
            sensor.nextRoad.adjSensors.put(oppositeDirOf(sensor.dir), sensor);

        if(sensor.crossroad.icon.coord.x-sensor.street.icon.coord.x == sensor.street.icon.coord.w){
            sensor.showPos = 0;
            sensor.px = sensor.crossroad.icon.coord.x;
            sensor.py = sensor.crossroad.icon.coord.y + sensor.crossroad.icon.coord.h/2;
        }
        else if(sensor.crossroad.icon.coord.y-sensor.street.icon.coord.y == sensor.street.icon.coord.h){
            sensor.showPos = 1;
            sensor.px = sensor.crossroad.icon.coord.x + sensor.crossroad.icon.coord.w/2;
            sensor.py = sensor.crossroad.icon.coord.y;
        }
        else if(sensor.street.icon.coord.x-sensor.crossroad.icon.coord.x == sensor.crossroad.icon.coord.w){
            sensor.showPos = 2;
            sensor.px = sensor.street.icon.coord.x;
            sensor.py = sensor.crossroad.icon.coord.y + sensor.crossroad.icon.coord.h/2;
        }
        else if(sensor.street.icon.coord.y-sensor.crossroad.icon.coord.y == sensor.crossroad.icon.coord.h){
            sensor.showPos = 3;
            sensor.px = sensor.crossroad.icon.coord.x + sensor.crossroad.icon.coord.w/2;
            sensor.py = sensor.street.icon.coord.y;
        }

        FontMetrics fm = sensor.icon.getFontMetrics(sensor.icon.getFont());
        int w = fm.stringWidth(sensor.icon.getText())+8, h = fm.getHeight();
        sensor.icon.setBounds(sensor.px-w/2, sensor.py-h/2, w, h);
    }

    private static void setAdjs(){
        crossroads[0].adjRoads.put(0, streets[2]);
        crossroads[0].adjRoads.put(1, streets[11]);
        crossroads[0].adjRoads.put(2, streets[6]);
        crossroads[0].adjRoads.put(3, streets[7]);
        crossroads[1].adjRoads.put(0, streets[3]);
        crossroads[1].adjRoads.put(1, streets[12]);
        crossroads[1].adjRoads.put(2, streets[7]);
        crossroads[1].adjRoads.put(3, streets[8]);
        crossroads[2].adjRoads.put(1, streets[13]);
        crossroads[2].adjRoads.put(2, streets[8]);
        crossroads[3].adjRoads.put(0, streets[11]);
        crossroads[3].adjRoads.put(1, streets[18]);
        crossroads[3].adjRoads.put(2, streets[14]);
        crossroads[3].adjRoads.put(3, streets[15]);
        crossroads[4].adjRoads.put(0, streets[12]);
        crossroads[4].adjRoads.put(1, streets[19]);
        crossroads[4].adjRoads.put(2, streets[15]);
        crossroads[4].adjRoads.put(3, streets[16]);
        crossroads[5].adjRoads.put(0, streets[13]);
        crossroads[5].adjRoads.put(1, streets[20]);
        crossroads[5].adjRoads.put(2, streets[16]);
        crossroads[5].adjRoads.put(3, streets[17]);
        crossroads[6].adjRoads.put(0, streets[18]);
        crossroads[6].adjRoads.put(3, streets[23]);
        crossroads[7].adjRoads.put(0, streets[19]);
        crossroads[7].adjRoads.put(1, streets[28]);
        crossroads[7].adjRoads.put(2, streets[23]);
        crossroads[7].adjRoads.put(3, streets[24]);
        crossroads[8].adjRoads.put(0, streets[20]);
        crossroads[8].adjRoads.put(1, streets[29]);
        crossroads[8].adjRoads.put(2, streets[24]);
        crossroads[8].adjRoads.put(3, streets[25]);

        //TODO when city direction is reversed, this will be wrong
        if(TrafficMap.DIRECTION){
            streets[0].adjRoads.put(0, crossroads[0]);
            streets[0].adjRoads.put(1, crossroads[1]);
            streets[6].adjRoads.put(2, crossroads[0]);
            streets[6].adjRoads.put(3, crossroads[3]);
            streets[17].adjRoads.put(2, crossroads[8]);
            streets[17].adjRoads.put(3, crossroads[5]);
            streets[28].adjRoads.put(0, crossroads[8]);
            streets[28].adjRoads.put(1, crossroads[7]);
        }
        else{
            streets[0].adjRoads.put(1, crossroads[0]);
            streets[0].adjRoads.put(0, crossroads[1]);
            streets[6].adjRoads.put(3, crossroads[0]);
            streets[6].adjRoads.put(2, crossroads[3]);
            streets[17].adjRoads.put(3, crossroads[8]);
            streets[17].adjRoads.put(2, crossroads[5]);
            streets[28].adjRoads.put(1, crossroads[8]);
            streets[28].adjRoads.put(0, crossroads[7]);
        }

        for(int i = 0;i < 3;i++)
            for(int j = 0;j < 2;j++){
                streets[7+8*i+j].adjRoads.put(2, crossroads[3*i+j]);
                streets[7+8*i+j].adjRoads.put(3, crossroads[1+3*i+j]);
                streets[11+7*j+i].adjRoads.put(0, crossroads[i+3*j]);
                streets[11+7*j+i].adjRoads.put(1, crossroads[3+i+3*j]);
            }

        setAccess(crossroads[0], 7, 6, 11, 2);
        setAccess(crossroads[1], 8, 7, 3, 12);
        setAccess(crossroads[2], 13, 8);
        setAccess(crossroads[3], 14, 15, 18, 11);
        setAccess(crossroads[4], 15, 16, 12, 19);
        setAccess(crossroads[5], 16, 17, 20, 13);
        setAccess(crossroads[6], 23, 18);
        setAccess(crossroads[7], 24, 23, 19, 28);
        setAccess(crossroads[8], 25, 24, 29, 20);
        setAccess(streets[0], 0, 1);
        setAccess(streets[6], 0, 3);
        setAccess(streets[7], 1, 0);
        setAccess(streets[8], 2, 1);
        setAccess(streets[11], 3, 0);
        setAccess(streets[12], 1, 4);
        setAccess(streets[13], 5, 2);
        setAccess(streets[15], 3, 4);
        setAccess(streets[16], 4, 5);
        setAccess(streets[17], 5, 8);
        setAccess(streets[18], 6, 3);
        setAccess(streets[19], 4, 7);
        setAccess(streets[20], 8, 5);
        setAccess(streets[23], 7, 6);
        setAccess(streets[24], 8, 7);
        setAccess(streets[28], 7, 8);
    }

    private static void setAccess(Road road, int entry1, int exit1, int entry2, int exit2){
        setAccess(road, entry1, exit1);
        setAccess(road, entry2, exit2);
    }

    private static void setAccess(Road road, int entry, int exit){
        Road in, out;
        if(road instanceof Road.Crossroad){
            in = streets[entry];
            out = streets[exit];
        }
        else{
            in = crossroads[entry];
            out = crossroads[exit];
        }

        if(TrafficMap.DIRECTION){
            road.entrance2exit.put(in, out);
            road.exit2entrance.put(out, in);
        }
        else{
            road.entrance2exit.put(out, in);
            road.exit2entrance.put(in, out);
        }
    }

    private static void combineRoads(){
        Set<Road> roads = new HashSet<>();
        roads.add(streets[0]);
        roads.add(streets[2]);
        roads.add(streets[3]);
        Road.combine(roads);

        roads.clear();
        roads.add(streets[6]);
        roads.add(streets[10]);
        roads.add(streets[14]);
        Road.combine(roads);

        roads.clear();
        roads.add(streets[17]);
        roads.add(streets[21]);
        roads.add(streets[25]);
        Road.combine(roads);

        roads.clear();
        roads.add(streets[28]);
        roads.add(streets[29]);
        roads.add(streets[31]);
        Road.combine(roads);

        roads.clear();
        roads.add(streets[1]);
        roads.add(streets[4]);
        roads.add(streets[5]);
        roads.add(streets[9]);
        roads.add(crossroads[2]);
        Road.combine(roads);

        roads.clear();
        roads.add(streets[22]);
        roads.add(streets[26]);
        roads.add(streets[27]);
        roads.add(streets[30]);
        roads.add(crossroads[6]);
        Road.combine(roads);
    }

    public static void checkRealCrash() {
        List<Road> set = new LinkedList<>(roads.values());
        while (!set.isEmpty()) {
            Road road = set.remove(0);
            road.checkRealCrash();

            set.remove(road);
            set.removeAll(road.combined);
        }
    }

    public static void updateRoadInfoPane(Location loc) {
        synchronized (roadPane) {
            roadPane.setText("");
            if (loc == null)
                return;

            if (loc instanceof Building)
                Dashboard.append2pane(loc.name, Color.BLACK, roadPane);
            else if (loc instanceof Road) {
                List<Pair<String, Color>> strings = new ArrayList<>();
                strings.add(new Pair<>(loc.name, Color.BLACK));

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
                    strings.add(new Pair<>(car.name, car.icon.color));
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
                    strings.add(new Pair<>(sb.toString(), Color.BLACK));
                });
                Dashboard.append2pane(strings, roadPane);

//                StringBuilder sb = new StringBuilder();
//                sb.append(road.name).append("\n");
//                if (road.getPermitted() != null) {
//                    sb.append("Permitted Car:\n");
//                    sb.append(road.getPermitted().name).append("\n");
//                }
//                if (!road.cars.isEmpty()) {
//                    sb.append("Cars:\n");
//                    for (Car car : road.cars) {
//                        sb.append(car.name).append("\n");
//                    }
//                }
//                if (!road.waiting.isEmpty()) {
//                    sb.append("Waiting Cars:\n");
//                    for (Car car : road.waiting)
//                        sb.append(car.name).append("\n");
//                }
//                if (!road.realCars.isEmpty()) {
//                    sb.append("Real Cars:\n");
//                    for (Car car : road.realCars) {
//                        sb.append(car.name).append("\n");
//                    }
//                }
//                roadPane.setText(sb.toString());
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

    public static Road roadBehind(Sensor sensor){
        int bid = sensor.bid, id = sensor.sid;
        switch(bid){
            case 0:
                return TrafficMap.DIRECTION ? sensor.street : sensor.crossroad;
            case 1:
                return ((id == 0) ^ TrafficMap.DIRECTION) ? sensor.crossroad : sensor.street;
            case 2:
            case 4:
                return ((id < 2) ^ TrafficMap.DIRECTION) ? sensor.crossroad : sensor.street;
            case 3:
            case 7:
                return ((id % 2 == 1) ^ TrafficMap.DIRECTION) ? sensor.crossroad : sensor.street;
            case 5:
                return ((id > 1) ^ TrafficMap.DIRECTION) ? sensor.crossroad : sensor.street;
            case 6:
                return ((id == 0 || id == 3) ^ TrafficMap.DIRECTION) ? sensor.crossroad : sensor.street;
            case 8:
                return ((id > 0) ^ TrafficMap.DIRECTION) ? sensor.crossroad : sensor.street;
            case 9:
                return TrafficMap.DIRECTION ? sensor.crossroad : sensor.street;
        }
        return null;
    }

    public static class Coord{
        public int x, y, w, h;
        public int arcw ,arch;
        public int centerX, centerY;
    }
}