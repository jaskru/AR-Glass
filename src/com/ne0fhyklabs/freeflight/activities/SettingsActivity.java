package com.ne0fhyklabs.freeflight.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.service.DroneControlService;
import com.ne0fhyklabs.freeflight.settings.ApplicationSettings;
import com.ne0fhyklabs.freeflight.ui.SettingsDialogDelegate;


public class SettingsActivity extends FragmentActivity implements SettingsDialogDelegate {

    private DroneControlService mDroneControlService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDroneControlService = ((DroneControlService.LocalBinder) service).getService();
            if (mDroneControlService != null) {
                mDroneControlService.resume();
                mDroneControlService.requestDroneStatus();

                final FragmentManager fm = getSupportFragmentManager();
                Fragment settings = fm.findFragmentById(R.id.settings_screen);
                if (settings == null) {
                    settings = new SettingsDialog(SettingsActivity.this, SettingsActivity.this,
                            mDroneControlService, true);
                    fm.beginTransaction().add(R.id.settings_screen,
                            settings).commit();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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
    public void onDestroy(){
        super.onDestroy();
        unbindService(mConnection);
    }

    @Override
    public void prepareDialog(SettingsDialog dialog) {
        dialog.setAcceleroAvailable(true);
        dialog.setMagnetoAvailable(true);
        dialog.setFlying(false);
        dialog.enableAvailableSettings();
    }

    @Override
    public void onDismissed(SettingsDialog settingsDialog) {
        //Close the activity
        finish();
    }

    @Override
    public void onOptionChangedApp(SettingsDialog dialog, ApplicationSettings.EAppSettingProperty
            property, Object value) {

    }

    public DroneControlService getDroneControlService(){
        return mDroneControlService;
    }
}
