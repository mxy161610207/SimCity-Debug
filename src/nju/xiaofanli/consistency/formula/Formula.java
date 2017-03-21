/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.formula;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import nju.xiaofanli.consistency.context.ContextChange;

/**
 *
 * @author bingying
 * formula
 */
public abstract class Formula{
    protected String type; //��ʽ����
    protected boolean value; //�洢֮ǰ��Booleanֵ
    protected Set<Link> links = new HashSet<>(); //�洢֮ǰ��links
    protected String goalLink;
    
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
    
    public abstract void evaluateECC(Assignment node);
    
    public abstract void evaluatePCC(Assignment node,ContextChange change);
    
    public abstract void generateECC();
    
    public abstract void generatePCC(ContextChange change);
    //�ù�ʽ�Ƿ���һ��change���
    public abstract boolean affect(ContextChange change);
    //OCC��
    public abstract void setGoal(String goal);
    
	public abstract Formula createInitialFormula();

    @Override
    public abstract String toString();

    public boolean needBrackets() {
        return this instanceof AndFormula || this instanceof OrFormula || this instanceof ImpliesFormula;
    }

    protected abstract String getName4indentString();

    protected abstract List<Formula> getSubformula4indentString();

    public String getIndentString() {
        return getIndentString(0, new StringBuilder(), new Stack<>()).toString();
    }

    private StringBuilder getIndentString(int level, StringBuilder sb, Stack<Boolean> subformula2print) {
        sb.append("\n");
        if (!subformula2print.isEmpty()) {
            for (int i = 0; i < subformula2print.size() - 1; ++i) {
                // determines if we need to print | at this level to show the tree structure
                sb.append(subformula2print.get(i) ? "\u2502\u2003" : "\u2003\u2003");
            }
            sb.append(subformula2print.lastElement() ? "\u251c\u2500" : "\u2514\u2500");
        }

        sb.append(getName4indentString());
        List<Formula> subformula = getSubformula4indentString();
        for (int i = 0;i < subformula.size();i++) {
            subformula2print.push(i != subformula.size()-1); //has other subformula to print
            subformula.get(i).getIndentString(level+1, sb, subformula2print);
            subformula2print.pop();
        }

        return sb;
    }
}
