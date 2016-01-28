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
import nju.ics.lixiaofan.monitor.AppPkg;
import nju.ics.lixiaofan.monitor.PkgHandler;

public class Citizen implements Runnable{
	public String name;
	public Gender gender = null;
	public Job job = null;
	public Activity act = null, nextAct = null;
	public Location loc = null, dest = null;
	public Car car = null;
	public CitizenIcon icon = new CitizenIcon(this);
	
	public static enum Gender {
		Male, Female
	}
	
	public static enum Job{
		Student, Doctor, Police, Cook, IronMan
	}
	
	public static enum Activity {
		Wander, GoToWork, AtWork, GoToSchool, InClass, RescueTheWorld, HailATaxi, TakeATaxi, GetOff, 
		GoToHospital, UnderTreatment, GetSick, GetHungry, GoToEat, HavingMeals
	}

	public Citizen(String name, Gender gender, Job job) {
		this.name = name;
		this.gender = gender;
		this.job = job;
	}
	
	public static Gender genderOf(String gender){
		for(Gender g : Gender.values())
			if(g.toString().equals(gender))
				return g;
		return null;
	}
	
	public static Job jobOf(String job){
		for(Job j : Job.values())
			if(j.toString().equals(job))
				return j;
		return null;
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
				int xmax = icon.getParent().getWidth()-CitizenIcon.SIZE;
				int ymax = icon.getParent().getHeight()-CitizenIcon.SIZE;
				if(!icon.isVisible()){
					int x = (int) (Math.random() * xmax);
					int y = (int) (Math.random() * ymax);
					x = TrafficMap.streets[23].icon.coord.x;
					y = TrafficMap.streets[23].icon.coord.y;
					icon.setLocation(x, y);
					icon.setVisible(true);
					loc = Dashboard.getNearestSection(icon.getX()+CitizenIcon.SIZE/2, icon.getY()+CitizenIcon.SIZE/2);
					
					PkgHandler.send(new AppPkg().setCitizen(name, (double) x/TrafficMap.size, (double) y/TrafficMap.size));
					PkgHandler.send(new AppPkg().setCitizen(name, true));
				}
//				int x, y;
//				for(int count = 0;count < 3;count++){
//					try {
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					x = (int) (icon.getX() + 50 * Math.random() - 25);
//					if(x < 0)
//						x = 0;
//					else if(x > xmax)
//						x = xmax;
//					y = (int) (icon.getY() + 50 * Math.random() - 25);
//					if(y < 0)
//						y = 0;
//					else if(y > ymax)
//						y = ymax;
//					icon.setLocation(x, y);
//					loc = Dashboard.getNearestSection(icon.getX()+CitizenIcon.SIZE/2, icon.getY()+CitizenIcon.SIZE/2);
//					
//					PkgHandler.send(new AppPkg().setCitizen(name, (double) x/TrafficMap.size, (double) y/TrafficMap.size));
//				}
				CitizenControl.sendActReq(this, null, true);
				break;
			}
			case HailATaxi:
				if(loc == null || dest == null)
					break;
				Delivery.add(loc, dest, this);
//				Delivery.add(TrafficMap.crossings[5], TrafficMap.buildings.get(Building.Type.Restaurant), this);
				break;
			case TakeATaxi:{
				icon.setVisible(false);// get on the taxi
				PkgHandler.send(new AppPkg().setCitizen(name, false));
				break;
			}
			case GetOff:
				if(loc != null){
					int x, y;
					if(loc instanceof Section){
						x = ((Section) loc).icon.coord.centerX;
						y = ((Section) loc).icon.coord.centerY;
					}
					else{
						x = ((Building) loc).icon.coord.centerX;
						y = ((Building) loc).icon.coord.centerY;
					}
					icon.setLocation(x, y);
					icon.setVisible(true);
					
					PkgHandler.send(new AppPkg().setCitizen(name, (double) x/TrafficMap.size, (double) y/TrafficMap.size));
					PkgHandler.send(new AppPkg().setCitizen(name, true));
				}
				if(nextAct == null){
					dest = null;
					CitizenControl.sendActReq(this, null, true);
				}
				else{
					if (nextAct != Activity.AtWork
							&& nextAct != Activity.InClass
							&& nextAct != Activity.UnderTreatment
							&& nextAct != Activity.HavingMeals)
						dest = null;
					CitizenControl.sendActReq(this, nextAct);
					nextAct = null;
				}
				break;
			case GetSick:case GetHungry:
				for(int count = 0;count < 15;count++){
					icon.blink = !icon.blink;
					icon.repaint();
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				icon.blink = false;
				icon.repaint();
				if(act == Activity.GetSick)
					CitizenControl.sendActReq(this, Activity.GoToHospital);
				else if(act == Activity.GetHungry)
					CitizenControl.sendActReq(this, Activity.GoToEat);
				break;
			case GoToSchool:case GoToWork:case GoToHospital:case GoToEat:
				if(act == Activity.GoToHospital){
					nextAct = Activity.UnderTreatment;
					dest = TrafficMap.buildings.get(Building.Type.Hospital);
				}
				else if(act == Activity.GoToEat){
					nextAct = Activity.HavingMeals;
					dest = TrafficMap.buildings.get(Building.Type.Restaurant);
				}
				else{
					if(act == Activity.GoToWork)
						nextAct = Activity.AtWork;
					else if(act == Activity.GoToSchool)
						nextAct = Activity.InClass;
					
					switch (job) {
					case Student:
						dest = TrafficMap.buildings.get(Building.Type.School);
						break;
					case Cook:
						dest = TrafficMap.buildings.get(Building.Type.Restaurant);
						break;
					case Doctor:
						dest = TrafficMap.buildings.get(Building.Type.Hospital);
						break;
					case IronMan:
						dest = TrafficMap.buildings.get(Building.Type.StarkIndustries);
						break;
					case Police:
						dest = TrafficMap.buildings.get(Building.Type.PoliceStation);
						break;
					default:
						dest = null;
						break;
					}
				}
				
				if((loc instanceof Building && loc == dest) ||
						(loc instanceof Section && ((Building) dest).addrs.contains(loc))){
					CitizenControl.sendActReq(this, nextAct);
					nextAct = null;
				}
				else
					CitizenControl.sendActReq(this, Activity.HailATaxi);
				break;
			case AtWork:case InClass:case UnderTreatment:case HavingMeals:{
				loc = dest;
//				System.out.println(dest instanceof Building);
				int xmax = ((Building) dest).icon.getWidth() - CitizenIcon.SIZE;
				int ymax = ((Building) dest).icon.getHeight() - CitizenIcon.SIZE;
				int x = (int) (Math.random() * xmax) + ((Building) dest).icon.coord.x;
				int y = (int) (Math.random() * ymax) + ((Building) dest).icon.coord.y;
				icon.setLocation(x, y);
				
				PkgHandler.send(new AppPkg().setCitizen(name, (double) x/TrafficMap.size, (double) y/TrafficMap.size));
				for(int count = 0;count < 50;count++){
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
			case RescueTheWorld:
				for(int count = 0;count < 5;count++){
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
			default:
				break;
			}
		}
	}
	
	
	public static class CitizenIcon extends JButton{
		private static final long serialVersionUID = 1L;
		private Citizen citizen = null;
		public Color color = null;
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
				if(citizen.act != null && citizen.act == Activity.GetSick)
					g.setColor(Color.RED);
				else if(citizen.act != null && citizen.act == Activity.GetHungry)
					g.setColor(Color.YELLOW);
				else
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
