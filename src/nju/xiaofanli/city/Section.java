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

import javax.swing.ImageIcon;
import javax.swing.JButton;

import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Car.CarIcon;
import nju.xiaofanli.city.Section.Crossing.CrossingIcon;
import nju.xiaofanli.city.Section.Street.StreetIcon;
import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.Resource;
import nju.xiaofanli.device.sensor.Sensor;

public abstract class Section extends Location{
	public Map<Integer, Section> adjSects = new HashMap<>(); //adjacent sections
	public Map<Integer, Sensor> adjSensors = new HashMap<>(); //adjacent sensors
	public Map<Section, Section> entrance2exit = new HashMap<>(); //entrance -> exit
	public Map<Section, Section> exit2entrance = new HashMap<>(); //exit -> entrance
	public int[] dir = {TrafficMap.UNKNOWN_DIR, TrafficMap.UNKNOWN_DIR};
	public Queue<Car> cars = new LinkedList<>();//may contain phantoms
	public Queue<Car> realCars = new LinkedList<>();
	public Car[] permitted = {null};//let its type be an array to share its value among the combined
	public Set<Section> combined = new HashSet<>();
//	public Object mutex = new Object();//used by police thread and its notifier
	public Queue<Car> waiting = new LinkedList<>();//can replace mutex
	public SectionIcon icon = null;
	public BalloonIcon balloon = null;
	
	public static Section sectionOf(String name){
//		System.out.println(name);
		if(name == null)
			return null;
		String[] strs = name.split(" ");
		int id = Integer.parseInt(strs[1]);
		switch (strs[0]) {
			case "Crossing":
				return TrafficMap.crossings[id];
			case "Street":
				return TrafficMap.streets[id];
			default:
				return null;
		}
	}
	
	public boolean sameAs(Section s){
		return this == s || combined.contains(s);
	}
	
	public boolean isCombined(){
		return !combined.isEmpty();
	}
	
	public static void combine(Set<Section> sections){
		for(Section s : sections){
			for(Section other : sections)
				if(other != s){
					s.combined.add(other);
					other.cars = s.cars;
					other.realCars = s.realCars;
//					other.mutex = s.mutex;
					other.permitted = s.permitted;
					other.waiting = s.waiting;
					other.adjSects = s.adjSects;
					other.adjSensors = s.adjSensors;
					other.entrance2exit = s.entrance2exit;
					other.exit2entrance = s.exit2entrance;
					other.dir = s.dir;
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

	public static abstract class SectionIcon extends JButton{
		private static final long serialVersionUID = 1L;
		public final static int cubeSize = CarIcon.SIZE;
		private final static int cubeInset = CarIcon.INSET;
		public int id;
		public Section section = null;
		public TrafficMap.Coord coord = new TrafficMap.Coord();
		
		public SectionIcon() {
			setOpaque(false);
			setContentAreaFilled(false);
//			setBorderPainted(false);
		}
		
//		@Override
//		public void setEnabled(boolean b) {
//			super.setEnabled(b);
//			System.out.println(section.name);
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
				
			if(this instanceof CrossingIcon)
				g.drawOval(1, 1, coord.w-2, coord.h-2);
			else if(this instanceof StreetIcon)
				g.drawRoundRect(1, 1, coord.w-2, coord.h-2, coord.arcw, coord.arch);
		}

		private static Color LIGHT_GREEN = new Color(0, 255, 127);
		private static Color BROWN = new Color(165, 42, 42);
		protected void paintComponent(Graphics g) {
//			System.out.println(section.name);
			super.paintComponent(g);
			g.setFont(Dashboard.bold14dialog);
            FontMetrics fm = g.getFontMetrics();
			int n = section.realCars.size();
			for(Car car : section.cars)
				if(!car.hasPhantom())
					n++;
			switch(n){
			case 0:
				g.setColor(LIGHT_GREEN); break;
			case 1:
				g.setColor(Color.YELLOW); break;
			default:
				g.setColor(BROWN); break;
			}
			
			if(this instanceof CrossingIcon)
				g.fillOval(0, 0, coord.w, coord.h);
			else if(this instanceof StreetIcon)
				g.fillRoundRect(0, 0, coord.w, coord.h, coord.arcw, coord.arch);
			
			n = section.cars.size() + section.realCars.size();
			if(n > 0){
				boolean vertical = this instanceof StreetIcon && ((StreetIcon) this).isVertical;
				int x = vertical ? (coord.w - cubeSize) / 2 : (coord.w-n*cubeSize-(n-1)*cubeInset) / 2;
				int y = vertical ? (coord.h-n*cubeSize-(n-1)*cubeInset) / 2 : (coord.h - cubeSize) / 2;
				
				for(Car car : section.cars){
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
                        g.drawString("FAKE", x+(cubeSize-fm.stringWidth("FAKE"))/2, y+fm.getAscent());
                        if(car.icon.color.equals(Color.BLACK))
                            g.setColor(Color.BLACK);
                    }
					if(vertical)
						y += cubeSize + cubeInset;
					else
						x += cubeSize + cubeInset;
				}
				
				for(Car car : section.realCars){
					g.setColor(car.icon.color);
					g.fillRect(x, y, cubeSize, cubeSize);
					g.setColor(Color.BLACK);
					g.drawRect(x, y, cubeSize, cubeSize);
                    if(car.icon.color.equals(Color.BLACK))
                        g.setColor(Color.WHITE);
                    g.drawString("REAL", x+(cubeSize-fm.stringWidth("REAL"))/2, y+fm.getAscent());
                    if(car.icon.color.equals(Color.BLACK))
                        g.setColor(Color.BLACK);
					if(vertical)
						y += cubeSize + cubeInset;
					else
						x += cubeSize + cubeInset;
				}
			}
			
			//draw sections
			if(Dashboard.showSection){
				g.setColor(Color.BLACK);
				String str = String.valueOf(id);
				g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2);
			}
		}

        public void repaintAll(){
            this.repaint();
            if (section.isCombined())
                section.combined.forEach(s -> s.icon.repaint());
        }
	}
	
	public void displayBalloon(int type, String sensor, String car, boolean isResolutionEnabled) {
		if(!Dashboard.showBalloon)
			return;
		balloon.type = type;
		balloon.sensor = sensor;
		balloon.car = car;
		balloon.duration = 3000;//display for 3s
		balloon.setIcon(isResolutionEnabled);
		balloon.setVisible(true);
		
		PkgHandler.send(new AppPkg().setBalloon(name, type, sensor, car, isResolutionEnabled));
	}
	
	public void reset() {
		cars.clear();
		realCars.clear();
		setPermitted(null);
		waiting.clear();
	}
	
	public static class BalloonIcon extends JButton{
		private static final long serialVersionUID = 1L;
		public static int WIDTH = 70;
		public static int HEIGHT = 70;
		private static HashMap<String, ImageIcon> balloons = new HashMap<>();
		public int duration = 0;
		public int type;
		public String sensor = "", car = "";
		
		public BalloonIcon() {
			setOpaque(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setVisible(false);
		}
		
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			FontMetrics fm = g.getFontMetrics();
			String str = "";
			if(type == Context.FP)
				str = "-FP-";
			else if(type == Context.FN)
				str = "-FN-";
			g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2-24);
			str = sensor;
			g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2-12);
			str = car;
			g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2);
		}
		
		public static void readBalloonImage(){
			balloons.put("red", Resource.getRedBalloonImageIcon());
			balloons.put("green", Resource.getGreenBalloonImageIcon());
		}
		
		private void setIcon(boolean resolutionEnabled){
			if(resolutionEnabled)
				setIcon(balloons.get("green"));
			else
				setIcon(balloons.get("red"));
		}
	}
	
	public static class Crossing extends Section{
		public static class CrossingIcon extends SectionIcon{
			private static final long serialVersionUID = 1L;
		}
	}
	
	public static class Street extends Section{
		public static class StreetIcon extends SectionIcon{
			private static final long serialVersionUID = 1L;
			public boolean isVertical;
		}
	}
}