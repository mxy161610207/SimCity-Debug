/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.formula;

import java.util.Set;

import nju.ics.lixiaofan.consistency.context.ContextChange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * 一元操作公式，not
 */
public class NotFormula extends Formula {
    private Formula formula;
    
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(NotFormula.class.getName());
    
    public NotFormula(String name) {
        super(name);
    }
    
    public void setSubFormula(Formula sub_formula) {
        formula = sub_formula;
        value = !formula.value;
    }
    
    public Formula getFormula() {
    	return formula;
    }
    
    @Override
    public boolean evaluateECC(Assignment node) {
    	value = !formula.evaluateECC(node);
        return value;
    }

    @Override
    public Set<Link> generateECC() {
    	links = Link.flip(formula.generateECC());
        return links;
    }

    @Override
    public boolean affect(ContextChange change) {
        return formula.affect(change);
    }

    @Override
    public boolean evaluatePCC(Assignment node,ContextChange change) {
        if(affect(change))
        	value = !(formula.evaluatePCC(node,change));
        return value;
    }

    @Override
    public Set<Link> generatePCC(ContextChange change) {
        if(affect(change))
        	links = Link.flip(formula.generatePCC(change));
        return links;
    }

    @Override
    public void setGoal(String goal) {
        goalLink = goal;
        if(!goalLink.equals("null")) {
            formula.setGoal(Boolean.toString(!Boolean.parseBoolean(goalLink)));
        }
        else {
            formula.setGoal("null");
        }
    }
}
