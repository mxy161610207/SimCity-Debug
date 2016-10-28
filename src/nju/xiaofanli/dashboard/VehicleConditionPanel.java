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
        private final Map<Integer, JLabel> stateIcons = new HashMap<>(), dirIcons = new HashMap<>();
        private final double ASCENT_PIXEL;
        private final int ICON_SIZE;
		Entry(Car car) {
			this.car = car;
			this.icon = car.icon;
//			text.setLineWrap(true);
//            text.setWrapStyleWord(true);
			text.setEditable(false);
			text.setBackground(null);
            text.setFont(Resource.plain17dialog);

            ASCENT_PIXEL = text.getFont().createGlyphVector(text.getFontMetrics(text.getFont()).getFontRenderContext(), "A").getVisualBounds().getHeight();
            ICON_SIZE = (int) (ASCENT_PIXEL * 1.5);
            stateIcons.put(Car.MOVING, new JLabel(Resource.loadImage(Resource.MOVING_ICON, ICON_SIZE, ICON_SIZE)));
            stateIcons.put(Car.STOPPED, new JLabel(Resource.loadImage(Resource.STOP_ICON, ICON_SIZE, ICON_SIZE)));
            dirIcons.put(TrafficMap.NORTH, new JLabel(Resource.loadImage(Resource.UP_ARROW_ICON, ICON_SIZE, ICON_SIZE)));
            dirIcons.put(TrafficMap.SOUTH, new JLabel(Resource.loadImage(Resource.DOWN_ARROW_ICON, ICON_SIZE, ICON_SIZE)));
            dirIcons.put(TrafficMap.WEST, new JLabel(Resource.loadImage(Resource.LEFT_ARROW_ICON, ICON_SIZE, ICON_SIZE)));
            dirIcons.put(TrafficMap.EAST, new JLabel(Resource.loadImage(Resource.RIGHT_ARROW_ICON, ICON_SIZE, ICON_SIZE)));
            dirIcons.put(TrafficMap.UNKNOWN_DIR, new JLabel(Resource.loadImage(Resource.QUESTION_MARK_ICON, ICON_SIZE, ICON_SIZE)));
            stateIcons.values().forEach(label -> label.setAlignmentY((float) (ASCENT_PIXEL/ICON_SIZE/2 + 0.5)));
            dirIcons.values().forEach(label -> label.setAlignmentY((float) (ASCENT_PIXEL/ICON_SIZE/2 + 0.5)));
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
                    text.insertComponent(stateIcons.get(car.getState()));
                    doc.insertString(doc.getLength(), " ", null);
                    text.setCaretPosition(doc.getLength());
                    text.insertComponent(dirIcons.get(car.dir));
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
