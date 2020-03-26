package com.shinetech.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 读取properties文件的工具类
 */
public class PropertyUtils {

    private static Properties properties = null;


    public static synchronized Properties getInstance() {
        InputStream inputStream = null;

        if (properties == null) {
            // new single
            try {
                ClassLoader cl = PropertyUtils.class.getClassLoader();
                if  (cl !=  null) {
                    inputStream = cl.getResourceAsStream("shinetech-rte.properties");
                }  else {
                    inputStream = ClassLoader.getSystemResourceAsStream("shinetech-rte.properties");
                }
                properties =  new  Properties();
                properties.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                try {
                    if(inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }


        return properties;
    }


}
