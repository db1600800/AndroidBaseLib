package com.chinapost.publiclibrary;



/**
 * 错误代码类 appSignIn时使用
 * 
 * @author Tzz
 * 
 */
public class ErrorMsg {

	// ============错误码============//
	/************* <成功:0000> *****************/
	public final static String ERROR_SUCCESS = "0000";
	/************* <网络错误:D001> *****************/
	public final static String ERROR_NOT_NET = "D001";
	/************* <xml报文格式错误:D002> *****************/
	public final static String ERROR_FORMAT = "D002";
	/************* <服务器地址错误:D003> *****************/
	public final static String ERROR_SERVICE = "D003";
	/************* <其它错误错误:D004> *****************/
	public final static String ERROR_OTHER = "D004";
	/************* <数据格式不正确:D008> *****************/
	public final static String ERROR_DATA_FORMAT_ERROR = "D008";
	/************* <服务器内部错误:D009> *****************/
	public final static String ERROR_SERVICE_IN_ERROR = "D009";
	/************* <签到失败:D0010> *****************/
	public final static String ERROR_SINGIN_ERROR = "D010";

	public final static String ERROR_TEXT_SUCCESS = "成功";

	public final static String ERROR_TEXT_FAILED = "交易失败.";

	public final static String ERROR_TEXT_SERVICE_FAILED = "服务器内部错误";
	
	public final static String ERROR_TEXT_SERVICE_ADDRESS_FAILED = "服务器地址错误";

	public final static String ERROR_TEXT_NOT_NET = "网络不可用";
	
	public final static String ERROR_TEXT_RSA = "RSA加密出错.";
	
	public final static String ERROR_DATA_FORMAT_ERROR_STRING = "数据格式错误";
	
	public final static String ERROR_TIMEOUT = "请求超时.";
	
	public final static String SINGIN_ERROR = "签到失败.";
	

	public String errorCode = "";
	// ============错误类型============//

	public String errorType = "";
	// ============错误描述============//
	public String errorDesc = "";

}
