/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.middleware;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import nju.ics.lixiaofan.consistency.context.*;
import nju.ics.lixiaofan.consistency.dataLoader.*;

/**
 *
 * @author bingying
 * ������
 */
public class Middleware {
    /**
     * @param args the command line arguments
     */
	
    private static HashMap<String,Pattern> patterns = new HashMap<String,Pattern>();
    private static ArrayList<Rule> rules = new ArrayList<Rule>();
    public static int changeNum;
    
    public static void main(String[] args) {
        //code application logic here
    	Configuration c = new Configuration();
        c.init("/nju/ics/lixiaofan/consistency/config/System.properties");
               
        //��context�������ݣ��õ�contexts
        ArrayList<Pattern> patternList = PatternLoader.parserXml("src/nju/ics/lixiaofan/consistency/config/pattern.xml");
        for(Pattern pattern : patternList)
        	patterns.put(pattern.getName(), pattern);
        
        //��constraint���õ�rules
        rules = RuleLoader.parserXml("src/nju/ics/lixiaofan/consistency/config/rule.xml");
        
        //����change����
        new ChangeOperate(patterns,rules);
        
        if(Configuration.getConfigStr("optimizingStrategy").equals("ON")) {
            for(int i = 0;i < rules.size();i++) {
                String temp = Configuration.getConfigStr("goalLink" + (i+1));
                rules.get(i).getFormula().setGoal(temp);
            }
        }
        
        //ѭ����ȡÿһ��change�����д���
        changeNum = 0;
        ContextChange change;
        Demo demo = new Demo("src/nju/ics/lixiaofan/consistency/config/changes.txt");
        String strategy = Configuration.getConfigStr("resolutionStrategy");
        long bs = Calendar.getInstance().getTimeInMillis();
        while(demo.hasNextChange()) {//ѭ������changes
            changeNum++;
            //System.out.println(changeNum);
            change = demo.nextChange();
            ChangeOperate.singleChange(change,strategy);
            //display();
            //System.exit(1);
        }
        System.out.println(Calendar.getInstance().getTimeInMillis() - bs);
    }
    
    public static HashMap<String,Pattern> getPatterns() {
    	return patterns;
    }
    
    public static ArrayList<Rule> getRules() {
    	return rules;
    }
    
    public static void display() {
        for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
            String name = (String)entry.getKey();
            Pattern context = (Pattern)entry.getValue();
            System.out.println(name + ":");
            for (Map.Entry<String, Context> entry2 : context.getContexts().entrySet()) {
                System.out.print(" " + entry2.getKey());
            }
            System.out.println("");
        }
    }
}
