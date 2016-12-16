/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.middleware;

import nju.xiaofanli.Resource;
import nju.xiaofanli.StateSwitcher;
import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.consistency.context.ContextChange;
import nju.xiaofanli.consistency.context.Pattern;
import nju.xiaofanli.consistency.context.Rule;
import nju.xiaofanli.consistency.dataLoader.Configuration;
import nju.xiaofanli.consistency.dataLoader.PatternLoader;
import nju.xiaofanli.consistency.dataLoader.RuleLoader;
import nju.xiaofanli.dashboard.Dashboard;
import nju.xiaofanli.dashboard.TrafficMap;
import nju.xiaofanli.device.car.Car;
import nju.xiaofanli.device.sensor.BrickHandler;
import nju.xiaofanli.device.sensor.Sensor;
import nju.xiaofanli.util.Pair;
import nju.xiaofanli.util.StyledText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author bingying
 */
public class Middleware {
    private static HashMap<String, Pattern> patterns = new HashMap<>();
    private static HashMap<String, Rule> rules = new HashMap<>();
    private static String resolutionStrategy;
    static int changeNum = 0;
    private static final Queue<Context> queue = new LinkedList<>();
    private static boolean detectionEnabled = false, resolutionEnabled = false;
    static {
        Configuration.init("/nju/xiaofanli/consistency/config/System.properties");
        Set<Pattern> patternSet = PatternLoader.parserXml("src/nju/xiaofanli/consistency/config/patterns.xml");
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

        Set<Rule> ruleSet = RuleLoader.parserXml("src/nju/xiaofanli/consistency/config/rules.xml");
        for(Rule rule : ruleSet){
            rule.setInitialFormula();
            rules.put(rule.getName(), rule);
//            System.out.println(rule.getName() + ": " + rule.getFormula());
        }

        new Operation(patterns, rules);
        resolutionStrategy = Configuration.getConfigStr("resolutionStrategy");
    }

    private static Runnable handler = () -> {
        Thread thread = Thread.currentThread();
        StateSwitcher.register(thread);
        //noinspection InfiniteLoopStatement
        while(true){
            synchronized (queue) {
                while(queue.isEmpty() || !StateSwitcher.isNormal()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException e) {
//							e.printStackTrace();
                        if(StateSwitcher.isResetting() && !StateSwitcher.isThreadReset(thread))
                            clear();
                    }
                }
            }

            Context context;
            synchronized (queue) {
                context = queue.poll();
            }
            checkConsistency(context);
        }
    };

    public Middleware() {
        //code application logic here
        new Thread(handler, "MiddleWare Handler").start();
    }

    public static void checkConsistency(Object subject, Object direction, Object state, Object category, Object predicate, Object prev,
                                        Object object, Object next, Object timestamp, Car car, Sensor sensor, boolean isRealCar, boolean triggerEvent) {
        checkConsistency(getContext(subject, direction, state, category, predicate, prev, object, next, timestamp, car, sensor, isRealCar, triggerEvent));
    }

    public static void checkConsistency(Context context) {
        Car car = (Car) context.getFields().get("car");
        Sensor sensor = (Sensor) context.getFields().get("sensor");
        boolean isRealCar = (boolean) context.getFields().get("real");
        boolean triggerEvent = (boolean) context.getFields().get("trigger");

        Map<String, List<ContextChange>> changes = getChanges(context);
        // check consistency
        Pair<Integer, List<Context>> res = Operation.operate(changes, resolutionStrategy);
        if(res == null)
            return;

        // update cars' and sensors' states
        switch (res.first) {
            case Context.Normal:
                BrickHandler.switchState(car, sensor, isRealCar, true, triggerEvent);
                break;
            case Context.FP:
                if (detectionEnabled) {
                    sensor.showBalloon(Context.FP, car.name, resolutionEnabled);
                    if (resolutionEnabled) {
                        StyledText text = new StyledText();
                        text.append("Fixed a sensor error (sensor ").append(sensor.name, Resource.LIGHT_SKY_BLUE).append(" detected ")
                                .append(car.name, car.icon.color).append(").\n");
                        Dashboard.log(text);
                    }
                }
                if (!resolutionEnabled && detectionEnabled) //if (!resolutionEnabled)
                    BrickHandler.switchState(car, sensor, isRealCar, false, triggerEvent);
                break;
//            case Context.FN:
//                if (detectionEnabled)
//                    sensor.showBalloon(Context.FN, car.name, resolutionEnabled);
//                if (resolutionEnabled || !detectionEnabled && !resolutionEnabled) //if (resolutionEnabled)
//                    BrickHandler.switchState(car, sensor, isRealCar, true);
//                break;
        }
//        display();
    }

    public static Context getContext(Object subject, Object direction, Object state, Object category, Object predicate, Object prev,
                                     Object object, Object next, Object timestamp, Car car, Sensor sensor, boolean isRealCar, boolean triggerEvent) {
        Context context = new Context();
        context.addField("subject", subject);
        context.addField("direction", direction);
        context.addField("state", state);
        context.addField("category", category);
        context.addField("predicate", predicate);
        context.addField("prev", prev);
        context.addField("object", object);
        context.addField("next", next);
        context.addField("timestamp", timestamp);
        context.addField("car", car);
        context.addField("sensor", sensor);
        context.addField("real", isRealCar);
        context.addField("trigger", triggerEvent);
        return context;
    }

    /**
     * @param context used to generate changes (addition or deletion)
     * @return context changes derived from the context, which are separated by rules
     */
    private static Map<String, List<ContextChange>> getChanges(Context context) {
        Map<String, List<ContextChange>> changes = new HashMap<>();
        patterns.values().stream().filter(context::matches).forEach(pattern -> {
            if (!changes.containsKey(pattern.getRule()))
                changes.put(pattern.getRule(), new ArrayList<>());
            if (pattern.isFull()) {
                ContextChange deletion = new ContextChange(ContextChange.DELETION, pattern, pattern.getContexts().peek());
                changes.get(pattern.getRule()).add(deletion);
            }

            ContextChange addition = new ContextChange(ContextChange.ADDITION, pattern, context);
            changes.get(pattern.getRule()).add(addition);
        });
        return changes;
    }

    public static void add(Object subject, Object direction, Object state, Object category, Object predicate, Object prev,
                           Object object, Object next, Object timestamp, Car car, Sensor sensor, boolean isRealCar, boolean triggerEvent) {
        if(StateSwitcher.isResetting())
            return;
        Context context = getContext(subject, direction, state, category, predicate, prev, object, next, timestamp, car, sensor, isRealCar, triggerEvent);
        synchronized (queue) {
            queue.add(context);
            queue.notify();
        }
    }

    /**
     * Only called in the initial phase, directly add true contexts to patterns
     */
    public static void addInitialContext(Object subject, Object direction, Object state, Object category, Object predicate,
                                         Object prev, Object object, Object next, Object timestamp, Car car, Sensor sensor) {
        Context context = getContext(subject, direction, state, category, predicate, prev, object, next, timestamp, car, sensor, false, true);
        Map<String, List<ContextChange>> changes = getChanges(context);
        Operation.operate(changes, resolutionStrategy);
//        display();
    }

    public static void clear(){
        synchronized (queue) {
            queue.clear();
        }
    }

    public static void reset() {
        detectionEnabled = resolutionEnabled = false;
        patterns.values().forEach(Pattern::reset);
        rules.values().forEach(Rule::reset);
    }

    public static HashMap<String,Pattern> getPatterns() {
        return patterns;
    }

    public static HashMap<String, Rule> getRules() {
        return rules;
    }

    public static void main(String[] args) {
        TrafficMap.getInstance();
        new Middleware();
        File file = new File("src/nju/xiaofanli/consistency/config/test case.txt");

        Queue<String> list = new LinkedList<>();
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
            add(s[0], Integer.parseInt(s[1]), Integer.parseInt(s[2]), s[3], s[4], s[5], s[6], s[7], Long.parseLong(s[8]),
                    null, null, false, true);
        }
    }

    private static void display() {
        for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
            String name = entry.getKey();
            Pattern pat = entry.getValue();
            if(pat.getContexts().isEmpty())
                continue;
            System.out.println(name);
            pat.getContexts().forEach(Context::print);
        }
    }


    public static void enableDetection(boolean detectionEnabled) {
        Middleware.detectionEnabled = detectionEnabled;
    }

    public static boolean isDetectionEnabled(){
        return detectionEnabled;
    }

    public static void enableResolution(boolean resolutionEnabled) {
        Middleware.resolutionEnabled = resolutionEnabled;
    }

    public static boolean isResolutionEnabled(){
        return resolutionEnabled;
    }
}
