/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * 将每一个context change转换为一个类保存下来
 */
public class ContextChange {
	private int type;
	private Pattern pattern;
	private Context context;
	private ContextChange original = null;//only to store the original change for update type
    
	public final static int ADDITION = 1;
	public final static int DELETION = 2;
	public final static int UPDATE = 3;
	
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(ContextChange.class.getName());
    
    public ContextChange(int type, Context context) {
    	this.type = type;
    	this.context = context;
	}
    
	public ContextChange() {
	}
	
	public void setType(int type) {
        this.type = type;
    }
	
    public int getType() {
        return type;
    } 
    
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    public void setOriginal(ContextChange original){
    	this.original = original;
    }
    
    public ContextChange getOriginal(){
    	return original;
    }
    
    public Pattern getPattern() {
        return pattern;
    }
    
//    public String getContextName() {
//        return contextName;
//    }
    
    public Context getContext() {
        return context;
    }
}
