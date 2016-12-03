package nju.xiaofanli.dashboard;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;

import nju.xiaofanli.control.Police;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Car.CarIcon;
import nju.xiaofanli.dashboard.Road.Crossroad.CrossroadIcon;
import nju.xiaofanli.dashboard.Road.Street.StreetIcon;
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
	public Queue<Car> allRealCars = new LinkedList<>();
	public Car permitted = null;
	public int numSections;
	public final Queue<Car> waiting = new LinkedList<>();//can replace mutex
	public Map<Integer, Map<String, Integer>> timeouts = new HashMap<>(); //<car dir , car name> -> remaining time
	public RoadIconPanel icon = new RoadIconPanel(this);

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

	public static class RoadIconPanel extends JPanel {
		public Road road = null;
		public TrafficMap.Coord coord = new TrafficMap.Coord();
		public final List<RoadIcon> icons = new ArrayList<>();

		private RoadIconPanel(Road road) {
			this.road = road;
			road.numSections = 0;
			setLayout(null);
			setOpaque(false);
			setBackground(null);
		}

		public void addCrossroadIcon(int x, int y, int w, int h) {
			road.numSections++;
			CrossroadIcon icon = new CrossroadIcon(road);
			add(icon);
			icons.add(icon);
			icon.coord.x = x;
			icon.coord.y = y;
			icon.coord.w = w;
			icon.coord.h = h;
			icon.coord.centerX = icon.coord.x + icon.coord.w/2;
			icon.coord.centerY = icon.coord.y + icon.coord.h/2;
			icon.setBounds(icon.coord.x, icon.coord.y, icon.coord.w, icon.coord.h);
		}

		public void addCrossroadIcon(TrafficMap.Coord coord) {
			addCrossroadIcon(0, 0, coord.w, coord.h);
		}

		public void addStreetIcon(int x, int y, int w, int h, int arcw, int arch, boolean isVertical) {
			road.numSections++;
			StreetIcon icon = new StreetIcon(road);
			add(icon);
			icons.add(icon);
			icon.coord.x = x;
			icon.coord.y = y;
			icon.coord.w = w;
			icon.coord.h = h;
			icon.coord.arcw = arcw;
			icon.coord.arch = arch;
			icon.isVertical = isVertical;
			icon.coord.centerX = icon.coord.x + icon.coord.w/2;
			icon.coord.centerY = icon.coord.y + icon.coord.h/2;
			icon.setBounds(icon.coord.x, icon.coord.y, icon.coord.w, icon.coord.h);
		}

		public void addStreetIcon(TrafficMap.Coord coord, boolean isVertical) {
			addStreetIcon(0, 0, coord.w, coord.h, coord.arcw, coord.arch, isVertical);
		}

		private static Random random = new Random();
		public RoadIcon getARoadIcon() {
			return icons.isEmpty() ? null : icons.get(random.nextInt(icons.size()));
		}
	}

	public static abstract class RoadIcon extends JPanel {
		private static final long serialVersionUID = 1L;
		public final static int cubeSize = CarIcon.SIZE;
		private final static int cubeInset = CarIcon.INSET;
		public Road road = null;
		public TrafficMap.Coord coord = new TrafficMap.Coord();
		private Map<String, JLabel[]> carIcons = new HashMap<>();
		private Map<String, JLabel> citizenIcons = new HashMap<>();
        private JLabel idLabel = null;

		private RoadIcon(Road road) {
            setLayout(null);
			setOpaque(false);
//			setContentAreaFilled(false);
//			setBorderPainted(false);
            this.road = road;
            Resource.getCarIcons().forEach((name, icons) -> carIcons.put(name, new JLabel[]{new JLabel(icons[0]), new JLabel(icons[1]), new JLabel(icons[2])}));
            for (JLabel[] labels : carIcons.values()) {
                for (JLabel label : labels)
                    label.setSize(label.getPreferredSize());
            }

            Resource.getCitizenIcons().forEach((name, icon) -> citizenIcons.put(name, new JLabel(icon)));
			citizenIcons.values().forEach(label -> label.setSize(label.getPreferredSize()));

            idLabel = new JLabel(String.valueOf(road.id));
            idLabel.setFont(Resource.bold17dialog);
            idLabel.setSize(idLabel.getPreferredSize());
            add(idLabel);

			citizenIcons.values().forEach(label -> {
				label.setVisible(false);
				add(label);
			});

			for (JLabel[] labels : carIcons.values()) {
                for (JLabel label : labels) {
                    label.setVisible(false);
                    add(label);
                }
            }
		}
		
//		@Override
//		public void setEnabled(boolean b) {
//			super.setEnabled(b);
//			System.out.println(road.name);
//		}

        public boolean isPressed = false, isEntered = false;
		protected void paintBorder(Graphics g) {
//			super.paintBorder(g);
            if (!isPressed && !isEntered)
                return;
            g.setColor(isPressed ? Color.BLACK : Color.GRAY);
			((Graphics2D) g).setStroke(new BasicStroke(2.0f));

			if(this instanceof CrossroadIcon)
				g.drawOval(1, 1, coord.w-2, coord.h-2);
			else if(this instanceof StreetIcon)
				g.drawRoundRect(1, 1, coord.w-2, coord.h-2, coord.arcw, coord.arch);
		}

		protected void paintComponent(Graphics g) {
            super.paintComponent(g);
//			System.out.println(road.name);
			g.setFont(Resource.bold16dialog);
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

            Set<JLabel> visibleLabel = new HashSet<>();
			n = road.cars.size() + road.realCars.size();
			if(n > 0){
				boolean vertical = this instanceof StreetIcon && ((StreetIcon) this).isVertical;
				int x = vertical ? (coord.w-cubeSize)/2 : (coord.w-n*cubeSize-(n-1)*cubeInset)/2;
				int y = vertical ? (coord.h-n*cubeSize-(n-1)*cubeInset)/2 : (coord.h-cubeSize)/2;
				
				for(Car car : road.cars){
                    JLabel carIcon = carIcons.get(car.name)[car.hasPhantom() ? 1 : 0];
					if(car.isLoading && !Dashboard.blink){
						if(vertical)
							y += cubeSize + cubeInset;
						else
							x += cubeSize + cubeInset;
						continue;
					}
                    carIcon.setLocation(x, y);
                    carIcon.setVisible(true);
                    visibleLabel.add(carIcon);

                    g.setColor(Color.BLACK);
                    g.drawRect(carIcon.getX()-1, carIcon.getY()-1, carIcon.getWidth()+1, carIcon.getHeight()+1);

					if (car.passenger != null) {
						JLabel citizenIcon = citizenIcons.get(car.passenger.name);
						citizenIcon.setLocation(carIcon.getX()+carIcon.getWidth()/2, carIcon.getY()+carIcon.getHeight()/2);
						citizenIcon.setVisible(true);
						visibleLabel.add(citizenIcon);
					}

					if(vertical)
						y += cubeSize + cubeInset;
					else
						x += cubeSize + cubeInset;
				}
				
				for(Car car : road.realCars){
                    JLabel label = carIcons.get(car.name)[2];
                    label.setLocation(x, y);
                    label.setVisible(true);
                    visibleLabel.add(label);

                    g.setColor(Color.BLACK);
                    g.drawRect(label.getX()-1, label.getY()-1, label.getWidth()+1, label.getHeight()+1);
					if(vertical)
						y += cubeSize + cubeInset;
					else
						x += cubeSize + cubeInset;
				}
			}

			for (JLabel[] labels : carIcons.values()) {
                for (JLabel label : labels)
                    if (!visibleLabel.contains(label) && label.isVisible())
                        label.setVisible(false); // NOTE: this will cause the invocation of paintComponent() again, so be careful of infinite loop
            }
            citizenIcons.values().forEach(label -> {
				if (!visibleLabel.contains(label) && label.isVisible())
					label.setVisible(false); // NOTE: this will cause the invocation of paintComponent() again, so be careful of infinite loop
			});
		}

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
			if (idLabel != null)
            	idLabel.setLocation((width-idLabel.getWidth())/2, (height-idLabel.getHeight())/2);
        }

        public void showRoadNumber(boolean b) {
			if (idLabel != null)
            	idLabel.setVisible(b);
        }

	}
	
	public void reset() {
		cars.clear();
		realCars.clear();
		allRealCars.clear();
		waiting.clear();
		permitted = null;
	}

	public void checkRealCrash() {
//        Set<Car> allRealCars = new HashSet<>(realCars);
//        allRealCars.addAll(cars.stream().filter(car -> !car.hasPhantom()).collect(Collectors.toSet()));
        if(allRealCars.size() > 1) {
            // stop all cars to keep the scene intact
			if (!TrafficMap.crashOccurred) {
				TrafficMap.crashOccurred = true;
				Resource.getConnectedCars().forEach(car -> car.notifyPolice(Police.REQUEST2STOP));
			}

            allRealCars.forEach(car -> {
            	if (!car.isInCrash) {
					car.isInCrash = true;
					if (Dashboard.playCrashSound)
						Command.send(car, Command.HORN_ON);
				}
            });

			Dashboard.showCrashEffect(this);
            Dashboard.playCrashSound();

			List<Car> frontCars = new ArrayList<>();
			Set<Integer> dirs = new HashSet<>();
			allRealCars.stream().filter(car -> !dirs.contains(car.getRealDir())).forEach(car -> {
				dirs.add(car.getRealDir());
				frontCars.add(car);
			});
			Dashboard.showCrashDialog(frontCars);
        }
        else{
            allRealCars.forEach(car -> {
            	if (car.isInCrash) {
					car.isInCrash = false;
					if (Dashboard.playCrashSound)
						Command.send(car, Command.HORN_OFF);

					if (TrafficMap.crashOccurred) {
						boolean crash = false;
						for (Car car2 : Resource.getConnectedCars()) {
							if (car2.isInCrash) {
								crash = true;
								break;
							}
						}
						TrafficMap.crashOccurred = crash;
					}
				}
            });
        }
    }

    public static class Crossroad extends Road {
		public static class CrossroadIcon extends RoadIcon {
			private static final long serialVersionUID = 1L;
            public CrossroadIcon (Road road) {
                super(road);
            }
		}
	}
	
	public static class Street extends Road {
		public static class StreetIcon extends RoadIcon {
			private static final long serialVersionUID = 1L;
			public boolean isVertical;
            public StreetIcon (Road road) {
                super(road);
            }
		}
	}
}