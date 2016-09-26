/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author bingying
 * ������context��������������Ӧ��ֵ
 */
public class Context {
	private String name;//����
	private HashMap<String, Object> fields = new HashMap<>();//����field�Լ����Ӧ��ֵ
	private Set<Pattern> patterns = new HashSet<>();//������Щpattern
    
	private int state;//undecided,consistent,bad,inconsistent
	public static int ctxNum = 0;
    
	public static final int Normal = 0;
	public static final int FP = 1;
	public static final int FN = 2;
	
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
        String str = "";
        str += " ";
        str += fields.get("id");
        return str;
    }
    
    public void print(){
        StringBuilder sb = new StringBuilder("    " + name);
    	for(Map.Entry<String, Object> e : fields.entrySet())
    		sb.append("\t[" + e.getKey() + "] " + e.getValue());
        System.out.println(sb);
    }
}
