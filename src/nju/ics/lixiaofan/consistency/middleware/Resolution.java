/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.middleware;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import nju.ics.lixiaofan.consistency.context.Context;
import nju.ics.lixiaofan.consistency.context.ContextChange;
import nju.ics.lixiaofan.consistency.context.Pattern;
import nju.ics.lixiaofan.consistency.context.Rule;
import nju.ics.lixiaofan.consistency.formula.Link;

/**
 *
 * @author bingying
 * resolution
 */
public class Resolution {
    public static void resolve(ContextChange change, Set<Link> links, String strategy) {
        if(strategy.equals("Drop-all")) {
        	Set<Context> contexts = getContexts(links);
        	for(Context context : contexts) {
        		for(Pattern pattern : context.getPatterns()){
        			ContextChange newChange = new ContextChange(ContextChange.DELETION, pattern, context);
	                Detection.detect(newChange);
        		}
        	}
        }
        else if(strategy.equals("Drop-latest")) {
            ContextChange newChange = flip(change);
            Detection.detect(newChange);
        }
        else if(strategy.equals("Drop-random")) {
        	Set<Context> contexts = getContexts(links);
        	Context context = new ArrayList<Context>(contexts).get((int)Math.random()*contexts.size());
    		for(Pattern pattern : context.getPatterns()){
    			ContextChange newChange = new ContextChange(ContextChange.DELETION, pattern, context);
    			newChange.setPattern(pattern);
                Detection.detect(newChange);
    		}
        }
    }
    
	public static void resolve(Rule rule, ArrayList<ContextChange> changes, Set<Link> links, String strategy) {
		if(strategy.equals("Drop-latest")){
			ArrayList<ContextChange> newChanges = new ArrayList<ContextChange>();
//			System.out.println("Resolution");
			for(int i = changes.size()-1;i >= 0;i--){
				ContextChange newChange = flip(changes.get(i));
				newChanges.add(newChange);
//				System.out.println((newChange.getType()==1?"ADD":"DEL")+"\t"+newChange.getContext().getName()+"\t"+newChange.getPattern().getName());
			}
			Detection.detect(rule, newChanges);
		}
	}
    
    private static Set<Context> getContexts(Set<Link> links) {
    	Set<Context> result = new HashSet<Context>();
        for(Link l : links)
            result.addAll(l.getBinding().values());
    	return result;
    }
    
    //获得相反的change
	private static ContextChange flip(ContextChange change) {
        ContextChange flipChange = new ContextChange();
        flipChange.setPattern(change.getPattern());
        flipChange.setContext(change.getContext());
        flipChange.setCtxIdx(change.getCtxIdx());
        flipChange.setType(change.getType() == ContextChange.DELETION ? ContextChange.ADDITION : ContextChange.DELETION);
        return flipChange;
    }
}
