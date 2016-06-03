package nju.ics.lixiaofan.dashboard;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.car.Car.CarIcon;

public class VehicleConditionPanel extends JPanel{
	private static final long serialVersionUID = 1L;
	private Map<Car, Entry> entries = new HashMap<Car, Entry>();
	public VehicleConditionPanel() {
		setLayout(new GridLayout(0, 1));
//		setBackground(Color.WHITE);
	}
	
	public void updateVC(Car car){
		if(entries.containsKey(car)){
			entries.get(car).update();
		}
	}
	
	public void addCar(Car car){
		if(car == null)
			return;
		if(!entries.containsKey(car)){
			Entry e = new Entry(car);
			entries.put(car, e);
			add(e);
			updateVC(car);
		}
	}
	
	public void removeCar(Car car){
		if(car == null)
			return;
		if(entries.containsKey(car)){
			Entry e = entries.remove(car);
			remove(e);
			repaint();
		}
	}
	
	private class Entry extends JPanel{
		private static final long serialVersionUID = 1L;
		private Car car = null;
		private CarIcon icon = null;
		private JTextArea text = new JTextArea();
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		public Entry(Car car) {
			this.car = car;
			this.icon = car.icon;
			text.setLineWrap(true);
			text.setWrapStyleWord(true);
			text.setEditable(false);
			
			setLayout(gbl);
			add(icon);
			add(text);
			gbc.insets = new Insets(1, 5, 1, 5);
			
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbl.setConstraints(icon, gbc);
			
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx++;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbl.setConstraints(text, gbc);
		}
		
		public void update(){
			String str = car.name+" (" + car.getStatusStr() + ") "+car.getDirStr();
			if(car.loc != null)
				str += "\nLoc: " + car.loc.name;
			if(car.dest != null)
				str += "\nDst: " + car.dest.name;
			
			text.setText(str);
			repaint();
		}
	}
}
