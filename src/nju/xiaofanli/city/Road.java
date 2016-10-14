package nju.xiaofanli.city;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JButton;

import nju.xiaofanli.control.Police;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Car.CarIcon;
import nju.xiaofanli.city.Road.Crossroad.CrossroadIcon;
import nju.xiaofanli.city.Road.Street.StreetIcon;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.Resource;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.sensor.Sensor;

public abstract class Road extends Location{
	public Map<Integer, Road> adjRoads = new HashMap<>(); //adjacent roads
	public Map<Integer, Sensor> adjSensors = new HashMap<>(); //adjacent sensors
	public Map<Road, Road> entrance2exit = new HashMap<>(); //entrance -> exit
	public Map<Road, Road> exit2entrance = new HashMap<>(); //exit -> entrance
	public int[] dir = {TrafficMap.UNKNOWN_DIR, TrafficMap.UNKNOWN_DIR};
	public Queue<Car> cars = new LinkedList<>();//may contain phantoms
	public Queue<Car> realCars = new LinkedList<>();
	public Car[] permitted = {null};//let its type be an array to share its value among the combined
	public Set<Road> combined = new HashSet<>();
//	public Object mutex = new Object();//used by police thread and its notifier
	public Queue<Car> waiting = new LinkedList<>();//can replace mutex
	public RoadIcon icon = null;
	public Sensor.BalloonIcon balloon = null;
	
	public static Road roadOf(String name){
//		System.out.println(name);
		if(name == null)
			return null;
		String[] strs = name.split(" ");
		int id = Integer.parseInt(strs[1]);
		switch (strs[0]) {
			case "Crossroad":
				return TrafficMap.crossroads[id];
			case "Street":
				return TrafficMap.streets[id];
			default:
				return null;
		}
	}
	
	public boolean sameAs(Road road){
		return this == road || combined.contains(road);
	}
	
	public boolean isCombined(){
		return !combined.isEmpty();
	}
	
	public static void combine(Set<Road> roads){
		for(Road road : roads){
			for(Road other : roads)
				if(other != road){
					road.combined.add(other);
					other.cars = road.cars;
					other.realCars = road.realCars;
//					other.mutex = road.mutex;
					other.permitted = road.permitted;
					other.waiting = road.waiting;
					other.adjRoads = road.adjRoads;
					other.adjSensors = road.adjSensors;
					other.entrance2exit = road.entrance2exit;
					other.exit2entrance = road.exit2entrance;
					other.dir = road.dir;
				}
		}
	}
	
	public void setPermitted(Car car){
		permitted[0] = car;
	}
	
	public Car getPermitted(){
		return permitted[0];
	}
	
	public void addWaitingCar(Car car){
		//noinspection SynchronizeOnNonFinalField
		synchronized (waiting) {
			if(!waiting.contains(car)){
				waiting.add(car);
			}
		}
	}
	
	public void removeWaitingCar(Car car){
		//noinspection SynchronizeOnNonFinalField
		synchronized (waiting) {
			if(waiting.contains(car)){
				waiting.remove(car);
			}
		}
	}

	public boolean isOccupied(){
		return !cars.isEmpty();
	}

	public static abstract class RoadIcon extends JButton{
		private static final long serialVersionUID = 1L;
		public final static int cubeSize = CarIcon.SIZE;
		private final static int cubeInset = CarIcon.INSET;
		public int id;
		public Road road = null;
		public TrafficMap.Coord coord = new TrafficMap.Coord();
		
		public RoadIcon() {
			setOpaque(false);
			setContentAreaFilled(false);
//			setBorderPainted(false);
		}
		
//		@Override
//		public void setEnabled(boolean b) {
//			super.setEnabled(b);
//			System.out.println(road.name);
//		}
		
		protected void paintBorder(Graphics g) {
//			super.paintBorder(g);
			((Graphics2D) g).setStroke(new BasicStroke(2.0f));
			if(getModel().isPressed())
				g.setColor(Color.black);
			else if(getModel().isRollover())
				g.setColor(Color.gray);
			else
				return;
				
			if(this instanceof CrossroadIcon)
				g.drawOval(1, 1, coord.w-2, coord.h-2);
			else if(this instanceof StreetIcon)
				g.drawRoundRect(1, 1, coord.w-2, coord.h-2, coord.arcw, coord.arch);
		}

		protected void paintComponent(Graphics g) {
//			System.out.println(road.name);
			super.paintComponent(g);
			g.setFont(Dashboard.bold16dialog);
            FontMetrics fm = g.getFontMetrics();
			int n = road.realCars.size();
			for(Car car : road.cars)
				if(!car.hasPhantom())
					n++;
			switch(n){
			case 0:
				g.setColor(Resource.LIGHT_SKY_BLUE); break;
			case 1:
				g.setColor(Color.YELLOW); break;
			default:
				g.setColor(Resource.CHOCOLATE); break;
			}
			
			if(this instanceof CrossroadIcon)
				g.fillOval(0, 0, coord.w, coord.h);
			else if(this instanceof StreetIcon)
				g.fillRoundRect(0, 0, coord.w, coord.h, coord.arcw, coord.arch);
			
			n = road.cars.size() + road.realCars.size();
			if(n > 0){
				boolean vertical = this instanceof StreetIcon && ((StreetIcon) this).isVertical;
				int x = vertical ? (coord.w - cubeSize) / 2 : (coord.w-n*cubeSize-(n-1)*cubeInset) / 2;
				int y = vertical ? (coord.h-n*cubeSize-(n-1)*cubeInset) / 2 : (coord.h - cubeSize) / 2;
				
				for(Car car : road.cars){
					if(car.isLoading && !Dashboard.blink){
						if(vertical)
							y += cubeSize + cubeInset;
						else
							x += cubeSize + cubeInset;
						continue;
					}
					g.setColor(car.icon.color);
					g.fillRect(x, y, cubeSize, cubeSize);
					g.setColor(Color.BLACK);
					g.drawRect(x, y, cubeSize, cubeSize);
					if(car.hasPhantom()) {
                        if(car.icon.color.equals(Color.BLACK))
                            g.setColor(Color.WHITE);
                        g.drawString("F", x+2, y+fm.getAscent()-2);
                        if(car.icon.color.equals(Color.BLACK))
                            g.setColor(Color.BLACK);
                    }
					if(vertical)
						y += cubeSize + cubeInset;
					else
						x += cubeSize + cubeInset;
				}
				
				for(Car car : road.realCars){
					g.setColor(car.icon.color);
					g.fillRect(x, y, cubeSize, cubeSize);
					g.setColor(Color.BLACK);
					g.drawRect(x, y, cubeSize, cubeSize);
                    if(car.icon.color.equals(Color.BLACK))
                        g.setColor(Color.WHITE);
                    g.drawString("R", x+2, y+fm.getAscent()-2);
                    if(car.icon.color.equals(Color.BLACK))
                        g.setColor(Color.BLACK);
					if(vertical)
						y += cubeSize + cubeInset;
					else
						x += cubeSize + cubeInset;
				}
			}
			
			//draw roads
			if(Dashboard.showRoad){
//                g.setFont(Dashboard.bold20dialog);
				g.setColor(Color.BLACK);
				String str = String.valueOf(id);
				g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2);
			}
		}

        public void repaintAll(){
            this.repaint();
            if (road.isCombined())
                road.combined.forEach(r -> r.icon.repaint());
        }
	}
	
	public void reset() {
		cars.clear();
		realCars.clear();
		setPermitted(null);
		waiting.clear();
	}

	public void checkRealCrash() {
        Set<Car> allRealCars = new HashSet<>(realCars);
        allRealCars.addAll(cars.stream().filter(car -> !car.hasPhantom()).collect(Collectors.toSet()));
        if(allRealCars.size() > 1) {
            // stop all crashed cars to keep the scene intact
            allRealCars.forEach(car -> {
                car.isInCrash = true;
//                car.finalState = STOPPED;
                car.notifyPolice(Police.REQUEST2STOP);
                if(!car.isHornOn)
                    Command.send(car, Command.HORN_ON);
            });

            Dashboard.playCrashSound();
        }
        else{
            allRealCars.forEach(car -> {
                car.isInCrash = false;
                if(car.isHornOn)
                    Command.send(car, Command.HORN_OFF);
            });
        }
    }

    public static class Crossroad extends Road {
		public static class CrossroadIcon extends RoadIcon {
			private static final long serialVersionUID = 1L;
		}
	}
	
	public static class Street extends Road {
		public static class StreetIcon extends RoadIcon {
			private static final long serialVersionUID = 1L;
			public boolean isVertical;
		}
	}
}