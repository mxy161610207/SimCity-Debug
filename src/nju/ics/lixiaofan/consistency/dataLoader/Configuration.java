/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nju.ics.lixiaofan.consistency.dataLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author bingying
 * ∂¡»°≈‰÷√Œƒº˛
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
        String str = systemProperties.getProperty(key).trim();
        return str;
    }
    
    public static int getConfigInt(String key) {
        int i = Integer.parseInt(systemProperties.getProperty(key).trim());
        return i;
    }
    
    public static double getConfigNumeric(String key) {
        double d = Double.parseDouble(systemProperties.getProperty(key).trim());
        return d;
    }
    
    public static boolean getConfigBool(String key) {
        boolean d = Boolean.parseBoolean(systemProperties.getProperty(key).trim());
        return d;
    }
}
