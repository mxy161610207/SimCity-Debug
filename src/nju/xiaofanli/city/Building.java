package nju.xiaofanli.city;

import nju.xiaofanli.Resource;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public class Building extends Location{
	public Type type = null;
	public int block = -1;//block number
	public BuildingIcon icon = null;
	public Set<Section> addrs = null;
	
	public enum Type {
		Hospital, School, PoliceStation, Restaurant, StarkIndustries
	}
	
	public Building(String name, Type type, int block, String iconFile) {
		this.name = name;
		this.type = type;
		this.block  = block;
        this.icon = new BuildingIcon(this, iconFile);
        this.addrs = new HashSet<>();
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
        private final String iconFile;
		TrafficMap.Coord coord = new TrafficMap.Coord();
		
		BuildingIcon(Building building, String iconFile) {
			setOpaque(false);
			setContentAreaFilled(false);
//			setBorderPainted(false);
			this.building = building;
            this.iconFile = iconFile;
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
		
		void setIcon(){
			imageIcon = Resource.loadImage(iconFile, coord.w, coord.h);
			setIcon(imageIcon);
		}
		
//		protected void paintComponent(Graphics g) {
//			super.paintComponent(g);
//		}
	}
}
