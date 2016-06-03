package com.chinapost.publiclibrary;

import java.util.HashMap;

/**
 * 信息返回类，在callWebService时使用
 * 
 * @author Tzz
 * 
 */
public class MsgReturn {

	// 本设计提及到的方法KEY值约定：
	// ServiceInvoker.callWebservice：KEY_CALL

	public ErrorMsg errorMsg;
	public HashMap<String, Object> map;

}
