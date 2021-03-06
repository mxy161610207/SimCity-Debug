package nju.xiaofanli.consistency.middleware;

import java.util.*;
import java.util.Map.Entry;

import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.consistency.context.ContextChange;
import nju.xiaofanli.consistency.context.Pattern;
import nju.xiaofanli.consistency.context.Rule;
import nju.xiaofanli.consistency.formula.Link;
import nju.xiaofanli.util.Counter;
import nju.xiaofanli.util.Pair;

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
//			System.out.println(rule.getId());
//			for(ContextChange change : changes)
//				System.out.println((change.getType()==1?"ADD":"DEL")+"\t"+change.getContext().getId()+"\t"+change.getPattern().getId());
			Middleware.changeNum += changes.size();
			Set<Link> links = Detection.detect(rule, changes);
//			System.out.println("Inconsistency "+ (rule.getValue()?"undetected":"detected"));
	        if(links != null && !links.isEmpty()){
//	        	for(Link link : links){
//	        		if(!link.getViolated())
//	        			continue;
//	        		for(Map.Entry<String, Context> entry : link.getBinding().entrySet())
//	        			System.out.print(entry.getKey()+": "+entry.getValue().getId()+"\t");
//	        		System.out.println();
//	        	}
				links.forEach(link -> {
					if (link.getViolated())
						Counter.addInconCtx(link.getBinding().values());
				});
//	        	System.out.println("---------------------");
	        	System.out.println("Violated Rule: " + rule.getName());
	            Resolution.resolve(rule, changes, links, strategy);
	            return false;
	        }
//	        System.out.println("---------------------");
        }
		return true;
	}

	static Pair<Integer, List<Context>> operate(Map<Rule, List<ContextChange>> changes, String strategy) {
		if(changes.isEmpty())
			return null;
		changes.keySet().forEach(Rule::setInUse);
		if(strategy.equals("Drop-latest")){
			boolean[] inconsistent = new boolean[]{ false };
			Set<Rule> violated = new HashSet<>();
			changes.forEach((rule, changeList) -> {
				Counter.increaseRuleEvals(rule);
				if (!operate(rule, changeList, strategy)) {
					inconsistent[0] = true;
					rule.increaseViolatedTime();
					violated.add(rule);
					Counter.increaseRuleviols(rule);
				}
			});

			if (inconsistent[0]) {
				changes.forEach((rule, changeList) -> {
					if (!violated.contains(rule))
						Resolution.resolve(rule, changeList, null, strategy); //rollback the consistent rule
				});
				return new Pair<>(Context.FP, null); //TODO currently only support FP detection
			}
			else
				return new Pair<>(Context.Normal, null);
		}
		return null;
	}
}
