package nju.ics.lixiaofan.context;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Context implements Cloneable{
	public String time;//时间
	public String sensor;//传感器
	public String car;//小车
	public String direction;//方向
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
	
    public Context(String sensor,String car,String direction) {
        this.time = sdf.format(new Date());
        this.sensor = sensor;
        this.car = car;
        this.direction = direction;
    }
    
    public Context clone() throws CloneNotSupportedException {
    	return (Context) super.clone();
    }
}

