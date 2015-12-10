/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.formula;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;


import nju.ics.lixiaofan.consistency.context.ContextChange;
import nju.ics.lixiaofan.consistency.context.Pattern;
import nju.ics.lixiaofan.consistency.dataLoader.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * forall
 */
public class ForallFormula extends Formula {
    private String variable;//变量名
    private Pattern pattern;//pattern
    private Formula subFormula;//子公式
    private LinkedList<SubNode> subNodes = new LinkedList<SubNode>();
    
    public static Log logger = LogFactory.getLog(ForallFormula.class.getName());
    
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
//        Pattern ct = MiddleWare.getPatterns().get(pattern);
        for(SubNode subNode : subNodes) {
//            Context ctx = ct.getContexts().get(subNode.getContext());
            node.setVar(variable, subNode.getContext());
            subNode.evaluateEcc(node);
            value =  subNode.value && value;
            node.deleteVar(variable);
        }
        return value;
    }

    @Override
    public Set<Link> generateECC() {
        if(Configuration.getConfigStr("optimizingStrategy").equals("ON")) {
            if(!goalLink.equals("false")) {
                return new HashSet<Link>();
            }
        }
        
//        Pattern pat = MiddleWare.getPatterns().get(pattern);
        links = new HashSet<Link>();
        for(SubNode subNode : subNodes) {
//            Context ctx = pat.getContexts().get(subNode.getContext());
            if(!subNode.value)
                links = Link.union(links, Link.linkCartesian(new Link(true, variable, subNode.getContext()),subNode.generateEcc()));
        }
        return links;
    }

    @Override
    public boolean affect(ContextChange change) {
    	return change.getPattern() == pattern || subFormula.affect(change);
    }

    @Override
    public boolean evaluatePCC(Assignment node,ContextChange change) {
        if(!affect(change))
        	return value;
        else if(subFormula.affect(change)) {
        	 value = true;
//             Pattern pat = MiddleWare.getPatterns().get(pattern);
             for(SubNode subNode : subNodes) {
//                 Context ctx = pat.getContexts().get(subNode.getContext());
                 node.setVar(variable,subNode.getContext());
                 subNode.evaluatePcc(node,change);
                 value = subNode.value && value;
                 node.deleteVar(variable);
             }
        }
        else if(change.getType() == ContextChange.ADDITION) {
        	SubNode subNode = subNodes.getLast();
//            Pattern pat = MiddleWare.getPatterns().get(pattern);
//            Context ctx = pat.getContexts().get(subNode.getContext());
            node.setVar(variable, subNode.getContext());
            
            subNode.evaluateEcc(node);
            value = subNode.value && value;
            node.deleteVar(variable);
        }
        else if(change.getType() == ContextChange.DELETION) {
        	value = true;
            for(SubNode subNode : subNodes)
                value = value && subNode.value;
        }
//        else if(change.getOperate() == ContextChange.UPDATE) {
//            ContextChange deletion = new ContextChange();
//            deletion.setOperate(2);
//            deletion.setPatternName(change.getPatternName());
//            deletion.setContextName(change.getContextName());
//            Pattern pat = MiddleWare.getPatterns().get(change.getPatternName());
//            Context ctx = pat.getContexts().get(change.getContextName());
//            deletion.addContent(ctx.getFields().values());
//            
//            ChangeOperate.change(deletion);
//            evaluatePCC(node,deletion);
//            
//            change.setOperate(1);
//            ChangeOperate.change(change);
//            value = evaluatePCC(node,change);
//            change.setOperate(3);//reset its operator
//        }
        return value;
    }

    @Override
    public Set<Link> generatePCC(ContextChange change) {
        if(Configuration.getConfigStr("optimizingStrategy").equals("ON")) {
            if(!goalLink.equals("false")) {
                return new HashSet<Link>();
            }
        }
        if(!affect(change))
        	return links;
        else if(subFormula.affect(change)) {
        	links = new HashSet<Link>();
//            Pattern pat = MiddleWare.getPatterns().get(this.pattern);
            for(SubNode subNode : subNodes) {
//                Context ctx = pat.getContexts().get(subNode.getContext());
                if(!subNode.value)
                    links = Link.union(links, Link.linkCartesian(new Link(true, variable, subNode.getContext()), subNode.generatePcc(change)));
            }
        }
        else if(change.getType() == ContextChange.ADDITION) {
            SubNode subNode = subNodes.getLast();
//            Pattern pat = MiddleWare.getPatterns().get(pattern);
//            Context ctx = pat.getContexts().get(subNode.getContext());
            if(!subNode.value)
                links = Link.union(links, Link.linkCartesian(new Link(true, variable, subNode.getContext()), subNode.generateEcc()));
        }
        else if(change.getType() == ContextChange.DELETION) {
        	links = new HashSet<Link>();
//            Pattern pat = MiddleWare.getPatterns().get(pattern);
            for(SubNode subNode : subNodes){
            	if(!subNode.value){
//	            	Context ctx = pat.getContexts().get(subNode.getContext());
	            	links = Link.union(links, Link.linkCartesian(new Link(true, variable, subNode.getContext()), subNode.links));
            	}
            }
        }
//        else if(change.getOperate() == ContextChange.UPDATE) {
//            ContextChange deletion = new ContextChange();
//            deletion.setOperate(2);
//            deletion.setPatternName(change.getPatternName());
//            deletion.setContextName(change.getContextName());
//            Pattern pat = MiddleWare.getPatterns().get(change.getPatternName());
//            Context ctx = pat.getContexts().get(change.getContextName());
//            deletion.addContent(ctx.getFields().values());
//            ChangeOperate.change(deletion);
//            generatePCC(deletion);
//            
//            change.setOperate(1);
//            ChangeOperate.change(change);
//            links = generatePCC(change);
//            change.setOperate(3);//reset its operator
//        }
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
    
}
