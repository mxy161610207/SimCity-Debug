/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.formula;

import java.util.HashMap;

import nju.xiaofanli.consistency.context.Context;

/**
 *
 * @author bingying
 * һ�μ��Ĳ��� v1 = element1,v2 = element2....
 */
public class Assignment {
    private HashMap<String, Context> vars;
    
    public Assignment() {
        vars = new HashMap<>();
    }
    
    public Assignment(Assignment node) {
        vars = new HashMap<>(node.vars);
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
