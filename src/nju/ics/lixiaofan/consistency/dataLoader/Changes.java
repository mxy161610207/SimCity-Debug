/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.dataLoader;

import nju.ics.lixiaofan.consistency.context.ContextChange;

/**
 *
 * @author Administrator
 */
public abstract class Changes {
    protected Configuration config;
    
    protected String sequence;
    
    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
    public void setConfig(Configuration config) {
        this.config = config;
    }
    
    public abstract boolean hasNextChange();
    public abstract ContextChange nextChange();
    //public abstract ContextPool initContextPool();
    
    //public abstract Instance convertChg2Inst();
    //public abstract Instance convertChgs2Inst();
    
    public abstract void close();
    
    public abstract int getChanges();
}
