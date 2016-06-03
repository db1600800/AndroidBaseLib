/**
 * 
 */
package com.chinapost.baselib.utils.push;

import java.util.UUID;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * @author Jam
 * 2013-7-3 上午11:03:18
 */
public class PushUtils {

	/**
	 * 获取推送的tokenId
	 * @param context
	 * @return
	 */
	public static String getTokenId(Context context) {
		final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    	final String tmDevice, tmSerial, tmPhone, androidId;
    	tmDevice = "" + tm.getDeviceId();
    	tmSerial = "" + tm.getSimSerialNumber();
    	androidId = "" + android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    	UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
    	String uniqueId = deviceUuid.toString();
    	uniqueId = uniqueId.replaceAll("-", "");
    	return uniqueId;
	}
}
