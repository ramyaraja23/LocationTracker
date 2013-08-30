package com.locationtracker;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

/**
 * Manages location tracking setup and operation
 */
public class LocationTracker {

	private static final String TAG = "LOCATION_TRACKER";
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	private LocationData currentBestLocation;
	private LocationManager mLocationManager; // reference to LocationManager
	private MyLocationListener myLocationListener; // reference to LocationListener
	private Context context; 	
	private Handler handler;	
	
	
	

	
	private static LocationTracker LOCATION_TRACKER = null;
	
	public static LocationTracker getLocationHelper(Context context, Handler handler)
	{
		if(LOCATION_TRACKER == null)
		{
			return new LocationTracker(context, handler);
		}
		return LOCATION_TRACKER;
	}
	
	/**
	 * Singleton constructor. Also used to initialize settings
	 * @param context
	 * @param handler
	 */
	private LocationTracker(Context context, Handler handler){
		this.context = context;
		this.handler = handler;		// Receives handler object from Service
		
		// Initialize LocationManager
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		// is GPS enabled?
		final boolean gpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);	
		// is Network_Provider enabled?
		final boolean networkProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		// if GPS is not available .. priority to GPS
		if (!gpsEnabled) 
		{
			Log.d(TAG, "Enabling Location Detection");
			enableLocationSettings();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * start the location source settings activity to let the user set network providers
	 */
	private void enableLocationSettings() 
	{
		Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.context.startActivity(settingsIntent);
	}
	
	/**
	 * Request location updates from Network Providers
	 */
	public void setupLocationDetection() 
	{
		Log.d(TAG, "Setting up Location Detection");
		myLocationListener = new MyLocationListener();
		
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);
		/* 
		 * Gets and sets initial location estimate from cache 
		 * Or sets default initial location for Android versions that cannot call getLastKnownLocation() 
		 */
		Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (lastKnownLocation != null) {
			currentBestLocation = new LocationData(
					lastKnownLocation.getLatitude(),
					lastKnownLocation.getLongitude(),
					lastKnownLocation.getAccuracy(),
					lastKnownLocation.getBearing(),
					lastKnownLocation.getSpeed(), lastKnownLocation.getTime(),
					lastKnownLocation.getProvider());

					sendMessageToService(currentBestLocation);
		}
		else
		{
			currentBestLocation = new LocationData(0, 0, 0, 0, 0, 0, null);
		}
		
	}
	
	
	/**
	 * Listens for changes in location
	 */
	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			// If the new location is a better location estimate than the current best location send the location to the service
			if(isBetterLocation(loc, currentBestLocation)){
				LocationData locData = new LocationData(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy(), 
						loc.getBearing(), loc.getSpeed(), loc.getTime(), loc.getProvider());
				Log.d(TAG, "Location Data: " + locData.toString());
				
				// If the new location is significantly different from last-known location data
				if(isSignificantlyDifferent(loc, currentBestLocation))
				{
					sendMessageToService(locData);
					currentBestLocation = locData;
				}
			}
		}

		@Override
		public void onProviderDisabled(String provider) {

		}

		@Override
		public void onProviderEnabled(String provider) {

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

	}
	
	/**
	 * stop Location Tracker
	 */
	public void stop(){
		mLocationManager.removeUpdates(myLocationListener);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Following function sends location data as a message to Service handler object
	 * @param locData
	 */
	private void sendMessageToService(LocationData locData){
		Message msg = Message.obtain();
		msg.obj = locData;      
		handler.sendMessage(msg);
	}
	
	
	
	
	/* The following code to get better location estimates is from the sample code at
	 * http://developer.android.com/guide/topics/location/strategies.html
	 */

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location, LocationData currentBestLocation) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/*
	 * Currently accurate location data are obtained almost every second and updated to the application and user
	 * Following function optimizes this by specifying a threshold value for how different the new location data must be
	 * to be considered as an updated value.
	 */
	
   /** 
    * Returns if the new location data is significantly different or not
	 * @param location - Newly obtained location data
	 * @param currentBestLocation - Previous, recentmost location that has been recorded to be significant
	 * @return 
	 */
	private boolean isSignificantlyDifferent(Location location, LocationData currentBestLocation){
		if(distFrom(location.getLatitude(), location.getLongitude(), currentBestLocation.getLatitude(), 
				currentBestLocation.getLongitude()) >= Constants.THRESHOLD_DISTANCE){
			return true;
		}
		return false;
	}
	
	
	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	
	/**
	 * Source: http://stackoverflow.com/questions/120283/working-with-latitude-longitude-values-in-java00
	 * @param latitude1
	 * @param longitude1
	 * @param latitude2
	 * @param longitude2
	 * @return distance along earth between (latitude1, longitude1) and (latitude2, longitude2)
	 */
	private static double distFrom(double latitude1, double longitude1, double latitude2, double longitude2) {
	    double earthRadius = 3958.75;
	    double dLat = Math.toRadians(latitude2-latitude1);
	    double dLng = Math.toRadians(longitude2-longitude1);
	    double sindLat = Math.sin(dLat / 2);
	    double sindLng = Math.sin(dLng / 2);
	    double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
	            * Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2));
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    return dist;
	}

}
