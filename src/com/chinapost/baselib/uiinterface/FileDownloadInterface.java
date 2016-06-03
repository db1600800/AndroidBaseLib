/**
 * 
 */
package com.chinapost.baselib.uiinterface;

/**
 * @author Jam
 * 2013-6-28 下午3:28:18
 */
public interface FileDownloadInterface {

	public void start();
	
	public void update(int downloadedSize);
	
	public void stop();
}
