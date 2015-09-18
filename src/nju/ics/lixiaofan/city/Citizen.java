package nju.ics.lixiaofan.city;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Car.CarIcon;

public class Citizen {
	public String name;
	public Gender gender = null;
	public Job job = null;
	public Section loc = null, dest = null;
	public Car car = null;
	public CitizenIcon icon = null;
	
	public static enum Gender {
		Male, Female
	}
	
	public static enum Job{
		None, Student, Driver, Doctor, Police, Cook
	}
	
	public Citizen(String name, Gender gender, Job job) {
		this.name = name;
		this.gender = gender;
		this.job = job;
		icon = new CitizenIcon(this);
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
			setPreferredSize(new Dimension(SIZE, SIZE));
//			setBorderPainted(false);
			this.citizen = citizen;
			color = new Color((int) (Math.random()*256), (int) (Math.random()*256), (int) (Math.random()*256));
//			setSize(size, size);
//			setMinimumSize(new Dimension(size, size));
			addMouseListener(new Listener());
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
				g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2);
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
