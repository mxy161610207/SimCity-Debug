package nju.xiaofanli.city;

import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.application.Delivery;
import nju.xiaofanli.application.monitor.AppPkg;
import nju.xiaofanli.application.monitor.PkgHandler;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.car.Car.CarIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Citizen implements Runnable{
    public final String name;
    public final Gender gender;
    public final Job job;
    private Activity act = null, nextAct = null;
    public Activity state = null;
    private long delay = 0;
    public Location loc = null, dest = null;
    public Car car = null;
    public CitizenIcon icon = new CitizenIcon(this);

    public enum Gender {
        Male, Female
    }

    public enum Job{
        Student, Doctor, Police, Cook, IronMan
    }

    public enum Activity {
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

    public void reset() {
        act = nextAct = state = null;
        loc = dest = null;
        car = null;
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
                        if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
                            act = null;
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
                    int xmax = icon.getParent().getWidth() - CitizenIcon.SIZE;
                    int ymax = icon.getParent().getHeight() - CitizenIcon.SIZE;
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
                            loc = Dashboard.getNearestSection(icon.getX() + CitizenIcon.SIZE / 2, icon.getY() + CitizenIcon.SIZE / 2);

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
                        loc = Dashboard.getNearestSection(icon.getX() + CitizenIcon.SIZE / 2, icon.getY() + CitizenIcon.SIZE / 2);

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
                    assert loc != null && dest != null;
                    act = null;
                    Delivery.add(loc, dest, this);
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
                        icon.setLocation(x, y);
                        icon.setVisible(true);

                        PkgHandler.send(new AppPkg().setCitizen(name, (double) x/TrafficMap.SIZE, (double) y/TrafficMap.SIZE));
                        PkgHandler.send(new AppPkg().setCitizen(name, true));
                    }
                    if(nextAct == null){
                        dest = null;
                        state = act = null;
                        PkgHandler.send(new AppPkg().setCitizen(name, "None"));
                    }
                    else{
                        if (nextAct != Activity.AtWork && nextAct != Activity.InClass
                                && nextAct != Activity.UnderTreatment && nextAct != Activity.HavingMeals)
                            dest = null;
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
                        assert dest != null;
                        loc = dest;
//                        System.out.println(dest instanceof Building);
                        int xmax = ((Building) dest).icon.getWidth() - CitizenIcon.SIZE;
                        int ymax = ((Building) dest).icon.getHeight() - CitizenIcon.SIZE;
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
            }
        }
    }

    public static class CitizenIcon extends JButton{
        private static final long serialVersionUID = 1L;
        private Citizen citizen = null;
        public Color color = null;
        private boolean showName = false;
        private boolean showState = false;
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
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    showState = !showState;
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    showName = true;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    showName = false;
                    showState = false;
                }
            });
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
            if(showState){
                g.setColor(Color.BLACK);
                String str = citizen.state == null ? "None" : citizen.act.toString();
                FontMetrics fm = g.getFontMetrics();
                g.drawString(str, (int) (1.2*SIZE), (getHeight()+fm.getAscent())/2);
            }
            else if(showName){
                g.setColor(Color.BLACK);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(citizen.name, (int) (1.2*SIZE), (getHeight()+fm.getAscent())/2);
            }
        }
    }
}
