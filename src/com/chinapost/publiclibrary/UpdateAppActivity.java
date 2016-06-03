package com.chinapost.publiclibrary;

import java.io.File;
import java.net.URLConnection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.chinapost.baselib.uiinterface.FileDownloadInterface;
import com.chinapost.baselib.utils.Constants;
import com.chinapost.baselib.utils.FileDownloadUtils;

@SuppressLint("NewApi")
public class UpdateAppActivity extends Activity implements FileDownloadInterface {
	
	private ProgressDialog progressDialog;
	private int updateAkpLength = 0;
	private String stringText;
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏
		requestWindowFeature(Window.FEATURE_NO_TITLE);//无标题栏
		RelativeLayout layout = new  RelativeLayout(this);
		setContentView(layout);
		Bundle bundle = this.getIntent().getExtras();
		final String appId = bundle.getString("appId");
		final String version = bundle.getString("version");
		final String appUrl = bundle.getString("appUrl");
		final boolean oldVersionEnable = bundle.getBoolean("oldVersionEnable");
		
		AlertDialog.Builder builder = new Builder(UpdateAppActivity.this);
		builder.setTitle("温馨提示");
		builder.setCancelable(false);
		builder.setPositiveButton("更新", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				stringText = "";
				new Thread(){
					@Override
					public void run() {
						String fileName = null;
						try {
							String externalStorage = null;
							if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
								File sdPath = Environment.getExternalStorageDirectory();
								externalStorage = sdPath.getAbsolutePath() + Constants.UPDATE_APP_DIR;
							}
							String innerStorage = UpdateAppActivity.this.getCacheDir().getAbsolutePath();
							URLConnection conn = FileDownloadUtils.getConnection(appUrl);
							updateAkpLength = conn.getContentLength();
							fileName = FileDownloadUtils.downloadFile(UpdateAppActivity.this, conn, externalStorage, innerStorage, appId + "_" + version + ".apk", true);
							File updateApk = new File(fileName);
							if (updateApk.exists()) {
								Uri uri = Uri.fromFile(updateApk);
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(uri, "application/vnd.android.package-archive");
								startActivity(intent);
							}
						} catch (final Exception e) {
							e.printStackTrace();
							handler.post(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(UpdateAppActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
								}
							});
						} finally {
							handler.sendEmptyMessage(0);//关闭等待界面
						}
						UpdateAppActivity.this.finish();
						super.run();
					}
				}.start();
			}
		});
		//旧版本可用，不强制更新
		if (oldVersionEnable) {
			stringText = "检测到新程序，是否更新?";
		} else {//旧版本不可用，强制更新
			stringText = "旧程序已无法使用，如不更新将退出程序，是否更新?";
		}
		//设置title标题
		builder.setMessage(stringText);
		//取消按钮
		builder.setNegativeButton("取消", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(stringText.equals("旧程序已无法使用，如不更新将退出程序，是否更新?")) {
					//强制退出
					System.exit(0);
					android.os.Process.killProcess(android.os.Process.myPid());
					
				}
				UpdateAppActivity.this.finish();
			}
		});
		final AlertDialog alertDialog = builder.create();
		alertDialog.setCancelable(false);
		alertDialog.setCanceledOnTouchOutside(false);
		//强制关闭程序
		alertDialog.setOnDismissListener(new OnDismissListener() {
		    
		    @Override
		    public void onDismiss(DialogInterface dialog) {
			if(stringText.equals("旧程序已无法使用，如不更新将退出程序，是否更新?")) {
			    //强制退出
			    System.exit(0);
			    android.os.Process.killProcess(android.os.Process.myPid());
			}
		    }
		});
		alertDialog.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (KeyEvent.KEYCODE_BACK == keyCode) {
					alertDialog.dismiss();
					UpdateAppActivity.this.finish();
				}
				return true;
			}
		});
		alertDialog.show();
	}

	@Override
	public void start() {
		Log.i("软件更新","下载开始...");
		handler.post(new Runnable(){
			@Override
			public void run() {
				progressDialog = new ProgressDialog(UpdateAppActivity.this);
				progressDialog.setTitle("更新");
				progressDialog.setMessage("下载软件更新包");
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				Log.i("软件更新","文件大小：" + updateAkpLength);
				progressDialog.setMax(100);
				progressDialog.setIndeterminate(false);
				progressDialog.setCanceledOnTouchOutside(false);
				progressDialog.show();
			}
		});
	}

	@Override
	public void update(int downloadedSize) {
		Log.i("软件更新","下载了" + downloadedSize);
		final int increment = updateAkpLength <= 0 ? downloadedSize/1024 : downloadedSize * 100/updateAkpLength;
		if (progressDialog != null) {
			handler.post(new Runnable(){
				@Override
				public void run() {
					progressDialog.setProgress(increment);
				}
			});
		}
	}

	@Override
	public void stop() {
		Log.i("软件更新","下载结束...");
		if (progressDialog != null) {
			handler.post(new Runnable(){
				@Override
				public void run() {
					progressDialog.dismiss();
				}
			});
		}
	}

}
