/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * ��ÿһ��context changeת��Ϊһ���ౣ������
 */
public class ContextChange {
	private int type;
	private Pattern pattern;
	private Context context;
	private int ctxIdx = -1;//the index where the context should be added to or deleted from the pattern queue
    
	public final static int ADDITION = 1;
	public final static int DELETION = 2;
	
    @SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(ContextChange.class.getName());
    
    public ContextChange(int type, Pattern pattern, Context context) {
    	this.type = type;
    	this.pattern = pattern;
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
    
    public Pattern getPattern() {
        return pattern;
    }
    
//    public String getContextName() {
//        return contextName;
//    }
    
    public Context getContext() {
        return context;
    }

	public int getCtxIdx() {
		return ctxIdx;
	}

	public void setCtxIdx(int index) {
		this.ctxIdx = index;
	}
}
