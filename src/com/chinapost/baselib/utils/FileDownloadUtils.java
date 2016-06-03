/**
 * 网络文件下载
 */
package com.chinapost.baselib.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.chinapost.baselib.uiinterface.FileDownloadInterface;

/**
 * @author jam
 * 2013-4-18 下午1:56:46
 */
public class FileDownloadUtils {
	
	/**
	 * 文件下载
	 * @param downUrl
	 * @param filePath
	 */
	private static void downloadFile(String downUrl, String filePath, boolean overwrite) {
		FileOutputStream fos = null;
		ByteArrayInputStream bis = null;
		try {
			File file = null;
			file = new File(filePath);
			if (overwrite) {
				file.deleteOnExit();
				file.createNewFile();
				byte[] fileBytes = download(downUrl);
				byte[] buffer = new byte[1024];
				bis = new ByteArrayInputStream(fileBytes);
				fos = new FileOutputStream(file);
				while(bis.read(buffer)!=-1){
					fos.write(buffer);
				}
			} else {
				if(!file.exists()) {
					file.createNewFile();
					byte[] fileBytes = download(downUrl);
					byte[] buffer = new byte[1024];
					bis = new ByteArrayInputStream(fileBytes);
					fos = new FileOutputStream(file);
					while(bis.read(buffer)!=-1){
						fos.write(buffer);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(bis != null) {
					bis.close();
				}
				if(fos != null) {
					fos.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 下载
	 * @param path
	 * @return
	 */
	private static byte[] download(String path) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet();
			request.setURI(new URI(path));
			HttpResponse response = client.execute(request);
			InputStream is = response.getEntity().getContent();
			byte[] buffer = new byte[1024];
			int size = 0;
			while((size = is.read(buffer)) != -1) {
				os.write(buffer, 0, size);
			}
			return os.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	/**
	 * 软件更新包下载
	 * @param url
	 * @param appId
	 * @param version
	 */
	public static String updateAppDownload(String url, String appId, String version, String innerStoragePath) {
		String pathStr = Constants.UPDATE_APP_DIR;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File sdPath = Environment.getExternalStorageDirectory();
			pathStr = sdPath.getAbsolutePath() + Constants.UPDATE_APP_DIR;
			File path = new File(pathStr);
			if (!path.exists()) {
				path.mkdirs();
			}
		} else {
			pathStr = innerStoragePath;
		}
		
		String fileName = appId + "_" + version + ".apk";
		downloadFile(url, pathStr + "/" + fileName, true);
		Log.i("版本更新","下载文件至：" + pathStr + "/" + fileName);
		return pathStr + "/" + fileName;
	}
	
	/**
	 * 更新软件配置文件
	 * @param url
	 * @param configPath
	 * @return
	 */
	public static String updateConfigDownload(String url, String configPath) {
		if (!configPath.startsWith("/sdcard")) {
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				File sdPath = Environment.getExternalStorageDirectory();
				File config = new File(sdPath.getAbsolutePath() + configPath);
				if (config.exists()) {
					configPath = config.getPath();
				}
			}
		} else {
			File sdPath = Environment.getExternalStorageDirectory();
			File config = new File(sdPath.getAbsoluteFile() + configPath.substring(7));
			configPath = config.getAbsolutePath();
		}
		String path = configPath.substring(0, configPath.lastIndexOf("/"));
		File configFile = new File(path);
		if (!configFile.exists()) {
			configFile.mkdirs();
		}
		if (configFile.exists()) {
			downloadFile(url, configPath, true);
		} else {
			return "无权限创建:" + configPath;
		}
		return configPath;
	}
	
	/**
	 * 下载文件
	 * @param filedownloadInterface 界面接口（进度条功能）
	 * @param urlStr	下载链接
	 * @param sdcardStoragePath	内存卡存放地址
	 * @param appStoragePath	机身内存存放地址
	 * @param fileName	文件名称
	 * @return	最终文件的存放目录
	 * @throws IOException
	 */
	public static String downloadFile(FileDownloadInterface filedownloadInterface, URLConnection urlConn, String sdcardStoragePath, String appStoragePath, String fileName, boolean overwrite) throws IOException {
		int length = urlConn.getContentLength();
		Log.i("下载文件", "下载文件大小:" + length);
		
		String path = null;
		boolean storageAvailable = false;
		boolean externalStorageAvailable = false;
		if (sdcardStoragePath != null && !"".equals(sdcardStoragePath)) {
			if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				File sdcardDir = Environment.getExternalStorageDirectory();
				StatFs sf = new StatFs(sdcardDir.getPath());
				long blockSize = sf.getBlockSize();
				long availableCount = sf.getAvailableBlocks();
				Log.i("下载文件", "检测到内存卡剩余：" + (blockSize * availableCount) + "(BlockSize:" + blockSize + ",AvailableBlocks:" + availableCount + ")");
				if ((blockSize * availableCount) >= length) {
					externalStorageAvailable = true;
					path = sdcardStoragePath;
				}
			}
		}
		if (appStoragePath != null && !"".equals(appStoragePath)) {
			if (!externalStorageAvailable) {
				File root = Environment.getRootDirectory(); 
				StatFs sf = new StatFs(root.getPath());
				long blockSize = sf.getBlockSize();
				long availableCount = sf.getAvailableBlocks();
				Log.i("下载文件", "检测到机身存储剩余：" + (blockSize * availableCount) + "(BlockSize:" + blockSize + ",AvailableBlocks:" + availableCount + ")");
				if ((blockSize * availableCount) >= length) {
					storageAvailable = true;
					path = appStoragePath;
				}
			}
		}
		if (storageAvailable || externalStorageAvailable) {
			if (!path.endsWith("" + File.separatorChar)) {
				path += File.separatorChar;
			}
			File pathDir = new File(path);
			if (!pathDir.exists()) {
				boolean isMkdirsSuccess = pathDir.mkdirs();
				if (!isMkdirsSuccess) {
					throw new RuntimeException("无权限创建" + path);
				}
			}
			File file = new File(path + fileName);
			if (overwrite) {
				file.deleteOnExit();
				file.createNewFile();
			} else {
				if (file.exists()) {
					throw new RuntimeException("文件已存在");
				}
			}
			download(filedownloadInterface, urlConn, file);
			changePermission(appStoragePath, fileName, path);
		} else {
			throw new RuntimeException("内存空间不足");
		}
		return path + fileName;
	}

	/**
	 * 文件下载
	 * @param filedownloadInterface
	 * @param urlConn
	 * @param file
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static void download(FileDownloadInterface filedownloadInterface,
			URLConnection urlConn, File file) throws IOException,
			FileNotFoundException {
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			is = urlConn.getInputStream();
			fos = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int totalSize = 0;
			int size = 0;
			if (filedownloadInterface != null) {
				filedownloadInterface.start();
			}
			while((size = is.read(buffer)) != -1) {
				fos.write(buffer, 0, size);
				totalSize += size;
				Log.i("更新配置文件", "已下载" + totalSize);
				if (filedownloadInterface != null) {
					filedownloadInterface.update(totalSize);
				}
			}
			if (filedownloadInterface != null) {
				filedownloadInterface.stop();
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 下载至手机内存的，更改权限，否则无法安装
	 * 由于Android系统API的权限设定导致无权限安装
	 * @param appStoragePath
	 * @param fileName
	 * @param path
	 * @throws IOException
	 */
	private static void changePermission(String appStoragePath,
			String fileName, String path) throws IOException {
		if (path.equals(appStoragePath)) {
			String command = "chmod 666 " + path + fileName;
		    Runtime runtime = Runtime.getRuntime();
		    runtime.exec(command);
		}
	}

	/**
	 * 获取与服务器连接
	 * @param urlStr
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static URLConnection getConnection(String urlStr)
			throws MalformedURLException, IOException {
		URL url = new URL(urlStr);
		URLConnection urlConn = url.openConnection();
		urlConn.connect();
		return urlConn;
	}
	
}
