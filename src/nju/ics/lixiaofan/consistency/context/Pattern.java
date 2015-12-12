/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.context;


import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * 每一个对象都是一类contexts
 */
public class Pattern {
    public String name;
//    private ArrayList<String> fields = new ArrayList<String>();
    private HashMap<String, Object> fields = new HashMap<String, Object>();//各个field以及其对应的值
    private HashMap<String, Context> contexts = new HashMap<String, Context>();
    private HashSet<String> rules = new HashSet<String>();//rules that use this pattern
    
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(Pattern.class.getName());
    
    public Pattern(String name) {
        this.name = name;
    }
    
    public int getSize() {//该pattern中context的个数
    	return contexts.size();
    }
    
    public String getName() {
    	return name;
    }
    
    public void addRule(String rule){
    	rules.add(rule);
    }
    
//    public ArrayList<String> getFields() {
//    	return fields;
//    }
    
    public HashMap<String, Object> getFields() {
    	return fields;
    }
    
    public HashMap<String, Context> getContexts() {
    	return contexts;
    }
    
    public Context getContext(String name){
    	return contexts.get(name);
    }
    
//    public void addField(String field) {
//        fields.add(field);
//    }
    
    public void addField(String key, Object value) {
        fields.put(key,value);
    }
    
    public void addContext (String key,Context e) {
        contexts.put(key,e);
    }
    
    public boolean deleteContext(String key) {
        if(contexts.containsKey(key)) {
            contexts.remove(key);
            return true;
        }
        else
            return false;
    }
    
    @Override
    public String toString() {
        String str = new String();
        str += " ";
        str += name;
        return str;
    }
}
