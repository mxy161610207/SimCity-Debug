/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.formula;

import java.util.Set;

import nju.ics.lixiaofan.consistency.context.ContextChange;

/**
 *
 * @author bingying
 * ->��ʽ
 */
public class ImpliesFormula extends Formula {
    private Formula first; 
    private Formula second;
    
//    @SuppressWarnings("unused")
//	private static Log logger = LogFactory.getLog(AndFormula.class.getName());
    
    public ImpliesFormula(String name) {
        super(name);
    }
    
    public void setSubFormula(Formula left,Formula right) {
        first = left;
        second = right;
        value = !first.value || second.value;
    }

    public Formula getFirst() {
    	return first;
    }

    public Formula getSecond() {
    	return second;
    }
    
    @Override
    public boolean evaluateECC(Assignment node) {
        value = !first.evaluateECC(node) || second.evaluateECC(node);
        return value;
    }

    @Override
    public Set<Link> generateECC() {
    	Set<Link> l1 = first.generateECC();
    	Set<Link> l2 = second.generateECC();
        
        if(first.value)
    		links = second.value ? l2 : Link.linkCartesian(Link.flip(l1), l2);
    	else
    		links = second.value ? Link.union(Link.flip(l1), l2) : Link.flip(l1);
        return links;
    }

    @Override
    public boolean affect(ContextChange change) {
        return first.affect(change) || second.affect(change);
    }

    @Override
    public boolean evaluatePCC(Assignment node,ContextChange change) {
    	if(first.affect(change)){
    		if(second.affect(change))
    			value = !first.evaluatePCC(node,change) || second.evaluatePCC(node,change);
    		else
    			value = !first.evaluatePCC(node,change) || second.value;
    	}
    	else if(second.affect(change))
    		value = !first.value || second.evaluatePCC(node,change);
    	
        return value;
    }

    @Override
    public Set<Link> generatePCC(ContextChange change) {
    	if(!affect(change))
    		return links;
    	
    	Set<Link> l1 = first.affect(change) ? first.generatePCC(change) : first.links;
    	Set<Link> l2 = second.affect(change) ? second.generatePCC(change) : second.links;
    	
    	if(first.value)
    		links = second.value ? l2 : Link.linkCartesian(Link.flip(l1), l2);
    	else
    		links = second.value ? Link.union(Link.flip(l1), l2) : Link.flip(l1);
        return links;
    }

    @Override
    public void setGoal(String goal) {
        goalLink = goal;
        if(!goalLink.equals("null")) {
            first.setGoal(Boolean.toString(!Boolean.parseBoolean(goalLink)));
            second.setGoal(goalLink);
        }
        else {
            first.setGoal("null");
            second.setGoal("null");
        }
    }
    
	@Override
	public ImpliesFormula createInitialFormula() {
		ImpliesFormula f = new ImpliesFormula(type);
    	f.value = value;
    	f.first = first.createInitialFormula();
    	f.second = second.createInitialFormula();
		return f;
	}
}
