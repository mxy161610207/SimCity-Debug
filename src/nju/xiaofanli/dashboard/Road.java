package nju.xiaofanli.dashboard;

import nju.xiaofanli.Resource;
import nju.xiaofanli.control.Police;
import nju.xiaofanli.dashboard.Road.Crossroad.CrossroadIcon;
import nju.xiaofanli.dashboard.Road.Street.StreetIcon;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Car.CarIcon;
import nju.xiaofanli.device.car.Command;
import nju.xiaofanli.device.sensor.Sensor;
import nju.xiaofanli.util.StyledText;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

public abstract class Road extends Location{
	public final Map<TrafficMap.Direction, Road> adjRoads = new HashMap<>(); //adjacent roads
	public final Map<TrafficMap.Direction, Sensor> adjSensors = new HashMap<>(); //adjacent sensors
	public final Map<Road, Road> entrance2exit = new HashMap<>(); //entrance -> exit
	public final Map<Road, Road> exit2entrance = new HashMap<>(); //exit -> entrance
	public final TrafficMap.Direction[] dir = {TrafficMap.Direction.UNKNOWN, TrafficMap.Direction.UNKNOWN};
	public final Queue<Car> cars = new LinkedList<>();//may contain phantoms
	public final Queue<Car> realCars = new LinkedList<>();
	public final Queue<Car> allRealCars = new LinkedList<>();
	public Car permitted = null;
	public JPanel crashLettersPanel = null;
	public int numSections;
	public final Queue<Car> waiting = new LinkedList<>();//can replace mutex
	public final Map<TrafficMap.Direction, Map<String, Integer>> timeouts = new HashMap<>(); //<car dir , car name> -> remaining time
	public final Map<TrafficMap.Direction, Boolean> isStraight = new HashMap<>(); // whether this road is straight or curved in the physical world
	public final RoadIconPanel iconPanel = new RoadIconPanel(this);

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
		public final Road road;
		public final TrafficMap.Coord coord = new TrafficMap.Coord();
		private final Map<String, JLabel[]> carIcons = new HashMap<>();
		private final Map<String, JLabel> citizenIcons = new HashMap<>();
		private final JLabel idLabel, mLabel;

		private RoadIcon(Road road) {
			setLayout(null);
			setOpaque(false);
//			setContentAreaFilled(false);
//			setBorderPainted(false);
			this.road = road;
			Resource.getCarIcons().forEach((name, icons) -> {
				JLabel[] labels = new JLabel[icons.length];
				for (int i = 0;i < icons.length;i++) {
					labels[i] = new JLabel(icons[i]);
					labels[i].setSize(labels[i].getPreferredSize());
				}
				carIcons.put(name, labels);
			});

			Resource.getCitizenIcons().forEach((name, icon) -> citizenIcons.put(name, new JLabel(icon)));
			citizenIcons.values().forEach(label -> label.setSize(label.getPreferredSize()));

			idLabel = new JLabel(String.valueOf(road.id));
			idLabel.setFont(Resource.en16bold);
			idLabel.setSize(idLabel.getPreferredSize());
			add(idLabel);

			mLabel = new JLabel("M");
			mLabel.setFont(Resource.en16bold);
			mLabel.setBorder(BorderFactory.createEmptyBorder(-6, -1, -5, -1));
			mLabel.setBackground(Color.WHITE);
			mLabel.setOpaque(true);
			mLabel.setSize(mLabel.getPreferredSize());
			mLabel.setVisible(false);
			add(mLabel);

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
//			g.setFont(Resource.en16bold);
			switch(road.allRealCars.size()){
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
			int n = road.cars.size() + road.realCars.size();
			int iconId = 0;
			if(n > 0){
				boolean isVertical = this instanceof StreetIcon && ((StreetIcon) this).isVertical;
				boolean showThumbnail = n > 2;
				int iconSize = showThumbnail ? CarIcon.SIZE2 : CarIcon.SIZE;
				int iconInset = showThumbnail ? CarIcon.INSET2 : CarIcon.INSET;
				int iconOffset = iconSize + iconInset;
				int x;
				int y;
				if (showThumbnail) {
					x = isVertical ? (coord.w-2*iconSize-iconInset)/2 : (coord.w-4*iconSize-3*iconInset)/2;
					y = isVertical ? (coord.h-4*iconSize-3*iconInset)/2 : (coord.h-2*iconSize-iconInset)/2;
				}
				else {
					x = isVertical ? (coord.w-iconSize)/2 : (coord.w-n*iconSize-(n-1)*iconInset)/2;
					y = isVertical ? (coord.h-n*iconSize-(n-1)*iconInset)/2 : (coord.h-iconSize)/2;
				}

				for(Car car : road.cars){
					int iconIdx = 0;
					if (showThumbnail) iconIdx += 3;
					if (car.hasPhantom()) iconIdx++;
					JLabel carIcon = carIcons.get(car.name)[iconIdx];
					if(!car.isLoading || Dashboard.blink) {
						carIcon.setLocation(x, y);
						carIcon.setVisible(true);
						visibleLabel.add(carIcon);

						g.setColor(Color.BLACK);
						g.drawRect(carIcon.getX() - 1, carIcon.getY() - 1, carIcon.getWidth() + 1, carIcon.getHeight() + 1);

						if (!showThumbnail && car.passenger != null) {
							JLabel citizenIcon = citizenIcons.get(car.passenger.name);
							citizenIcon.setLocation(carIcon.getX() + carIcon.getWidth() / 2, carIcon.getY() + carIcon.getHeight() / 2);
							citizenIcon.setVisible(true);
							visibleLabel.add(citizenIcon);

							if (car.passenger.manual) {
								mLabel.setLocation(citizenIcon.getX()+citizenIcon.getWidth()-mLabel.getWidth(), citizenIcon.getY()+citizenIcon.getHeight()-mLabel.getHeight());
								mLabel.setVisible(true);
								visibleLabel.add(mLabel);
							}
						}
					}

					if (showThumbnail && iconId == 3) {
						if (isVertical) { x += iconOffset; y -= 3 * iconOffset; } else { x -= 3 * iconOffset; y += iconOffset; }
					}
					else {
						if (isVertical) y += iconOffset; else x += iconOffset;
					}
					iconId++;
				}

				for(Car car : road.realCars){
					JLabel label = carIcons.get(car.name)[showThumbnail ? 5 : 2];
					label.setLocation(x, y);
					label.setVisible(true);
					visibleLabel.add(label);

					g.setColor(Color.BLACK);
					g.drawRect(label.getX()-1, label.getY()-1, label.getWidth()+1, label.getHeight()+1);
					if (showThumbnail && iconId == 3) {
						if (isVertical) { x += iconOffset; y -= 3 * iconOffset; } else { x -= 3 * iconOffset; y += iconOffset; }
					}
					else {
						if (isVertical) y += iconOffset; else x += iconOffset;
					}
					iconId++;
				}
			}

			for (JLabel[] labels : carIcons.values()) {
				for (JLabel label : labels)
					if (!visibleLabel.contains(label) && label.isVisible())
						label.setVisible(false); // CAUTION: this will invoke paintComponent() again, so be careful of infinite loop
			}
			citizenIcons.values().forEach(label -> {
				if (!visibleLabel.contains(label) && label.isVisible())
					label.setVisible(false); // CAUTION: this will invoke paintComponent() again, so be careful of infinite loop
			});

			if (!visibleLabel.contains(mLabel) && mLabel.isVisible())
				mLabel.setVisible(false); // CAUTION: this will invoke paintComponent() again, so be careful of infinite loop
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
		crashLettersPanel = null;
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

			StyledText enText = new StyledText(), chText = new StyledText();
			Iterator<Car> iter = allRealCars.iterator();
			Car crashedCar = iter.next();
			enText.append(crashedCar.name, crashedCar.icon.color);
			chText.append(crashedCar.name, crashedCar.icon.color);
			while (iter.hasNext()) {
				crashedCar = iter.next();
				enText.append(", ").append(crashedCar.name, crashedCar.icon.color);
				chText.append("，").append(crashedCar.name, crashedCar.icon.color);
			}
			enText.append(" crashed at ").append(name, Resource.DEEP_SKY_BLUE).append(".\n");
			enText.append(" 在 ").append(name, Resource.DEEP_SKY_BLUE).append(" 相撞。\n");
			Dashboard.log(enText, chText);

			List<Car> frontCars = new ArrayList<>();
			Set<TrafficMap.Direction> dirs = new HashSet<>();
			allRealCars.stream().filter(car -> !dirs.contains(car.getRealDir())).forEach(car -> {
				dirs.add(car.getRealDir());
				frontCars.add(car);
			});
			Dashboard.logCrashEvent(frontCars);
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

			Dashboard.hideCrashEffect(this);
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