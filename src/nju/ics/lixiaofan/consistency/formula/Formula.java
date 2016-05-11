/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.formula;

import java.util.HashSet;
import java.util.Set;

import nju.ics.lixiaofan.consistency.context.ContextChange;

/**
 *
 * @author bingying
 * formula
 */
public abstract class Formula{
    protected String type; //公式类型
    protected boolean value; //存储之前的Boolean值
    protected Set<Link> links = new HashSet<Link>(); //存储之前的links
    protected String goalLink;
    
//    @SuppressWarnings("unused")
//	private static Log logger = LogFactory.getLog(Formula.class.getName());
    
    public Formula(String type) {
        this.type = type;
    }
    
    public String getType() {
    	return type;
    }
    
    public boolean getValue() {
    	return value;
    }
    
    public void setValue(boolean value) {
    	this.value = value;
    }
    
    public Set<Link> getLinks() {
    	return links;
    }
    
    public void setLinks(Set<Link> link) {
    	this.links = link;
    }
    
    public abstract boolean evaluateECC(Assignment node);
    
    public abstract boolean evaluatePCC(Assignment node,ContextChange change);
    
    public abstract Set<Link> generateECC();
    
    public abstract Set<Link> generatePCC(ContextChange change);
    //该公式是否与一条change相关
    public abstract boolean affect(ContextChange change);
    //OCC用
    public abstract void setGoal(String goal);
    
	public abstract Formula createInitialFormula();
}
