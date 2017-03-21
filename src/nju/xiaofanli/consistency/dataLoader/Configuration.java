/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.dataLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author bingying
 * ��ȡ�����ļ�
 * 
 */
public class Configuration {
    private static final Properties systemProperties;

    static {
        systemProperties = new Properties();
    }
    public static void init(String filename) {
        InputStream propertyInputStream = Configuration.class.getResourceAsStream(filename);
        if (propertyInputStream == null) {
            System.err.println("System.property inputstream is null");
        }    
        
        try {
            systemProperties.load(propertyInputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static String getConfigStr(String key) {
        return systemProperties.getProperty(key).trim();
    }
    
    public static int getConfigInt(String key) {
        return Integer.parseInt(systemProperties.getProperty(key).trim());
    }
    
    public static double getConfigNumeric(String key) {
        return Double.parseDouble(systemProperties.getProperty(key).trim());
    }
    
    public static boolean getConfigBool(String key) {
        return Boolean.parseBoolean(systemProperties.getProperty(key).trim());
    }
}
