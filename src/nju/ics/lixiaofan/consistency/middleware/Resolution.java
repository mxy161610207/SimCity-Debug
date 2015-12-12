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
        		ContextChange newChange = new ContextChange(ContextChange.DELETION, context);
        		for(Pattern pattern : context.getPatterns()){
        			newChange.setPattern(pattern);
	                if(ChangeOperate.change(newChange))
	                	Detection.singleChangeDetect(newChange);
        		}
        	}
        }
        else if(strategy.equals("Drop-latest")) {
            ContextChange newChange = flip(change);
            if(ChangeOperate.change(newChange))
            	Detection.singleChangeDetect(newChange);
        }
        else if(strategy.equals("Drop-random")) {
        	Set<Context> contexts = getContexts(links);
        	Context context = new ArrayList<Context>(contexts).get((int)Math.random()*contexts.size());
        	ContextChange newChange = new ContextChange(ContextChange.DELETION, context);
    		for(Pattern pattern : context.getPatterns()){
    			newChange.setPattern(pattern);
                if(ChangeOperate.change(newChange))
                	Detection.singleChangeDetect(newChange);
    		}
        }
    }
    
    public static Set<Context> getContexts(Set<Link> links) {
    	Set<Context> result = new HashSet<Context>();
        for(Link l : links)
            result.addAll(l.getBinding().values());
    	return result;
    }
    
    //获得相反的change
	public static ContextChange flip(ContextChange change) {
        ContextChange flipChange = new ContextChange();
        if(change.getType() == ContextChange.UPDATE){
        	flipChange.setPattern(change.getOriginal().getPattern());
            flipChange.setContext(change.getOriginal().getContext());
            flipChange.setContext(change.getOriginal().getContext());
            flipChange.setType(ContextChange.UPDATE);
        }
        else{
	        flipChange.setPattern(change.getPattern());
	        flipChange.setContext(change.getContext());
	        flipChange.setContext(change.getContext());
	        if(change.getType() != ContextChange.DELETION)
	        	flipChange.setType(ContextChange.DELETION);
	        else
	        	flipChange.setType(ContextChange.ADDITION);
        }
        return flipChange;
    }
}
