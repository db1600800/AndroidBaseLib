/**
 * 
 */
package com.chinapost.publiclibrary;

import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.chinapost.baselib.baseservice.BaseServiceInvoker;
import com.chinapost.baselib.utils.imei.ImeiUtils;
import com.chinapost.baselib.webservice.exception.ServerAddressException;

/**
 * @author jam
 * 2013-4-17 下午5:59:24
 */
public class DeviceTraceService extends Service {

	private LocationManager locationManager;
	
	private final LocationListener locateListener = new LocationListener() {
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
		
		@Override
		public void onProviderEnabled(String provider) {
		}
		
		@Override
		public void onProviderDisabled(String provider) {
		}
		
		@Override
		public void onLocationChanged(Location location) {
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			Log.d("上传地理位置", "latitude:" + latitude + " longitude:" + longitude);
			uploadLocation(latitude, longitude);
		}
	};
	
	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	

	@Override
	public void onStart(Intent intent, int startId) {
		Log.d("定位服务", "启动定位服务...");
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(false);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
        	uploadLocation(location.getLatitude(), location.getLongitude());
        }
        locationManager.requestLocationUpdates(provider, 2000, 10, locateListener);
		super.onStart(intent, startId);
	}



	/**
	 * 上传地理位置
	 * @param latitude
	 * @param longitude
	 */
	private void uploadLocation(final double latitude, final double longitude) {
		new Thread() {
			@Override
			public void run() {
				try {
					BaseServiceInvoker.locate(latitude, longitude, ImeiUtils.getImei(DeviceTraceService.this));
				} catch (ServerAddressException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				super.run();
			}
		}.start();
	}
}
