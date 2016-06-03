/**
 * 
 */
package com.chinapost.publiclibrary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * @author jam
 * 2013-4-17 上午10:37:34
 */
public class AndroidUIReceiver extends BroadcastReceiver {

	public static String ACTION_UPDATE_APP_SUFFIX = ".updateapp";
	
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("UIRecevier", "接收到广播，Action：" + intent.getAction());
		Bundle bundle = intent.getExtras();
		String appId = bundle.getString("appId");
		String action = appId + ACTION_UPDATE_APP_SUFFIX;
		if (action.equals(intent.getAction())) {
			Log.i("UIReceiver", "广播是发向本软件的，准备弹出软件更新界面...");
			Intent updateintent = new Intent(context, UpdateAppActivity.class);
			updateintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			updateintent.putExtras(intent.getExtras());//传参
			context.startActivity(updateintent);
		}
	}

}
