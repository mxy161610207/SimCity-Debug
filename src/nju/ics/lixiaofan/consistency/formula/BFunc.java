/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.formula;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import nju.ics.lixiaofan.car.Car;
import nju.ics.lixiaofan.consistency.context.Context;
import nju.ics.lixiaofan.consistency.context.ContextChange;
import nju.ics.lixiaofan.resource.ResourceProvider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * 各种函数
 */
public class BFunc extends Formula {
	//bfunc的参数存储
    private HashMap<String,Param> params = new HashMap<String,Param>();
	//bfunc的参数表示
    public static class Param {
        public String var = null, field = null;
        public Param(String _var, String _field) {
            var = _var;
            field = _field;
        }
    }
    
    @SuppressWarnings("unused")
	private  static Log logger = LogFactory.getLog(BFunc.class.getName());
    
    public BFunc(String name) {
        super(name);
    }
    
    public HashMap<String,Param> getParam() {
    	return params;
    }
    
    public void setParam(HashMap<String,Param> params) {
    	this.params = params;
    }
    
    public void addParam(String pos, String var, String field) {
        if (params.get(pos) == null) {
            params.put(pos, new Param(var, field));
        } else {  // pos should be unique
            System.out.println("incorrect position");
            System.exit(1);
        }
    }
    
    private long convert(String time) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss:SSS" );
        Date date = sdf.parse(time);
        return date.getTime();
    }
    
    private Object getValue(int pos, HashMap<String,Context> varEnv) {
        Param p = params.get(Integer.toString(pos));
        if (p == null) {
            System.out.println("incorrect position: " + pos);
            System.exit(1);
        }

        return varEnv.get(p.var).getFields().get(p.field);
    }
    
    private boolean funcBeforeTime(HashMap<String, Context> var) {
    	long v1 = (long) getValue(1, var);
    	long v2 = (long) getValue(2, var);
//    	if (v1 == null || v2 == null)
//    		return false;
    	
		return v1 < v2;
	}

	private boolean funcSameTime(HashMap<String, Context> var) {
		long v1 = (long) getValue(1, var);
		long v2 = (long) getValue(2, var);
//    	if (v1 == null || v2 == null)
//    		return false;
    	
    	//two contexts of one car occurring less than 300ms will be considered same time
		return Math.abs(v1 - v2) < 300;
	}

	private boolean funcStillStatus(HashMap<String, Context> var) {
		int v1 = (int) getValue(1, var);
//		if(v1 == null)
//			return false;
		
		return v1 == Car.STILL;
	}

	private boolean funcSameLocation(HashMap<String, Context> var) {
		String v1 = (String) getValue(1, var);
    	String v2 = (String) getValue(2, var);
    	if (v1 == null || v2 == null)
    		return false;
    	
		return ResourceProvider.getSection(v1).sameAs(ResourceProvider.getSection(v2));
	}
    
    @Override
    public boolean evaluateECC(Assignment node) {
    	if (type.equals("still_status")) 
            return funcStillStatus(node.getVar());
    	else if (type.equals("same_location")) 
            return funcSameLocation(node.getVar());
    	else if (type.equals("same_time")) 
            return funcSameTime(node.getVar());
    	else if (type.equals("before_time")) 
            return funcBeforeTime(node.getVar());
    	else if (type.equals("test")) 
            return funcTest(node.getVar());
        else if (type.equals("true"))
            return funcTrue(node.getVar());
    	else if (type.equals("equal"))
    		return funcEqual(node.getVar());
    	else if (type.equals("not_equal"))
            return funcNotEqual(node.getVar());
    	else if (type.equals("same")) 
            return funcSame(node.getVar());
    	else if (type.equals("not_same"))
            return funcNotSame(node.getVar());
    	else if (type.equals("smaller"))
            return funcSmaller(node.getVar());
    	else if (type.equals("overlap"))
            return funcOverlap(node.getVar());
    	else if (type.equals("before")) {
            try {
                return funcBefore(node.getVar());
            } catch (ParseException ex) {
                Logger.getLogger(BFunc.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
    	else if (type.equals("sz_loc_range"))
            return funcSZLocRange(node.getVar());
    	else if (type.equals("sz_loc_dist")) 
            return funcSZLocDist(node.getVar());
    	else if (type.equals("sz_loc_close"))
            return funcSZLocClose(node.getVar());
    	else if (type.equals("sz_spd_close"))
            return funcSZSpdClose(node.getVar());
    	else if (type.equals("sf_late"))
            return funcSFLate(node.getVar());
    	else if (type.equals("sf_worktime"))
            return funcSFWorktime(node.getVar());
    	else if (type.equals("sf_close"))
            return funcSFClose(node.getVar());
    	else if (type.equals("sf_far"))
            return funcSFFar(node.getVar());
    	else {
            System.out.println("incorrect function: " + type);
            System.exit(1);
        }

        return false;
    }

	@Override
    public Set<Link> generateECC() {
        return new HashSet<Link>();
    }

    @Override
    public boolean affect(ContextChange change) {
        return false;
    }

    @Override
    public boolean evaluatePCC(Assignment node,ContextChange change) {
    	value = evaluateECC(node);
        return value;
    }

    @Override
    public Set<Link> generatePCC(ContextChange change) {
    	return new HashSet<Link>();
    }

    @Override
    public void setGoal(String goal) {
        goalLink = goal;
    }
    
    private boolean funcTest(HashMap<String, Context> varEnv) {
        String v1 = (String) getValue(1, varEnv);
    	if (v1 == null) return false;
    	
    	int lon = Integer.parseInt(v1);  // Longitude
    	
    	// The longitude and latitude should be in [112, 116] and [20, 24], respectively
    	boolean result = true;
    	if (lon < 112 || lon > 116) {
    		result = false;
    	}
    	
    	return result;
    }
    
    private boolean funcTrue(HashMap<String, Context> varEnv) {
        return true;
    }
    
    // Compares a field value to a constant string (should be idential)
    private boolean funcEqual(HashMap<String, Context> varEnv) {
        String v1 = (String) getValue(1, varEnv);
        if (v1 == null) {
            return false;
        }

        // The second value should be identical to the first value
        Param p2 = params.get(Integer.toString(2));  // pos = 2
        boolean result = true;
        if (p2 == null) {
            return false;
        } else {
            String v2 = p2.var;  // Should be a string constant
            if (!v1.equals(v2)) {
                result = false;
            }
        }

        return result;
    }

    // Compares a field value to a constant string (should not be idential)
    private boolean funcNotEqual(HashMap<String, Context> varEnv) {
        String v1 = (String) getValue(1, varEnv);
        if (v1 == null) {
            return false;
        }

        // The second value should not be identical to the first value
        Param p2 = params.get(Integer.toString(2));  // pos = 2
        boolean result = true;
        if (p2 == null) {
            return false;
        } else {
            String v2 = p2.var;  // Should be a string constant
            if (v1.equals(v2)) {
                result = false;
            }
        }

        return result;
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
        Param p3 = params.get(Integer.toString(3));  // pos = 3
        if (p3 != null) {
            String v3 = p3.var;  // Should be a string constant
            if (!v1.equals(v3)) {
                return false;
            }
        }

        return true;
    }

    // Compares two field values (should not be idential)
    private boolean funcNotSame(HashMap<String, Context> varEnv) {
        String v1 = (String) getValue(1, varEnv);
        String v2 = (String) getValue(2, varEnv);
        if (v1 == null || v2 == null) {
            return false;
        }

        // The first two values should not be identical
        boolean result = true;
        if (v1.equals(v2)) {
            result = false;
        }

        return result;
    }

    private boolean funcSmaller(HashMap<String, Context> varEnv) {
        String v1 = (String) getValue(1, varEnv);
        String v2 = (String) getValue(2, varEnv);
        if (v1 == null || v2 == null) {
            return false;
        }

        // The number from v1 should be smaller than that from v2
        int n1 = (new Integer(v1)).intValue();
        int n2 = (new Integer(v2)).intValue();
        if (n1 >= n2) {
            return false;
        }

        return true;
    }

    private boolean funcOverlap(HashMap<String, Context> varEnv) {
        String v1 = (String) getValue(1, varEnv);
        String v2 = (String) getValue(2, varEnv);
        String v3 = (String) getValue(3, varEnv);
        String v4 = (String) getValue(4, varEnv);
        if (v1 == null || v2 == null || v3 == null || v4 == null) {
            return false;
        }

        // Two values should be overlapping
        if (v1.compareTo(v4) > 0 || v2.compareTo(v3) < 0) {
            return false;
        }

        return true;
    }

    private boolean funcBefore(HashMap<String, Context> varEnv) throws ParseException {
        String v1 = (String) getValue(1, varEnv);
        String v2 = (String) getValue(2, varEnv);
        Param p3 = params.get(Integer.toString(3));  // pos = 3
        if (p3 == null) {
            System.out.println("incorrect position");
            System.exit(1);
        }
        String v3 = p3.var;
        if (v1 == null || v2 == null || v3 == null) {
            return false;
        }

        long t1 = convert(v1);
        long t2 = convert(v2);
        long t = Long.parseLong(v3);

        // t1 <= t2 && t1 + t >= t2 should hold
        if (t1 > t2 || t1 + t < t2) {
            return false;
        }

        return true;
    }

    // For SUTPC
    private boolean funcSZLocRange(HashMap<String, Context> varEnv) {
    	String v1 = (String) getValue(1, varEnv);
        //System.out.println(v1);
    	if (v1 == null) return false;
        
    	StringTokenizer st = new StringTokenizer(v1, "_");  // Format: longitude_latitude_speed
    	double lon = Double.parseDouble(st.nextToken());  // Longitude
    	double lat = Double.parseDouble(st.nextToken());  // Latitude
    	
    	// The longitude and latitude should be in [112, 116] and [20, 24], respectively
    	boolean result = true;
    	if (lon < 112.0 || lon > 116.0 || lat < 20.0 || lat > 24.0) {
    		result = false;
    	}
    	
    	return result;
    }
    
    // For SUTPC
    private boolean funcSZLocDist(HashMap<String, Context> varEnv) {
    	String v1 = (String) getValue(1, varEnv);
    	String v2 = (String) getValue(2, varEnv);
    	if (v1 == null || v2 == null) {
    		return false;
    	}

    	StringTokenizer st = new StringTokenizer(v1, "_");  // Format: longitude_latitude_speed
    	double lon1 = Double.parseDouble(st.nextToken());  // Longitude 1
    	double lat1 = Double.parseDouble(st.nextToken());  // Latitude 1
    	st = new StringTokenizer(v2, "_");  // Format: longitude_latitude_speed
    	double lon2 = Double.parseDouble(st.nextToken());  // Longitude 2
    	double lat2 = Double.parseDouble(st.nextToken());  // Latitude 2
    	double dist = Math.sqrt((lon2 - lon1) * (lon2 - lon1) + (lat2 - lat1) * (lat2 - lat1));
    	
    	// The distance should be no more than 0.026 (assuming the speed is no more than 200 km/h)
    	boolean result = true;
    	if (dist > 0.025) {
    		result = false;
    	}
    	
    	return result;
    }
    
    // For SUTPC
    private boolean funcSZLocClose(HashMap<String, Context> varEnv) {
    	String v1 = (String) getValue(1, varEnv);
    	String v2 = (String) getValue(2, varEnv);
    	if (v1 == null || v2 == null) {
    		return false;
    	}

    	StringTokenizer st = new StringTokenizer(v1, "_");  // Format: longitude_latitude_speed
    	double lon1 = Double.parseDouble(st.nextToken());  // Longitude 1
    	double lat1 = Double.parseDouble(st.nextToken());  // Latitude 1
    	st = new StringTokenizer(v2, "_");  // Format: longitude_latitude_speed
    	double lon2 = Double.parseDouble(st.nextToken());  // Longitude 2
    	double lat2 = Double.parseDouble(st.nextToken());  // Latitude 2
    	double dist = Math.sqrt((lon2 - lon1) * (lon2 - lon1) + (lat2 - lat1) * (lat2 - lat1));
    	
    	// The distance should be no more than 0.001 as 'close'
    	boolean result = true;
    	if (dist > 0.001) {
    		result = false;
    	}

    	return result;
    }

    // For SUTPC
    private boolean funcSZSpdClose(HashMap<String, Context> varEnv) {
    	String v1 = (String) getValue(1, varEnv);
    	String v2 = (String) getValue(2, varEnv);
    	if (v1 == null || v2 == null) {
    		return false;
    	}

    	StringTokenizer st = new StringTokenizer(v1, "_");  // Format: longitude_latitude_speed
    	st.nextToken();  // Skip longitude
    	st.nextToken();  // Skip latitude
    	int spd1 = Integer.parseInt(st.nextToken());  // Speed 1
    	st = new StringTokenizer(v2, "_");  // Format: longitude_latitude_speed
    	st.nextToken();  // Skip longitude
    	st.nextToken();  // Skip latitude
    	int spd2 = Integer.parseInt(st.nextToken());  // Speed 2
    	
    	// The speed difference should be no more than 50 (as 'close')
    	boolean result = true;
    	if (Math.abs(spd2 - spd1) > 50) {
    		result = false;
    	}

    	return result;
    }

    // For Siafu
    private boolean funcSFLate(HashMap<String, Context> varEnv) {
    	String v1 = (String) getValue(1, varEnv);
    	if (v1 == null) {
    		return false;
    	}
    	
    	int hour = Integer.parseInt(v1.substring(11, 13));  // Hour in simulation time
    	boolean result = false;
    	if (hour >= 19) {  // Later than 7pm
    		result = true;
    	}
    	
    	return result;
    }

    // For Siafu
    private boolean funcSFWorktime(HashMap<String, Context> varEnv) {
    	String v1 = (String) getValue(1, varEnv);
    	if (v1 == null) {
    		return false;
    	}
    	
    	int hour = Integer.parseInt(v1.substring(11, 13));  // Hour in simulation time
    	boolean result = false;
    	if (hour >= 8 && hour <= 17) {  // Between 8am and 5pm
    		result = true;
    	}
    	
    	return result;
    }

    // For Siafu
    private boolean funcSFClose(HashMap<String, Context> varEnv) {
    	String v1 = (String) getValue(1, varEnv);
    	String v2 = (String) getValue(2, varEnv);
    	if (v1 == null || v2 == null) {
    		return false;
    	}

    	StringTokenizer st = new StringTokenizer(v1, "#");  // Format: latitude#longitude
    	double lat1 = Double.parseDouble(st.nextToken());  // Latitude 1
    	double lon1 = Double.parseDouble(st.nextToken());  // Longitude 1
    	st = new StringTokenizer(v2, "#");  // Format: latitude#longitude
    	double lat2 = Double.parseDouble(st.nextToken());  // Latitude 2
    	double lon2 = Double.parseDouble(st.nextToken());  // Longitude 2
    	double dist = Math.sqrt((lon2 - lon1) * (lon2 - lon1) + (lat2 - lat1) * (lat2 - lat1));

    	// The distance should be no more than 0.000012 (100 m) as 'close'
    	boolean result = true;
    	if (dist > 0.000012) {
    		result = false;
    	}

    	return result;
    }

    // For Siafu
    private boolean funcSFFar(HashMap<String, Context> varEnv) {
    	String v1 = (String) getValue(1, varEnv);
    	String v2 = (String) getValue(2, varEnv);
    	if (v1 == null || v2 == null) {
    		return false;
    	}

    	StringTokenizer st = new StringTokenizer(v1, "#");  // Format: latitude#longitude
    	double lat1 = Double.parseDouble(st.nextToken());  // Latitude 1
    	double lon1 = Double.parseDouble(st.nextToken());  // Longitude 1
    	st = new StringTokenizer(v2, "#");  // Format: latitude#longitude
    	double lat2 = Double.parseDouble(st.nextToken());  // Latitude 2
    	double lon2 = Double.parseDouble(st.nextToken());  // Longitude 2
    	double dist = Math.sqrt((lon2 - lon1) * (lon2 - lon1) + (lat2 - lat1) * (lat2 - lat1));

    	// The distance should be no less than 0.000018 (150 m) as 'far'
    	boolean result = true;
    	if (dist < 0.000018) {
    		result = false;
    	}

    	return result;
    }
}
