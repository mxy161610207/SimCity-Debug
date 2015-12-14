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

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.consistency.context.*;
import nju.ics.lixiaofan.consistency.dataLoader.*;
import nju.ics.lixiaofan.sensor.BrickHandler;
import nju.ics.lixiaofan.sensor.Sensor;

/**
 *
 * @author bingying
 * 主程序
 */
public class Middleware {
    private static HashMap<String, Pattern> patterns = new HashMap<String, Pattern>();
    private static HashMap<String, Rule> rules = new HashMap<String, Rule>();
    private static String resolutionStrategy;
    public static int changeNum = 0;
    
    private static Queue<HashMap<String, ArrayList<ContextChange>>> queue = new LinkedList<HashMap<String, ArrayList<ContextChange>>>();
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
    			
    			HashMap<String, ArrayList<ContextChange>> sequences = queue.poll();
    			boolean detected = false;
    			for(Map.Entry<String, ArrayList<ContextChange>> entry : sequences.entrySet()){
    				changeNum += entry.getValue().size();
    				detected = Operation.operate(rules.get(entry.getKey()), entry.getValue(), resolutionStrategy);
    				//if an inconsistency is detected, then no need to check the rest (already violated)
        	        if(detected)
        	        	break;
    			}
    			if(!detected){
    				@SuppressWarnings("unchecked")
					Context ctx = ((ArrayList<ContextChange>[])(sequences.values().toArray()))[0].get(0).getContext();
    				BrickHandler.add((Car)ctx.getFields().get("car"), (Sensor)ctx.getFields().get("sensor"));
    			}
    		}
    	}
    };
    
    public Middleware() {
        //code application logic here
    	Configuration c = new Configuration();
        c.init("/nju/ics/lixiaofan/consistency/config/System.properties");
               
        //读context具体内容，得到contexts
        HashSet<Pattern> patternSet = PatternLoader.parserXml("src/nju/ics/lixiaofan/consistency/config/patterns.xml");
        for(Pattern pattern : patternSet){
        	patterns.put(pattern.getName(), pattern);
        	switch (pattern.getName()) {
			case "latest":
				pattern.setContxtNum(1);
				break;
			default:
				pattern.setContxtNum(2);
				break;
			}
//        	System.out.println(pattern.getName());
//        	for(Map.Entry entry : pattern.getFields().entrySet())
//        		System.out.println(entry.getKey() + " " + entry.getValue());
        }
        
        //读constraint，得到rules
       	HashSet<Rule> ruleSet = RuleLoader.parserXml("src/nju/ics/lixiaofan/consistency/config/rules.xml");
       	for(Rule rule : ruleSet)
       		rules.put(rule.getName(), rule);
        
        //单条change处理
        new Operation(patterns,rules);
        
        resolutionStrategy = Configuration.getConfigStr("resolutionStrategy");
        
        handler.start();
	}
    
	public static void add(Object subject, Object direction, Object status,
			Object category, Object predicate, Object prev, Object object, Object timestamp, Car car, Sensor sensor) {
		Context context = new Context();
		context.addField("subject", subject);
		context.addField("direction", direction);
		context.addField("status", status);
		context.addField("category", category);
		context.addField("predicate", predicate);
		context.addField("prev", prev);
		context.addField("object", object);
		context.addField("timestamp", timestamp);
		context.addField("car", car);
		context.addField("sensor", sensor);
		
		HashMap<String, ArrayList<ContextChange>> sequences = new HashMap<String, ArrayList<ContextChange>>();
		for(Pattern pattern : patterns.values()){
			if(context.matches(pattern)){
				if(!sequences.containsKey(pattern.getRule()))
					sequences.put(pattern.getRule(), new ArrayList<ContextChange>());
				if(pattern.isFull()){
					ContextChange deletion = new ContextChange(ContextChange.DELETION, pattern, pattern.getContexts().peek());
					sequences.get(pattern.getRule()).add(deletion);
				}
				
				ContextChange addition = new ContextChange(ContextChange.ADDITION, pattern, context);
				sequences.get(pattern.getRule()).add(addition);
			}
		}
		
		if(!sequences.isEmpty())
			synchronized (queue) {
				queue.add(sequences);
				queue.notify();
			}
	}
    
    public static HashMap<String,Pattern> getPatterns() {
    	return patterns;
    }
    
    public static HashMap<String, Rule> getRules() {
    	return rules;
    }
    
//  public static void main(String[] args) {
//  new Middleware();
//   
////   //循环获取每一条change并进行处理
////   Demo demo = new Demo("src/nju/ics/lixiaofan/consistency/config/changes.txt");
//////   long bs = Calendar.getInstance().getTimeInMillis();
////   while(demo.hasNextChange()) {//循环处理changes
////       changeNum++;
////       //System.out.println(changeNum);
////       ChangeOperate.singleChange(demo.nextChange(), resolutionStrategy);
////       //display();
////       //System.exit(1);
////   }
//////   System.out.println(Calendar.getInstance().getTimeInMillis() - bs);
//}
    
    public static void display() {
        for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
            String name = (String)entry.getKey();
            Pattern context = (Pattern)entry.getValue();
            System.out.println(name + ":");
            for (Context ctx : context.getContexts()) {
                System.out.print(" " + ctx.getName());
            }
            System.out.println("");
        }
    }
}
