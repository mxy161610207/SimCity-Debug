/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.xiaofanli.consistency.dataLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * ��ȡ�����ļ�
 * 
 */
public class Configuration {
    private static Log logger;
    private static final Properties systemProperties;

    static {
        logger = LogFactory.getLog(Configuration.class.getName());     
        systemProperties = new Properties();
    }
    public void init(String filename) {
        InputStream propertyInputStream = Configuration.class.getResourceAsStream(filename);
        if (propertyInputStream == null) {
            logger.error("System.property inputstream is null");
        }    
        
        try {
            systemProperties.load(propertyInputStream);
        } catch (IOException ex) {
            logger.error(ex);
        }
//        logger.info("SystemProperty initialization OK!");
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
