package com.ne0fhyklabs.freeflight;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.StrictMode;
import android.util.Log;
import com.ne0fhyklabs.freeflight.settings.ApplicationSettings;

public class FreeFlightApplication extends Application {
	private static final String TAG = "FreeFlightApplication";
    
	private ApplicationSettings settings;
	
	static {
		System.loadLibrary("avutil");
		System.loadLibrary("swscale");
		System.loadLibrary("avcodec");
		System.loadLibrary("avfilter");
		System.loadLibrary("avformat");
		System.loadLibrary("avdevice");
		System.loadLibrary("adfreeflight");
	}
	
	@SuppressLint("NewApi")
    @Override
	public void onCreate()
	{
		super.onCreate();
		Log.d(TAG, "OnCreate");

		settings = new ApplicationSettings(this);

        //If we're on debug mode, enable strict mode (android.os.StrictMode)
//        if (BuildConfig.DEBUG) {
//            Log.d(TAG, "Enabling strict mode");
//
//            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                    .detectAll().penaltyLog().build());
//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                    .detectAll().penaltyLog().build());
//        }
	}

	
	@Override
	public void onTerminate()
	{
		Log.d(TAG, "OnTerminate");
		super.onTerminate();
	}

	
	public ApplicationSettings getAppSettings()
	{
		return settings;
	}

}
