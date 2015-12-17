package nju.ics.lixiaofan.city;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.Section.BallonIcon;
import nju.ics.lixiaofan.city.Section.Crossing;
import nju.ics.lixiaofan.city.Section.Crossing.CrossingIcon;
import nju.ics.lixiaofan.city.Section.Street;
import nju.ics.lixiaofan.city.Section.Street.StreetIcon;
import nju.ics.lixiaofan.sensor.Sensor;

public class TrafficMap extends JPanel{
	private static final long serialVersionUID = 1L;
	public static ConcurrentHashMap<String, Car> cars = new ConcurrentHashMap<String, Car>();
	public static Crossing[] crossings = new Crossing[9];
	public static Street[] streets = new Street[32];
	public static Map<String, Section> sections = new HashMap<String, Section>();
	public static List<List<Sensor>> sensors = new ArrayList<List<Sensor>>();
	public static List<Citizen> citizens = new ArrayList<Citizen>();
	public static ConcurrentHashMap<Building.Type, Building> buildings = new ConcurrentHashMap<Building.Type, Building>();
	public static boolean showSensors = false, showSections = false;
	public final static boolean dir = true;
	
	public static final int sh = 37;//street height
	private static final int sw = sh * 2;//street width
	private static final int cw = (int) (sh * 1.5);//crossing width
	private static final int aw = cw / 2;
	private static final int u = cw + sw;
	private static final int u2 = (cw-sh)/2;
	private static final int u3 = sw+(cw+sh)/2;
	private static final int u4 = u+sh;
	public static final int size = 4*(sw+cw)+sh;

	public TrafficMap() {
		setLayout(null);
		setPreferredSize(new Dimension(size, size));
		initSections();
		initSensors();
		
		for(Citizen c : citizens){
			add(c.icon);
			new Thread(c).start();
		}
		for(Section s : sections.values()){
			add(s.balloon);
		}
		for(Building b : buildings.values()){
			add(b.icon);
			placeBuilding(b);
		}
		for(Section s : sections.values()){
			add(s.icon);
		}
	}
	
	protected void paintChildren(Graphics g) {
		super.paintChildren(g);
		//draw sensors
		if(showSensors){
			g.setColor(Color.BLACK);
			for(List<Sensor> slist : sensors)
				for(Sensor s : slist){
					switch (s.showPos) {
					case 0:
						g.drawString("B"+s.bid+"S"+(s.sid+1), s.px-15, s.py-28);
						g.drawLine(s.px, s.py-25, s.px, s.py);
						break;
					case 1:
						g.drawString("B"+s.bid+"S"+(s.sid+1), s.px+27, s.py+4);
						g.drawLine(s.px, s.py, s.px+25, s.py);
						break;
					case 2:
						g.drawString("B"+s.bid+"S"+(s.sid+1), s.px-15, s.py+37);
						g.drawLine(s.px, s.py, s.px, s.py+25);
						break;
					case 3:
						g.drawString("B"+s.bid+"S"+(s.sid+1), s.px-57, s.py+4);
						g.drawLine(s.px-25, s.py, s.px, s.py);
						break;
					default:
						break;
					}
				}
		}
	}
	
	private void placeBuilding(Building building){
		if(building == null || building.block < 0 || building.block > 15)
			return;
		int size = streets[7].icon.coord.w;
		int x = crossings[0].icon.coord.x - size;
		int y = crossings[0].icon.coord.y - size;
		int u = size + crossings[0].icon.coord.w;
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
			building.addrs.add(streets[2]);
			building.addrs.add(streets[6]);
			building.addrs.add(crossings[0]);
			break;
		case 1:
			building.addrs.add(streets[0]);
			building.addrs.addAll(streets[0].combined);
			building.addrs.add(streets[7]);
			building.addrs.add(crossings[0]);
			building.addrs.add(crossings[1]);
			break;
		case 2:
			building.addrs.add(streets[3]);
			building.addrs.add(streets[4]);
			building.addrs.add(streets[8]);
			building.addrs.add(crossings[1]);
			building.addrs.add(crossings[2]);
			break;
		case 3:
			building.addrs.add(streets[1]);
			building.addrs.addAll(streets[1].combined);
			break;
		case 4:
			building.addrs.add(streets[6]);
			building.addrs.addAll(streets[6].combined);
			building.addrs.add(streets[11]);
			building.addrs.add(crossings[0]);
			building.addrs.add(crossings[3]);
			break;
		case 5:case 6:case 9:case 10:{
			int a = building.block / 9;
			int b = 1 - (building.block % 2);
			int offset = 8*a+b;
			building.addrs.add(streets[7+offset]);
			building.addrs.add(streets[10+offset]);
			building.addrs.add(streets[11+offset]);
			building.addrs.add(streets[15+offset]);
			offset = 3*a+b;
			building.addrs.add(crossings[offset]);
			if(offset != 1)
				building.addrs.add(crossings[1+offset]);
			if(offset != 3)
				building.addrs.add(crossings[3+offset]);
			building.addrs.add(crossings[4+offset]);
		}
			break;
		case 7:
			building.addrs.add(streets[9]);
			building.addrs.add(streets[13]);
			building.addrs.add(streets[17]);
			building.addrs.add(crossings[2]);
			building.addrs.add(crossings[5]);
			break;
		case 8:
			building.addrs.add(streets[14]);
			building.addrs.add(streets[18]);
			building.addrs.add(streets[22]);
			building.addrs.add(crossings[3]);
			building.addrs.add(crossings[6]);
			break;
		case 11:
			building.addrs.add(streets[17]);
			building.addrs.addAll(streets[17].combined);
			building.addrs.add(streets[20]);
			building.addrs.add(crossings[5]);
			building.addrs.add(crossings[8]);
			break;
		case 12:
			building.addrs.add(streets[22]);
			building.addrs.addAll(streets[22].combined);
			break;
		case 13:
			building.addrs.add(streets[27]);
			building.addrs.add(streets[28]);
			building.addrs.add(streets[23]);
			building.addrs.add(crossings[6]);
			building.addrs.add(crossings[7]);
			break;
		case 14:
			building.addrs.add(streets[28]);
			building.addrs.addAll(streets[28].combined);
			building.addrs.add(streets[24]);
			building.addrs.add(crossings[7]);
			building.addrs.add(crossings[8]);
			break;
		case 15:
			building.addrs.add(streets[25]);
			building.addrs.add(streets[29]);
			building.addrs.add(crossings[8]);
			break;
		default:
			break;
		}
		Set<Section> newS = new HashSet<Section>();
		for(Section s : building.addrs)
			if(s.isCombined)
				newS.addAll(s.combined);
		building.addrs.addAll(newS);
	}
	
	public static void initSections() {
		for(int i = 0;i < 9;i++){
			crossings[i] = new Crossing();
			crossings[i].id = i;
			crossings[i].name = "Crossing "+i;
			sections.put(crossings[i].name, crossings[i]);
			crossings[i].icon = new CrossingIcon();
			crossings[i].icon.id = i;
			crossings[i].icon.section = crossings[i];
//			crossings[i].icon.coord.x = (i%3+1)*u;
//			crossings[i].icon.coord.y = (i/3+1)*u;
			crossings[i].icon.coord.x = (sh+cw)/2 + sw + (i%3) * u;
			crossings[i].icon.coord.y = (sh+cw)/2 + sw + (i/3) * u;
			crossings[i].icon.coord.w = cw;
			crossings[i].icon.coord.h = cw;
			crossings[i].icon.coord.centerX = crossings[i].icon.coord.x + crossings[i].icon.coord.w/2;
			crossings[i].icon.coord.centerY = crossings[i].icon.coord.y + crossings[i].icon.coord.h/2;
			crossings[i].icon.setBounds(crossings[i].icon.coord.x, crossings[i].icon.coord.y,
					crossings[i].icon.coord.w, crossings[i].icon.coord.h);
		}
		for(int i = 0;i < 32;i++){
			streets[i] = new Street();
			streets[i].id = i;
			streets[i].name = "Street "+i;
			sections.put(streets[i].name, streets[i]);
			streets[i].icon = new StreetIcon();
			streets[i].icon.id = i;
			streets[i].icon.section = streets[i];
			int quotient = i / 8;
			int remainder = i % 8;
			//vertical streets
			if(remainder > 1 && remainder < 6){
				((StreetIcon )streets[i].icon).isVertical = true;
				streets[i].icon.coord.w = sh;
				streets[i].icon.coord.arcw = aw;
				streets[i].icon.coord.arch = aw;
				switch (quotient) {
				case 0:case 2:
					streets[i].icon.coord.x = (remainder-1) * u;
					streets[i].icon.coord.y = (quotient==0) ? 0 : u*2+(sh+cw)/2;
					if(streets[i].id == 21)
						streets[i].icon.coord.y -= (sh+cw)/2;
					break;
				case 1:case 3:
					streets[i].icon.coord.x = (remainder-2) * u;
					streets[i].icon.coord.y = quotient*u+(sh+cw)/2;
					if(streets[i].id == 10 || streets[i].id == 26)
						streets[i].icon.coord.y -=(sh+cw)/2;
					break;
				}
				
				if(quotient == 0)
					streets[i].icon.coord.h = remainder != 5 ? u3 : u4;
				else if(quotient == 3)
					streets[i].icon.coord.h = remainder != 2 ? u3 : u4;
				else if(i == 10 || i ==21)
					streets[i].icon.coord.h = u4;
				else
					streets[i].icon.coord.h = sw;
			}
			//horizontal streets
			else{
				((StreetIcon )streets[i].icon).isVertical = false;
				streets[i].icon.coord.h = sh;
				streets[i].icon.coord.arcw = aw;
				streets[i].icon.coord.arch = aw;
				switch(remainder){
				case 6:
					streets[i].icon.coord.x = 0;
					streets[i].icon.coord.y = (quotient+1) * u;
					break;
				case 7:
					streets[i].icon.coord.x = (remainder-6)*u+(sh+cw)/2;
					streets[i].icon.coord.y = (quotient+1) * u;
					if(streets[i].id == 31)
						streets[i].icon.coord.x += sw+(cw-sh)/2;
					break;
				case 0:case 1:
					if(streets[i].id > 1){
						streets[i].icon.coord.x = (remainder+2)*u+(sh+cw)/2;
						streets[i].icon.coord.y = quotient * u;
					}
					else{
						streets[i].icon.coord.x = (remainder*2+1)*u;
						streets[i].icon.coord.y = 0;
					}
					break;
				}
				
				switch(remainder){
				case 6:
					streets[i].icon.coord.w = quotient != 3 ? u-u2 : u4;
					break;
				case 7:
					streets[i].icon.coord.w = i != 31 ? sw : u4;
					break;
				case 0:
					streets[i].icon.coord.w = i != 0 ? sw : u4;
					break;
				case 1:
					streets[i].icon.coord.w = quotient != 0 ? u3 : u4;
					break;
				}
			}
			
			streets[i].icon.coord.centerX = streets[i].icon.coord.x + streets[i].icon.coord.w/2;
			streets[i].icon.coord.centerY = streets[i].icon.coord.y + streets[i].icon.coord.h/2;
			streets[i].icon.setBounds(streets[i].icon.coord.x, streets[i].icon.coord.y, 
					streets[i].icon.coord.w, streets[i].icon.coord.h);
		}
		
		for(Section section : sections.values()){
			section.balloon = new Section.BallonIcon();
			section.balloon.setBounds(section.icon.coord.centerX
					- BallonIcon.WIDTH / 2, section.icon.coord.centerY
					- BallonIcon.HEIGHT, BallonIcon.WIDTH, BallonIcon.HEIGHT);
		}
		
		setCombined();
		setAdjs();
	}
	
	public static void initSensors(){
		for(int i = 0;i < 10;i++){
			ArrayList<Sensor> loc = new ArrayList<Sensor>();
			sensors.add(loc);
			loc.add(new Sensor());
			loc.add(new Sensor());
			if(i > 0 && i < 9){
				loc.add(new Sensor());
				if(i == 2 || i == 3 || i == 6 || i == 7)
					loc.add(new Sensor());
			}
		}
		setSensor(0, 0, 0, 2, 0);
		setSensor(0, 1, 0, 6, 2);
		setSensor(1, 0, 2, 8, 2);
		setSensor(1, 1, 1, 3, 1);
		setSensor(1, 2, 1, 8, 2);
		
		setSensor(2, 0, 1, 7, 2);
		setSensor(2, 1, 3, 11, 0);
		setSensor(2, 2, 0, 11, 0);
		setSensor(2, 3, 0, 7, 2);
		
		setSensor(3, 0, 5, 16, 3);
		setSensor(3, 1, 1, 12, 1);
		setSensor(3, 2, 4, 12, 1);
		setSensor(3, 3, 4, 16, 3);
		
		setSensor(4, 0, 5, 13, 0);
		setSensor(4, 1, 5, 17, 3);
		setSensor(4, 2, 2, 13, 0);
		setSensor(5, 0, 3, 18, 0);
		setSensor(5, 1, 3, 14, 3);
		setSensor(5, 2, 6, 18, 0);
		
		setSensor(6, 0, 3, 15, 3);
		setSensor(6, 1, 7, 19, 1);
		setSensor(6, 2, 4, 15, 3);
		setSensor(6, 3, 4, 19, 1);
		
		setSensor(7, 0, 7, 24, 2);
		setSensor(7, 1, 8, 20, 0);
		setSensor(7, 2, 5, 20, 0);
		setSensor(7, 3, 8, 24, 2);
	
		setSensor(8, 0, 6, 23, 2);
		setSensor(8, 1, 7, 28, 1);
		setSensor(8, 2, 7, 23, 2);
		setSensor(9, 0, 8, 29, 0);
		setSensor(9, 1, 8, 25, 2);
		
		Queue<Sensor> orders = new LinkedList<Sensor>();
		orders.add(sensors.get(0).get(0));
		orders.add(sensors.get(1).get(1));
		orders.add(sensors.get(3).get(1));
		orders.add(sensors.get(3).get(2));
		orders.add(sensors.get(6).get(3));
		orders.add(sensors.get(6).get(1));
		orders.add(sensors.get(8).get(1));
		orders.add(sensors.get(9).get(0));
		orders.add(sensors.get(7).get(1));
		orders.add(sensors.get(7).get(2));
		orders.add(sensors.get(4).get(0));
		orders.add(sensors.get(4).get(2));
		orders.add(sensors.get(1).get(0));
		orders.add(sensors.get(1).get(2));
		orders.add(sensors.get(2).get(0));
		orders.add(sensors.get(2).get(3));
		orders.add(sensors.get(0).get(1));
		orders.add(sensors.get(5).get(1));
		orders.add(sensors.get(6).get(0));
		orders.add(sensors.get(6).get(2));
		orders.add(sensors.get(3).get(3));
		orders.add(sensors.get(3).get(0));
		orders.add(sensors.get(4).get(1));
		orders.add(sensors.get(9).get(1));
		orders.add(sensors.get(7).get(3));
		orders.add(sensors.get(7).get(0));
		orders.add(sensors.get(8).get(2));
		orders.add(sensors.get(8).get(0));
		orders.add(sensors.get(5).get(2));
		orders.add(sensors.get(5).get(0));
		orders.add(sensors.get(2).get(1));
		orders.add(sensors.get(2).get(2));
		orders.add(sensors.get(0).get(0));
		
		Sensor prev = orders.poll(), next;
		while(!orders.isEmpty()){
			next = orders.poll();
			if(TrafficMap.dir){
				prev.nextSensor = next;
				next.prevSensor = prev;
			}
			else{
				prev.prevSensor = next;
				next.nextSensor = prev;
			}
			prev = next;
		}
	}

	private static void setSensor(int cid, int sid, int c, int s, int dir){
		Sensor sensor = sensors.get(cid).get(sid);
		sensor.bid = cid;
		sensor.sid = sid;
		sensor.state = 2;
		sensor.crossing = crossings[c];
		sensor.street = streets[s];
		crossings[c].sensors.add(sensor);
		streets[s].sensors.add(sensor);
		
		if(sensor.crossing.icon.coord.x-sensor.street.icon.coord.x == sensor.street.icon.coord.w){
			sensor.showPos = 0;
			sensor.px = sensor.crossing.icon.coord.x;
			sensor.py = sensor.crossing.icon.coord.y + sensor.crossing.icon.coord.h/2;
		}
		else if(sensor.crossing.icon.coord.y-sensor.street.icon.coord.y == sensor.street.icon.coord.h){
			sensor.showPos = 1;
			sensor.px = sensor.crossing.icon.coord.x + sensor.crossing.icon.coord.w/2;
			sensor.py = sensor.crossing.icon.coord.y;
		}
		else if(sensor.street.icon.coord.x-sensor.crossing.icon.coord.x == sensor.crossing.icon.coord.w){
			sensor.showPos = 2;
			sensor.px = sensor.street.icon.coord.x;
			sensor.py = sensor.crossing.icon.coord.y + sensor.crossing.icon.coord.h/2;
		}
		else if(sensor.street.icon.coord.y-sensor.crossing.icon.coord.y == sensor.crossing.icon.coord.h){
			sensor.showPos = 3;
			sensor.px = sensor.crossing.icon.coord.x + sensor.crossing.icon.coord.w/2;
			sensor.py = sensor.street.icon.coord.y;
		}
		
		if(TrafficMap.dir)
			sensor.dir = dir;
		else if(dir < 2)
			sensor.dir = 1 - dir;
		else
			sensor.dir = 5 - dir;
		
		Section section = sectionBehind(sensor);
		if(section == sensor.crossing){
			sensor.isEntrance = true;
			sensor.nextSection = sensor.crossing;
			sensor.prevSection = sensor.street;
			sensor.street.dir[0] = sensor.dir;
//			if(sensor.street.isCombined){
//				for(Section s1: sensor.street.combined)
//					s1.dir[0] = sensor.dir;
//			}
		}
		else{
			sensor.isEntrance = false;
			sensor.nextSection = sensor.street;
			sensor.prevSection = sensor.crossing;
			if(sensor.crossing.dir[0] < 0)
				sensor.crossing.dir[0] = sensor.dir;
			else
				sensor.crossing.dir[1] = sensor.dir;
			
//			if(sensor.crossing.isCombined){
//				for(Section s1: sensor.crossing.combined)
//					s1.dir[0] = sensor.dir;
//			}
		}
	}
	
	private static void setAdjs(){
		crossings[0].adjs.put(0, streets[2]);
		crossings[0].adjs.put(1, streets[11]);
		crossings[0].adjs.put(2, streets[6]);
		crossings[0].adjs.put(3, streets[7]);
		crossings[1].adjs.put(0, streets[3]);
		crossings[1].adjs.put(1, streets[12]);
		crossings[1].adjs.put(2, streets[7]);
		crossings[1].adjs.put(3, streets[8]);
		crossings[2].adjs.put(1, streets[13]);
		crossings[2].adjs.put(2, streets[8]);
		crossings[3].adjs.put(0, streets[11]);
		crossings[3].adjs.put(1, streets[18]);
		crossings[3].adjs.put(2, streets[14]);
		crossings[3].adjs.put(3, streets[15]);
		crossings[4].adjs.put(0, streets[12]);
		crossings[4].adjs.put(1, streets[19]);
		crossings[4].adjs.put(2, streets[15]);
		crossings[4].adjs.put(3, streets[16]);
		crossings[5].adjs.put(0, streets[13]);
		crossings[5].adjs.put(1, streets[20]);
		crossings[5].adjs.put(2, streets[16]);
		crossings[5].adjs.put(3, streets[17]);
		crossings[6].adjs.put(0, streets[18]);
		crossings[6].adjs.put(3, streets[23]);
		crossings[7].adjs.put(0, streets[19]);
		crossings[7].adjs.put(1, streets[28]);
		crossings[7].adjs.put(2, streets[23]);
		crossings[7].adjs.put(3, streets[24]);
		crossings[8].adjs.put(0, streets[20]);
		crossings[8].adjs.put(1, streets[29]);
		crossings[8].adjs.put(2, streets[24]);
		crossings[8].adjs.put(3, streets[25]);
		
		//TODO when sys dir is reversed, it's wrong
		if(TrafficMap.dir){
			streets[0].adjs.put(0, crossings[0]);
			streets[0].adjs.put(1, crossings[1]);
			streets[6].adjs.put(2, crossings[0]);
			streets[6].adjs.put(3, crossings[3]);
			streets[17].adjs.put(2, crossings[8]);
			streets[17].adjs.put(3, crossings[5]);
			streets[28].adjs.put(0, crossings[8]);
			streets[28].adjs.put(1, crossings[7]);
		}
		else{
			streets[0].adjs.put(1, crossings[0]);
			streets[0].adjs.put(0, crossings[1]);
			streets[6].adjs.put(3, crossings[0]);
			streets[6].adjs.put(2, crossings[3]);
			streets[17].adjs.put(3, crossings[8]);
			streets[17].adjs.put(2, crossings[5]);
			streets[28].adjs.put(1, crossings[8]);
			streets[28].adjs.put(0, crossings[7]);
		}
		
		for(int i = 0;i < 3;i++)
			for(int j = 0;j < 2;j++){
				streets[7+8*i+j].adjs.put(2, crossings[3*i+j]);
				streets[7+8*i+j].adjs.put(3, crossings[1+3*i+j]);
				streets[11+7*j+i].adjs.put(0, crossings[i+3*j]);
				streets[11+7*j+i].adjs.put(1, crossings[3+i+3*j]);
			}
		
		setAccess(crossings[0], 7, 6, 11, 2);
		setAccess(crossings[1], 8, 7, 3, 12);
		setAccess(crossings[2], 13, 8);
		setAccess(crossings[3], 14, 15, 18, 11);
		setAccess(crossings[4], 15, 16, 12, 19);
		setAccess(crossings[5], 16, 17, 20, 13);
		setAccess(crossings[6], 23, 18);
		setAccess(crossings[7], 24, 23, 19, 28);
		setAccess(crossings[8], 25, 24, 29, 20);
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
	
	private static void setAccess(Section s, int entry1, int exit1, int entry2, int exit2){
		setAccess(s, entry1, exit1);
		setAccess(s, entry2, exit2);
	}
	
	private static void setAccess(Section s, int entry, int exit){
		Section in, out;
		if(s instanceof Crossing){
			in = streets[entry];
			out = streets[exit];
		}
		else{
			in = crossings[entry];
			out = crossings[exit];
		}
			
		if(TrafficMap.dir){
			s.exits.put(in, out);
			s.entrances.put(out, in);
		}
		else{
			s.exits.put(out, in);
			s.entrances.put(in, out);
		}
	}
	
	private static void setCombined(){
		Set<Section> sections = new HashSet<Section>();
		sections.add(streets[0]);
		sections.add(streets[2]);
		sections.add(streets[3]);
		setCombined(sections);
		
		sections.clear();
		sections.add(streets[6]);
		sections.add(streets[10]);
		sections.add(streets[14]);
		setCombined(sections);
		
		sections.clear();
		sections.add(streets[17]);
		sections.add(streets[21]);
		sections.add(streets[25]);
		setCombined(sections);
		
		sections.clear();
		sections.add(streets[28]);
		sections.add(streets[29]);
		sections.add(streets[31]);
		setCombined(sections);
		
		sections.clear();
		sections.add(streets[1]);
		sections.add(streets[4]);
		sections.add(streets[5]);
		sections.add(streets[9]);
		sections.add(crossings[2]);
		setCombined(sections);
		
		sections.clear();
		sections.add(streets[22]);
		sections.add(streets[26]);
		sections.add(streets[27]);
		sections.add(streets[30]);
		sections.add(crossings[6]);
		setCombined(sections);
	}
	
	private static void setCombined(Set<Section> sections){
		for(Section s : sections){
			s.isCombined = true;
			if(s.combined == null)
				s.combined = new HashSet<Section>();
			for(Section other : sections)
				if(other != s){
					s.combined.add(other);
					other.cars = s.cars;
					other.mutex = s.mutex;
					other.permitted = s.permitted;
					other.waiting = s.waiting;
					other.adjs = s.adjs;
					other.exits = s.exits;
					other.entrances = s.entrances;
					other.dir = s.dir;
					other.sensors = s.sensors;
				}
		}
	}
	
	private static Section sectionBehind(Sensor sensor){
		int bid = sensor.bid, id = sensor.sid;
		
		switch(bid){
		case 0:
			return TrafficMap.dir ? sensor.street : sensor.crossing;
		case 1:
			return ((id == 0) ^ TrafficMap.dir) ? sensor.crossing : sensor.street;
		case 2:
		case 4:
			return ((id < 2) ^ TrafficMap.dir) ? sensor.crossing : sensor.street;
		case 3:
		case 7:
			return ((id % 2 == 1) ^ TrafficMap.dir) ? sensor.crossing : sensor.street;
		case 5:
			return ((id > 1) ^ TrafficMap.dir) ? sensor.crossing : sensor.street;
		case 6:
			return ((id == 0 || id == 3) ^ TrafficMap.dir) ? sensor.crossing : sensor.street;
		case 8:
			return ((id > 0) ^ TrafficMap.dir) ? sensor.crossing : sensor.street;
		case 9:
			return TrafficMap.dir ? sensor.crossing : sensor.street;
		}
		return null;
	}
	
	public static class Coord{
		public int x, y, w, h;
		public int arcw ,arch;
		public int centerX, centerY;
	}
}