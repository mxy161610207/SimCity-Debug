package nju.ics.lixiaofan.city;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Car.CarIcon;
import nju.ics.lixiaofan.control.CitizenControl;
import nju.ics.lixiaofan.control.Delivery;
import nju.ics.lixiaofan.dashboard.Dashboard;

public class Citizen implements Runnable{
	public String name;
	public Gender gender = null;
	public Job job = null;
	public Activity act = null;
	public Section loc = null, dest = null;
	public Car car = null;
	public CitizenIcon icon = null;
	
	public static enum Gender {
		Male, Female
	}
	
	public static enum Job{
		Student, Driver, Doctor, Police, Cook, IronMan
	}
	
	public static enum Activity{
		Wander, GoToWork, Working, GotoSchool, InClass, Driving, Cooking, RescueTheWorld, HailATaxi, TakeATaxi, GetOff
	}
	
	public Citizen(String name, Gender gender, Job job) {
		this.name = name;
		this.gender = gender;
		this.job = job;
		icon = new CitizenIcon(this);
	}
	
	public void run() {
		while(true){
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if(act == null)
				continue;
			switch (act) {
			case Wander:{
				int xmax = icon.getParent().getWidth()-icon.getWidth();
				int ymax = icon.getParent().getHeight()-icon.getHeight();
				if(!icon.isVisible()){
					int x = (int) (Math.random() * xmax);
					int y = (int) (Math.random() * ymax);
					icon.setLocation(x, y);
					icon.setVisible(true);
					loc = Dashboard.getNearestSection(icon.getX()+CitizenIcon.SIZE/2, icon.getY()+CitizenIcon.SIZE/2);
				}
				int count = 0, x, y;
				while(count < 3){
					count++;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					x = (int) (icon.getX() + 50 * Math.random() - 25);
					if(x < 0)
						x = 0;
					else if(x > xmax)
						x = xmax;
					y = (int) (icon.getY() + 50 * Math.random() - 25);
					if(y < 0)
						y = 0;
					else if(y > ymax)
						y = ymax;
					icon.setLocation(x, y);
					loc = Dashboard.getNearestSection(icon.getX()+CitizenIcon.SIZE/2, icon.getY()+CitizenIcon.SIZE/2);
				}
				CitizenControl.sendActReq(this, null, true);
				break;
			}
			case HailATaxi:
				if(loc == null || dest == null)
					break;
				Delivery.add(loc, dest, this);
				break;
			case TakeATaxi:
				icon.setVisible(false);// get on the taxi
				break;
			case GetOff:
				if(loc != null){
					icon.setLocation(loc.icon.coord.centerX, loc.icon.coord.centerY);
					icon.setVisible(true);
				}
				CitizenControl.sendActReq(this, null, true);
				break;
			case RescueTheWorld:{
				int count = 0;
				while(count < 5){
					count++;
					icon.blink = !icon.blink;
					icon.repaint();
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				icon.blink = false;
				icon.repaint();
				CitizenControl.sendActReq(this, null, true);
				break;
			}
			default:
				break;
			}
		}
	}
	
	
	public static class CitizenIcon extends JButton{
		private static final long serialVersionUID = 1L;
		private Citizen citizen = null;
		private Color color = null;
		private boolean showName = false;
		private boolean showAct = false;
		public boolean blink = false;
		public static final int SIZE = (int) (0.8*CarIcon.SIZE);
		public CitizenIcon(Citizen citizen) {
			setOpaque(false);
			setContentAreaFilled(false);
			setSize(new Dimension(10*SIZE, SIZE));
			setVisible(false);
//			setBorderPainted(false);
			this.citizen = citizen;
			color = new Color((int) (Math.random()*256), (int) (Math.random()*256), (int) (Math.random()*256));
			addMouseListener(new Listener());
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
				
			g.drawOval(1, 1, SIZE-1, SIZE-1);
		}
		
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if(!blink){
				g.setColor(color);
				g.fillOval(0, 0, SIZE, SIZE);
				g.setColor(Color.BLACK);
				g.drawOval(1, 1, SIZE-2, SIZE-2);
			}
			if(showAct){
				g.setColor(Color.BLACK);
				String str = citizen.act == null ? "None" : citizen.act.toString();
				FontMetrics fm = g.getFontMetrics();
				g.drawString(str, (int) (1.2*SIZE), (getHeight()+fm.getAscent())/2);
			}
			else if(showName){
				g.setColor(Color.BLACK);
				String str = citizen.name;
				FontMetrics fm = g.getFontMetrics();
				g.drawString(str, (int) (1.2*SIZE), (getHeight()+fm.getAscent())/2);
			}
		}
		
		private class Listener implements MouseListener{

			public void mouseClicked(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
				showName = true;
			}

			public void mouseExited(MouseEvent e) {
				showName = false;
				showAct = false;
			}

			public void mousePressed(MouseEvent e) {
				showAct = !showAct;
			}

			public void mouseReleased(MouseEvent e) {
			}
			
		}
	}
}
