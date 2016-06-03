/**
 * 
 */
package com.chinapost.baselib.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

/**
 * @author Jam
 * 2013-7-9 下午7:05:23
 */
public class FileUploadUtils {

	public static boolean uploadFile(String url, File file,
			Map<String, String> param) throws ClientProtocolException,
			IOException {
		FileBody bin = null;
		HttpParams httpParam = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParam, 45000);
		HttpClient httpclient = new DefaultHttpClient(httpParam);
		HttpPost httppost = new HttpPost(url);
		if (file != null) {
			bin = new FileBody(file);
		}
		
		
		MultipartEntity reqEntity = new MultipartEntity();
		for (Entry<String, String> entry : param.entrySet()) {
			StringBody stringBody = new StringBody(entry.getValue(), Charset.forName("UTF-8"));
			reqEntity.addPart(entry.getKey(), stringBody);
		}
		reqEntity.addPart("data", bin);

		httppost.setEntity(reqEntity);
		Log.i("上传日志","执行: " + httppost.getRequestLine());
		Log.i("上传日志","上传文件'" + file.getName() + "'");

		HttpResponse response = httpclient.execute(httppost);
		Log.i("上传日志", "" + response.getStatusLine().getStatusCode());
		if (response.getStatusLine().getStatusCode() == 200) {
			return true;
		} else {
			return false;
		}
	}
}
