/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.formula;

import java.util.HashMap;

import nju.ics.lixiaofan.consistency.context.Context;

/**
 *
 * @author bingying
 * 一次检测的参数 v1 = element1,v2 = element2....
 */
public class Assignment {
    private HashMap<String, Context> vars;
    
    public Assignment() {
        vars = new HashMap<String, Context>();
    }
    
    public Assignment(Assignment node) {
        vars = new HashMap<String,Context>(node.vars);
    }
    
    public HashMap<String, Context> getVar() {
        return vars;
    }
    
    public void setVar(String variable,Context element) {
        vars.put(variable,element);
    }
    
    public void deleteVar(String variable) {
        vars.remove(variable);
    }
}
