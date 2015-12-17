package nju.ics.lixiaofan.city;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import nju.ics.lixiaofan.city.TrafficMap.Coord;

public class Building extends Location{
	public Type type = null;
	public int block;//block number
	public BuildingIcon icon = new BuildingIcon(this);
	public Set<Section> addrs = new HashSet<Section>();
	
	public static enum Type {
		Hospital, School, PoliceStation, Restaurant, StarkIndustries
	}
	
	public Building(String name, Type type, int block) {
		this.name = name;
		this.type = type;
		this.block  = block;
	}
	
	public static Type typeOf(String type){
		for(Type t : Type.values())
			if(t.toString().equals(type))
				return t;
		return null;
	}
	
	public static class BuildingIcon extends JButton{
		private static final long serialVersionUID = 1L;
		private Building building = null;
		private ImageIcon imageIcon = null;
		public Coord coord = new Coord();
		
		public BuildingIcon(Building building) {
			setOpaque(false);
			setContentAreaFilled(false);
//			setBorderPainted(false);
			this.building = building;
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
			
			g.drawRect(1, 1, getWidth()-2, getHeight()-2);
		}
		
//		@Override
//		protected void paintComponent(Graphics g) {
//			super.paintComponent(g);
//			g.drawString("test", 10, 10);
//		}
		
		public void setIcon(){
			switch (building.type) {
			case StarkIndustries:
				imageIcon = new ImageIcon("res/stark_industries.png");
				break;
			case Hospital:
				imageIcon = new ImageIcon("res/hospital.png");
				break;
			case School:
				imageIcon = new ImageIcon("res/nju.png");
				break;
			case PoliceStation:
				imageIcon = new ImageIcon("res/shield.png");
				break;
			case Restaurant:
				imageIcon = new ImageIcon("res/java.png");
				break;
			default:
				return;
			}
			Image image = imageIcon.getImage();
			if(imageIcon.getIconWidth() > imageIcon.getIconHeight())
				image = image.getScaledInstance(coord.w, -1, Image.SCALE_SMOOTH);
			else
				image = image.getScaledInstance(-1, coord.h, Image.SCALE_SMOOTH);
			imageIcon = new ImageIcon(image);
			setIcon(imageIcon);
		}
		
//		protected void paintComponent(Graphics g) {
//			super.paintComponent(g);
//		}
	}
}
