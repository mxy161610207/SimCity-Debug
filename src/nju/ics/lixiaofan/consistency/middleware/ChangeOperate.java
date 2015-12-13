package nju.ics.lixiaofan.consistency.middleware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nju.ics.lixiaofan.consistency.context.ContextChange;
import nju.ics.lixiaofan.consistency.context.Pattern;
import nju.ics.lixiaofan.consistency.context.Rule;

public class ChangeOperate {
    public static HashMap<String,Pattern> patterns = new HashMap<String,Pattern>();
	@SuppressWarnings("unused")
	private static HashSet<Rule> rules = new HashSet<Rule>();
	
	public ChangeOperate(HashMap<String,Pattern> patterns, HashSet<Rule> rules) {
		ChangeOperate.patterns = patterns;
		ChangeOperate.rules = rules;
		new Detection(patterns, rules);
		//new Resolution(contextsForResolve);
	}

    //contextchange操作
    public static boolean change(ContextChange change) {
    	if(change.getPattern() == null)
    		return false;
    	
    	switch (change.getType()) {
		case ContextChange.ADDITION:
			change.getPattern().addContext(change.getContext());
			change.getContext().addPattern(change.getPattern());
			break;
		case ContextChange.DELETION:
			change.getPattern().deleteContext(change.getContext());
			change.getContext().deletePattern(change.getPattern());
			break;
		case ContextChange.UPDATE:
			break;
		default:
			return false;
		}
        return true;
    }
	
	//对一条change的处理(Drop-latest/Drop-all/Drop-random)
	public static void singleChange(ContextChange change, String strategy) {
		if(strategy.equals("Drop-latest") || strategy.equals("Drop-all") || strategy.equals("Drop-random")) {
			/*ArrayList<Link> link = */Detection.singleChangeDetect(change);
//	        if(link != null) {//该context经检测是一致的
//	            Resolution.resolve(change,link,strategy);
//	        }
        }
	}
	
	//对多条change的处理(Drop-latest/Drop-all/Drop-random)
	//return value: true/false: inconsistency detected/undetected
	public static boolean multiChange(ArrayList<ContextChange> changes, String strategy) {
		if(strategy.equals("Drop-latest") || strategy.equals("Drop-all") || strategy.equals("Drop-random")) {
			/*ArrayList<Link> link = */Detection.multiChangeDetect(changes);
//	        if(link != null) {//该context经检测是一致的
//	            Resolution.resolve(change,link,strategy);
//	        }
        }
		return false;
	}
}
