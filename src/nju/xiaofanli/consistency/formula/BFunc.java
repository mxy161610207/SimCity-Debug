/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.formula;

import nju.xiaofanli.Resource;
import nju.xiaofanli.consistency.context.Context;
import nju.xiaofanli.consistency.context.ContextChange;
import nju.xiaofanli.device.car.Car;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author bingying
 * ���ֺ���
 */
public class BFunc extends Formula {
	//bfunc�Ĳ����洢
    private HashMap<Integer, Param> params = new HashMap<>();
	//bfunc�Ĳ�����ʾ
    private static class Param {
        String var = null, field = null;
        Param(String _var, String _field) {
            var = _var;
            field = _field;
        }
    }
    
    public BFunc(String name) {
        super(name);
    }
    
    public HashMap<Integer, Param> getParam() {
    	return params;
    }
    
    public void setParam(HashMap<Integer, Param> params) {
    	this.params = params;
    }
    
    public void addParam(int pos, String var, String field) {
        if (params.get(pos) == null) {
            params.put(pos, new Param(var, field));
        } else {  // pos should be unique
            System.out.println("incorrect position");
            System.exit(1);
        }
    }

    private Object getValue(int pos, HashMap<String,Context> varEnv) {
        Param p = params.get(pos);
        if (p == null) {
            System.out.println("incorrect position: " + pos);
            System.exit(1);
        }

        return varEnv.get(p.var).getFields().get(p.field);
    }
    
    private boolean funcEarlierThan(HashMap<String, Context> var) {
    	long v1 = (long) getValue(1, var);
    	long v2 = (long) getValue(2, var);
//    	if (v1 == null || v2 == null)
//    		return false;
    	
		return v1 < v2;
	}

	private boolean funcShortTime(HashMap<String, Context> var) {
		long v1 = (long) getValue(1, var);
		long v2 = (long) getValue(2, var);
//    	if (v1 == null || v2 == null)
//    		return false;

        // mxy_edit: change short time from 200 to 100
		//return Math.abs(v1 - v2) < 200;
        return Math.abs(v1 - v2) < 100;
        // mxy_edit End.
	}

	private boolean funcStillState(HashMap<String, Context> var) {
		int v1 = (int) getValue(1, var);
//		if(v1 == null)
//			return false;
		
		return v1 == Car.STOPPED;
	}

	private boolean funcSameLocation(HashMap<String, Context> var) {
		String v1 = (String) getValue(1, var);
    	String v2 = (String) getValue(2, var);
    	if (v1 == null || v2 == null)
    		return false;

		return Resource.getRoad(v1) == Resource.getRoad(v2);
	}
    
    @Override
    public void evaluateECC(Assignment node) {
        switch (type) {
            case "same":
                value = funcSame(node.getVar()); break;
            case "still_state":
                value = funcStillState(node.getVar()); break;
            case "same_location":
                value = funcSameLocation(node.getVar()); break;
            case "short_time":
                value = funcShortTime(node.getVar()); break;
            case "earlier_than":
                value = funcEarlierThan(node.getVar()); break;
            default:
                System.out.println("incorrect function: " + type);
                System.exit(1);
        }
    }

	@Override
    public void generateECC() {
        links = new HashSet<>();
    }

    @Override
    public boolean affect(ContextChange change) {
        return false;
    }

    @Override
    public void evaluatePCC(Assignment node, ContextChange change) {
    	evaluateECC(node);
    }

    @Override
    public void generatePCC(ContextChange change) {
        generateECC();
    }

    @Override
    public void setGoal(String goal) {
        goalLink = goal;
    }

    // Compares two field values and a constant string if any (should be idential)
    private boolean funcSame(HashMap<String,Context> varEnv) {
        String v1 = (String) getValue(1, varEnv);
        String v2 = (String) getValue(2, varEnv);
        if (v1 == null || v2 == null) {
            return false;
        }

        // The first two values should be identical
        if (!v1.equals(v2)) {
            return false;
        }

        // The third value (if any) should be identical to the first two values
        Param p3 = params.get(3);  // pos = 3
        if (p3 != null) {
            String v3 = p3.var;  // Should be a string constant
            if (!v1.equals(v3)) {
                return false;
            }
        }

        return true;
    }

    @Override
	public BFunc createInitialFormula() {
		BFunc f = new BFunc(type);
    	f.value = value;
    	f.params = new HashMap<>(params);
		return f;
	}

    @Override
    public String toString() {
        String s = type + "(";
        int pos = 1;
        if (params.containsKey(pos)) {
            Param p = params.get(pos);
            s += p.var + "." + p.field;
            pos++;
            while (params.containsKey(pos)) {
                p = params.get(pos);
                s += ", " + p.var + "." + p.field;
                pos++;
            }
        }
        s += ")";
        return s;
    }

    @Override
    protected String getName4indentString() {
        return toString();
    }

    @Override
    protected List<Formula> getSubformula4indentString() {
        return new ArrayList<>();
    }
}
