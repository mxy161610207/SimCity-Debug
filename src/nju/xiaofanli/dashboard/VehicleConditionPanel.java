package nju.xiaofanli.dashboard;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import nju.xiaofanli.Resource;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Car.CarIcon;

class VehicleConditionPanel extends JPanel{
	private static final long serialVersionUID = 1L;
	private Map<Car, Entry> entries = new HashMap<>();
//    private GridBagConstraints gbc = new GridBagConstraints();
	VehicleConditionPanel() {
		setLayout(new GridLayout(6, 1));
//		setBackground(Color.WHITE);
//        gbc.fill = GridBagConstraints.BOTH;
	}
	
	void updateVC(Car car){
		if(entries.containsKey(car)){
			entries.get(car).update();
		}
	}
	
	void addCar(Car car){
		if(car == null)
			return;
		if(!entries.containsKey(car)){
			Entry e = new Entry(car);
			entries.put(car, e);
			add(e);
			updateVC(car);
		}
	}
	
	void removeCar(Car car){
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
		private final JTextPane text = new JTextPane();
		private final GridBagConstraints gbc = new GridBagConstraints();
		Entry(Car car) {
			this.car = car;
			this.icon = car.icon;
//			text.setLineWrap(true);
//            text.setWrapStyleWord(true);
			text.setEditable(false);
			text.setBackground(null);
            text.setFont(Resource.plain17dialog);

			setLayout(new GridBagLayout());
			gbc.insets = new Insets(1, 5, 1, 5);

            gbc.gridheight = GridBagConstraints.REMAINDER;
            gbc.weighty = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			add(icon, gbc);

            gbc.fill = GridBagConstraints.BOTH;
			gbc.gridx += gbc.gridwidth;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.weightx = 1;
			add(text, gbc);
		}
		
		void update(){
			synchronized (text) {
                text.setText("");
                StyledDocument doc = text.getStyledDocument();
                try {
                    doc.insertString(doc.getLength(), car.name + " ", null);
                    text.setCaretPosition(doc.getLength());
                    text.insertIcon(car.getState() == Car.MOVING ? Resource.MOVING_ICON : Resource.STOP_ICON);
                    doc.insertString(doc.getLength(), " ", null);
                    text.setCaretPosition(doc.getLength());
                    switch (car.dir) {
                        case TrafficMap.NORTH:
                            text.insertIcon(Resource.UP_ARROW_ICON); break;
                        case TrafficMap.SOUTH:
                            text.insertIcon(Resource.DOWN_ARROW_ICON); break;
                        case TrafficMap.WEST:
                            text.insertIcon(Resource.LEFT_ARROW_ICON); break;
                        case TrafficMap.EAST:
                            text.insertIcon(Resource.RIGHT_ARROW_ICON); break;
                        default:
                            text.insertIcon(Resource.QUESTION_MARK_ICON); break;
                    }
                    if (car.loc != null)
                        doc.insertString(doc.getLength(), "\nLoc: " + car.loc.name, null);
//                    if (car.dest != null)
//                        doc.insertString(doc.getLength(), "\nDest: " + car.dest.name, null);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
//				String str = car.name + " (" + car.getStateStr() + ") " + car.getDirStr();
//				if (car.loc != null)
//					str += "\nLoc: " + car.loc.name;
//				if (car.dest != null)
//					str += "\nDest: " + car.dest.name;
//
//				text.setText(str);
				repaint();
			}
		}
	}
}
