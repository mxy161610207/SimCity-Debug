package nju.xiaofanli.city;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Car.CarIcon;

import javax.swing.*;
import java.awt.*;

public class Citizen extends Thread {
    public final String name;
    public final Gender gender;
    public final Job job;
    private Activity act = null, nextAct = null;
    public Activity state = null;
    private long delay = 0;
    public Location loc = null, dest = null;
    public Car car = null;
    public CitizenIcon icon = null;
    public boolean releasedByUser = false; //used in taking taxis

    public enum Gender {
        Male, Female
    }

    public enum Job{
        Student, Doctor, Police, Cook, SuperHero, Reporter
    }

    public enum Activity {
        Wander, GoToWork, AtWork, GoToSchool, InClass, RescueTheWorld, HailATaxi, TakeATaxi, GetOff,
        GoToHospital, UnderTreatment, GetSick, GetHungry, GoToEat, HavingMeals, Disappear
    }

    public Citizen(String name, Gender gender, Job job, String iconFile) {
        this.name = name;
        this.gender = gender;
        this.job = job;
        this.icon = new CitizenIcon(this, iconFile);
        setName("Citizen: " + name);
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
        act = nextAct = state = null;
        loc = dest = null;
        car = null;
        delay = 0;
        releasedByUser = false;
        icon.setVisible(false);
    }

    public void setActivity(Activity act){
        this.act = act;
        synchronized (this){
            this.notify();
        }
    }

    public void run() {
        Thread thread = Thread.currentThread();
        StateSwitcher.register(thread);

        int[] count = new int[Activity.values().length];
        for(int i = 0;i < count.length;i++)
            count[i] = 0;
        //noinspection InfiniteLoopStatement
        while(true){
            while(act == null || !StateSwitcher.isNormal())
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
//                        e.printStackTrace();
                        if(StateSwitcher.isResetting()) {
                            act = null;
                            StateSwitcher.unregister(thread);
                            return;
                        }
                    }
                }
            if(act == null)
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
            state = act;
            switch (act) {
                case Wander: {
                    int xmax = icon.getParent().getWidth() - icon.dimension.width;
                    int ymax = icon.getParent().getHeight() - icon.dimension.height;
                    if (count[act.ordinal()] == 0) {
                        count[act.ordinal()] = 3;
                        delay = 1000;
                        if (!icon.isVisible()) {
                            int x = (int) (Math.random() * xmax);
                            int y = (int) (Math.random() * ymax);
//						x = TrafficMap.streets[23].icon.coord.x;
//						y = TrafficMap.streets[23].icon.coord.y;
                            icon.setLocation(x, y);
                            icon.setVisible(true);
                            loc = Dashboard.getNearestSection(icon.getCenterX(), icon.getCenterY());

                            PkgHandler.send(new AppPkg().setCitizen(name, (double) x / TrafficMap.SIZE, (double) y / TrafficMap.SIZE));
                            PkgHandler.send(new AppPkg().setCitizen(name, true));
                        }
                    } else {
                        count[act.ordinal()]--;
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
                        loc = Dashboard.getNearestSection(icon.getCenterX(), icon.getCenterY());

                        PkgHandler.send(new AppPkg().setCitizen(name, (double) x / TrafficMap.SIZE, (double) y / TrafficMap.SIZE));

                        if (count[act.ordinal()] == 0) {
                            state = act = null;
                            delay = 0;
                            PkgHandler.send(new AppPkg().setCitizen(name, "None"));
                        }
                    }
                    break;
                }
                case HailATaxi:
                    act = null;
                    if(!icon.isVisible()){
                        int x, y;
                        if(loc instanceof Section){
                            x = ((Section) loc).icon.coord.centerX;
                            y = ((Section) loc).icon.coord.centerY;
                        }
                        else{
                            x = ((Building) loc).icon.coord.centerX;
                            y = ((Building) loc).icon.coord.centerY;
                        }
                        icon.setLocation(x-icon.getWidth()/4, y-icon.getHeight()/4);
                        icon.setVisible(true);
                    }
                    Delivery.add(loc, dest, this, releasedByUser);
                    break;
                case TakeATaxi:
                    act = null;
                    icon.setVisible(false);// get on the taxi
                    PkgHandler.send(new AppPkg().setCitizen(name, false));
                    break;
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
                        icon.setLocation(x-icon.getWidth()/4, y-icon.getHeight()/4);
                        icon.setVisible(true);

                        PkgHandler.send(new AppPkg().setCitizen(name, (double) x/TrafficMap.SIZE, (double) y/TrafficMap.SIZE));
                        PkgHandler.send(new AppPkg().setCitizen(name, true));
                    }
                    if(nextAct == null){
//                        dest = null;
//                        state = act = null;
//                        PkgHandler.send(new AppPkg().setCitizen(name, "None"));
                        delay = 3000;
                        act = Activity.Disappear;
                    }
                    else{
                        if (nextAct != Activity.AtWork && nextAct != Activity.InClass
                                && nextAct != Activity.UnderTreatment && nextAct != Activity.HavingMeals) {
                            dest = null;
                        }
                        act = nextAct;
                        nextAct = null;
                        PkgHandler.send(new AppPkg().setCitizen(name, act.toString()));
                    }
                    break;
                case GetSick:case GetHungry:
                    if(count[act.ordinal()] == 0){
                        count[act.ordinal()] = 15;
                        delay = 300;
                    }
                    else{
                        count[act.ordinal()]--;
                        icon.blink = !icon.blink;
                        icon.repaint();
                        if(count[act.ordinal()] == 0){
                            delay = 0;
                            icon.blink = false;
                            icon.repaint();
                            if(act == Activity.GetSick)
                                act = Activity.GoToHospital;
                            else if(act == Activity.GetHungry)
                                act = Activity.GoToEat;
                            PkgHandler.send(new AppPkg().setCitizen(name, act.toString()));
                        }
                    }
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
                            || (loc instanceof Section && ((Building) dest).addrs.contains(loc))){
                        act = nextAct;
                        nextAct = null;
                    }
                    else
                        act = Activity.HailATaxi;
                    PkgHandler.send(new AppPkg().setCitizen(name, act.toString()));
                    break;
                case AtWork:case InClass:case UnderTreatment:case HavingMeals:
                    if(count[act.ordinal()] == 0){
                        count[act.ordinal()] = 50;
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
                        count[act.ordinal()]--;
                        icon.blink = !icon.blink;
                        icon.repaint();
                        if(count[act.ordinal()] == 0){
                            delay = 0;
                            state = act = null;
                            icon.blink = false;
                            icon.repaint();
                            PkgHandler.send(new AppPkg().setCitizen(name, "None"));
                        }
                    }
                    break;
                case RescueTheWorld:
                    if(count[act.ordinal()] == 0){
                        count[act.ordinal()] = 5;
                        delay = 500;
                    }
                    else{
                        count[act.ordinal()]--;
                        icon.blink = !icon.blink;
                        icon.repaint();
                        if(count[act.ordinal()] == 0){
                            delay = 0;
                            state = act = null;
                            icon.blink = false;
                            icon.repaint();
                            PkgHandler.send(new AppPkg().setCitizen(name, "None"));
                        }
                    }
                    break;
                case Disappear:
                    StateSwitcher.unregister(thread);
                    reset();
                    synchronized (TrafficMap.freeCitizens) {
                        if (!TrafficMap.freeCitizens.contains(this))
                            TrafficMap.freeCitizens.add(this);
                    }
                    return;
            }
        }
    }

    public static class CitizenIcon extends JLabel{
        private static final long serialVersionUID = 1L;
        private final Citizen citizen;
        private ImageIcon imageIcon;
        public final Color color;
        private boolean showName = false;
        private boolean showState = false;
        public boolean blink = false;
        final Dimension dimension;
        static final int AVATAR_SIZE = CarIcon.SIZE;//(int) (0.8*CarIcon.AVATAR_SIZE);

        CitizenIcon(Citizen citizen, String iconFile) {
            this.citizen = citizen;
            setOpaque(false);
//            setContentAreaFilled(false);
            setVisible(false);
            setBorder(null);
//			setBorderPainted(false);
//            setMargin(new Insets(0, 0, 0, 0));
            color = new Color((int) (Math.random()*256), (int) (Math.random()*256), (int) (Math.random()*256));
            imageIcon = Resource.loadImage(iconFile, AVATAR_SIZE, AVATAR_SIZE);
            setIcon(imageIcon);
            setText(citizen.name);
            setVerticalTextPosition(SwingConstants.BOTTOM);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setFont(new Font(Font.DIALOG, Font.BOLD, 14));
            FontMetrics fm = getFontMetrics(getFont());
            dimension = new Dimension(Math.max(AVATAR_SIZE, fm.stringWidth(citizen.name)+40), AVATAR_SIZE +fm.getHeight());
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
