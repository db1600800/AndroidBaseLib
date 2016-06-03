/**
 * 
 */
package com.chinapost.baselib.utils;

/**
 * @author jam
 * 2013-4-15 下午4:52:18
 */
public class Constants {

	//SharedPreferences
	public final static String SHAREDPREFERENCES_NSME = "chinapost_config";//应用数据存储文件名
	public final static String SP_SERVER_ADDRESS = "server_address";//服务器地址
	public final static String SP_NEW_DEVICE = "new_device";//新设备，如新设备第一次未激活可以进入登录页面
	
	/** 软件更新包目录 **/
	public final static String UPDATE_APP_DIR = "/GDPOST/APK";
	
	/** 针对服务端请求 **/
	public final static String REQUEST_ACTION_CHECK = "check";//检查设备及软件
	public final static String REQUEST_ACTION_LOGIN = "login";//登录
	public final static String REQUEST_ACTION_LOCATE = "locate";//定位
	public final static String REQUEST_ACTION_LOG = "log";//日志记录
	public final static String REQUEST_ACTION_TEST = "test";//测试联通性
	
	/** 针对客户端的指令 **/
	public final static String COMMAND_NOACTIVE = "noActive";//设备未激活
	public final static String COMMAND_LOGIN_PAGE = "loginPage";//跳转登录页
	public final static String COMMAND_MAIN_PAGE = "mainPage";//跳转主页面
	public final static String COMMAND_QUIT = "quit";//退出
	public final static String COMMAND_OLD_VERSITON_UNABLEL = "oldVersionUnable";//就版本无法使用
	
	/** 设备状态 **/
	public final static String DEVICE_STATUS_NOACTIVE = "noActive";//设备未激活
	public final static String DEVICE_STATUS_NORMAL = "normal";//设备正常
	public final static String DEVICE_STATUS_FORBIDDEN = "forbidden";//设备停用
}
