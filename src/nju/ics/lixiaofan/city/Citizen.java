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
import nju.ics.lixiaofan.city.SectionIcon.CrossingButton;
import nju.ics.lixiaofan.city.SectionIcon.StreetButton;

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
		Wander, GoToWork, Working, GotoSchool, InClass, Driving, Cooking, RescueTheWorld
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
			case Wander:
				if(!icon.isVisible()){
					icon.setVisible(true);
					icon.setLocation(100, 100);
				}
				break;
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
		public static final int SIZE = (int) (0.8*CarIcon.SIZE);
		public CitizenIcon(Citizen citizen) {
			setOpaque(false);
			setContentAreaFilled(false);
			setSize(new Dimension(5*SIZE, SIZE));
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
				
			g.drawOval(0, 0, SIZE, SIZE);
		}
		
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(color);
			g.fillOval(0, 0, SIZE, SIZE);
			g.setColor(Color.BLACK);
			g.drawOval(0, 0, SIZE, SIZE);
			
			if(showName){
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
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}
			
		}
	}
}
