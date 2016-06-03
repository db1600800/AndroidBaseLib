/**
 * 
 */
package com.chinapost.baselib.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Jam
 * 2013-5-9 下午2:39:05
 */
public class PropertiesUtils {

	public static Properties getProperties () {
		Properties properties = new Properties();
		InputStream is = PropertiesUtils.class.getResourceAsStream("/assets/webconfig.properties");
		try {
			properties.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return properties;
	}
}
