/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.formula;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nju.ics.lixiaofan.consistency.context.Context;

/**
 *
 * @author bingying
 * link的各种的操作
 */
public class Link {
    private boolean violated;
    private HashMap<String, Context> binding = new HashMap<String, Context>();
    
    public Link() {
	}
    
    public Link(boolean violated, String variable, Context ctx) {
    	this.violated = violated;
    	binding.put(variable, ctx);
	}
    
    public Link(boolean violated, HashMap<String,Context> binding) {
    	this.violated = violated;
    	this.binding.putAll(binding);
	}

	public void setViolated(boolean violated) {
    	this.violated = violated;
    }
    
    public boolean getViolated() {
    	return violated;
    }
    
    public void setBinding(HashMap<String,Context> binding) {
    	this.binding = binding;
    }
    
    public HashMap<String,Context> getBinding() {
    	return binding;
    }
    
    public static Set<Link> flip(Set<Link> links) {
    	Set<Link> newLinks = new HashSet<Link>();
    	for(Link link : links)
    		newLinks.add(new Link(!link.violated, link.binding));
        return newLinks;
    }
    
    public static Link linkCartesian(Link l1, Link l2) {
    	if(l1 == null || l2 == null || l1.violated != l2.violated)
    		return null;
    	
    	Link newLink = new Link();
        newLink.violated = l1.violated;
        newLink.binding.putAll(l1.binding);
        newLink.binding.putAll(l2.binding);
        return newLink;
    }
    
    public static Set<Link> linkCartesian(Set<Link> l1, Set<Link> l2) {
        if(l1.isEmpty())
            return new HashSet<Link>(l2);
        if(l2.isEmpty())
            return new HashSet<Link>(l1);
        Set<Link> result = new HashSet<Link>();
        for(Link i : l1)
            for(Link j : l2)
            	if(i.violated == j.violated)
            		result.add(linkCartesian(i, j));
        return result;
    }
    
    public static Set<Link> linkCartesian(Link l1, Set<Link> l2) {
        Set<Link> result = new HashSet<Link>();
        if(l2.isEmpty()) {
            result.add(l1);
            return result;
        }
        for(Link l : l2) 
        	if(l1.violated == l.violated)
        		result.add(linkCartesian(l1, l));
        return result;
    }
    
    public static Set<Link> union(Set<Link> l1, Set<Link> l2) {
        if(l1 == null || l1.isEmpty())
            return new HashSet<Link>(l2);
        else if(l2 == null || l2.isEmpty())
            return new HashSet<Link>(l1);
            
        Set<Link> temp = new HashSet<Link>(l1);
        temp.addAll(l2);
        return temp;
    }
    
    @Override
    public String toString() {
        String str = new String();
        /*str += "(";
        if(violated == true)
            str += "violated";
        else
            str += "satisfy";
        str += ",{";*/
        for (Context context : binding.values()) {
            /*str += "(";
            str += entry.getKey();
            str += ",";*/
            str += context.toString();
            /*str += ")";
            str += ",";*/
        }
        /*str = str.substring(0,str.length()-1);
        str += "})";*/
        return str;
    }
    
    public boolean equals(Link link) {
        if(violated != link.violated || binding.size() != link.binding.size())
            return false;
        else {
            for (Map.Entry<String, Context> entry : binding.entrySet()) {
                String name = (String)entry.getKey();
                Context context = (Context)entry.getValue();
                if(!link.binding.containsKey(name) || !link.binding.get(name).equals(context))
                    return false;
            }
        }
        return true;
    }
}
