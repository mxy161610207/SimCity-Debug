package nju.ics.lixiaofan.consistency.middleware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import nju.ics.lixiaofan.consistency.context.*;
import nju.ics.lixiaofan.consistency.dataLoader.Configuration;
import nju.ics.lixiaofan.consistency.formula.*;
import nju.ics.lixiaofan.consistency.formula.BFunc.Param;

public class Detection {
	public static HashMap<String,Pattern> patterns = new HashMap<String,Pattern>();
	private static HashSet<Rule> rules = new HashSet<Rule>();
	
	public Detection(HashMap<String,Pattern> patterns, HashSet<Rule> rules) {
		Detection.patterns = patterns;
		Detection.rules = rules;
	}
	
    //ECC/PCC检测
    public static void detect(Rule rule, ContextChange change) {
        Assignment node = new Assignment();
        if(Configuration.getConfigStr("checkingStragegy").equals("PCC")) {
        	if(change.getType() != ContextChange.UPDATE){
	            rule.setFormula(createTreePCC(rule.getFormula(),change));
	            rule.getFormula().evaluatePCC(node,change);
	            rule.getFormula().generatePCC(change);
        	}
        	else{
        		Context ctx = change.getPattern().getContext(change.getContext().getName());
        		ContextChange original = new ContextChange(ContextChange.DELETION, change.getPattern(), ctx);
                ChangeOperate.change(original);
                rule.setFormula(createTreePCC(rule.getFormula(), original));
	            rule.getFormula().evaluatePCC(node, original);
	            rule.getFormula().generatePCC(original);
	            original.setType(ContextChange.UPDATE);
	            change.setOriginal(original);//if this change need resolution, then use the original to update
                
                change.setType(ContextChange.ADDITION);
                ChangeOperate.change(change);
                rule.setFormula(createTreePCC(rule.getFormula(), change));
	            rule.getFormula().evaluatePCC(node, change);
	            rule.getFormula().generatePCC(change);
                change.setType(ContextChange.UPDATE);//reset its type
        	}
        }
		if(Configuration.getConfigStr("checkingStragegy").equals("ECC")) {
			rule.setFormula(createTreeECC(rule.getFormula()));
			rule.getFormula().evaluateECC(node);
        	rule.getFormula().generateECC();
        }
    }
     
    //对一条change的检测
	public static Set<Link> singleChangeDetect(ContextChange change) {
		if(change == null || !ChangeOperate.change(change))
			return new HashSet<Link>();
        if(Configuration.getConfigStr("schedulingStrategy").equals("OFF")) {       
            for(Rule rule : rules) {//循环检测所有的约束
                if(rule.affect(change)) {
                	Set<Link> diffLinks = new HashSet<Link>(), prevLinks = rule.getLinks();
                	detect(rule, change);
                    if(!rule.getValue()) {
                        diffLinks = diff(rule.getLinks(), prevLinks);
                        for(Link l : diffLinks)
                            System.out.println(Middleware.changeNum + " " + rule.getName() + l.toString());                       
                    }
                	if(!diffLinks.isEmpty())
                		return diffLinks;
                }
            }
        }
        return null;
    }
	
    //对多条change的检测
	public static Set<Link> multiChangeDetect(ArrayList<ContextChange> changes) {
		//TODO
		return null;
	}
   
	//构建计算树（PCC）
	private static Formula createTreePCC(Formula formula,ContextChange change) {
        if(formula.getType().equals("forall")) {
            ForallFormula result = new ForallFormula("forall");
            result.setPattern(((ForallFormula)formula).getVariable(), ((ForallFormula)formula).getPattern());
            result.setSubFormula(((ForallFormula)formula).getSubFormula());
            result.setSubNodes(new LinkedList<SubNode>(((ForallFormula)formula).getSubNodes()));
            result.setValue(((ForallFormula)formula).getValue());
            result.setLinks(((ForallFormula)formula).getLinks());
            if(result.getSubFormula().affect(change)) {
                for(SubNode subNode : result.getSubNodes())
                    subNode.setFormula(createTreePCC(((ForallFormula)formula).getSubFormula(),change));
            }
            else if(change.getType() == ContextChange.ADDITION) {
                SubNode subNode = new SubNode(change.getContext());
                subNode.setFormula(createTreeECC(((ForallFormula)formula).getSubFormula()));
                result.addSubNode(subNode);
            }
            else if(change.getType() == ContextChange.DELETION) {
                for(int i = 0;i < result.getSubNodes().size();i++) {
                    if(result.getSubNodes().get(i).getContext().equals(change.getContext())) {
                        result.getSubNodes().remove(i);
                        break;
                    }
                }
            }
//            else if(change.getType() == ContextChange.UPDATE) {   
//            }
            return result;
        }
        else if(formula.getType().equals("exists")) {
            ExistsFormula result = new ExistsFormula("exists");
            result.setPattern(((ExistsFormula)formula).getVariable(),((ExistsFormula)formula).getPattern());
            result.setSubFormula(((ExistsFormula)formula).getSubFormula());
            result.setSubNodes(new LinkedList<SubNode>(((ExistsFormula)formula).getSubNodes()));
            result.setValue(((ExistsFormula)formula).getValue());
            result.setLinks(((ExistsFormula)formula).getLinks());
            if(result.getSubFormula().affect(change)) {
            	for(SubNode subNode : result.getSubNodes())
                    subNode.setFormula(createTreePCC(((ExistsFormula)formula).getSubFormula(),change));
            }
            else if(change.getType() == ContextChange.ADDITION) {
                SubNode subNode = new SubNode(change.getContext());
                subNode.setFormula(createTreeECC(((ExistsFormula)formula).getSubFormula()));
                result.addSubNode(subNode);
            }
            else if(change.getType() == ContextChange.DELETION) {
                for(int i = 0;i < result.getSubNodes().size();i++) {
                    if(result.getSubNodes().get(i).getContext() == change.getContext()) {
                        result.getSubNodes().remove(i);
                        break;
                    }
                }
            }
//            else if(change.getType() == ContextChange.UPDATE) {     
//            }
            return result;
        }
        else if(formula.getType().equals("and")) {
            AndFormula result = new AndFormula("and");
            result.setValue(((AndFormula)formula).getValue());
            result.setLinks(((AndFormula)formula).getLinks());
            result.setSubFormula(createTreePCC(((AndFormula)formula).getFirst(),change),createTreePCC(((AndFormula)formula).getSecond(),change));
            return result;
        }
        else if(formula.getType().equals("or")) {
            OrFormula result = new OrFormula("or");
            result.setValue(((OrFormula)formula).getValue());
            result.setLinks(((OrFormula)formula).getLinks());
            result.setSubFormula(createTreePCC(((OrFormula)formula).getFirst(),change),createTreePCC(((OrFormula)formula).getSecond(),change));
            return result;
        }
        else if(formula.getType().equals("implies")) {
            ImpliesFormula result = new ImpliesFormula("implies");
            result.setValue(((ImpliesFormula)formula).getValue());
            result.setLinks(((ImpliesFormula)formula).getLinks());
            result.setSubFormula(createTreePCC(((ImpliesFormula)formula).getFirst(),change),createTreePCC(((ImpliesFormula)formula).getSecond(),change));
            return result;
        }
        else if(formula.getType().equals("not")) {
            NotFormula result = new NotFormula("not");
            result.setValue(((NotFormula)formula).getValue());
            result.setLinks(((NotFormula)formula).getLinks());
            result.setSubFormula(createTreePCC(((NotFormula)formula).getFormula(), change));
            return result;
        }
        else if(formula.getType().equals("bfunction")) {
            BFunc result = new BFunc(formula.getType());
            result.setValue(((BFunc)formula).getValue());
            result.setLinks(((BFunc)formula).getLinks());
            result.setParam(new HashMap<String, Param>(((BFunc)formula).getParam()));
            return result;
        }
        return formula;
    }
	
	//构建计算树（ECC）
    private static Formula createTreeECC(Formula formula) {
        if(formula.getType().equals("forall")) {
            ForallFormula result = new ForallFormula("forall");
            result.setPattern(((ForallFormula)formula).getVariable(),((ForallFormula)formula).getPattern());
            result.setSubFormula(((ForallFormula)formula).getSubFormula());
            for(Context ctx : ((ForallFormula)formula).getPattern().getContexts()) {
                SubNode element = new SubNode(ctx);
                element.setFormula(createTreeECC(((ForallFormula)formula).getSubFormula()));
                result.addSubNode(element);
            }
            return result;
        }
        else if(formula.getType().equals("exists")) {
            ExistsFormula result = new ExistsFormula("exists");
            result.setPattern(((ExistsFormula)formula).getVariable(),((ExistsFormula)formula).getPattern());
            result.setSubFormula(((ExistsFormula)formula).getSubFormula());
            for (Context ctx : ((ForallFormula)formula).getPattern().getContexts()) {
                SubNode element = new SubNode(ctx);
                element.setFormula(createTreeECC(((ExistsFormula)formula).getSubFormula()));
                result.addSubNode(element);
            }
            return result;
        }
        else if(formula.getType().equals("and")) {
            AndFormula result = new AndFormula("and");
            result.setSubFormula(createTreeECC(((AndFormula)formula).getFirst()),createTreeECC(((AndFormula)formula).getSecond()));
            return result;
        }
        else if(formula.getType().equals("or")) {
            OrFormula result = new OrFormula("or");
            result.setSubFormula(createTreeECC(((OrFormula)formula).getFirst()),createTreeECC(((OrFormula)formula).getSecond()));
            return result;
        }
        else if(formula.getType().equals("implies")) {
            ImpliesFormula result = new ImpliesFormula("implies");
            result.setSubFormula(createTreeECC(((ImpliesFormula)formula).getFirst()),createTreeECC(((ImpliesFormula)formula).getSecond()));
            return result;
        }
        else if(formula.getType().equals("not")) {
            NotFormula result = new NotFormula("not");
            result.setSubFormula(createTreeECC(((NotFormula)formula).getFormula()));
            return result;
        }
        else if(formula.getType().equals("bfunction")) {
            BFunc result = new BFunc(formula.getType());
            result.setParam(new HashMap<String, Param>(((BFunc)formula).getParam()));
            return result;
        }
        return formula;
    }
    
	//求差集
    public static Set<Link> diff(Set<Link> link1, Set<Link> link2) {//link1-link2
    	Set<Link> diffLinks = new HashSet<Link>(link1);
        for(Link l1 : link1)
            for(Link l2 : link2)
                if(l1.equals(l2))
                    diffLinks.remove(l1);
        return diffLinks;
    }
}
