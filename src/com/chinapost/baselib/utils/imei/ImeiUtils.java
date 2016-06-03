/**
 * 
 */
package com.chinapost.baselib.utils.imei;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * @author Jam
 * 2013-5-9 下午1:58:47
 */
public class ImeiUtils {

	/**
	 * 获取IMEI号码
	 * @param context
	 * @return
	 */
	public static String getImei(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getDeviceId();
	}
}
