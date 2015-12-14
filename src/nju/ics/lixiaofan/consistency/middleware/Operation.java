package nju.ics.lixiaofan.consistency.middleware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import nju.ics.lixiaofan.consistency.context.ContextChange;
import nju.ics.lixiaofan.consistency.context.Pattern;
import nju.ics.lixiaofan.consistency.context.Rule;
import nju.ics.lixiaofan.consistency.formula.Link;

public class Operation {
    public static HashMap<String,Pattern> patterns = new HashMap<String,Pattern>();
	@SuppressWarnings("unused")
	private static HashMap<String, Rule> rules = new HashMap<String, Rule>();
	
	public Operation(HashMap<String,Pattern> patterns, HashMap<String, Rule> rules) {
		Operation.patterns = patterns;
		Operation.rules = rules;
		new Detection(patterns, rules);
		//new Resolution(contextsForResolve);
	}

    //contextchange操作
    public static void change(ContextChange change) {
    	switch (change.getType()) {
		case ContextChange.ADDITION:
			change.getPattern().addContext(change.getContext());
			change.getContext().addPattern(change.getPattern());
			break;
		case ContextChange.DELETION:
			change.getPattern().deleteContext(change.getContext());
			change.getContext().deletePattern(change.getPattern());
			break;
		default:
			break;
		}
    }
	
	//对一条change的处理(Drop-latest/Drop-all/Drop-random)
	public static void operate(ContextChange change, String strategy) {
		if(strategy.equals("Drop-latest") || strategy.equals("Drop-all") || strategy.equals("Drop-random")) {
			/*ArrayList<Link> link = */Detection.detect(change);
//	        if(link != null) {//该context经检测是一致的
//	            Resolution.resolve(change,link,strategy);
//	        }
        }
	}
	
	//对多条change的处理(Drop-latest/Drop-all/Drop-random)
	//return value: if an inconsistency is detected, return true, vice versa
	public static boolean operate(Rule rule, ArrayList<ContextChange> changes, String strategy) {
		if(strategy.equals("Drop-latest") || strategy.equals("Drop-all") || strategy.equals("Drop-random")) {
			Set<Link> links = Detection.detect(rule, changes);
	        if(links != null && !links.isEmpty()){
	            Resolution.resolve(rule, changes, links, strategy);
	            return true;
	        }
        }
		return false;
	}
}
