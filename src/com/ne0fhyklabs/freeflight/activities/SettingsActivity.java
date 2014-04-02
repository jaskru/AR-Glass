package com.ne0fhyklabs.freeflight.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;

import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.fragments.SettingsFragment;
import com.ne0fhyklabs.freeflight.receivers.DroneReadyReceiver;
import com.ne0fhyklabs.freeflight.receivers.DroneReadyReceiverDelegate;
import com.ne0fhyklabs.freeflight.service.DroneControlService;

public class SettingsActivity extends FragmentActivity {

    private DroneControlService mDroneControlService;
    private Fragment mSettingsFragment;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDroneControlService = ((DroneControlService.LocalBinder) service).getService();
            if (mDroneControlService != null) {
                mDroneControlService.resume();
                mDroneControlService.requestDroneStatus();

                final FragmentManager fm = getFragmentManager();
                mSettingsFragment = fm.findFragmentById(R.id.settings_screen);
                if (mSettingsFragment == null) {
                    mSettingsFragment = new SettingsFragment();
                    fm.beginTransaction().add(R.id.settings_screen, mSettingsFragment).commit();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            removeSettingsFragment();
            mDroneControlService = null;
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        bindService(new Intent(this, DroneControlService.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mDroneControlService != null)
            mDroneControlService.resume();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mDroneControlService != null)
            mDroneControlService.pause();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        unbindService(mConnection);
    }

    public DroneControlService getDroneControlService(){
        return mDroneControlService;
    }

    private void removeSettingsFragment() {
        //Remove the settings fragment
        getFragmentManager().beginTransaction().remove(mSettingsFragment).commit();
    }
}
