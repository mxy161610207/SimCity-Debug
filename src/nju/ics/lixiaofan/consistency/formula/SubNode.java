/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.formula;

import java.util.Set;

import nju.ics.lixiaofan.consistency.context.Context;
import nju.ics.lixiaofan.consistency.context.ContextChange;

/**
 *
 * @author bingying
 * Forall 与 Exists Formulae 被赋以某个context instance的子节点
 */
public class SubNode{
    private Formula formula;
    private Context context;
    protected boolean value;
    protected Set<Link> links;

    public SubNode(Context context) {
    	this.context = context;
	}

	public void setFormula(Formula formula) {
    	this.formula = formula;
    }
    
    public Formula getFormula() {
    	return formula;
    }
    
    public void setContext(Context context) {
    	this.context = context;
    }
    
    public Context getContext() {
    	return context;
    }
    
    public boolean evaluateEcc(Assignment node) {
    	value = formula.evaluateECC(node);
        return value;
    }

    public boolean evaluatePcc(Assignment node, ContextChange change) {
    	value = formula.evaluatePCC(node,change);
        return value;
    }

    public Set<Link> generateEcc() {
    	links = formula.generateECC();
        return links;
    }

    public Set<Link> generatePcc(ContextChange change) {
    	links = formula.generatePCC(change);
        return links;
    }
}
