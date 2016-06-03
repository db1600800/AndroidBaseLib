/**
 * 
 */
package com.chinapost.publiclibrary;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.kobjects.base64.Base64;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.chinapost.baselib.baseservice.BaseServiceInvoker;
import com.chinapost.baselib.log.ConfigureLog4J;
import com.chinapost.baselib.log.LogLevel;
import com.chinapost.baselib.utils.Constants;
import com.chinapost.baselib.utils.EncryptionAndDecryption;
import com.chinapost.baselib.utils.FileDownloadUtils;
import com.chinapost.baselib.utils.imei.ImeiUtils;
import com.chinapost.baselib.utils.push.PushUtils;
import com.chinapost.baselib.webservice.WebServiceUtils;
import com.chinapost.baselib.webservice.exception.ServerAddressException;

/**
 * 底座通信接口
 * 
 * @author Jam 2013-5-31 下午5:43:10
 */
public class ServiceInvoker {

    private static String baseUrl = "";// 服务器地址
    private static Context mContext;// 当前调用上下文
    private Handler handler;// handler线程安全工具类
    private static String mAppID = "";// 软件唯一标识
    private static String mAppVersion = "";// 软件版本号
    private static int mMsgSeqNo = 1;// 报文流水号
    private static String mKeyVersion = "";// 无线网关密钥版本号
    private static String mServiceKey = "";// 无线网关密钥
    private static String mLocalPublicKey = "";// 本地公钥
    private static String mLocalPrivateKey = "";// 本地私钥
    private static String mLogicID = "";// 逻辑设备号
    private static String mTokenType = "";// 令牌识别码
    private static String mToken = "";// 令牌
    public final static String KEY_CALL = "KEY_CALL";// Map 统一请求与返回key
    public static final String FILE_NAME = "share_data";

    public ServiceInvoker(Context context, Handler handler) {
	mContext = context;
	this.handler = handler;

    }

    /**
     * 退出时需清空所有的静态变量
     */
    public static void clearService() {
	mAppID = "";// 软件唯一标识
	mAppVersion = "";// 软件版本号
	mMsgSeqNo = 1;// 报文流水号
	mKeyVersion = "";// 无线网关密钥版本号
	mServiceKey = "";// 无线网关密钥
	mLocalPublicKey = "";// 本地公钥
	mLocalPrivateKey = "";// 本地私钥
	mLogicID = "";// 逻辑设备号
	mTokenType = "";// 令牌识别码
	mToken = "";// 令牌
	RSAUtil.clearKeys();// 置空公钥与私钥
    }

    public static boolean isLocalPublicKeyEmpty() {
	return TextUtils.isEmpty(mLocalPublicKey);
    }

    /**
     * 重新置空所有的变量值
     */
    public void initService() {
	clearService();
	WebServiceUtils.initConfig();// 初始化配置文件
	baseUrl = WebServiceUtils.BeseURL;
	SharedPreferences sp = mContext.getSharedPreferences(FILE_NAME,
		Context.MODE_APPEND);
	mLogicID = sp.getString("logicID", "");
	if (TextUtils.isEmpty(mLocalPublicKey)) {
	    // 初始化本地公|私约
	    mLocalPublicKey = RSAUtil.getPublicKey();
	    mLocalPrivateKey = RSAUtil.getPrivateKey();
	}
    }

    /**
     * App端调用该接口与后台交互通信
     * 
     * @param requestStr
     *            具体业务报文内容
     * @param formName
     *            业务代号 拼接报文
     * @return ErrorMsg对象
     */
    int requestCount = 0;// 交互报文重发次数
    int MAX_REQUESTCOUNT = 1;// 交互报文最大重发次数
    String requestStrRe;
    String formNameRe;

    public MsgReturn callWebService(String requestStr, String formName) {

	requestCount++;
	// 初次把主体报文以及业务代号保存到临时变量中
	if (requestCount == 1) {
	    requestStrRe = requestStr;
	    formNameRe = formName;
	}
	MsgReturn msgReturn = new MsgReturn();
	msgReturn.errorMsg = new ErrorMsg();
	msgReturn.errorMsg.errorType = "TYPE_JH";
	msgReturn.map = new HashMap<String, Object>();
	// -=-=-=-=网络不可用-=-=-=-=-
	if (!NetUtil.isConnect(mContext)) {
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_NOT_NET;
	    return msgReturn;
	}

	try {
	    // 签到报文组装
	    JSONObject rootParam = new JSONObject();
	    JSONObject jsonBusinessParam = new JSONObject();
	    rootParam.put("action", "business");
	    jsonBusinessParam.put("appId", mAppID);// Appid
	    jsonBusinessParam.put("appVer", mAppVersion);// App版本
	    jsonBusinessParam.put("keyVer", mKeyVersion);// 服务器公钥版本
	    JSONObject message = new JSONObject();
	    message.put("logicId", mLogicID);// 逻辑设备号
	    message.put("functionId", formName);// 业务类型代号
	    message.put("msgId", ++mMsgSeqNo);// 报文流水号
	    message.put("tokenType", mTokenType);// token类型
	    message.put("token", mToken);// token
	    message.put("businessParam", requestStr);// 主体报文
	    // message内容编码转换需转换三次,请注意转换的编码格式为ISO-8859-1
	    // 字符串转为字节数组需要转换一次
	    byte[] data = message.toString().getBytes();
	    // byte[] data = message.toString().getBytes(RSAUtil.CHAR_SET);
	    // 使用服务器公钥加密
	    byte[] encodedData = null;
	    try {
		encodedData = RSAUtil.encryptByPublicKey(data, mServiceKey);
	    } catch (Exception e) {
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_OTHER;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_RSA;
		e.printStackTrace();
	    }
	    // 将字节数组转为字符串需要转换一次
	    String encode = new String(encodedData, RSAUtil.CHAR_SET);
	    // 将字节数组转为字符串,使用Base64编码需要转换一次
	    encode = Base64.encode(encode.getBytes(RSAUtil.CHAR_SET));
	    jsonBusinessParam.put("message", encode);
	    rootParam.put("param", jsonBusinessParam);
	    String request = rootParam.toString();
	    String rtn = WebServiceUtils.getReturnInfo(request);
	    // 不为空打印log
	    // if(!TextUtils.isEmpty(WebServiceUtils.ENCRYPT_KEY)) {
	    // Log.d("Tang", "request:----->"+message.toString());
	    // Log.d("Tang", "last request:----->"+request);
	    // }
	    if (TextUtils.isEmpty(rtn)) {
		// -=-=-=-=网络不可用-=-=-=-=-
		if (NetUtil.isConnect(mContext)) {
		    // 服务器内部错误
		    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE_IN_ERROR;
		    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_FAILED;
		} else {
		    // 网络不稳定或者网络不通错误
		    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
		    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_NOT_NET;
		}
	    } else {
		JSONObject resultObject = new JSONObject(rtn);
		String err_code = resultObject.optString("err_code");
		String err_msg = resultObject.optString("err_msg");
		msgReturn.errorMsg.errorCode = err_code;
		msgReturn.errorMsg.errorDesc = err_msg;
		// 返回码为：0000
		boolean decryptSucess = true;// 解密成功 hao修改
		if (err_code.equals(ErrorMsg.ERROR_SUCCESS)) {
		    // hao修改
		    try {
			requestCount = 0;// 重发次数归零
			// 5、json对象获取param内容值
			String param = resultObject.optString("param");

			byte[] decodeParam = Base64.decode(param);
			// 6、对param内容转码
			String string = new String(decodeParam,
				RSAUtil.CHAR_SET);
			byte[] bytes = string.getBytes(RSAUtil.CHAR_SET);
			// 7、使用RSA私钥解密
			byte[] decryptByPrivateKey = RSAUtil
				.decryptByPrivateKey(bytes, mLocalPrivateKey);

			String respond = new String(decryptByPrivateKey, "GBK");

			JSONObject resultParam = new JSONObject(respond);
			mTokenType = resultParam.optString("tokenType",
				mTokenType);
			mToken = resultParam.optString("token", mToken);
			String businessParam = resultParam
				.optString("businessParam");
			// 不为空打印log
			// for log
//			if (!TextUtils.isEmpty(WebServiceUtils.ENCRYPT_KEY)) {
//			    Log.d("Tang", "respond——>" + respond);
//			}
			msgReturn.map.put(KEY_CALL, businessParam);
			// 如果err_code为“WG1002 报文流水重复”或者“WG1004
			// 获取报文流水号失败”，则需要进行报文流水重置；
		    } catch (Exception e) {
			//解密失败
			
			decryptSucess=false;
			Log.i("hobby","解密失败except");
		    }

		} 
		
		if (err_code.equalsIgnoreCase(WGErrorCode.WG1002)
			|| err_code.equalsIgnoreCase(WGErrorCode.WG1004)) {
		    // 报文流水重置
		    MsgReturn messageIdReset = messageIdReset();
		    if (!messageIdReset.errorMsg.errorCode
			    .equals(ErrorMsg.ERROR_SUCCESS)) {
			return messageIdReset;
		    }

		    // 重置 mMsgSeqNo 为 1；
		    mMsgSeqNo = 1;
//		    Log.i("Tang", "mMsgSeqNo-->" + mMsgSeqNo);
		    // 重发次数小于或者等于最大重发次数，发起重发
		    if (requestCount <= MAX_REQUESTCOUNT) {
			msgReturn = callWebService(requestStrRe, formNameRe);// 重发报文
		    } else {
			requestCount = 0;// 重发次数归零
		    }
		} else if (err_code.equalsIgnoreCase(WGErrorCode.WG1007)
			|| err_code.equalsIgnoreCase(WGErrorCode.WG2002)
			||err_code.equalsIgnoreCase(WGErrorCode.WG2004)
			|| err_code.equalsIgnoreCase(WGErrorCode.WG2003)||decryptSucess==false) {
		    Log.i("hobby","解密失败");
		    errorCountFlag++;
		    if (err_code.equalsIgnoreCase(WGErrorCode.WG1007)) {
			// 如果为“wg1007 设备逻辑号不存在”，则置mLogicID为空
			mLogicID = "";
			// 把本地存储的logicID也移除
			SharedPreferences sp = mContext.getSharedPreferences(
				FILE_NAME, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sp.edit();
			editor.remove("logicID");
			editor.commit();
			editor.clear();
		    }
		    // 签到错误次数最多循环执行3次，超过3次仍然错误则当签到失败处理；
		    if (errorCountFlag > MAX_ERROR_COUNT) {
			errorCountFlag = 0;
			return msgReturn;
		    } else {
			mServiceKey = "";
			msgReturn = appSignIn(mAppID, mAppVersion);
			final ErrorMsg appSignIn = msgReturn.errorMsg;
			String errorCode = appSignIn.errorCode;
			String errorDesc = appSignIn.errorDesc;

			/************* <成功:0000> *****************/
			if (ErrorMsg.ERROR_SUCCESS.equals(errorCode)) {
			    // 签到成功重新发起上次失败的交易
			    msgReturn = callWebService(requestStrRe, formNameRe);// 重发报文
			}
		    }
		}
	    }
	} catch (SocketTimeoutException e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TIMEOUT;
	} catch (IOException e) {
	    e.printStackTrace();
	    if (NetUtil.isConnect(mContext)) {
		// 服务器内部错误
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE_IN_ERROR;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_FAILED;
	    } else {
		// 网络不稳定或者网络不通错误
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_NOT_NET;
	    }
	} catch (XmlPullParserException e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_FORMAT;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_DATA_FORMAT_ERROR_STRING;
	} catch (ServerAddressException e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_ADDRESS_FAILED;
	} catch (Exception e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_OTHER;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_FAILED;
	}
	return msgReturn;
    }

    // /////////////////////////////////////////////////////////////////////////////////
    /**
     * 2.2.1获取接入平台RSA公钥
     */
    private MsgReturn rsaPublicKey() {
	MsgReturn msgReturn = new MsgReturn();
	msgReturn.errorMsg = new ErrorMsg();
	msgReturn.errorMsg.errorType = "TYPE_GY";
	msgReturn.map = new HashMap<String, Object>();
	try {
	    // 签到报文组装
	    JSONObject rootParam = new JSONObject();
	    JSONObject jsonBusinessParam = new JSONObject();
	    rootParam.put("action", "rsaPublicKey");
	    jsonBusinessParam.put("appId", mAppID);
	    jsonBusinessParam.put("appVer", mAppVersion);
	    rootParam.put("param", jsonBusinessParam);
	    String requestParam = new String(rootParam.toString().getBytes());

	    String rtn = WebServiceUtils.getReturnInfo(requestParam);
	    // Log.i("Tang", "rtn----->"+rtn);
	    // 返回的字符串为空判断
	    // 判断通讯过程是否正常，如果不是，则更新MsgReturn的错误码为“D001 网络错误”，函数返回；
	    if (TextUtils.isEmpty(rtn)) {
		// -=-=-=-=网络不可用-=-=-=-=-
		if (NetUtil.isConnect(mContext)) {
		    // 服务器内部错误
		    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE_IN_ERROR;
		    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_FAILED;
		} else {
		    // 网络不稳定或者网络不通错误
		    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
		    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_NOT_NET;
		}
	    } else {
		// 如果接口有返回数据，则进行下面逻辑处理：
		// 1、对返回字符串进行base64反编码；如果编码过程出错，则更新MsgReturn的错误码为“D008
		// 数据格式不正确”，函数返回；
		// 2、编码成功后，分解获取JSON串中的“err_code”：
		// 1）如果获取出错，则更新MsgReturn的错误码为“D008 数据格式不正确”，函数返回；
		// 2）判断“err_code”是否为“0000”，如果不是，则获取“err_msg”对应的描述信息，然后分别更新到MsgReturn的mErrorMsg，函数返回；
		// 3）“err_code”为“0000”时，分解获取 “public_key” 与
		// “public_key_ver”，分别更新到属性 mServiceKey 与 mKeyVersion；

		JSONObject resultObject = new JSONObject(rtn);
		String err_code = resultObject.optString("err_code");
		String err_msg = resultObject.optString("err_msg");
		// 返回码为：0000
		if (err_code.equals(ErrorMsg.ERROR_SUCCESS)) {
		    // TODO
		    // 5、json对象获取param内容值
		    String param = resultObject.optString("param");
		    byte[] decodeParam = Base64.decode(param);
		    // 6、对param内容转码
		    String paramStr = new String(decodeParam, RSAUtil.CHAR_SET);
		    JSONObject resultParam = new JSONObject(paramStr);
		    mServiceKey = resultParam.optString("public_key",
			    mServiceKey);// 获取接入平台公约
		    Log.i("hobby", "servicekey:"+mServiceKey);
		    mKeyVersion = resultParam.optString("public_key_ver",
			    mKeyVersion);// 获取接入平台公约版本
		}
		msgReturn.errorMsg.errorCode = err_code;
		msgReturn.errorMsg.errorDesc = err_msg;
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    if (NetUtil.isConnect(mContext)) {
		// 服务器内部错误
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE_IN_ERROR;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_FAILED;
	    } else {
		// 网络不稳定或者网络不通错误
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_NOT_NET;
	    }
	} catch (XmlPullParserException e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_FORMAT;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_DATA_FORMAT_ERROR_STRING;
	} catch (ServerAddressException e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_ADDRESS_FAILED;
	} catch (Exception e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_DATA_FORMAT_ERROR;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_DATA_FORMAT_ERROR_STRING;
	}
	return msgReturn;
    }

    int errorCountFlag = 1;// 签到错误次数
    final int MAX_ERROR_COUNT = 3;// 最大签到错误次数

    /**
     * 2.2.2APP申请签到
     */
    public MsgReturn appSignIn(String appID, String appVersion) {

	MsgReturn msgReturn = new MsgReturn();
	msgReturn.errorMsg = new ErrorMsg();
	msgReturn.errorMsg.errorType = "TYPE_QD";
	msgReturn.map = new HashMap<String, Object>();
	mAppID = appID;
	mAppVersion = appVersion;
	if (TextUtils.isEmpty(mServiceKey)) {

	    // 这里调用获取后台服务公钥
	    MsgReturn rsaPublicKey = rsaPublicKey();

	    if (!rsaPublicKey.errorMsg.errorCode.equals(ErrorMsg.ERROR_SUCCESS)) {
		return rsaPublicKey;
	    }
	}
	try {
	    // 签到报文组装
	    JSONObject rootParam = new JSONObject();
	    JSONObject jsonBusinessParam = new JSONObject();
	    rootParam.put("action", "appSignIn");
	    jsonBusinessParam.put("appId", mAppID);
	    jsonBusinessParam.put("appVer", mAppVersion);
	    jsonBusinessParam.put("keyVer", mKeyVersion);
	    JSONObject message = new JSONObject();
	    message.put("appKey", mLocalPublicKey);// 本地公钥
	    message.put("logicId", mLogicID);
	    // message内容编码转换需转换三次,请注意转换的编码格式为ISO-8859-1
	    // 字符串转为字节数组需要转换一次
	    byte[] data = message.toString().getBytes(RSAUtil.CHAR_SET);
	    // 使用服务器公钥加密
	    byte[] encodedData = null;
	    try {
		encodedData = RSAUtil.encryptByPublicKey(data, mServiceKey);
	    } catch (Exception e) {
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_OTHER;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_RSA;
		e.printStackTrace();
	    }
	    // 将字节数组转为字符串需要转换一次
	    String encode = new String(encodedData, RSAUtil.CHAR_SET);
	    // 将字节数组转为字符串,使用Base64编码需要转换一次
	    encode = Base64.encode(encode.getBytes(RSAUtil.CHAR_SET));
	    jsonBusinessParam.put("message", encode);
	    rootParam.put("param", jsonBusinessParam);
	    String request = rootParam.toString();

	    // 1、获取返回值
	    String rtn = WebServiceUtils.getReturnInfo(request);
//	    Log.i("Tang", "appSignIn——rtn----->" + rtn);

	    if (TextUtils.isEmpty(rtn)) {
		// -=-=-=-=网络不可用-=-=-=-=-
		if (NetUtil.isConnect(mContext)) {
		    // 服务器内部错误
		    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE_IN_ERROR;
		    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_FAILED;
		} else {
		    // 网络不稳定或者网络不通错误
		    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
		    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_NOT_NET;
		}
	    } else {
		// 4、通过字符串获取json对象
		JSONObject resultObject = new JSONObject(rtn);
		String err_code = resultObject.optString("err_code");
		String err_msg = resultObject.optString("err_msg");
		msgReturn.errorMsg.errorCode = err_code;
		msgReturn.errorMsg.errorDesc = err_msg;
		// 1）如果为“wg1007 设备逻辑号不存在”，则置mLogicID为空，然后按“7.应用签到”再发一次签到；
		// 2）如果为“wg2002 密钥版本号比较失败” 或者 “wg2003
		// 报文解密失败”，则先“2.获取无线网关公钥”调用接口获取公钥，成功后按“7.应用签到”再发一次签到；
		// 3）以上响应码错误，最多循环执行3次，超过3次仍然错误则当签到失败处理；
		// 4）其它错误码则获取“err_msg”对应的描述信息，然后分别更新到MsgReturn的mErrorMsg，函数返回；
		// 3、如果“err_code”为“0000”时，则进行下面逻辑处理：
		// 1）用base64对param部分内容进行解码，如果解码出错，则更新MsgReturn的错误码为“D008
		// 数据格式不正确”，函数返回；
		// 2）解码成功后按照标准的RSA算法用 mLocalPrivateKey（本地私钥）对 param
		// 内容进行解密，如果解码出错，则更新MsgReturn的错误码为“D007 解密出错”，函数返回；
		// 3）解密成功后，分解“logicId”对应的内容，然后更新到 mLogicID；
		// 4）初始化报文流水号 mMsgSeqNo 为 1；
		// 5）更新类属性mAppID 为 传入参数：appID；
		// 6）更新类属性mAppVersion 为 传入参数：appVersion；
		// 返回码为：0000

		boolean decryptSucess = true;// 解密成功 hao修改

		if (err_code.equals(ErrorMsg.ERROR_SUCCESS)) {
		    // 5、json对象获取param内容值
		    String param = resultObject.optString("param");
		    byte[] decodeParam = Base64.decode(param);
		    // 6、对param内容转码
		    String string = new String(decodeParam, RSAUtil.CHAR_SET);
		    byte[] bytes = string.getBytes(RSAUtil.CHAR_SET);
		    // 7、使用RSA私钥解密

		   
		    try {
			
			 byte[] decryptByPrivateKey  = RSAUtil.decryptByPrivateKey(
				bytes, mLocalPrivateKey);
			
			JSONObject resultParam = new JSONObject(new String(
				decryptByPrivateKey));
			mLogicID = resultParam.optString("logicId", mLogicID);
			// 把本地存储的logicID保存
			SharedPreferences sp = mContext.getSharedPreferences(
				FILE_NAME, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sp.edit();
			editor.putString("logicID", mLogicID);
			editor.commit();
			editor.clear();
			mMsgSeqNo = 1;
			errorCountFlag = 0;
		    } catch (Exception e) {
			decryptSucess=false;
			Log.i("hobby","解密失败except");
		    }
		  

			
		    

		}

		if (err_code.equalsIgnoreCase(WGErrorCode.WG1007)
			|| err_code.equalsIgnoreCase(WGErrorCode.WG2002)
			|| err_code.equalsIgnoreCase(WGErrorCode.WG2003)
			||err_code.equalsIgnoreCase(WGErrorCode.WG2004)
			|| decryptSucess==false) {
		    Log.i("hobby","解密失败");
		    errorCountFlag++;
		    if (err_code.equalsIgnoreCase(WGErrorCode.WG1007)) {
			// 如果为“wg1007 设备逻辑号不存在”，则置mLogicID为空
			mLogicID = "";
			// 把本地存储的logicID也移除
			SharedPreferences sp = mContext.getSharedPreferences(
				FILE_NAME, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = sp.edit();
			editor.remove("logicID");
			editor.commit();
			editor.clear();
		    }
		    // 签到错误次数最多循环执行3次，超过3次仍然错误则当签到失败处理；
		    if (errorCountFlag > MAX_ERROR_COUNT) {
			errorCountFlag = 0;
			msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SINGIN_ERROR;
			msgReturn.errorMsg.errorDesc = ErrorMsg.SINGIN_ERROR;
			return msgReturn;
		    } else {
			mServiceKey = "";
			msgReturn = appSignIn(mAppID, mAppVersion);

		    }
		}

	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    if (NetUtil.isConnect(mContext)) {
		// 服务器内部错误
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE_IN_ERROR;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_FAILED;
	    } else {
		// 网络不稳定或者网络不通错误
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_NOT_NET;
	    }
	} catch (XmlPullParserException e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_FORMAT;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_DATA_FORMAT_ERROR_STRING;
	} catch (ServerAddressException e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_ADDRESS_FAILED;
	} catch (Exception e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_DATA_FORMAT_ERROR;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_DATA_FORMAT_ERROR_STRING;
	}
	return msgReturn;
    }

    /**
     * 2.2.3报文流水重置
     */
    private MsgReturn messageIdReset() {
	MsgReturn msgReturn = new MsgReturn();
	msgReturn.errorMsg = new ErrorMsg();
	msgReturn.errorMsg.errorType = "TYPE_CZ";
	try {
	    // 签到报文组装
	    JSONObject rootParam = new JSONObject();
	    JSONObject jsonBusinessParam = new JSONObject();
	    rootParam.put("action", "messageIdReset");
	    jsonBusinessParam.put("appId", mAppID);
	    jsonBusinessParam.put("appVer", mAppVersion);
	    jsonBusinessParam.put("keyVer", mKeyVersion);
	    JSONObject message = new JSONObject();
	    // 设置需要重置的逻辑id
	    message.put("logicId", mLogicID);
	    // message内容编码转换需转换三次,请注意转换的编码格式为ISO-8859-1
	    // 字符串转为字节数组需要转换一次
	    byte[] data = message.toString().getBytes(RSAUtil.CHAR_SET);
	    // 使用服务器公钥加密
	    byte[] encodedData = null;
	    try {
		encodedData = RSAUtil.encryptByPublicKey(data, mServiceKey);
	    } catch (Exception e) {
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_OTHER;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_RSA;
		e.printStackTrace();
	    }
	    // 将字节数组转为字符串需要转换一次
	    String encode = new String(encodedData, RSAUtil.CHAR_SET);
	    // 将字节数组转为字符串,使用Base64编码需要转换一次
	    encode = Base64.encode(encode.getBytes(RSAUtil.CHAR_SET));
	    jsonBusinessParam.put("message", encode);
	    rootParam.put("param", jsonBusinessParam);
	    String request = rootParam.toString();

	    // 1、获取返回值
	    String rtn = WebServiceUtils.getReturnInfo(request);
	    // 4、通过字符串获取json对象
	    JSONObject resultObject = new JSONObject(rtn);

	    if (TextUtils.isEmpty(rtn)) {
		// -=-=-=-=网络不可用-=-=-=-=-
		if (NetUtil.isConnect(mContext)) {
		    // 服务器内部错误
		    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE_IN_ERROR;
		    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_FAILED;
		} else {
		    // 网络不稳定或者网络不通错误
		    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
		    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_NOT_NET;
		}
	    } else {
		// 如果err_code为“wg2002 密钥版本号比较失败”或者“2003 报文解密失败”，则需要重新获取无线网关公钥；
		String err_code = resultObject.optString("err_code");
		String err_msg = resultObject.optString("err_msg");
		if (err_code.equalsIgnoreCase(WGErrorCode.WG2002)
			||err_code.equalsIgnoreCase(WGErrorCode.WG2004)
			|| err_code.equalsIgnoreCase(WGErrorCode.WG2003)) {
		    mServiceKey = "";
		    // 这里调用获取后台服务公钥
		    MsgReturn rsaPublicKey = rsaPublicKey();

		    if (!rsaPublicKey.errorMsg.errorCode
			    .equals(ErrorMsg.ERROR_SUCCESS)) {
			return rsaPublicKey;
		    }
		}
		msgReturn.errorMsg.errorCode = err_code;
		msgReturn.errorMsg.errorDesc = err_msg;
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    if (NetUtil.isConnect(mContext)) {
		// 服务器内部错误
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE_IN_ERROR;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_FAILED;
	    } else {
		// 网络不稳定或者网络不通错误
		msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_NOT_NET;
		msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_NOT_NET;
	    }
	} catch (XmlPullParserException e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_FORMAT;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_DATA_FORMAT_ERROR_STRING;
	} catch (ServerAddressException e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_SERVICE;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_TEXT_SERVICE_ADDRESS_FAILED;
	} catch (Exception e) {
	    e.printStackTrace();
	    msgReturn.errorMsg.errorCode = ErrorMsg.ERROR_DATA_FORMAT_ERROR;
	    msgReturn.errorMsg.errorDesc = ErrorMsg.ERROR_DATA_FORMAT_ERROR_STRING;
	}
	return msgReturn;
    }

    // /////////////////////////////////////////////////////////////////////////////////

    /**
     * 启动检查
     * 
     * @param appId
     * @param appVersion
     * @param appConfigVersion
     * @return
     * @throws RemoteExceptionL
     */
    public String checkUpdates(final String appId, String appVersion,
	    String appConfigVersion) throws RemoteException {
	Log.i("启动", "启动检查开始...");
	long startTime = System.currentTimeMillis();
	JSONObject returnJson = new JSONObject();
	try {
	    String imei = null;
	    try {
		imei = ImeiUtils.getImei(mContext);
	    } catch (SecurityException e) {
		Log.e("启动", "项目获取IMEI号需配置READ_PHONE_STATE权限");
	    }
	    String returnStr = BaseServiceInvoker.startup(appId, imei,
		    appVersion, appConfigVersion);
	    Log.d("BaseService", "check() result=" + returnStr);
	    JSONObject returnJsonObject = null;
	    try {
		returnJsonObject = new JSONObject(returnStr);
		String dataStr = returnJsonObject.getString("data");
		JSONObject dataJsonObject = new JSONObject(dataStr);
		String deviceStatus = dataJsonObject.getString("deviceStatus");
		if (Constants.DEVICE_STATUS_NORMAL.equals(deviceStatus)) {// 正常状态
		    final String newAppVersion = dataJsonObject
			    .getString("appVersion");
		    final boolean oldVersionEnable = dataJsonObject
			    .getBoolean("oldVersionEnable");
		    final String newAppConfigVersion = dataJsonObject
			    .getString("appConfigVersion");
		    if (!"".equals(newAppConfigVersion)
			    && !newAppConfigVersion.equals(appConfigVersion)) {
			final String configUrl = dataJsonObject
				.getString("configUrl");
			final String configPath = dataJsonObject
				.getString("configPath");
			// 修改为单线程 20131010 20：57
			updateConfig(configUrl, configPath, appId);

			// 多线程
			/*
			 * new Thread() {
			 * 
			 * @Override public void run() { String result =
			 * updateConfig(configUrl, configPath, appId);
			 * super.run(); } }.start();
			 */
		    }
		    if (!"".equals(newAppVersion)
			    && !newAppVersion.equals(appVersion)) {
			final String appUrl = dataJsonObject
				.getString("appUrl");
			handler.postDelayed(new Runnable() {
			    @Override
			    public void run() {
				Intent intent = new Intent();
				intent.setAction(appId
					+ AndroidUIReceiver.ACTION_UPDATE_APP_SUFFIX);
				intent.putExtra("appId", appId);
				intent.putExtra("version", newAppVersion);
				intent.putExtra("appUrl", appUrl);
				intent.putExtra("oldVersionEnable",
					oldVersionEnable);
				Log.i("软件更新",
					"检测到软件更新，发送广播，Action:"
						+ appId
						+ AndroidUIReceiver.ACTION_UPDATE_APP_SUFFIX);
				mContext.sendBroadcast(intent);
			    }
			}, 1000);
		    }
		    if (oldVersionEnable) {
			boolean singleLoginApp = dataJsonObject
				.getBoolean("singleLoginApp");
			boolean loginStatus = dataJsonObject
				.getBoolean("loginStatus");
			if (singleLoginApp && loginStatus) {
			    returnJson.put("action",
				    Constants.COMMAND_MAIN_PAGE);
			    updateNewDeviceStatus();
			} else {
			    returnJson.put("action",
				    Constants.COMMAND_LOGIN_PAGE);
			}
		    } else {
			returnJson.put("action",
				Constants.COMMAND_OLD_VERSITON_UNABLEL);
		    }
		} else if (Constants.DEVICE_STATUS_NOACTIVE
			.equals(deviceStatus)) {// 未激活状态
		    SharedPreferences sharedPreferences = mContext
			    .getSharedPreferences(
				    Constants.SHAREDPREFERENCES_NSME,
				    Context.MODE_PRIVATE);
		    String newDevice = sharedPreferences.getString(
			    Constants.SP_NEW_DEVICE, null);
		    if (newDevice == null || "".equals(newDevice)) {// 新设备时跳转至登录页
			returnJson.put("action", Constants.COMMAND_LOGIN_PAGE);
		    } else {
			returnJson.put("action", Constants.COMMAND_NOACTIVE);
		    }
		} else if (Constants.DEVICE_STATUS_FORBIDDEN
			.equals(deviceStatus)) {// 停用状态
		    deleteData();
		    startDeviceTraceService();
		    returnJson.put("action", Constants.COMMAND_QUIT);
		}
	    } catch (JSONException e) {
		e.printStackTrace();
	    }
	} catch (ServerAddressException e) {
	    e.printStackTrace();
	    handler.post(new Runnable() {
		@Override
		public void run() {
		    handler.post(new Runnable() {
			@Override
			public void run() {
			    Toast.makeText(mContext, "未配置服务器地址",
				    Toast.LENGTH_SHORT).show();
			}
		    });
		}
	    });
	    try {
		returnJson.put("action", Constants.COMMAND_QUIT);
	    } catch (JSONException e1) {
		e1.printStackTrace();
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	    handler.post(new Runnable() {
		@Override
		public void run() {
		    handler.post(new Runnable() {
			@Override
			public void run() {
			    Toast.makeText(mContext, "网络异常，无法与服务器建立连接",
				    Toast.LENGTH_LONG).show();
			}
		    });
		}
	    });
	    try {
		returnJson.put("action", Constants.COMMAND_QUIT);
	    } catch (JSONException e1) {
		e1.printStackTrace();
	    }
	} catch (Exception e) {
	    Log.e("启动程序异常", "捕获异常", e);
	    try {
		returnJson.put("action", Constants.COMMAND_QUIT);
	    } catch (JSONException e1) {
		e1.printStackTrace();
	    }
	}
	Log.d("BaseService", "check() return=" + returnJson.toString());
	Log.i("启动", "启动方法耗时:" + (System.currentTimeMillis() - startTime) + "ms");
	return returnJson.toString();
    }

    public String versionUpdates(final String appId, String appVersion) {
	String returnVersion = appVersion;
	try {
	    String imei = null;
	    try {
		imei = ImeiUtils.getImei(mContext);
	    } catch (SecurityException e) {
		Log.e("启动", "项目获取IMEI号需配置READ_PHONE_STATE权限");
	    }
	    String returnStr = BaseServiceInvoker.startup(appId, imei,
		    appVersion, "");
	    Log.d("BaseService", "versionUpdates() result=" + returnStr);
	    JSONObject returnJsonObject = null;
	    try {
		returnJsonObject = new JSONObject(returnStr);
		String dataStr = returnJsonObject.getString("data");
		JSONObject dataJsonObject = new JSONObject(dataStr);
		String deviceStatus = dataJsonObject.getString("deviceStatus");
		if (Constants.DEVICE_STATUS_NORMAL.equals(deviceStatus)) {// 正常状态
		    final String newAppVersion = dataJsonObject
			    .getString("appVersion");
		    final boolean oldVersionEnable = dataJsonObject
			    .getBoolean("oldVersionEnable");
		    if (!"".equals(newAppVersion)
			    && !newAppVersion.equals(appVersion)) {
			returnVersion = newAppVersion;
			final String appUrl = dataJsonObject
				.getString("appUrl");
			handler.postDelayed(new Runnable() {
			    @Override
			    public void run() {
				Intent intent = new Intent();
				intent.setAction(appId
					+ AndroidUIReceiver.ACTION_UPDATE_APP_SUFFIX);
				intent.putExtra("appId", appId);
				intent.putExtra("version", newAppVersion);
				intent.putExtra("appUrl", appUrl);
				intent.putExtra("oldVersionEnable",
					oldVersionEnable);
				Log.i("软件更新",
					"检测到软件更新，发送广播，Action:"
						+ appId
						+ AndroidUIReceiver.ACTION_UPDATE_APP_SUFFIX);
				mContext.sendBroadcast(intent);
			    }
			}, 0);
		    }
		}
	    } catch (JSONException e) {
		e.printStackTrace();
	    }
	} catch (ServerAddressException e) {
	    e.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	    handler.post(new Runnable() {
		@Override
		public void run() {
		    handler.post(new Runnable() {
			@Override
			public void run() {
			    Toast.makeText(mContext, "网络出错，请重试.",
				    Toast.LENGTH_LONG).show();
			}
		    });
		}
	    });
	}
	Log.d("BaseService", "versionUpdates() return=" + returnVersion);
	return returnVersion;
    }

    /**
     * 登录
     * 
     * @param username
     * @param password
     * @param appId
     * @return
     * @throws RemoteException
     */
    public String login(String username, String password, String appId)
	    throws RemoteException {
	try {
	    String imei = null;
	    try {
		imei = ImeiUtils.getImei(mContext);
	    } catch (SecurityException e) {
		e.printStackTrace();
	    }
	    String tokenId = null;
	    tokenId = PushUtils.getTokenId(mContext);
	    String result = BaseServiceInvoker.login(appId, username, password,
		    imei, tokenId);
	    try {
		JSONObject resultJson = new JSONObject(result);
		String dataStr = resultJson.getString("data");
		JSONObject dataJson = new JSONObject(dataStr);
		String success = dataJson.getString("success");
		if (success.equals("success")) {
		    updateNewDeviceStatus();
		}
	    } catch (JSONException e) {
		e.printStackTrace();
	    }
	    return result;
	} catch (ServerAddressException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;
    }

    /**
     * 远程日志记录
     * 
     * @param appId
     * @param log
     * @param level
     * @param type
     * @throws RemoteException
     */
    public boolean log(String appId, String log, String level, String type)
	    throws RemoteException {
	boolean b = false;
	try {
	    String imei = null;
	    try {
		imei = ImeiUtils.getImei(mContext);
	    } catch (SecurityException e) {
		e.printStackTrace();
	    }
	    String result = BaseServiceInvoker.log(appId, log, level, type,
		    imei);
	    if (result != null && !"".equals(result)) {
		JSONObject resultJson = new JSONObject(result);
		b = resultJson.getBoolean("success");
	    }
	} catch (ServerAddressException e) {
	    Log.e("日志上传异常", e.getMessage(), e);
	} catch (JSONException e) {
	    Log.e("日志上传异常", e.getMessage(), e);
	} catch (IOException e) {
	    Log.e("日志上传异常", e.getMessage(), e);
	} catch (Exception e) {
	    Log.e("-日志上传异常-", e.getMessage(), e);
	}
	return b;
    }

    /**
     * 本地日志记录
     * 
     * @param title
     * @param log
     * @param logLevel
     * @throws RemoteException
     */
    public void logLocal(String title, String log, String logLevel)
	    throws RemoteException {
	ConfigureLog4J.getInstance();
	Logger logger = Logger.getLogger(title);
	if (LogLevel.LOG_LEVEL_DEBUG.equals(logLevel)) {
	    logger.debug(log);
	} else if (LogLevel.LOG_LEVEL_ERROR.equals(logLevel)) {
	    logger.error(log);
	} else if (LogLevel.LOG_LEVEL_FATAL.equals(logLevel)) {
	    logger.fatal(log);
	} else if (LogLevel.LOG_LEVEL_INFO.equals(logLevel)) {
	    logger.info(log);
	} else if (LogLevel.LOG_LEVEL_WARN.equals(logLevel)) {
	    logger.warn(log);
	}
    }

    /**
     * 加密
     * 
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    public static String encrypt(String data, String key) throws Exception {
	return EncryptionAndDecryption.encrypt(data, key);
    }

    /**
     * 解密
     * 
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    public static String decrypt(String data, String key) throws Exception {
	return EncryptionAndDecryption.decrypt(data, key);
    }

    /**
     * 获取底座版本
     * 
     * @return
     */
    public static String getBaseLibVersion() {
	return "1.8";
    }

    /**
     * 更新软件配置文件
     * 
     * @param configUrl
     * @param configPath
     * @param appId
     *            发布广播，作为广播的
     * @throws IOException
     * @throws MalformedURLException
     */
    private String updateConfig(String configUrl, String configPath,
	    String appId) {
	Log.i("更新配置文件", "开始更新配置文件...");
	long startTime = System.currentTimeMillis();
	String externalStorage = null;
	String innerStorage = null;
	String fileName = configPath.substring(configPath.lastIndexOf("/") + 1);
	configPath = configPath.substring(0, configPath.lastIndexOf("/") + 1);
	if (configPath.startsWith("/sdcard")) {
	    if (Environment.MEDIA_MOUNTED.equals(Environment
		    .getExternalStorageState())) {
		File sdPath = Environment.getExternalStorageDirectory();
		externalStorage = configPath.replaceFirst("/sdcard",
			sdPath.getAbsolutePath());
	    }
	} else {
	    innerStorage = configPath;
	}
	String result = null;
	try {
	    URLConnection conn = FileDownloadUtils.getConnection(configUrl);
	    result = FileDownloadUtils.downloadFile(null, conn,
		    externalStorage, innerStorage, fileName, true);
	} catch (Exception e) {
	    e.printStackTrace();
	    if ("内存空间不足".equals(e.getMessage())) {
		handler.post(new Runnable() {
		    @Override
		    public void run() {
			Toast.makeText(mContext, "内存空间已满，无法更新配置文件",
				Toast.LENGTH_LONG).show();
		    }
		});
	    }
	}
	Log.d("更新配置文件", "" + result);
	if (result != null) {
	    File file = new File(result);
	    if (file.exists()) {
		Intent intent = new Intent();
		intent.setAction(appId + ".updatedconfig");
		intent.putExtra("configFile", file.getName());
		intent.putExtra("configPath", result);
		mContext.sendBroadcast(intent);
		Log.d("更新配置文件", "配置文件更新成功");
		Log.d("更新配置文件", "系统广播:" + appId + ".updatedconfig");
	    } else {
		Log.d("更新配置文件", "配置文件更新失败");
	    }
	}
	Log.d("更新配置文件", "更新配置文件用时:" + (System.currentTimeMillis() - startTime)
		+ "ms");
	return result;
    }

    /**
     * 删除敏感数据
     */
    private void deleteData() {
	if (Environment.MEDIA_MOUNTED.equals(Environment
		.getExternalStorageState())) {
	    File dataFolder = Environment.getExternalStorageDirectory();
	    String sdFolderPath = dataFolder.getAbsolutePath() + "/GDPOST";
	    File sdDataFolder = new File(sdFolderPath);
	    if (sdDataFolder.exists() && sdDataFolder.isDirectory()) {
		Log.d("设备丢失", "删除目录：" + sdDataFolder.getPath());
		deleteDir(sdDataFolder);
	    }
	}
	String dataPath = "/GDPOST";
	File dataFolder = new File(dataPath);
	if (dataFolder.exists() && dataFolder.isDirectory()) {
	    Log.d("设备丢失", "删除目录：" + dataFolder.getPath());
	    deleteDir(dataFolder);
	}
    }

    /**
     * 递归删除文件夹内文件夹和文件
     * 
     * @param dir
     */
    private void deleteDir(File dir) {
	for (File file : dir.listFiles()) {
	    if (file.isDirectory()) {
		deleteDir(file);
	    } else if (file.isFile()) {
		deleteFile(file);
	    }
	}
    }

    /**
     * 删除文件
     * 
     * @param file
     */
    private void deleteFile(File file) {
	if (file.exists()) {
	    file.delete();
	}
    }

    /**
     * 设备地理位置上传服务
     */
    private void startDeviceTraceService() {
	Intent intent = new Intent(mContext, DeviceTraceService.class);
	mContext.startService(intent);
    }

    /**
     * 更改状态码(未激活的设备下次启动将不再进入登录界面)
     */
    private void updateNewDeviceStatus() {
	SharedPreferences sharedPreferences = mContext.getSharedPreferences(
		Constants.SHAREDPREFERENCES_NSME, Context.MODE_PRIVATE);
	String newDevice = sharedPreferences.getString(Constants.SP_NEW_DEVICE,
		null);
	if (newDevice == null || "".equals(newDevice)) {
	    Editor editor = sharedPreferences.edit();
	    editor.putString(Constants.SP_NEW_DEVICE, "No");
	    editor.commit();
	}
    }
}
