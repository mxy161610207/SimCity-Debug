package nju.ics.lixiaofan.city;

import java.awt.Dimension;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import nju.ics.lixiaofan.car.Car.CarIcon;
import nju.ics.lixiaofan.city.SectionIcon.Coord;

public class Building {
	public String name;
	public Type type = null;
	public BuildingIcon icon = null;
	
	public static enum Type {
		Hospital, School, PoliceStation, Restaurant, StarkIndustries
	}
	
	public Building(String name, Type type) {
		this.name = name;
		this.type = type;
		this.icon = new BuildingIcon(this);
	}
	
	public static class BuildingIcon extends JButton{
		private static final long serialVersionUID = 1L;
		private Building building = null;
		private ImageIcon imageIcon = null;
		public static final int SIZE = 3*CarIcon.SIZE;
		public Coord coord = new Coord();
		
		public BuildingIcon(Building building) {
			setOpaque(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setSize(new Dimension(SIZE, SIZE));
			this.building = building;
			switch (this.building.type) {
			case StarkIndustries:
				imageIcon = new ImageIcon("res/stark_industries.png");
				Image image = imageIcon.getImage().getScaledInstance(SIZE, -1, Image.SCALE_DEFAULT);
				imageIcon = new ImageIcon(image);
				setIcon(imageIcon);
				break;
			default:
				break;
			}
		}
		
//		protected void paintComponent(Graphics g) {
//			super.paintComponent(g);
//		}
	}
}
