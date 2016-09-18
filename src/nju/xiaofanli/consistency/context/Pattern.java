/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.context;


import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * ÿһ��������һ��contexts
 */
public class Pattern {
    private String name;
//    private ArrayList<String> fields = new ArrayList<String>();
    private HashMap<String, Object> fields = new HashMap<>();//����field�Լ����Ӧ��ֵ
    private LinkedList<Context> contexts = new LinkedList<>();
    private int ctxNum = Integer.MAX_VALUE;
    private String rule;//assumption: a pattern can only be used by one rule
    
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(Pattern.class.getName());
    
    public Pattern(String name) {
        this.name = name;
    }
    
    public int getSize() {//��pattern��context�ĸ���
    	return contexts.size();
    }
    
    public String getName() {
    	return name;
    }
    
    public void setRule(String rule){
    	this.rule = rule;
    }
    
    public String getRule(){
    	return rule;
    }
    
//    public ArrayList<String> getFields() {
//    	return fields;
//    }
    
    public HashMap<String, Object> getFields() {
    	return fields;
    }
    
    public LinkedList<Context> getContexts() {
    	return contexts;
    }
    
//    public Context getContext(String name){
//    	return contexts.get(name);
//    }
    
//    public void addField(String field) {
//        fields.add(field);
//    }
    
    public void addField(String key, Object value) {
        fields.put(key,value);
    }
    
    public void addContext(Context ctx) {
        contexts.add(ctx);
    }
    
	public void addContext(int ctxIdx, Context context) {
		contexts.add(ctxIdx, context);
	}
    
    public void deleteContext(Context ctx) {
        if(contexts.contains(ctx))
            contexts.remove(ctx);
    }
    
    public void setMaxCtxNum(int num){
    	this.ctxNum = num;
    }
    
    public boolean isFull(){
    	return contexts.size() == ctxNum;
    }

	public void reset() {
		contexts.clear();
	}
	
    @Override
    public String toString() {
        String str = "";
        str += " ";
        str += name;
        return str;
    }
}
