package nju.xiaofanli.device.sensor;

import nju.xiaofanli.Resource;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.dashboard.Road;
import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.device.car.Car;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.min;

public class Sensor {
    public int bid;
    public int sid;
    public String name;
    public int state;
    public int reading = 30;
    public int entryThreshold, leaveThreshold;
    public Car car = null;
    public Road nextRoad = null;
    public Road prevRoad = null;
    public Sensor nextSensor = null;
    public Sensor prevSensor = null;
    public TrafficMap.Direction dir;
    public int px, py;
    public JButton icon = null;
    public BalloonIcon balloon = null;

    public final static int INITIAL = 0;
    public final static int DETECTED = 1;
    public final static int UNDETECTED = 2;

    public Sensor(int bid, int sid) {
        this.bid = bid;
        this.sid = sid;
        name = "B" + bid + "S" + (sid+1);
        state = Sensor.UNDETECTED;
        entryThreshold = (int) (undetectedReading[bid][sid] * 0.65);
        //mxy_edit old=0.75;10;
        leaveThreshold = (int) (undetectedReading[bid][sid] * 0.8);
        //mxy_edit old=0.9; entryThreshold + 1; //11;

        icon = new JButton(name);
        icon.setFont(Resource.en16bold);
        icon.setVisible(false);
        icon.setMargin(new Insets(0, 0, 0, 0));
        icon.addMouseListener(new SensorIconListener(this));

        balloon = new BalloonIcon(this);
    }

    boolean entryDetected(int reading){
        return reading <= entryThreshold;
    }

    boolean leaveDetected(int reading){
        return reading >= leaveThreshold;
    }

    public void reset() {
        if(entryDetected(reading))
            state = DETECTED;
        else if(leaveDetected(reading))
            state = UNDETECTED;
        else
//			state = Sensor.INITIAL;
            state = UNDETECTED;
    }

    public TrafficMap.Direction getNextRoadDir() {
        return nextRoad.dir[1] == TrafficMap.Direction.UNKNOWN ? nextRoad.dir[0] : dir;
    }

    private static class SensorIconListener extends MouseAdapter{
        private Sensor sensor;
        SensorIconListener(Sensor sensor) {
            this.sensor = sensor;
        }
        public void mousePressed(MouseEvent e) {
            switch (e.getButton()) {
                case MouseEvent.BUTTON1:
//				System.out.println("left click");
                    BrickHandler.insert(sensor, 0, System.currentTimeMillis());
                    break;
                case MouseEvent.BUTTON3:
//				System.out.println("right click");
                    BrickHandler.insert(sensor, 30, System.currentTimeMillis());
                    break;
                case MouseEvent.BUTTON2: {
                    BrickServer.showingSensor = BrickServer.showingSensor != sensor ? sensor : null;
                    String str = "[" + sensor.name + "] state=";
                    switch (sensor.state) {
                        case INITIAL:
                            str += "INITIAL";
                            break;
                        case DETECTED:
                            str += "DETECTED";
                            break;
                        case UNDETECTED:
                            str += "UNDETECTED";
                            break;
                    }
                    str += ", entryThreshold="+sensor.entryThreshold+", leaveThreshold="+sensor.leaveThreshold+", reading="+sensor.reading;
                    System.out.println(str);
                    //mxy_edit: output DETECT log to file
//                    File f= new File("mxy_temp\\Sensor.txt");
//                    try (FileOutputStream fop = new FileOutputStream(f,true)){
//                        if(!f.exists()){
//                            f.createNewFile();
//                        }
//                        byte[] content = str.getBytes();
//                        fop.write(content);
//                        fop.flush();
//                        fop.close();
//                    }catch (IOException ex){
//                        ex.printStackTrace();
//                    }
                    // == EDIT END ==
                }
                break;
            }
        }
    }

    private static final int[][] undetectedReading = new int[10][4];
    static {
        //mxy_edit
        undetectedReading[0][0] = 25;   //25-26
        undetectedReading[0][1] = 29;   //29-30
        undetectedReading[1][0] = 25;   //25-26
        undetectedReading[1][1] = 27;   //27-30
        undetectedReading[1][2] = 27;   //27-30
        undetectedReading[2][0] = 19;   //19-20
        undetectedReading[2][1] = 29;   //29-30
        undetectedReading[2][2] = 28;   //28-31
        undetectedReading[2][3] = 21;   //21-22
        undetectedReading[3][0] = 24;   //24-24
        undetectedReading[3][1] = 25;   //25-26
        undetectedReading[3][2] = 25;   //25-26
        undetectedReading[3][3] = 20;   //20-22
        undetectedReading[4][0] = 23;   //23-24
        undetectedReading[4][1] = 23;   //23-24
        undetectedReading[4][2] = 25;   //24-27
        undetectedReading[5][0] = 18;   //18-19
        undetectedReading[5][1] = 24;   //24-25
        undetectedReading[5][2] = 22;   //22-23
        undetectedReading[6][0] = 19;   //19-19
        undetectedReading[6][1] = 23;   //23-24
        undetectedReading[6][2] = 23;   //23-24
        undetectedReading[6][3] = 22;   //22-23
        undetectedReading[7][0] = 27;   //27-28
        undetectedReading[7][1] = 22;   //22-24
        undetectedReading[7][2] = 24;   //24-25
        undetectedReading[7][3] = 26;   //26-26
        undetectedReading[8][0] = 19;   //19-20
        undetectedReading[8][1] = 29;   //29-31
        undetectedReading[8][2] = 20;   //20-21
        undetectedReading[9][0] = 27;   //27-28
        undetectedReading[9][1] = 31;   //31-32
//        undetectedReading[0][0] = 23;
//        undetectedReading[0][1] = 26;
//        undetectedReading[1][0] = 22;
//        undetectedReading[1][1] = 16;
//        undetectedReading[1][2] = 17;
//        undetectedReading[2][0] = 20;
//        undetectedReading[2][1] = 18;
//        undetectedReading[2][2] = 21;
//        undetectedReading[2][3] = 16;
//        undetectedReading[3][0] = 14;
//        undetectedReading[3][1] = 20;
//        undetectedReading[3][2] = 16;
//        undetectedReading[3][3] = 16;
//        undetectedReading[4][0] = 14;
//        undetectedReading[4][1] = 18;
//        undetectedReading[4][2] = 13;
//        undetectedReading[5][0] = 16;
//        undetectedReading[5][1] = 21;
//        undetectedReading[5][2] = 15;
//        undetectedReading[6][0] = 16;
//        undetectedReading[6][1] = 17;
//        undetectedReading[6][2] = 16;
//        undetectedReading[6][3] = 18;
//        undetectedReading[7][0] = 15;
//        undetectedReading[7][1] = 18;
//        undetectedReading[7][2] = 17;
//        undetectedReading[7][3] = 19;
//        undetectedReading[8][0] = 16;
//        undetectedReading[8][1] = 23;
//        undetectedReading[8][2] = 22;
//        undetectedReading[9][0] = 20;
//        undetectedReading[9][1] = 18;
    }

    public void showBalloon(int type, String car, boolean isResolutionEnabled) {
        if(!Dashboard.showError)
            return;
        balloon.type = type;
        balloon.sensor = name;
        balloon.car = car;
        balloon.duration = 3000;//display for 3s
        balloon.setIcon(isResolutionEnabled);
        balloon.setVisible(true);

        PkgHandler.send(new AppPkg().setBalloon(type, name, car, isResolutionEnabled));
    }

    public static class BalloonIcon extends JButton{
        private static final long serialVersionUID = 1L;
        public static int WIDTH = 70;
        public static int HEIGHT = 70;
        private static Map<Boolean, Map<Boolean, ImageIcon>> balloons = new HashMap<>(); // <resolution, reversed> -> icon
        public int duration = 0;
        public int type;
        public String sensor = "", car = "";
        public final boolean useReversedIcon;

        static {
            balloons.put(false, new HashMap<>());
            balloons.put(true, new HashMap<>());
            balloons.get(false).put(false, Resource.loadImage("runtime/res/red_balloon.png", WIDTH, HEIGHT));
            balloons.get(false).put(true, Resource.loadImage("runtime/res/reversed_red_balloon.png", WIDTH, HEIGHT));
            balloons.get(true).put(false, Resource.loadImage("runtime/res/green_balloon.png", WIDTH, HEIGHT));
            balloons.get(true).put(true, Resource.loadImage("runtime/res/reversed_green_balloon.png", WIDTH, HEIGHT));
            WIDTH = balloons.get(false).get(false).getIconWidth();
            HEIGHT = balloons.get(false).get(false).getIconHeight();
        }

        BalloonIcon(Sensor sensor) {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setVisible(false);

            switch (sensor.bid*10+sensor.sid){
                case 0:case 21:case 22:case 50:case 52:case 40:case 42:case 71:case 72:case 90:
                    useReversedIcon = true; break;
                default:
                    useReversedIcon = false; break;
            }
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            FontMetrics fm = g.getFontMetrics();
            String str = "";
            if(type == Context.FP)
                str = "-FP-";
            else if(type == Context.FN)
                str = "-FN-";
            g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2+(useReversedIcon?-12:-24));
            str = sensor;
            g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2+(useReversedIcon?0:-12));
            str = car;
            g.drawString(str, (getWidth()-fm.stringWidth(str))/2, (getHeight()+fm.getAscent())/2+(useReversedIcon?12:0));
        }

        private void setIcon(boolean resolutionEnabled){
            setIcon(balloons.get(resolutionEnabled).get(useReversedIcon));
        }
    }
}
