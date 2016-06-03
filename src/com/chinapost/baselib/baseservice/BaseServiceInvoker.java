/**
 * 服务端请求交互
 */
package com.chinapost.baselib.baseservice;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;

import com.chinapost.baselib.webservice.WebServiceUtils;
import com.chinapost.baselib.webservice.exception.ServerAddressException;

/**
 * @author Jam
 * 2013-5-9 上午11:14:57
 */
public class BaseServiceInvoker {
	
	public final static String ACTION_STARTUP = "startup";
	public final static String ACTION_LOGIN = "login";
	public final static String ACTION_LOGOUT = "logout";
	public final static String ACTION_LOG = "log";
	public final static String ACTION_LOCATE = "locate";
	
	public static void initConnection(String serverAddress) {
		WebServiceUtils.init(serverAddress);
	}

	public static String startup(String appId, String deviceId, String appVersion, String appConfigVersion) throws ServerAddressException, IOException {
		JSONObject jsonParam = new JSONObject();
		try {
			jsonParam.put("action", ACTION_STARTUP);
			jsonParam.put("appId", appId);
			jsonParam.put("appVersion", appVersion);
			jsonParam.put("appConfigVersion", appConfigVersion);
			jsonParam.put("deviceId", deviceId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String returnStr = invoke(jsonParam);
		return returnStr;
	}
	
	public static String login(String appId, String username, String password, String deviceId, String tokenId) throws ServerAddressException, IOException {
		JSONObject jsonParam = new JSONObject();
		try {
			jsonParam.put("action", ACTION_LOGIN);
			jsonParam.put("appId", appId);
			jsonParam.put("username", username);
			jsonParam.put("password", password);
			jsonParam.put("deviceId", deviceId);
			jsonParam.put("tokenId", tokenId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String returnStr = invoke(jsonParam);
		return returnStr;
	}
	
	public static String logout(String deviceId) throws ServerAddressException, IOException {
		JSONObject jsonParam = new JSONObject();
		try {
			jsonParam.put("action", ACTION_LOGOUT);
			jsonParam.put("deviceId", deviceId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String returnStr = invoke(jsonParam);
		return returnStr;
	}
	
	public static String log(String appId, String log, String level, String type, String deviceId) throws ServerAddressException, IOException {
		JSONObject jsonParam = new JSONObject();
		try {
			jsonParam.put("action", ACTION_LOG);
			jsonParam.put("log", log);
			jsonParam.put("level", level);
			jsonParam.put("type", type);
			jsonParam.put("appId", appId);
			jsonParam.put("deviceId", deviceId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String returnStr = invoke(jsonParam);
		return returnStr;
	}
	
	public static void locate(double latitude, double longitude, String deviceId) throws ServerAddressException, IOException {
		JSONObject jsonParam = new JSONObject();
		try {
			jsonParam.put("action", ACTION_LOCATE);
			jsonParam.put("latitude", Double.toString(latitude));
			jsonParam.put("longitude", Double.toString(longitude));
			jsonParam.put("deviceId", deviceId);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		invoke(jsonParam);
	}
	
	private static String invoke(JSONObject jsonParam) throws ServerAddressException, IOException {
		String returnStr = null;
		try {
			returnStr = WebServiceUtils.getReturnInfo(jsonParam.toString());
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		return returnStr;
	}
}
