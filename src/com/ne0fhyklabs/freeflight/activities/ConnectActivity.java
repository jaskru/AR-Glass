
package com.ne0fhyklabs.freeflight.activities;

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
import android.util.Log;
import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.receivers.DroneConnectionChangeReceiverDelegate;
import com.ne0fhyklabs.freeflight.receivers.DroneConnectionChangedReceiver;
import com.ne0fhyklabs.freeflight.receivers.DroneReadyReceiver;
import com.ne0fhyklabs.freeflight.receivers.DroneReadyReceiverDelegate;
import com.ne0fhyklabs.freeflight.service.DroneControlService;

import java.util.Random;

public class ConnectActivity extends FragmentActivity implements ServiceConnection, DroneReadyReceiverDelegate,
        DroneConnectionChangeReceiverDelegate {

    private static final int[] TIPS = {
            R.layout.hint_screen_joypad_mode, R.layout.hint_screen_absolute_control,
            R.layout.hint_screen_record,
            R.layout.hint_screen_usb, R.layout.hint_screen_switch,
            R.layout.hint_screen_landing, R.layout.hint_screen_take_off,
            R.layout.hint_screen_emergency,
            R.layout.hint_screen_altitude, R.layout.hint_screen_hovering,
            // R.layout.hint_screen_geolocation,
            R.layout.hint_screen_share, R.layout.hint_screen_flip
    };

    private static final String TAG = ConnectActivity.class.getSimpleName();

    private DroneControlService mService;

    private BroadcastReceiver droneReadyReceiver;
    private BroadcastReceiver droneConnectionChangeReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Random random = new Random(System.currentTimeMillis());
        int tipNumber = random.nextInt(TIPS.length);

        setContentView(TIPS[tipNumber]);

        droneReadyReceiver = new DroneReadyReceiver(this);
        droneConnectionChangeReceiver = new DroneConnectionChangedReceiver(this);

        bindService(new Intent(this, DroneControlService.class), this, Context.BIND_AUTO_CREATE);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(this);
        Log.d(TAG, "Connect activity destroyed");
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mService != null) {
            mService.pause();
        }

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
        manager.unregisterReceiver(droneReadyReceiver);
        manager.unregisterReceiver(droneConnectionChangeReceiver);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (mService != null)
            mService.resume();

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
        manager.registerReceiver(droneReadyReceiver, new IntentFilter(DroneControlService
                .DRONE_STATE_READY_ACTION));
        manager.registerReceiver(droneConnectionChangeReceiver, new IntentFilter(
                DroneControlService.DRONE_CONNECTION_CHANGED_ACTION));
    }


    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((DroneControlService.LocalBinder) service).getService();

        mService.resume();
        mService.requestDroneStatus();
    }


    private void onOpenHudScreen() {
        Intent droneControlActivity = new Intent(ConnectActivity.this, ControlDroneActivity.class);
        droneControlActivity.putExtra("USE_SOFTWARE_RENDERING", false);
        droneControlActivity.putExtra("FORCE_COMBINED_CONTROL_MODE", false);
        startActivity(droneControlActivity);
    }


    public void onDroneConnected() {
        // We still waiting for onDroneReady event
        mService.requestConfigUpdate();
    }


    public void onDroneReady() {
        onOpenHudScreen();
    }


    public void onDroneDisconnected() {
        // Left unimplemented
    }


    public void onServiceDisconnected(ComponentName name) {
        // Left unimplemented
    }


}
