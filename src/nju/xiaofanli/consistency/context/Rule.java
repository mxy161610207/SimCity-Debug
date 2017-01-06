/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.context;

import nju.xiaofanli.consistency.formula.Formula;
import nju.xiaofanli.consistency.formula.Link;
import nju.xiaofanli.dashboard.Dashboard;

import java.util.Set;

/**
 *
 * @author bingying
 * ÿһ��constrainת��ΪRule���󱣴�����
 */
public class Rule {
    private String name;
    private Formula formula, initialFormula;//һ���߼���ʽ
    private String explanInEn, explanInCh;
    private int violatedTimes;
    //    @SuppressWarnings("unused")
//	private static Log logger = LogFactory.getLog(Rule.class.getId());
    
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
		violatedTimes = 0;
	}

	public int getViolatedTimes() {
	    return violatedTimes;
    }

    public void increaseViolatedTime() {
	    violatedTimes++;
        Dashboard.updateFixedError();
    }

	public void setExplanation(String explan, boolean useEnglish) {
	    if (useEnglish)
	        this.explanInEn = explan;
	    else
	        this.explanInCh = explan;
    }

    public String getExplanation(boolean useEnglish) {
	    return useEnglish ? explanInEn : explanInCh;
    }
}
