package nju.xiaofanli.dashboard;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Car.CarIcon;

import javax.swing.*;
import java.awt.*;

public class Citizen implements Runnable {
    public final String name;
    public final Gender gender;
    public final Job job;
    private Action action = null, nextAction = null;
    public Action state = null;
    private long delay = 0;
    public Location loc = null, dest = null;
    public Car car = null;
    public CitizenIcon icon = null;
    public boolean createdByUser = false; //used in taking taxis
    public boolean isRunning = false;

    public enum Gender {
        Male, Female
    }

    public enum Job{
        Student, Doctor, Police, Cook, SuperHero, Reporter
    }

    public enum Action {
        Wander, GoToWork, AtWork, GoToSchool, InClass, RescueTheWorld, HailATaxi, TakeATaxi, GetOff,
        GoToHospital, UnderTreatment, GetSick, GetHungry, GoToEat, HavingMeals, Disappear
    }

    public Citizen(String name, Gender gender, Job job, String iconFile, int rgb) {
        this.name = name;
        this.gender = gender;
        this.job = job;
        this.icon = new CitizenIcon(this, iconFile, rgb);
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

    public void reset() {
        action = nextAction = state = null;
        loc = dest = null;
        car = null;
        delay = 0;
        isRunning = createdByUser = false;
        icon.setVisible(false);
        synchronized (TrafficMap.freeCitizens) {
            if (!TrafficMap.freeCitizens.contains(this))
                TrafficMap.freeCitizens.add(this);
        }
    }

    public void setAction(Action action){
        this.action = action;
        synchronized (this){
            this.notify();
        }
    }

    public void startAction() {
        if(!isRunning) {
            isRunning = true;
            Resource.execute(this);
        }
    }

    public void run() {
        Thread thread = Thread.currentThread();
        StateSwitcher.register(thread);

        int[] count = new int[Action.values().length];
        for(int i = 0;i < count.length;i++)
            count[i] = 0;
        //noinspection InfiniteLoopStatement
        while(true){
            while(action == null || !StateSwitcher.isNormal())
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
//                        e.printStackTrace();
                        if(StateSwitcher.isResetting()) {
                            StateSwitcher.unregister(thread);
                            reset();
                            return;
                        }
                    }
                }
            if(action == null)
                continue;

            if(delay > 0) {
                long start = System.currentTimeMillis();
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                    if (StateSwitcher.isResetting()) {
                        thread.interrupt();
                        continue;
                    }
                    else if (StateSwitcher.isSuspending()) {
                        long leftTime = delay - (System.currentTimeMillis() - start);
                        if(leftTime > 0)
                            try {
                                Thread.sleep(leftTime);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                    }
                }
            }
            state = action;
            switch (action) {
                case Wander: {
                    int xmax = icon.getParent().getWidth() - icon.dimension.width;
                    int ymax = icon.getParent().getHeight() - icon.dimension.height;
                    if (count[action.ordinal()] == 0) {
                        count[action.ordinal()] = 3;
                        delay = 1000;
                        if (!icon.isVisible()) {
                            int x = (int) (Math.random() * xmax);
                            int y = (int) (Math.random() * ymax);
//						x = TrafficMap.streets[23].icon.coord.x;
//						y = TrafficMap.streets[23].icon.coord.y;
                            icon.setLocation(x, y);
                            icon.setVisible(true);
                            loc = Dashboard.getNearestRoad(icon.getCenterX(), icon.getCenterY());

                            PkgHandler.send(new AppPkg().setCitizen(name, (double) x / TrafficMap.SIZE, (double) y / TrafficMap.SIZE));
                            PkgHandler.send(new AppPkg().setCitizen(name, true));
                        }
                    } else {
                        count[action.ordinal()]--;
                        int x = (int) (icon.getX() + 50 * Math.random() - 25);
                        if (x < 0)
                            x = 0;
                        else if (x > xmax)
                            x = xmax;
                        int y = (int) (icon.getY() + 50 * Math.random() - 25);
                        if (y < 0)
                            y = 0;
                        else if (y > ymax)
                            y = ymax;
                        icon.setLocation(x, y);
                        loc = Dashboard.getNearestRoad(icon.getCenterX(), icon.getCenterY());

                        PkgHandler.send(new AppPkg().setCitizen(name, (double) x / TrafficMap.SIZE, (double) y / TrafficMap.SIZE));

                        if (count[action.ordinal()] == 0) {
                            state = action = null;
                            delay = 0;
                            PkgHandler.send(new AppPkg().setCitizen(name, "None"));
                        }
                    }
                    break;
                }
                case HailATaxi:
                    action = null;
                    if(!icon.isVisible()){
                        int x, y;
                        if(loc instanceof Road) {
                            Road.RoadIcon icon = ((Road) loc).icon.getARoadIcon();
                            x = icon.road.icon.coord.x + icon.coord.centerX;
                            y = icon.road.icon.coord.y + icon.coord.centerY;
                        }
                        else{
                            x = ((Building) loc).icon.coord.centerX;
                            y = ((Building) loc).icon.coord.centerY;
                        }
                        icon.setLocation(x-icon.getWidth()/8, y);
                        icon.setVisible(true);
                    }
                    Delivery.add(loc, dest, this, createdByUser);
                    break;
                case TakeATaxi:
                    action = null;
                    icon.setVisible(false);// get on the taxi
                    PkgHandler.send(new AppPkg().setCitizen(name, false));
                    break;
                case GetOff:
                    if(loc != null){
                        int x, y;
                        if(loc instanceof Road){
                            Road.RoadIcon icon = ((Road) loc).icon.getARoadIcon();
                            x = icon.road.icon.coord.x + icon.coord.centerX;
                            y = icon.road.icon.coord.y + icon.coord.centerY;
                        }
                        else{
                            x = ((Building) loc).icon.coord.centerX;
                            y = ((Building) loc).icon.coord.centerY;
                        }
                        icon.setLocation(x-icon.getWidth()/8, y);
                        icon.setVisible(true);

                        PkgHandler.send(new AppPkg().setCitizen(name, (double) x/TrafficMap.SIZE, (double) y/TrafficMap.SIZE));
                        PkgHandler.send(new AppPkg().setCitizen(name, true));
                    }
                    if(nextAction == null){
//                        dest = null;
//                        state = action = null;
//                        PkgHandler.send(new AppPkg().setCitizen(name, "None"));
                        delay = 3000;
                        action = Action.Disappear;
                    }
                    else{
                        if (nextAction != Action.AtWork && nextAction != Action.InClass
                                && nextAction != Action.UnderTreatment && nextAction != Action.HavingMeals) {
                            dest = null;
                        }
                        action = nextAction;
                        nextAction = null;
                        PkgHandler.send(new AppPkg().setCitizen(name, action.toString()));
                    }
                    break;
                case GetSick:case GetHungry:
                    if(count[action.ordinal()] == 0){
                        count[action.ordinal()] = 15;
                        delay = 300;
                    }
                    else{
                        count[action.ordinal()]--;
                        icon.blink = !icon.blink;
                        icon.repaint();
                        if(count[action.ordinal()] == 0){
                            delay = 0;
                            icon.blink = false;
                            icon.repaint();
                            if(action == Action.GetSick)
                                action = Action.GoToHospital;
                            else if(action == Action.GetHungry)
                                action = Action.GoToEat;
                            PkgHandler.send(new AppPkg().setCitizen(name, action.toString()));
                        }
                    }
                    break;
                case GoToSchool:case GoToWork:case GoToHospital:case GoToEat:
                    if(action == Action.GoToHospital){
                        nextAction = Action.UnderTreatment;
                        dest = TrafficMap.buildings.get(Building.Type.Hospital);
                    }
                    else if(action == Action.GoToEat){
                        nextAction = Action.HavingMeals;
                        dest = TrafficMap.buildings.get(Building.Type.Restaurant);
                    }
                    else{
                        if(action == Action.GoToWork)
                            nextAction = Action.AtWork;
                        else if(action == Action.GoToSchool)
                            nextAction = Action.InClass;

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
                            case SuperHero:
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

                    if((loc instanceof Building && loc == dest)
                            || (loc instanceof Road && ((Building) dest).addrs.contains(loc))){
                        action = nextAction;
                        nextAction = null;
                    }
                    else
                        action = Action.HailATaxi;
                    PkgHandler.send(new AppPkg().setCitizen(name, action.toString()));
                    break;
                case AtWork:case InClass:case UnderTreatment:case HavingMeals:
                    if(count[action.ordinal()] == 0){
                        count[action.ordinal()] = 50;
                        delay = 500;
                        if(dest == null)
                            throw new NullPointerException();
                        loc = dest;
                        int xmax = ((Building) dest).icon.getWidth() - icon.dimension.width;
                        int ymax = ((Building) dest).icon.getHeight() - icon.dimension.height;
                        int x = (int) (Math.random() * xmax) + ((Building) dest).icon.coord.x;
                        int y = (int) (Math.random() * ymax) + ((Building) dest).icon.coord.y;
                        icon.setLocation(x, y);

                        PkgHandler.send(new AppPkg().setCitizen(name, (double) x/TrafficMap.SIZE, (double) y/TrafficMap.SIZE));
                    }
                    else{
                        count[action.ordinal()]--;
                        icon.blink = !icon.blink;
                        icon.repaint();
                        if(count[action.ordinal()] == 0){
                            delay = 0;
                            state = action = null;
                            icon.blink = false;
                            icon.repaint();
                            PkgHandler.send(new AppPkg().setCitizen(name, "None"));
                        }
                    }
                    break;
                case RescueTheWorld:
                    if(count[action.ordinal()] == 0){
                        count[action.ordinal()] = 5;
                        delay = 500;
                    }
                    else{
                        count[action.ordinal()]--;
                        icon.blink = !icon.blink;
                        icon.repaint();
                        if(count[action.ordinal()] == 0){
                            delay = 0;
                            state = action = null;
                            icon.blink = false;
                            icon.repaint();
                            PkgHandler.send(new AppPkg().setCitizen(name, "None"));
                        }
                    }
                    break;
                case Disappear:
                    StateSwitcher.unregister(thread);
                    reset();
                    return;
            }
        }
    }

    public static class CitizenIcon extends JLabel{
        private static final long serialVersionUID = 1L;
        private final Citizen citizen;
        public ImageIcon imageIcon;
        public final Color color;
        public boolean blink = false;
        final Dimension dimension;
        static final int AVATAR_SIZE = CarIcon.SIZE;//(int) (0.8*CarIcon.AVATAR_SIZE);

        CitizenIcon(Citizen citizen, String iconFile, int rgb) {
            this.citizen = citizen;
            setOpaque(false);
//            setContentAreaFilled(false);
            setVisible(false);
            setBorder(null);
//			setBorderPainted(false);
//            setMargin(new Insets(0, 0, 0, 0));
            color = new Color(rgb);
            setForeground(color);
            imageIcon = Resource.loadImage(iconFile, AVATAR_SIZE, AVATAR_SIZE);
            setIcon(imageIcon);
            setText(citizen.name);
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setFont(new Font(Font.DIALOG, Font.BOLD, 14));
            FontMetrics fm = getFontMetrics(getFont());
            dimension = new Dimension(Math.max(AVATAR_SIZE, fm.stringWidth(citizen.name)+40), AVATAR_SIZE+fm.getHeight());
            setSize(dimension);
        }

//        protected void paintBorder(Graphics g) {
//			super.paintBorder(g);
//            ((Graphics2D )g).setStroke(new BasicStroke(2.0f));
//            if(getModel().isPressed())
//                g.setColor(Color.black);
//            else if(getModel().isRollover())
//                g.setColor(Color.gray);
//            else
//                return;
//
//            g.drawOval(1, 1, AVATAR_SIZE-1, AVATAR_SIZE-1);
//        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(!blink){
//                switch (citizen.state) {
//                    case GetSick:
//                        g.setColor(Color.RED);
//                        break;
//                    case GetHungry:
//                        g.setColor(Color.YELLOW);
//                        break;
//                    default:
//                        g.setColor(color);
//                        break;
//                }
//                g.fillOval(0, 0, AVATAR_SIZE, AVATAR_SIZE);
//                g.setColor(Color.BLACK);
//                g.drawOval(1, 1, AVATAR_SIZE-2, AVATAR_SIZE-2);
            }
//            if(showState){
//                g.setColor(Color.BLACK);
//                String str = citizen.state == null ? "None" : citizen.state.toString();
//                FontMetrics fm = g.getFontMetrics();
//                g.drawString(str, (int) (1.2*AVATAR_SIZE), (getHeight()+fm.getAscent())/2);
//            }
//            else if(showName){
//                g.setColor(Color.BLACK);
//                FontMetrics fm = g.getFontMetrics();
//                g.drawString(citizen.name, (int) (1.2*AVATAR_SIZE), (getHeight()+fm.getAscent())/2);
//            }
        }

//        private void setIcon() {
//            String filename;
//            switch (citizen.name) {
//                case "Tintin":
//                    filename = "res/tintin.png"; break;
//                case "Wade Wilson":
//                    filename = "res/deadpool.png"; break;
//                case "Scott Lang":
//                    filename = "res/antman.png"; break;
//                default:
//                    return;
//            }
//            imageIcon = Resource.loadImage(filename, AVATAR_SIZE, AVATAR_SIZE);
//            setIcon(imageIcon);
//        }

        public int getCenterX() {
            return getX() + dimension.width / 2;
        }

        public int getCenterY() {
            return getY() + dimension.height / 2;
        }
    }
}
