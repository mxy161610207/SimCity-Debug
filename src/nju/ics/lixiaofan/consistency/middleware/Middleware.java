/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.middleware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import nju.ics.lixiaofan.consistency.context.*;
import nju.ics.lixiaofan.consistency.dataLoader.*;

/**
 *
 * @author bingying
 * 主程序
 */
public class Middleware {
    private static HashMap<String, Pattern> patterns = new HashMap<String, Pattern>();
    private static HashSet<Rule> rules = new HashSet<Rule>();
    private static String resolutionStrategy;
    public static int changeNum = 0;
    public static int ctxNum = 0;
    
    private static Queue<ContextChange> queue = new LinkedList<ContextChange>();
    private static Thread handler = new Thread(){
    	public void run() {
    		while(true){
    			while(queue.isEmpty()){
    				synchronized (queue) {
    					try {
							queue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
    			}
    			
    			ContextChange change = queue.poll();
    			changeNum++;
    	        ChangeOperate.singleChange(change, resolutionStrategy);
    		}
    	}
    };
    
    public Middleware() {
        //code application logic here
    	Configuration c = new Configuration();
        c.init("/nju/ics/lixiaofan/consistency/config/System.properties");
               
        //读context具体内容，得到contexts
        HashSet<Pattern> patternList = PatternLoader.parserXml("src/nju/ics/lixiaofan/consistency/config/patterns.xml");
//        System.out.println(patternList.size());
        for(Pattern pattern : patternList){
        	patterns.put(pattern.getName(), pattern);
//        	System.out.println(pattern.getName());
//        	for(Map.Entry entry : pattern.getFields().entrySet())
//        		System.out.println(entry.getKey() + " " + entry.getValue());
        }
        
        //读constraint，得到rules
        rules = RuleLoader.parserXml("src/nju/ics/lixiaofan/consistency/config/rules.xml");
        
        //单条change处理
        new ChangeOperate(patterns,rules);
        
//        if(Configuration.getConfigStr("optimizingStrategy").equals("ON")) {
//            for(int i = 0;i < rules.size();i++) {
//                String temp = Configuration.getConfigStr("goalLink" + (i+1));
//                rules.get(i).getFormula().setGoal(temp);
//            }
//        }
        resolutionStrategy = Configuration.getConfigStr("resolutionStrategy");
        
        handler.start();
	}
    
    public static void main(String[] args) {
       new Middleware();
        
//        //循环获取每一条change并进行处理
//        Demo demo = new Demo("src/nju/ics/lixiaofan/consistency/config/changes.txt");
////        long bs = Calendar.getInstance().getTimeInMillis();
//        while(demo.hasNextChange()) {//循环处理changes
//            changeNum++;
//            //System.out.println(changeNum);
//            ChangeOperate.singleChange(demo.nextChange(), resolutionStrategy);
//            //display();
//            //System.exit(1);
//        }
////        System.out.println(Calendar.getInstance().getTimeInMillis() - bs);
    }
    
	public static void add(Object subject, Object direction, Object status,
			Object category, Object predicate, Object prev, Object object,
			Object timestamp) {
		Context context = new Context();
		context.setName(String.valueOf(ctxNum++));
		context.addField("subject", subject);
		context.addField("direction", direction);
		context.addField("status", status);
		context.addField("category", category);
		context.addField("predicate", predicate);
		context.addField("prev", prev);
		context.addField("object", object);
		context.addField("timestamp", timestamp);
		
		ArrayList<ContextChange> sequence = new ArrayList<ContextChange>();
		//TODO search for the matched pattern
		for(Pattern pattern : patterns.values()){
			if(context.matches(pattern))
		}
		
	}
    
    public static HashMap<String,Pattern> getPatterns() {
    	return patterns;
    }
    
    public static HashSet<Rule> getRules() {
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
