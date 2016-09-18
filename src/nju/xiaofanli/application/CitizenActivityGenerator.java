package nju.xiaofanli.application;

import nju.xiaofanli.Resource;
import nju.xiaofanli.city.Citizen;

public class CitizenActivityGenerator implements Runnable{
    public CitizenActivityGenerator(){
        new Thread(this).start();
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while(true){
            for(Citizen citizen : Resource.getCitizens()){
//					System.out.println(citizen.name);
                if(citizen.act != null)
                    continue;
                if(!citizen.icon.isVisible()){
                    citizen.setActivity(Citizen.Activity.Wander);
                    continue;
                }
                citizen.setActivity(Citizen.Activity.GetHungry);
//                double d = Math.random();
//                if(d < 0.01)
//                    citizen.setActivity(Activity.Wander);
//                else if(d < 0.35){
//                    if(citizen.job != Citizen.Job.Doctor)
//                        citizen.setActivity(Activity.GetSick);
//                }
//                else if(d < 0.6)
//                    citizen.setActivity(Activity.GetHungry);
//                else{
//                    if(citizen.job == Job.Student)
//                        citizen.setActivity(Activity.GoToSchool);
//                    else
//                        citizen.setActivity(Activity.GoToWork);
//                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
