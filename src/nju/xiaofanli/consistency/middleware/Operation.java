package nju.xiaofanli.consistency.middleware;

import java.util.*;
import java.util.Map.Entry;

import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.consistency.context.ContextChange;
import nju.xiaofanli.consistency.context.Pattern;
import nju.xiaofanli.consistency.context.Rule;
import nju.xiaofanli.consistency.formula.Link;

class Operation {
	private static HashMap<String,Pattern> patterns;
	private static HashMap<String, Rule> rules;
	
	Operation(HashMap<String, Pattern> patterns, HashMap<String, Rule> rules) {
		Operation.patterns = patterns;
		Operation.rules = rules;
		new Detection(patterns, rules);
		//new Resolution(contextsForResolve);
	}

    //contextchange����
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
	
	//��һ��change�Ĵ���(Drop-latest/Drop-all/Drop-random)
	public static void operate(ContextChange change, String strategy) {
		if(strategy.equals("Drop-latest") || strategy.equals("Drop-all") || strategy.equals("Drop-random")) {
			/*ArrayList<Link> link = */Detection.detect(change);
//	        if(link != null) {//��context�������һ�µ�
//	            Resolution.resolve(change,link,strategy);
//	        }
        }
	}
	
	//�Զ���change�Ĵ���(Drop-latest/Drop-all/Drop-random)
	private static boolean operate(Rule rule, List<ContextChange> changes, String strategy) {
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
	        	System.out.println("Violated Rule: " + rule.getName());
	            Resolution.resolve(rule, changes, links, strategy);
	            return false;
	        }
//	        System.out.println("---------------------");
        }
		return true;
	}
	
	static AbstractMap.SimpleImmutableEntry<Integer, List<Context>> operate(Map<String, List<ContextChange>> changes, String strategy) {
		if(changes.isEmpty())
			return null;
		if(strategy.equals("Drop-latest")){
			List<Entry<String, List<ContextChange>>> entries = new ArrayList<>(changes.entrySet());
			for(int i = 0;i < entries.size();i++){
				if(!operate(rules.get(entries.get(i).getKey()), entries.get(i).getValue(), strategy)){
					//drop the context where no inconsistency detected before
					for(int j = i - 1;j >= 0;j--)
						Resolution.resolve(rules.get(entries.get(j).getKey()), entries.get(j).getValue(), null, strategy);
					//if an inconsistency is detected, then no need to check the rest (already violated)
					return new AbstractMap.SimpleImmutableEntry<>(Context.FP, null); //TODO currently only support FP detection
				}
			}
			return new AbstractMap.SimpleImmutableEntry<>(Context.Normal, null);
		}
		return null;
	}
}
