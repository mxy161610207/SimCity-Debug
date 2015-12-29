package nju.ics.lixiaofan.city;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Car.CarIcon;
import nju.ics.lixiaofan.city.Section.Crossing.CrossingIcon;
import nju.ics.lixiaofan.city.Section.Street.StreetIcon;
import nju.ics.lixiaofan.city.TrafficMap.Coord;
import nju.ics.lixiaofan.consistency.context.Context;
import nju.ics.lixiaofan.consistency.middleware.Middleware;
import nju.ics.lixiaofan.dashboard.Dashboard;
import nju.ics.lixiaofan.sensor.Sensor;

public abstract class Section extends Location{
	public Map<Integer, Section> adjs = new HashMap<Integer, Section>(); //physical adjacency
	public Map<Section, Section> exits = new HashMap<Section, Section>(); //entrance -> exit
	public Map<Section, Section> entrances = new HashMap<Section, Section>(); //exit -> entrance
	public int[] dir = {-1, -1};
	public Queue<Car> cars = new LinkedList<Car>();//may contain phantoms
	public Set<String> realCars = new HashSet<String>();
	public Car[] permitted = {null};//making its class an array to share its value among the combined
	public boolean isCombined = false;
	public Set<Section> combined = null;
	public Object mutex = new Object();
	public Queue<Car> waiting = new LinkedList<Car>();
	public Set<Sensor> sensors = new HashSet<Sensor>();
	public SectionIcon icon = null;
	public BallonIcon balloon = null;
	
	public static Section sectionOf(String name){
		if(name == null)
			return null;
		String[] strs = name.split(" ");
		int id = Integer.parseInt(strs[1]);
		if(strs[0].equals("Crossing"))
			return TrafficMap.crossings[id];
		else if(strs[0].equals("Street"))
			return TrafficMap.streets[id];
		else
			return null;
	}
	
	public boolean sameAs(Section s){
		return this == s || isCombined && combined.contains(s);
	}
	
	public void addWaitingCar(Car car){
		synchronized (waiting) {
			if(!waiting.contains(car)){
				waiting.add(car);
			}
		}
	}
	
	public void removeWaitingCar(Car car){
		synchronized (waiting) {
			if(waiting.contains(car)){
				waiting.remove(car);
			}
		}
	}
	
	public boolean isOccupied(){
		return !cars.isEmpty();
	}
	
	public static class SectionIcon extends JButton{
		private static final long serialVersionUID = 1L;
		public final static int cubeSize = CarIcon.SIZE;
		private final static int cubeInset = CarIcon.INSET;
		public int id;
		public Section section = null;
		public Coord coord = new Coord();
		
		public SectionIcon() {
			setOpaque(false);
			setContentAreaFilled(false);
//			setBorderPainted(false);
		}
		
		protected void paintBorder(Graphics g) {
//			super.paintBorder(g);
			((Graphics2D )g).setStroke(new BasicStroke(2.0f));
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
		
		protected void paintComponent(Graphics g) {
//			System.out.println("children");
			super.paintComponent(g);
			int n = section.cars.size();
			switch(n){
			case 0:
				g.setColor(Color.GREEN); break;
			case 1:
				if(section.realCars.isEmpty())
					g.setColor(Color.YELLOW); 
				else{
					g.setColor(Color.RED);
					TrafficMap.playCrashSound();
				}
				break;
			default:
				g.setColor(Color.RED);
				TrafficMap.playCrashSound();
				break;
			}
			
			if(this instanceof CrossingIcon)
				g.fillOval(0, 0, coord.w, coord.h);
			else if(this instanceof StreetIcon)
				g.fillRoundRect(0, 0, coord.w, coord.h, coord.arcw, coord.arch);
			
			n += section.realCars.size();
			if(n > 0){
				boolean vertical = this instanceof StreetIcon ? ((StreetIcon )this).isVertical : false;
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
					switch(car.name){
					case Car.ORANGE:
						g.setColor(Color.ORANGE); break;
					case Car.BLACK:
						g.setColor(Color.BLACK); break;
					case Car.WHITE:
						g.setColor(Color.WHITE); break;
					case Car.RED:
						g.setColor(Color.RED); break;
					case Car.GREEN:
						g.setColor(Color.GREEN); break;
					case Car.SILVER:
						g.setColor(CarIcon.SILVER); break;
					}
					g.fillRect(x, y, cubeSize, cubeSize);
					g.setColor(Color.BLACK);
					g.drawString("FAKE", x, y+10);
					
					g.drawRect(x, y, cubeSize, cubeSize);
					if(vertical)
						y += cubeSize + cubeInset;
					else
						x += cubeSize + cubeInset;
				}
				
				for(String car : section.realCars){
					switch(car){
					case Car.ORANGE:
						g.setColor(Color.ORANGE); break;
					case Car.BLACK:
						g.setColor(Color.BLACK); break;
					case Car.WHITE:
						g.setColor(Color.WHITE); break;
					case Car.RED:
						g.setColor(Color.RED); break;
					case Car.GREEN:
						g.setColor(Color.GREEN); break;
					case Car.SILVER:
						g.setColor(CarIcon.SILVER); break;
					}
					g.fillRect(x, y, cubeSize, cubeSize);
					g.setColor(Color.BLACK);
					g.drawString("REAL", x, y+10);
					g.drawRect(x, y, cubeSize, cubeSize);
					if(vertical)
						y += cubeSize + cubeInset;
					else
						x += cubeSize + cubeInset;
				}
			}
			
			//draw sections
			if(TrafficMap.showSection){
				g.setColor(Color.BLACK);
				String str = id+"";
				FontMetrics fm = g.getFontMetrics();
				g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2);
			}
		}
	}
	
	public void displayBalloon(int type, String sensor, String car) {
		if(!TrafficMap.showBalloon)
			return;
		balloon.type = type;
		balloon.sensor = sensor;
		balloon.car = car;
		balloon.duration = 3000;//display for 3s
		balloon.setIcon(Middleware.isResolutionEnabled());
		balloon.setVisible(true);
	}
	
	public static class BallonIcon extends JButton{
		private static final long serialVersionUID = 1L;
		public static int WIDTH = 70;
		public static int HEIGHT = 70;
		private static HashMap<String, ImageIcon> balloons = new HashMap<>();
		public Section section = null;
		public int duration = 0;
		public int type;
		public String sensor = "", car = "";
		
		public BallonIcon() {
			setOpaque(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setVisible(false);
		}
		
		@Override
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
			HashMap<String, String> files = new HashMap<String, String>();
			files.put("red", "res/red_balloon.png");
			files.put("green", "res/green_balloon.png");
			for(Map.Entry<String, String> file : files.entrySet()){
				ImageIcon imageIcon = new ImageIcon(file.getValue());
				Image image = imageIcon.getImage();
				if(imageIcon.getIconWidth() > imageIcon.getIconHeight())
					image = image.getScaledInstance(WIDTH, -1, Image.SCALE_SMOOTH);
				else
					image = image.getScaledInstance(-1, HEIGHT, Image.SCALE_SMOOTH);
				imageIcon = new ImageIcon(image);
				balloons.put(file.getKey(), imageIcon);
			}
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