/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.formula;

import nju.xiaofanli.consistency.context.ContextChange;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author bingying
 * һԪ������ʽ��not
 */
public class NotFormula extends Formula {
    private Formula formula;
    
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
    public void evaluateECC(Assignment node) {
        formula.evaluateECC(node);
    	value = !formula.getValue();
    }

    @Override
    public void generateECC() {
        formula.generateECC();
    	links = Link.flip(formula.getLinks());
    }

    @Override
    public boolean affect(ContextChange change) {
        return formula.affect(change);
    }

    @Override
    public void evaluatePCC(Assignment node, ContextChange change) {
        if(affect(change)) {
            formula.evaluatePCC(node, change);
            value = !formula.getValue();
        }
    }

    @Override
    public void generatePCC(ContextChange change) {
        if(affect(change)) {
            formula.generatePCC(change);
            links = Link.flip(formula.getLinks());
        }
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
    
	@Override
	public NotFormula createInitialFormula() {
		NotFormula f = new NotFormula(type);
    	f.value = value;
    	f.formula = formula.createInitialFormula();
		return f;
	}

    @Override
    public String toString() {
        return "\u00ac" + (formula.needBrackets() ? "(" + formula + ")" : formula);
    }

    @Override
    protected String getName4indentString() {
        return "not";
    }

    @Override
    protected List<Formula> getSubformula4indentString() {
        return Collections.singletonList(formula);
    }
}
