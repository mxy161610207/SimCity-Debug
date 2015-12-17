package nju.ics.lixiaofan.consistency.middleware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import nju.ics.lixiaofan.consistency.context.Context;
import nju.ics.lixiaofan.consistency.context.ContextChange;
import nju.ics.lixiaofan.consistency.context.Pattern;
import nju.ics.lixiaofan.consistency.context.Rule;
import nju.ics.lixiaofan.consistency.formula.Link;

public class Operation {
    public static HashMap<String,Pattern> patterns = new HashMap<String,Pattern>();
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
			if(change.getCtxIdx() < 0 || change.getCtxIdx() > change.getPattern().getSize())
				change.setCtxIdx(change.getPattern().getSize());
			change.getPattern().addContext(change.getCtxIdx(), change.getContext());
			change.getContext().addPattern(change.getPattern());
			break;
		case ContextChange.DELETION:
			if(change.getCtxIdx() < 0 || change.getCtxIdx() >= change.getPattern().getSize())
				change.setCtxIdx(change.getPattern().getContexts().indexOf(change.getContext()));
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
	private static boolean operate(Rule rule, ArrayList<ContextChange> changes, String strategy) {
		if(strategy.equals("Drop-latest") || strategy.equals("Drop-all") || strategy.equals("Drop-random")) {
//			System.out.println(rule.getName());
//			for(ContextChange change : changes)
//				System.out.println((change.getType()==1?"ADD":"DEL")+"\t"+change.getContext().getName()+"\t"+change.getPattern().getName());
			Middleware.changeNum += changes.size();
			Set<Link> links = Detection.detect(rule, changes);
//			System.out.println("Inconsistency "+ (rule.getValue()?"undetected":"detected"));
	        if(links != null && !links.isEmpty()){
//	        	for(Link link : links){
//	        		if(!link.getViolated())
//	        			continue;
//	        		for(Map.Entry<String, Context> entry : link.getBinding().entrySet())
//	        			System.out.print(entry.getKey()+": "+entry.getValue().getName()+"\t");
//	        		System.out.println();
//	        	}
//	        	System.out.println("---------------------");
	            Resolution.resolve(rule, changes, links, strategy);
	            return false;
	        }
//	        System.out.println("---------------------");
        }
		return true;
	}
	
	public static ArrayList<Context> operate(HashMap<String, ArrayList<ContextChange>> sequence, String strategy) {
		if(sequence.isEmpty())
			return null;
		if(strategy.equals("Drop-latest")){
			ArrayList<Entry<String, ArrayList<ContextChange>>> entries = 
				new ArrayList<Entry<String, ArrayList<ContextChange>>>(sequence.entrySet());
			for(int i = 0;i < entries.size();i++){
				if(!operate(rules.get(entries.get(i).getKey()), entries.get(i).getValue(), strategy)){
					//drop the context where no inconsistency detected before
					for(int j = 0;j < i;j++)
						Resolution.resolve(rules.get(entries.get(j).getKey()), entries.get(j).getValue(), null, strategy);
					//if an inconsistency is detected, then no need to check the rest (already violated)
					return null;
				}
			}
			ArrayList<Context> result = new ArrayList<Context>();
			result.add(entries.get(0).getValue().get(0).getContext());
			return result;
		}
		return null;
	}
}
