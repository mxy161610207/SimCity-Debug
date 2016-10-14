package nju.xiaofanli.context;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Context {
    public String time;//ʱ��
    public String sensor;//������
    public String car;//С��
    public String direction;//����
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");

    public Context(String sensor,String car,String direction) {
        this.time = sdf.format(new Date());
        this.sensor = sensor;
        this.car = car;
        this.direction = direction;
    }
}

