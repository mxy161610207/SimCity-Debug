/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.middleware;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.city.TrafficMap;
import nju.ics.lixiaofan.consistency.context.*;
import nju.ics.lixiaofan.consistency.dataLoader.*;
import nju.ics.lixiaofan.control.Reset;
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
    
    private static Queue<Context> queue = new LinkedList<Context>();
    private static Thread handler = new Thread("MiddleWare Handler"){
    	public void run() {
    		while(true){
    			Thread curThread = Thread.currentThread();
    			Reset.addThread(curThread);
    			while(queue.isEmpty()){
    				synchronized (queue) {
    					try {
							queue.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
							if(Reset.isResetting() && Reset.checkThread(curThread))
								clear();
						}
					}
    			}
    			if(Reset.isResetting()){
    				if(Reset.checkThread(curThread))
    					clear();
    				continue;
    			}
    			
    			Context context = queue.poll();
    			if(!Middleware.isDetectionEnabled()){
					BrickHandler.add((Car) context.getFields().get("car"),
							(Sensor) context.getFields().get("sensor"),
							Context.Normal);
    				continue;
    			}
    			HashMap<String, ArrayList<ContextChange>> sequence = new HashMap<String, ArrayList<ContextChange>>();
    			//generate the sequence derived by the context
    			for(Pattern pattern : patterns.values()){
    				if(context.matches(pattern)){
    					if(!sequence.containsKey(pattern.getRule()))
    						sequence.put(pattern.getRule(), new ArrayList<ContextChange>());
    					if(pattern.isFull()){
							ContextChange deletion = new ContextChange(ContextChange.DELETION,
									pattern, pattern.getContexts().peek());
    						sequence.get(pattern.getRule()).add(deletion);
    					}
    					
    					ContextChange addition = new ContextChange(ContextChange.ADDITION, pattern, context);
    					sequence.get(pattern.getRule()).add(addition);
    				}
    			}
    			
    			ArrayList<Context> contexts = Operation.operate(sequence, resolutionStrategy);
//    			context.print();
    			if(contexts != null){
    				for(Context ctx : contexts){
//    					ctx.print();
						if (ctx == context)
							BrickHandler.add((Car) ctx.getFields().get("car"),
									(Sensor) ctx.getFields().get("sensor"),
									Context.Normal);
						else
							BrickHandler.add((Car) ctx.getFields().get("car"),
									(Sensor) ctx.getFields().get("sensor"),
									Context.FN);
    				}
    			}
    			else
					BrickHandler.add((Car) context.getFields().get("car"),
							(Sensor) context.getFields().get("sensor"),
							Context.FP);
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
				pattern.setMaxCtxNum(1);
				break;
			default:
				pattern.setMaxCtxNum(2);
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
        
        new Operation(patterns,rules);
        resolutionStrategy = Configuration.getConfigStr("resolutionStrategy");
        handler.start();
	}
    
	public static void add(Object subject, Object direction, Object status,
			Object category, Object predicate, Object prev, Object object, Object timestamp, Car car, Sensor sensor) {
		if(Reset.isResetting())
			return;
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
		
		synchronized (queue) {
			queue.add(context);
			queue.notify();
		}
	}
	
	public static void clear(){
		synchronized (queue) {
			queue.clear();
		}
	}
    
    public static HashMap<String,Pattern> getPatterns() {
    	return patterns;
    }
    
    public static HashMap<String, Rule> getRules() {
    	return rules;
    }
    
    public static void main(String[] args) {
    	new TrafficMap();
		new Middleware();
        File file = new File("test case.txt");
        
        Queue<String> list = new LinkedList<String>();
        if (file.exists() && file.isFile()) {
            try{
                BufferedReader input = new BufferedReader(new FileReader(file));
                String text;
                while((text = input.readLine()) != null)
                    list.add(text);
                input.close();
            }  
            catch(IOException ioException){
                System.err.println("File Error!");
            }
        }
		
        while(!list.isEmpty()){
        	String testCase = list.poll();
        	String[] s = testCase.split(", ");
        	
        	add(s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7], null, null);
        }
        
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
    
    private static boolean detectionFlag = false, resolutionFlag = false;
    public static boolean isDetectionEnabled() {
		return detectionFlag;
	}

	public static void setDetectionFlag(boolean detectionFlag) {
		Middleware.detectionFlag = detectionFlag;
	}

	public static boolean isResolutionEnabled() {
		return resolutionFlag;
	}

	public static void setResolutionFlag(boolean resolutionFlag) {
		Middleware.resolutionFlag = resolutionFlag;
	}
}
