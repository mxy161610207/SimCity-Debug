package nju.ics.lixiaofan.city;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Car.CarIcon;
import nju.ics.lixiaofan.dashboard.Dashboard;

public class SectionIcon extends JButton{
	private static final long serialVersionUID = 1L;
	public final static int cubeSize = CarIcon.SIZE;
	private final static int cubeInset = TrafficMap.sw - cubeSize;
	public int id;
	public Section section = null;
	
	public SectionIcon() {
//		super();
		setOpaque(false);
		setContentAreaFilled(false);
//		setBorderPainted(false);
	}
	
	public Coord coord = new Coord();
	public static class Coord{
		public int x, y, w, h;
		public int arcw ,arch;
		public int centerX, centerY;
	}
	
	protected void paintBorder(Graphics g) {
//		super.paintBorder(g);
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
	
//	public boolean contains(int x, int y) {
//		return super.contains(x, y);
//	}
	
	protected void paintComponent(Graphics g) {
//		System.out.println("children");
		super.paintComponent(g);
		int n = section.cars.size();
		switch(n){
		case 0:
			g.setColor(Color.GREEN); break;
		case 1:
			g.setColor(Color.YELLOW); break;
		default:
			g.setColor(Color.RED); break;
		}
		
		if(this instanceof CrossingIcon)
			g.fillOval(0, 0, coord.w, coord.h);
		else if(this instanceof StreetIcon)
			g.fillRoundRect(0, 0, coord.w, coord.h, coord.arcw, coord.arch);
		
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
				g.drawRect(x, y, cubeSize, cubeSize);
				if(vertical)
					y += cubeSize + cubeInset;
				else
					x += cubeSize + cubeInset;
			}
		}
		
		//draw sections
		if(TrafficMap.showSections){
			g.setColor(Color.BLACK);
			String str = id+"";
			FontMetrics fm = g.getFontMetrics();
			g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2);
		}
	}
	
	public static class CrossingIcon extends SectionIcon{
		private static final long serialVersionUID = 1L;
	}
	
	public static class StreetIcon extends SectionIcon{
		private static final long serialVersionUID = 1L;
		public boolean isVertical;
	}
}
