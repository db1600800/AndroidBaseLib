/**
 * 
 */
package com.chinapost.baselib.log;

import org.apache.log4j.Level;

import android.os.Environment;
import android.util.Log;
import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * @author Jam
 * 2013-5-31 下午2:34:13
 */
public class ConfigureLog4J {
	
	private static ConfigureLog4J configreLog4J;
	private final static LogConfigurator logConfigurator;
	
	static {
        logConfigurator = new LogConfigurator();

        String logFilePath = "/data/data/com.chinapost/GDPOST/LOG/gdpost.log";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	logFilePath = Environment.getExternalStorageDirectory() + "/GDPOST/LOG/gdpost.log";
        }
        logConfigurator.setFileName(logFilePath);
        logConfigurator.setRootLevel(Level.DEBUG);
        // Set log level of a specific logger
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.setUseFileAppender(true);
        logConfigurator.setMaxBackupSize(3);
        logConfigurator.setMaxFileSize(1000 * 100);
        logConfigurator.configure();
    }

	public static ConfigureLog4J getInstance() {
		if (configreLog4J == null) {
			configreLog4J = new ConfigureLog4J();
		}
		return configreLog4J;
	}

	public static String getLogPath() {
		String path = logConfigurator.getFileName();
		path = path.substring(0, path.lastIndexOf("/"));
		return path;
	}
}
