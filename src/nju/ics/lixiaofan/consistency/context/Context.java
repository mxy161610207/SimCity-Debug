/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 *
 * @author bingying
 * 单个的context，包括多个域及其对应的值
 */
public class Context {
	private String name;//主键
	private HashMap<String, Object> fields = new HashMap<String, Object>();//各个field以及其对应的值
	private Set<Pattern> patterns = new HashSet<Pattern>();//属于哪些pattern
    
	private int state;//undecided,consistent,bad,inconsistent
	public static int ctxNum = 0;
    
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(Context.class.getName());
    
    public Context() {
        state = -1;
        this.name = "ctx_" + ctxNum;
        ctxNum++;
        addField("name", name);
    }
    public HashMap<String,Object> getFields() {
    	return fields;
    }
    
//    public String get(String field) {
//        return (String)fields.get(field);
//    }
    
    public void addField(String fieldName, Object value) {
        fields.put(fieldName, value);
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean matches(Pattern pattern){
    	for(Map.Entry<String, Object> entry : pattern.getFields().entrySet()){
    		if(entry.getValue().equals("any"))
    			continue;
    		else if(!fields.containsKey(entry.getKey()) || !fields.get(entry.getKey()).equals(entry.getValue()))
    			return false;
    	}
    	
    	return true;
    }
    
    public void addPattern(Pattern pattern) {
        patterns.add(pattern);
    }
    
    public void deletePattern(Pattern pattern) {
        patterns.remove(pattern);
    }
    
    public Set<Pattern> getPatterns() {
        return patterns;
    }
    
    public void setState(int state) {
        this.state = state;
    }
    
    public int getState() {
        return state;
    }
    
    public boolean equals(Object arg) {
        /*if(!(fields.equals(e.fields)))
        	return false;*/
    	return arg instanceof Context && name.equals(((Context)arg).name);
    }
    
    @Override
    public String toString() {
        String str = new String();
        str += " ";
        str += fields.get("id");
        return str;
    }
}
