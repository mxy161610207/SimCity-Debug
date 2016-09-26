/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.formula;

import nju.xiaofanli.consistency.context.ContextChange;
import nju.xiaofanli.consistency.context.Pattern;
import nju.xiaofanli.consistency.dataLoader.Configuration;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author bingying
 * forall
 */
public class ForallFormula extends Formula {
    private String variable;//������
    private Pattern pattern;//pattern
    private Formula subFormula;//�ӹ�ʽ
    private LinkedList<SubNode> subNodes = new LinkedList<>();

//    public static Log logger = LogFactory.getLog(ForallFormula.class.getName());
    
    public ForallFormula(String name) {
        super(name);
        value = true;
    }
    
    public String getVariable() {
    	return variable;
    }
    
    public Pattern getPattern() {
    	return pattern;
    }
    
    public void setPattern(String var, Pattern pat) {
        variable = var;
        pattern = pat;
    }
    
    public Formula getSubFormula() {
    	return subFormula;
    }
    
    public void setSubFormula(Formula subformula) {
        this.subFormula = subformula;
    }
    
    public LinkedList<SubNode> getSubNodes() {
    	return subNodes;
    }
    
    public void setSubNodes(LinkedList<SubNode> subNodes) {
    	this.subNodes = subNodes;
    }
    
    public void addSubNode(SubNode element) {
        subNodes.add(element);
    }

    @Override
    public boolean evaluateECC(Assignment node) {
        value = true;
        for(SubNode subNode : subNodes) {
            node.setVar(variable, subNode.getContext());
            subNode.evaluateECC(node);
            value = value && subNode.value;
            node.deleteVar(variable);
        }

        return value;
    }

    @Override
    public Set<Link> generateECC() {
        if(Configuration.getConfigStr("optimizingStrategy").equals("ON")) {
            if(!goalLink.equals("false")) {
                return new HashSet<>();
            }
        }
        
        links = new HashSet<>();
        for(SubNode subNode : subNodes) {
            if(!subNode.value)
                links = Link.union(links, Link.linkCartesian(new Link(true, variable, subNode.getContext()),subNode.generateECC()));
        }
        return links;
    }

    @Override
    public boolean affect(ContextChange change) {
    	return change.getPattern() == pattern || subFormula.affect(change);
    }

    @Override
    public boolean evaluatePCC(Assignment node, ContextChange change) {
        if(!affect(change))
        	return value;
        else if(subFormula.affect(change)) {
        	 value = true;
             for(SubNode subNode : subNodes) {
                 node.setVar(variable,subNode.getContext());
                 subNode.evaluatePCC(node,change);
                 value = value && subNode.value;
                 node.deleteVar(variable);
             }
        }
        else if(change.getType() == ContextChange.ADDITION) {
        	SubNode subNode = subNodes.getLast();
            node.setVar(variable, subNode.getContext());
            
            subNode.evaluateECC(node);
            value = value && subNode.value;
            node.deleteVar(variable);
        }
        else if(change.getType() == ContextChange.DELETION) {
        	value = true;
            for(SubNode subNode : subNodes)
                value = value && subNode.value;
        }
        return value;
    }

    @Override
    public Set<Link> generatePCC(ContextChange change) {
        if(Configuration.getConfigStr("optimizingStrategy").equals("ON")) {
            if(!goalLink.equals("false")) {
                return new HashSet<>();
            }
        }
        if(!affect(change))
        	return links;
        else {
            if (subFormula.affect(change)) {
                links = new HashSet<>();
                for (SubNode subNode : subNodes) {
                    if (!subNode.value)
                        links = Link.union(links, Link.linkCartesian(new Link(true, variable, subNode.getContext()), subNode.generatePCC(change)));
                }
            } else if (change.getType() == ContextChange.ADDITION) {
                SubNode subNode = subNodes.getLast();
                if (!subNode.value)
                    links = Link.union(links, Link.linkCartesian(new Link(true, variable, subNode.getContext()), subNode.generateECC()));
            } else {
                if (change.getType() == ContextChange.DELETION) {
                    links = new HashSet<>();
                    for (SubNode subNode : subNodes) {
                        if (!subNode.value) {
                            links = Link.union(links, Link.linkCartesian(new Link(true, variable, subNode.getContext()), subNode.links));
                        }
                    }
                }
            }
        }
        return links;
    }

    @Override
    public void setGoal(String goal) {
        goalLink = goal;
        if(goalLink.equals("false")) {
            subFormula.setGoal(goalLink);
        }
        else {
            subFormula.setGoal("null");
        }
    }
    
	@Override
	public ForallFormula createInitialFormula() {
		ForallFormula f = new ForallFormula(type);
		f.value = value;
		f.variable = variable;
		f.pattern = pattern;
		f.subFormula = subFormula.createInitialFormula();
		return f;
	}
}
