/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.middleware;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import nju.ics.lixiaofan.consistency.context.ContextChange;
import nju.ics.lixiaofan.consistency.context.Rule;
import nju.ics.lixiaofan.consistency.dataLoader.Configuration;
import nju.ics.lixiaofan.consistency.formula.Link;

/**
 *
 * @author bingying
 * shap”√
 */
public class Schedule {
    private LinkedList<ContextChange> changes;
    private ArrayList<Rule> rules = new ArrayList<Rule>();
    private final int N = 5;
    
    public Schedule(ArrayList<Rule> rules) {
        this.rules = rules;
    }
    public void schedule(ContextChange change) {
        for(int i = 0;i < rules.size();i++) {
            Rule rule = rules.get(i);
            if(!rule.affect(change)) {
                rule.sch.put(change,"no");
            }
            else {
                if(rule.hazardPairProb(change,null) < 0.01) {
                    rule.sch.put(change,"yes");
                }
                else {
                    rule.sch.put(change,"undecided");
                }
            }
            for(int j = 0;j < changes.size();j++) {
                if(rule.sch.get(changes.get(j)).equals("undecided") && rule.isHazardPair(changes.get(j),change)) {
                    rule.sch.put(changes.get(j),"no");
                }
            }
            if(changes.size() >= N && rule.sch.get(changes.get(0)).equals("undecided")) {
                rule.sch.put(changes.get(0),"yes");
            }
        }
    }
    
    public void shap(ContextChange change) {
        changes.add(change);
        schedule(change);
        if(changes.size() >= N) {
            ContextChange topChange = changes.get(0);
            changes.remove(0);
            if(ChangeOperate.change(topChange)) {
                return;
            }
            for(int i = 0;i < rules.size();i++) {
                Rule rule = rules.get(i);
                Set<Link> links = new HashSet<Link>();
                if(rule.sch.get(topChange).equals("yes")) {
                    links = Detection.detect(rule,topChange);
                }
                if(!links.isEmpty()) {
                    Resolution.resolve(change,links,Configuration.getConfigStr("resolutionStrategy"));
                	return;
                }
            }
        }
    }
    
}
