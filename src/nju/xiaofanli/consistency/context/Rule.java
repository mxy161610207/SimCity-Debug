/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.context;

import java.util.HashMap;
import java.util.Set;

import nju.xiaofanli.consistency.formula.Formula;
import nju.xiaofanli.consistency.formula.Link;

/**
 *
 * @author bingying
 * ÿһ��constrainת��ΪRule���󱣴�����
 */
public class Rule {
    private String name;
    private Formula formula, initialFormula;//һ���߼���ʽ
    
    public HashMap<ContextChange,String> sch;
    
//    @SuppressWarnings("unused")
//	private static Log logger = LogFactory.getLog(Rule.class.getName());
    
    public Rule(String name) {
        this.name = name;
    }
    
    public String getName() {
    	return name;
    }
    
    public void setFormula(Formula formula) {
    	this.formula = formula;
    }
    
    public Formula getFormula() {
    	return formula;
    }
    
    public Set<Link> getLinks() {
        return formula.getLinks();
    }
    
    public boolean getValue() {
        return formula.getValue();
    }
    
    public boolean affect(ContextChange change) {
        return formula.affect(change);
    }
    /**
     * Only called when this rule is initialized
     */
	public void setInitialFormula() {
		initialFormula = formula.createInitialFormula();
	}
	
	public void reset() {
		formula = initialFormula.createInitialFormula();
	}
    
    public double hazardPairProb(ContextChange change,ContextChange laterChange) {
        return 0.9;
    }
    
    public boolean isHazardPair(ContextChange change,ContextChange laterChange) {
        return false;
    }
}
