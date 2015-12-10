/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.context;

import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 *
 * @author bingying
 * 单个的context，包括多个域及其对应的值
 */
public class Context {
	private String name;//主键
	private HashMap<String,Object> fields = new HashMap<String,Object>();//各个field以及其对应的值
	private Pattern pattern;//属于哪个pattern
    
	private int state;//undecided,consistent,bad,inconsistent
    
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(Context.class.getName());
    
    public Context() {
        state = -1;
    }
    public HashMap<String,Object> getFields() {
    	return fields;
    }
    public String get(String field) {
        return (String)fields.get(field);
    }
    
    public void addField(String fieldName,Object value) {
        fields.put(fieldName,value);
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }
    public Pattern getPattern() {
        return pattern;
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
