package com.locationtracker; 

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

@SuppressLint("NewApi") 
public class LocationTrackerActivity extends Activity implements OnClickListener
{
	private static final String TAG = "LocationDetectionActivity";
	private Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
	private static float text_size;
	private LocationData currentLocation = null;
	
	// Handler to pass to LocationTracker	 
	private final Handler handler = new Handler() {
		@Override
		/**
		 * process the location data sent to Activity by Service
		 */
		public void handleMessage(Message msg) {
			LocationData locData = (LocationData) msg.obj;
		}
	};
	
	// UI elements placeholders
	private ImageButton b1;
	private ImageButton b2;
	private ImageButton b3;
	private static final int START_BUTTON = R.id.imageButton1;
	private static final int STOP_BUTTON = R.id.imageButton2;
	private static final int EXIT_BUTTON = R.id.imageButton3;
	
	
	
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);	
		Log.d(TAG, "Started LocationDetectorActivity");
		
		setLayoutAndTitle();
		
		//Set On-Click Listeners to UI Elements
		b1 = (ImageButton)findViewById(START_BUTTON);
		b2 = (ImageButton)findViewById(STOP_BUTTON);
		b3 = (ImageButton)findViewById(EXIT_BUTTON);
		b1.setOnClickListener(this);
		b2.setOnClickListener(this);
		b3.setOnClickListener(this);
		
	}

	private void setLayoutAndTitle() {
		// Set up the window layout
		setContentView(R.layout.main);	
	
		/*
		 * Extracts device screen parameters to customize UI appearance
		 */
		int Measuredwidth = 0;  
		int Measuredheight = 0;  
		Point size = new Point();
		WindowManager w = getWindowManager();

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
		      w.getDefaultDisplay().getSize(size);
		      Measuredwidth = size.x;
		      Measuredheight = size.y; 
		    }else{
		      Display d = w.getDefaultDisplay(); 
		      Measuredwidth = d.getWidth(); 
		      Measuredheight = d.getHeight(); 
		    }
		
		text_size = Measuredwidth/35.0f; 
	}
	
	// Resume activity and start updating UI
	@Override
	protected void onResume() {
		super.onResume();
		if(currentLocation != null){
			updateLocationDataOnUI(currentLocation);
		}
	}
	
	 @Override
	    public void onBackPressed() {
		 //Do nothing when user by-mistake hard-kills
	 }

	private void exitApplication()
	{
		this.finish();
	}

	
	

	 

	@Override
	public void onClick(View v) 
	{
		switch (v.getId()) 
		{
		
		case START_BUTTON:
			if(!isServiceRunning(LocationTrackerService.class))
			{
				b1.setBackgroundResource(R.drawable.start_disable);
				b2.setBackgroundResource(R.drawable.stop_button);
				
				// UI Feedback
				TextView v1 = (TextView) findViewById(R.id.textView1);
				v1.setText("Please wait.. \n\n");
				v1.setTextSize(TypedValue.COMPLEX_UNIT_SP,text_size+5.0f );
				
				Intent intent = new Intent(this, LocationTrackerService.class);
				intent.putExtra(Constants.RECEIVER, new MyResultReceiver(null));
				startService(intent);
			}
			else
			{
				Log.d(TAG,"Service is already running");
				
			}
			break;
			
		case STOP_BUTTON:
			if (isServiceRunning(LocationTrackerService.class)) 
			{
				Log.d(TAG,"STOP BUTTON clicked");
				b2.setBackgroundResource(R.drawable.stop_disable);
				b1.setBackgroundResource(R.drawable.start_button);
				
				
				// UI Feedback
				TextView v1 = (TextView) findViewById(R.id.textView1);
				v1.setText("(Stopped) \n\n");
				v1.setTextSize(TypedValue.COMPLEX_UNIT_SP,text_size+5.0f );
				
				stopService(new Intent(this, LocationTrackerService.class));
			}
			else
			{
				Log.d(TAG,"Service is already stopped");
				
			}
			break;
			
		case EXIT_BUTTON:
			
			//Cleanly exit
			if (isServiceRunning(LocationTrackerService.class)) 
			{
				stopService(new Intent(this, LocationTrackerService.class));
			}
			else
			{
				
			}
			
			new AlertDialog.Builder(this)
		    .setTitle("Quit Application")
		    .setMessage("Are you sure you want to quit the application?")
		    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        	exitApplication();
		        }
		     })
		    .setNegativeButton("No", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            // do nothing
		        }
		     })
		     .show();
			
			
			
		}
	}
	
	

	/**
	 * Handle the message passing between activity and service 
	 */
	class MyResultReceiver extends ResultReceiver
	{

		public MyResultReceiver(Handler handler) 
		{
			super(handler);
		}
		
		@Override
	    protected void onReceiveResult(int resultCode, Bundle resultData) 
		{
			Log.d(TAG, "Receiced Result: " + resultCode);
			byte[] data = (byte[]) resultData.get("LOCATION_DATA");
			LocationData locData = LocationData.getFromBytes(data);
			Log.d(TAG, "LocationData from Service: " + locData.toString());
			currentLocation = locData;
			updateLocationDataOnUI(locData);
	    }
	}
	
	/**
	 * processes location data received and makes concurrent updates on UI
	 * @param locData
	 */
	private void updateLocationDataOnUI(LocationData locData)
	{
		final LocationData locationData = locData;
		runOnUiThread(new Runnable() 
		{
			@Override
			public void run() 
			{			
				TextView v1 = (TextView) findViewById(R.id.textView1);
				v1.setText("TIME  :  "+ format.format(new Date(locationData.getTime())).toString()+"\n\n");
				v1.setTextSize(TypedValue.COMPLEX_UNIT_SP,text_size+5.0f );
				
				TextView v2 = (TextView) findViewById(R.id.textView2);
				v2.setText("\t\t\tLATITUDE\t\t\t:\t\t\t"+locationData.getLatitude());
				v2.setTextSize(TypedValue.COMPLEX_UNIT_SP,text_size );
				
				TextView v3 = (TextView) findViewById(R.id.textView3);
				v3.setText("\t\t\tLONGITUDE\t\t:\t\t\t"+locationData.getLongitude());
				v3.setTextSize(TypedValue.COMPLEX_UNIT_SP,text_size );
				
				TextView v4 = (TextView) findViewById(R.id.textView4);
				v4.setText("\t\t\tBEARING \t\t\t:\t\t\t"+locationData.getBearing()+" degrees from North");
				v4.setTextSize(TypedValue.COMPLEX_UNIT_SP,text_size );
				
				TextView v5 = (TextView) findViewById(R.id.textView5);
				v5.setText("\t\t\tSPEED\t\t\t\t\t:\t\t\t"+locationData.getSpeed()+" m/sec");
				v5.setTextSize(TypedValue.COMPLEX_UNIT_SP,text_size );
				
				TextView v6 = (TextView) findViewById(R.id.textView6);
				v6.setText("\t\t\tPROVIDER\t\t\t:\t\t\t"+locationData.getProvider());
				v6.setTextSize(TypedValue.COMPLEX_UNIT_SP,text_size );
			}
		});
	}
	
	
	/**
	 * checks if a service is running in the background
	 * @param serviceClass Class of the service being queried
	 * @return true is service is running
	 */
	private boolean isServiceRunning(Class serviceClass) 
	{
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) 
		{
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}


	 
}
