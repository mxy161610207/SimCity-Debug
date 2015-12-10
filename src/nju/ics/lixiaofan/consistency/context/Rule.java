/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.context;

import java.util.HashMap;
import java.util.Set;

import nju.ics.lixiaofan.consistency.formula.Formula;
import nju.ics.lixiaofan.consistency.formula.Link;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * ÿһ��constrainת��ΪRule���󱣴�����
 */
public class Rule {
    private String name;
    private nju.ics.lixiaofan.consistency.formula.Formula formula;//һ���߼���ʽ
    
    public HashMap<ContextChange,String> sch;
    
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(Rule.class.getName());
    
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
    
    public double hazardPairProb(ContextChange change,ContextChange laterChange) {
        return 0.9;
    }
    
    public boolean isHazardPair(ContextChange change,ContextChange laterChange) {
        return false;
    }
}
