package com.locationtracker;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

/**
 * Service that detects the most accurate location in the background
 */
public class LocationTrackerService extends Service 
{
	public static final String TAG = "LOCATION_DETECTOR_SERVICE";
	private static LocationTracker LOCATION_TRACKER;
	private static StorageHelper STORAGE_HELPER;
		
	// ResultReceiver of Activity
	private ResultReceiver resultReceiver;
	
	//Handler object that will be passed to handle messages received from LocationTracker
	private final Handler handler = new Handler() 
	{
		@Override
		/**
		 * process the location data sent to service by LocationTracker
		 */
		public void handleMessage(Message msg) 
		{
			Log.d(TAG, "Receiving message......");
			LocationData locData = (LocationData) msg.obj;
			Log.d(TAG, "Received message from LocationTracker : \n" + locData.toString());
			
			Bundle bundle = new Bundle();
			bundle.putByteArray("LOCATION_DATA", locData.getBytes());
			resultReceiver.send(Constants.LOCATION_DATA_RESULT_CODE, bundle);
			
			//Using StorageHelper to write to file concurrently
			STORAGE_HELPER.write(locData.toCSVFormat());
		}
	};

	
	
	
	

	@Override
	/**
	 * Function called once when the service is created
	 */
	public void onCreate() 
	{
		Toast.makeText(this, "LocationDetectionService starting", Toast.LENGTH_SHORT).show();
		Log.d(TAG, "LocationDetectionService is starting");

		init();
	}

	/**
	 * Initialize the service
	 */
	private void init() 
	{
		// Initialize the LocationTracker
		Log.d(TAG, "initializing service..");
		LOCATION_TRACKER = LocationTracker.getLocationHelper(this, handler);
		Log.d(TAG, "initializing service.. successful");
		LOCATION_TRACKER.setupLocationDetection();
		Log.d(TAG, "initializing service.. setup");
		STORAGE_HELPER = StorageHelper.getStorageHelper(this);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		resultReceiver = (ResultReceiver) intent.getExtras().get(Constants.RECEIVER);
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) 
	{
		return null;
	}

	@Override
	/**
	 * Cleanly exit
	 */
	public void onDestroy() 
	{
		LOCATION_TRACKER.stop();
		STORAGE_HELPER.stop();
		Toast.makeText(this, "LocationDetectionService exiting", Toast.LENGTH_SHORT).show();
	}
}
