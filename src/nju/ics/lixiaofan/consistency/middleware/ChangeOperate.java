package nju.ics.lixiaofan.consistency.middleware;

import java.util.ArrayList;
import java.util.HashMap;

import nju.ics.lixiaofan.consistency.context.ContextChange;
import nju.ics.lixiaofan.consistency.context.Pattern;
import nju.ics.lixiaofan.consistency.context.Rule;

public class ChangeOperate {
    public static HashMap<String,Pattern> patterns = new HashMap<String,Pattern>();
	@SuppressWarnings("unused")
	private static ArrayList<Rule> rules = new ArrayList<Rule>();
	
	public ChangeOperate(HashMap<String,Pattern> patterns,ArrayList<Rule> rules) {
		ChangeOperate.patterns = patterns;
		ChangeOperate.rules = rules;
		new Detection(patterns,rules);
		//new Resolution(contextsForResolve);
	}

    //contextchange操作
    public static boolean change(ContextChange change) {
    	if(change.getPattern() == null)
    		return false;
    	
    	switch (change.getType()) {
		case ContextChange.ADDITION:
			change.getPattern().addContext(change.getContext().getName(), change.getContext());
			break;
		case ContextChange.DELETION:
			change.getPattern().deleteContext(change.getContext().getName());
			break;
		case ContextChange.UPDATE:
			break;
		default:
			return false;
		}
        return true;
    }
	
	//对一条change的处理(Drop-latest/Drop-all/Drop-random)
	public static void singleChangeTrivial(ContextChange change,String strategy) {
		if(!change(change))
			return;
		/*ArrayList<Link> link = */Detection.singleChangeDetect(change);
//        if(link != null) {//该context经检测是一致的
//            Resolution.resolve(change,link,strategy);
//        }
	}
	
	public static void singleChange(ContextChange change,String strategy) {
		//对一条change的处理(Drop-latest/Drop-all/Drop-random)
		if(strategy.equals("Drop-latest") || strategy.equals("Drop-all") || strategy.equals("Drop-random")) {
			singleChangeTrivial(change,strategy);
        }
	}

}
