package nju.xiaofanli.consistency.middleware;

import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.consistency.context.ContextChange;
import nju.xiaofanli.consistency.context.Pattern;
import nju.xiaofanli.consistency.context.Rule;
import nju.xiaofanli.consistency.dataLoader.Configuration;
import nju.xiaofanli.consistency.formula.*;

import java.util.*;

public class Detection {
	private static HashMap<String,Pattern> patterns;
	private static HashMap<String, Rule> rules;
	
	public Detection(HashMap<String,Pattern> patterns, HashMap<String, Rule> rules) {
		Detection.patterns = patterns;
		Detection.rules = rules;
	}
	
    //ECC/PCC���
    public static void check(Rule rule, ContextChange change) {
        Assignment node = new Assignment();
        if(Configuration.getConfigStr("checkingStrategy").equals("PCC")) {
            rule.setFormula(createTreePCC(rule.getFormula(), change));
            rule.getFormula().evaluatePCC(node, change);
            rule.getFormula().generatePCC(change);
        }
        else if(Configuration.getConfigStr("checkingStrategy").equals("ECC")) {
			rule.setFormula(createTreeECC(rule.getFormula()));
			rule.getFormula().evaluateECC(node);
        	rule.getFormula().generateECC();
        }
    }
     
    //��һ��change�ļ��
	public static Set<Link> detect(ContextChange change) {
        if(Configuration.getConfigStr("schedulingStrategy").equals("OFF")) {       
            for(Rule rule : rules.values()) {//ѭ��������е�Լ��
                if(rule.affect(change)) {
//                	Set<Link> prevLinks = rule.getLinks();
                	Operation.change(change);
                	check(rule, change);
                	return rule.getValue() ? null : rule.getLinks();//diff(rule.getLinks(), prevLinks);
                }
            }
        }
        return null;
    }
	
    //�Զ���change�ļ��
	public static Set<Link> detect(Rule rule, List<ContextChange> changes) {
//		Set<Link> prevLinks = rule.getLinks();
		for(ContextChange change : changes){
			Operation.change(change);
			check(rule, change);
		}
//		System.out.println(rule.getName() + ":" + rule.getValue());
		return rule.getValue() ? null : rule.getLinks();//diff(rule.getLinks(), prevLinks);
	}
   
	//������������PCC��
	private static Formula createTreePCC(Formula formula, ContextChange change) {
        switch (formula.getType()) {
            case "forall": {
                ForallFormula result = new ForallFormula("forall");
                result.setPattern(((ForallFormula) formula).getVariable(), ((ForallFormula) formula).getPattern());
                result.setSubFormula(((ForallFormula) formula).getSubFormula());
                result.setSubNodes(new LinkedList<>(((ForallFormula) formula).getSubNodes()));
                result.setValue(formula.getValue());
                result.setLinks(formula.getLinks());
                if(result.affect(change)) {
                    if (result.getSubFormula().affect(change)) {
                        for (SubNode subNode : result.getSubNodes())
                            subNode.setFormula(createTreePCC(subNode.getFormula(), change));
                    }
                    else if (change.getType() == ContextChange.ADDITION) {
                        SubNode subNode = new SubNode(change.getContext());
                        subNode.setFormula(createTreeECC(((ForallFormula) formula).getSubFormula()));
                        result.addSubNode(subNode);
                    }
                    else if (change.getType() == ContextChange.DELETION) {
                        for (int i = 0; i < result.getSubNodes().size(); i++) {
                            if (result.getSubNodes().get(i).getContext().equals(change.getContext())) {
                                result.getSubNodes().remove(i);
                                break;
                            }
                        }
                    }
                }
                return result;
            }
            case "exists": {
                ExistsFormula result = new ExistsFormula("exists");
                result.setPattern(((ExistsFormula) formula).getVariable(), ((ExistsFormula) formula).getPattern());
                result.setSubFormula(((ExistsFormula) formula).getSubFormula());
                result.setSubNodes(new LinkedList<>(((ExistsFormula) formula).getSubNodes()));
                result.setValue(formula.getValue());
                result.setLinks(formula.getLinks());
                if(result.affect(change)) {
                    if (result.getSubFormula().affect(change)) {
                        for (SubNode subNode : result.getSubNodes())
                            subNode.setFormula(createTreePCC(subNode.getFormula(), change));
                    }
                    else if (change.getType() == ContextChange.ADDITION) {
                        SubNode subNode = new SubNode(change.getContext());
                        subNode.setFormula(createTreeECC(((ExistsFormula) formula).getSubFormula()));
                        result.addSubNode(subNode);
                    }
                    else if (change.getType() == ContextChange.DELETION) {
                        for (int i = 0; i < result.getSubNodes().size(); i++) {
                            if (result.getSubNodes().get(i).getContext() == change.getContext()) {
                                result.getSubNodes().remove(i);
                                break;
                            }
                        }
                    }
                }
                return result;
            }
            case "and": {
                AndFormula result = new AndFormula("and");
                result.setValue(formula.getValue());
                result.setLinks(formula.getLinks());
                result.setSubFormula(createTreePCC(((AndFormula) formula).getFirst(), change), createTreePCC(((AndFormula) formula).getSecond(), change));
                return result;
            }
            case "or": {
                OrFormula result = new OrFormula("or");
                result.setValue(formula.getValue());
                result.setLinks(formula.getLinks());
                result.setSubFormula(createTreePCC(((OrFormula) formula).getFirst(), change), createTreePCC(((OrFormula) formula).getSecond(), change));
                return result;
            }
            case "implies": {
                ImpliesFormula result = new ImpliesFormula("implies");
                result.setValue(formula.getValue());
                result.setLinks(formula.getLinks());
                result.setSubFormula(createTreePCC(((ImpliesFormula) formula).getFirst(), change), createTreePCC(((ImpliesFormula) formula).getSecond(), change));
                return result;
            }
            case "not": {
                NotFormula result = new NotFormula("not");
                result.setValue(formula.getValue());
                result.setLinks(formula.getLinks());
                result.setSubFormula(createTreePCC(((NotFormula) formula).getFormula(), change));
                return result;
            }
            case "bfunction": {
                BFunc result = new BFunc(formula.getType());
                result.setValue(formula.getValue());
                result.setLinks(formula.getLinks());
                result.setParam(new HashMap<>(((BFunc) formula).getParam()));
                return result;
            }
        }
        return formula;
    }
	
	//������������ECC��
    private static Formula createTreeECC(Formula formula) {
        switch (formula.getType()) {
            case "forall": {
                ForallFormula result = new ForallFormula("forall");
                result.setPattern(((ForallFormula) formula).getVariable(), ((ForallFormula) formula).getPattern());
                result.setSubFormula(((ForallFormula) formula).getSubFormula());
                for (Context ctx : ((ForallFormula) formula).getPattern().getContexts()) {
                    SubNode element = new SubNode(ctx);
                    element.setFormula(createTreeECC(((ForallFormula) formula).getSubFormula()));
                    result.addSubNode(element);
                }
                return result;
            }
            case "exists": {
                ExistsFormula result = new ExistsFormula("exists");
                result.setPattern(((ExistsFormula) formula).getVariable(), ((ExistsFormula) formula).getPattern());
                result.setSubFormula(((ExistsFormula) formula).getSubFormula());
                for (Context ctx : ((ExistsFormula) formula).getPattern().getContexts()) {
                    SubNode element = new SubNode(ctx);
                    element.setFormula(createTreeECC(((ExistsFormula) formula).getSubFormula()));
                    result.addSubNode(element);
                }
                return result;
            }
            case "and": {
                AndFormula result = new AndFormula("and");
                result.setSubFormula(createTreeECC(((AndFormula) formula).getFirst()), createTreeECC(((AndFormula) formula).getSecond()));
                return result;
            }
            case "or": {
                OrFormula result = new OrFormula("or");
                result.setSubFormula(createTreeECC(((OrFormula) formula).getFirst()), createTreeECC(((OrFormula) formula).getSecond()));
                return result;
            }
            case "implies": {
                ImpliesFormula result = new ImpliesFormula("implies");
                result.setSubFormula(createTreeECC(((ImpliesFormula) formula).getFirst()), createTreeECC(((ImpliesFormula) formula).getSecond()));
                return result;
            }
            case "not": {
                NotFormula result = new NotFormula("not");
                result.setSubFormula(createTreeECC(((NotFormula) formula).getFormula()));
                return result;
            }
            case "bfunction": {
                BFunc result = new BFunc(formula.getType());
                result.setParam(new HashMap<>(((BFunc) formula).getParam()));
                return result;
            }
        }
        return formula;
    }
    
	//��
    public static Set<Link> diff(Set<Link> link1, Set<Link> link2) {//link1-link2
    	Set<Link> diffLinks = new HashSet<>(link1);
        for(Link l1 : link1)
            for(Link l2 : link2)
                if(l1.equals(l2))
                    diffLinks.remove(l1);
        return diffLinks;
    }
}
