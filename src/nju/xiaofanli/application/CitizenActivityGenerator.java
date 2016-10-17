package nju.xiaofanli.application;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.dashboard.Citizen;

public class CitizenActivityGenerator implements Runnable{
    public CitizenActivityGenerator(){
        Resource.getCitizens().forEach(x -> new Thread(x, x.name).start());
        new Thread(this, "Citizen Action Generator").start();
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while(true){
            if(StateSwitcher.isNormal()){
                for(Citizen citizen : Resource.getCitizens()){
//					System.out.println(citizen.name);
                    if(citizen.state != null)
                        continue;
                    if(!citizen.icon.isVisible()){
                        citizen.setAction(Citizen.Action.Wander);
                        continue;
                    }
                    citizen.setAction(Citizen.Action.GetHungry);
//                double d = Math.random();
//                if(d < 0.01)
//                    citizen.setAction(Action.Wander);
//                else if(d < 0.35){
//                    if(citizen.job != Citizen.Job.Doctor)
//                        citizen.setAction(Action.GetSick);
//                }
//                else if(d < 0.6)
//                    citizen.setAction(Action.GetHungry);
//                else{
//                    if(citizen.job == Job.Student)
//                        citizen.setAction(Action.GoToSchool);
//                    else
//                        citizen.setAction(Action.GoToWork);
//                }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
