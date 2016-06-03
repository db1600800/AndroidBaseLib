/**
 * 
 */
package com.chinapost.baselib.webservice;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Properties;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.xmlpull.v1.XmlPullParserException;


import android.util.Log;

import com.chinapost.baselib.utils.PropertiesUtils;
import com.chinapost.baselib.webservice.exception.ServerAddressException;

/**
 * @author Jam
 * 2013-5-9 上午10:49:24
 */
public class WebServiceUtils {

	private static String NAMESPACE = "";
	private static String USER = "";
	private static String PASSWORD = "";
	private static String METHOD_NAME = "";
	private static String URL_PORTAL = "";
	private static String URL_WEBSERVICE_PATH = "";
	private static String TIME_OUT = "";
	private static String URL;
	public static String BeseURL;
	private static HttpTransportSE ht;
	public static String ENCRYPT_KEY = "";
	private static Properties properties;
	
	public static void initConfig() {
		if(properties == null) {
			properties = PropertiesUtils.getProperties();
			NAMESPACE = properties.getProperty("namespace");
			USER = properties.getProperty("user");
			PASSWORD = properties.getProperty("password");
			METHOD_NAME = properties.getProperty("method_name");
			URL_PORTAL = properties.getProperty("url_portal");
			URL_WEBSERVICE_PATH = properties.getProperty("url_webservice_path");
			TIME_OUT = properties.getProperty("time_out");
			ENCRYPT_KEY = properties.getProperty("wg_password");
			BeseURL = properties.getProperty("baseserver_url");
			init(properties.getProperty("server_url"));
		}
	}

	public static String getReturnInfo(String args) throws IOException, XmlPullParserException, ServerAddressException,SocketTimeoutException {
		
		initConfig();
		if (ht == null) {
			ht = getHttpTransport(URL, Integer.parseInt(TIME_OUT));
			if (ht == null) {
				throw new ServerAddressException();
			}
		}
		
		SoapObject rpc = new SoapObject(NAMESPACE, METHOD_NAME);
		rpc.addProperty("arg0", args);
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.bodyOut = rpc;
		envelope.headerOut = addWSSecurityHeaders(USER, PASSWORD);
		SoapPrimitive returnInfo = null;
		ht.call("", envelope);
		returnInfo = (SoapPrimitive) envelope.getResponse();
		return returnInfo.toString();
	}
	
	public static String getReturnInfo(String url, String args,
			int timeout) throws IOException, XmlPullParserException,SocketTimeoutException {
		initConfig();
		SoapObject rpc = new SoapObject(NAMESPACE, METHOD_NAME);
		rpc.addProperty("arg0", args);
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(
				SoapEnvelope.VER11);
		envelope.bodyOut = rpc;
		envelope.headerOut = addWSSecurityHeaders(USER, PASSWORD);

		url = URL_PORTAL + url + URL_WEBSERVICE_PATH;
		ht = getHttpTransport(url, timeout);

		SoapPrimitive returnInfo = null;
		ht.call("", envelope);
		returnInfo = (SoapPrimitive) envelope.getResponse();
		return returnInfo.toString();
	}
	
	public static HttpTransportSE getHttpTransport(String url,int timeout) {
		if (ht == null) {
			ht = new HttpTransportSE(url, timeout);
			ht.setXmlVersionTag("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			ht.debug = false;
		}
		return ht;
	}
	
	private static Element[] addWSSecurityHeaders(String user, String password) {
		Element[] header = new Element[1];
		header[0] = new Element()
				.createElement(
						"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
						"Security");
		header[0].setAttribute(null, "mustUnderstand", "1");
		Element usernametoken = new Element()
				.createElement(
						"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
						"UsernameToken");
		// usernametoken.addChild(Node.TEXT,"");
		usernametoken
				.setAttribute(
						"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd",
						"Id", "UsernameToken-4");
		header[0].addChild(Node.ELEMENT, usernametoken);
		Element username = new Element().createElement(null, "n0:Username");
		username.addChild(Node.TEXT, user);
		usernametoken.addChild(Node.ELEMENT, username);
		Element pass = new Element().createElement(null, "n0:Password");
		pass
				.setAttribute(
						null,
						"Type",
						"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
		pass.addChild(Node.TEXT, password);
		usernametoken.addChild(Node.ELEMENT, pass);
		return header;
	}
	
	public static void init(String url) {
		initConfig();
		URL = URL_PORTAL + url + URL_WEBSERVICE_PATH;
		System.out.println("URL------>"+URL);
		ht = getHttpTransport(URL, Integer.parseInt(TIME_OUT));
	}
	
	public static void clean() {
		ht = null;
	}
	
}
