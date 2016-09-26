/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.formula;

import java.util.Set;

import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.consistency.context.ContextChange;

/**
 *
 * @author bingying
 * Forall �� Exists Formulae ������ĳ��context instance���ӽڵ�
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
    
    public boolean evaluateECC(Assignment node) {
    	value = formula.evaluateECC(node);
        return value;
    }

    public boolean evaluatePCC(Assignment node, ContextChange change) {
    	value = formula.evaluatePCC(node,change);
        return value;
    }

    public Set<Link> generateECC() {
    	links = formula.generateECC();
        return links;
    }

    public Set<Link> generatePCC(ContextChange change) {
    	links = formula.generatePCC(change);
        return links;
    }
}
