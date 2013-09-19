package com.parrot.freeflight.activities;

import android.annotation.SuppressLint;
import android.content.*;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.*;
import android.os.AsyncTask.Status;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.parrot.freeflight.R;
import com.parrot.freeflight.activities.base.DashboardActivityBase;
import com.parrot.freeflight.receivers.*;
import com.parrot.freeflight.service.DroneControlService;
import com.parrot.freeflight.service.intents.DroneStateManager;
import com.parrot.freeflight.tasks.CheckDroneNetworkAvailabilityTask;
import com.parrot.freeflight.tasks.CheckFirmwareTask;
import com.parrot.freeflight.tasks.CheckMediaAvailabilityTask;
import com.parrot.freeflight.transcodeservice.TranscodingService;
import com.parrot.freeflight.updater.FirmwareUpdateService;
import com.parrot.freeflight.utils.GPSHelper;

import java.io.File;

public class DashboardActivity extends DashboardActivityBase
        implements
        ServiceConnection,
        DroneAvailabilityDelegate,
        NetworkChangeReceiverDelegate,
        DroneFirmwareCheckReceiverDelegate,
        DroneConnectionChangeReceiverDelegate,
        MediaReadyDelegate {
    private DroneControlService mService;

    private BroadcastReceiver droneStateReceiver;
    private BroadcastReceiver networkChangeReceiver;
    private BroadcastReceiver droneFirmwareCheckReceiver;
    private BroadcastReceiver mediaReadyReceiver;
    private BroadcastReceiver droneConnectionChangeReceiver;

    private CheckMediaAvailabilityTask checkMediaTask;
    private CheckFirmwareTask checkFirmwareTask;
    private CheckDroneNetworkAvailabilityTask checkDroneConnectionTask;

    private boolean droneOnNetwork;
    private boolean firmwareUpdateAvailable;
    private EPhotoVideoState mediaState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        mediaState = EPhotoVideoState.UNKNOWN;
        initBroadcastReceivers();

        bindService(new Intent(this, DroneControlService.class), this, Context.BIND_AUTO_CREATE);

        if (GPSHelper.deviceSupportGPS(this) && !GPSHelper.isGpsOn(this)) {
            onNotifyAboutGPSDisabled();
        }
    }


    protected void initBroadcastReceivers() {
        droneStateReceiver = new DroneAvailabilityReceiver(this);
        networkChangeReceiver = new NetworkChangeReceiver(this);
        droneFirmwareCheckReceiver = new DroneFirmwareCheckReceiver(this);
        mediaReadyReceiver = new MediaReadyReceiver(this);
        droneConnectionChangeReceiver = new DroneConnectionChangedReceiver(this);
    }


    private void registerBroadcastReceivers() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance
                (getApplicationContext());
        broadcastManager.registerReceiver(droneStateReceiver, new IntentFilter(
                DroneStateManager.ACTION_DRONE_STATE_CHANGED));
        broadcastManager.registerReceiver(droneFirmwareCheckReceiver, new IntentFilter(
                DroneControlService.DRONE_FIRMWARE_CHECK_ACTION));

        IntentFilter mediaReadyFilter = new IntentFilter();
        mediaReadyFilter.addAction(DroneControlService.NEW_MEDIA_IS_AVAILABLE_ACTION);
        mediaReadyFilter.addAction(TranscodingService.NEW_MEDIA_IS_AVAILABLE_ACTION);
        broadcastManager.registerReceiver(mediaReadyReceiver, mediaReadyFilter);
        broadcastManager.registerReceiver(droneConnectionChangeReceiver,
                new IntentFilter(DroneControlService.DRONE_CONNECTION_CHANGED_ACTION));

        registerReceiver(networkChangeReceiver, new IntentFilter(WifiManager
                .NETWORK_STATE_CHANGED_ACTION));
    }


    private void unregisterReceivers() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance
                (getApplicationContext());
        broadcastManager.unregisterReceiver(droneStateReceiver);
        broadcastManager.unregisterReceiver(droneFirmwareCheckReceiver);
        broadcastManager.unregisterReceiver(mediaReadyReceiver);
        broadcastManager.unregisterReceiver(droneConnectionChangeReceiver);
        unregisterReceiver(networkChangeReceiver);
    }


    @Override
    protected void onDestroy() {
        unbindService(this);
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceivers();
        stopTasks();
    }


    @Override
    protected void onResume() {
        super.onResume();

        registerBroadcastReceivers();

        disableAllButtons();

        if (mService != null) {
            checkMedia();
        }
        checkDroneConnectivity();
    }


    private void disableAllButtons() {
        droneOnNetwork = false;
        firmwareUpdateAvailable = false;
        mediaState = EPhotoVideoState.NO_SDCARD;

        requestUpdateButtonsState();
    }


    @Override
    protected boolean onStartFreeflight() {
        if (!droneOnNetwork) {
            return false;
        }

        Intent connectActivity = new Intent(this, ConnectActivity.class);
        startActivity(connectActivity);

        return true;
    }


    @Override
    protected boolean onStartFirmwareUpdate() {
        if (firmwareUpdateAvailable) {
            if (mService != null && mService.isUSBInserted() && !FirmwareUpdateService.isRunning
                    ()) {
                onNotifyAboutUSBStickRemove();
            }
            else {
                Intent updatefirmwareActivity = new Intent(this, UpdateFirmwareActivity.class);
                startActivity(updatefirmwareActivity);
            }
        }

        return firmwareUpdateAvailable;
    }


    protected void onUSBStickRemoveDialogDismissed() {
        Intent updatefirmwareActivity = new Intent(this, UpdateFirmwareActivity.class);
        startActivity(updatefirmwareActivity);
    }


    @Override
    protected boolean onStartPhotosVideos() {
        Intent intent = new Intent(this, MediaActivity.class);
        startActivity(intent);

        return true;
    }


    public void onFirmwareChecked(boolean updateRequired) {
        firmwareUpdateAvailable = updateRequired;

        requestUpdateButtonsState();
    }


    public void onMediaReady(File mediaFile) {
        // Triggering check for new media if photo/video button is disabled
        // If new media is found this will result in enabling the button.
        if (!getPhotoVideoState().equals(EPhotoVideoState.READY)) {
            checkMedia();
        }
    }

    public void onNetworkChanged(NetworkInfo info) {
        Log.d(TAG, "Network state has changed. State is: " + (info.isConnected() ? "CONNECTED" :
                "DISCONNECTED"));

        if (mService != null && info.isConnected()) {
            checkDroneConnectivity();
        }
        else {
            firmwareUpdateAvailable = false;
            droneOnNetwork = false;
            requestUpdateButtonsState();
        }
    }


    public void onDroneConnected() {
        if (mService != null) {
            mService.pause();
        }
    }


    public void onDroneDisconnected() {
        // Left unimplemented
    }


    public void onDroneAvailabilityChanged(boolean droneOnNetwork) {
        if (droneOnNetwork) {
            Log.d(TAG, "AR.Drone connection [CONNECTED]");
            this.droneOnNetwork = droneOnNetwork;

            requestUpdateButtonsState();
            checkFirmware();
        }
        else {
            Log.d(TAG, "AR.Drone connection [DISCONNECTED]");
        }
    }


    private void checkFirmware() {
        if (checkFirmwareTask != null && checkFirmwareTask.getStatus() != Status.FINISHED) {
            checkFirmwareTask.cancel(true);
        }

        checkFirmwareTask = new CheckFirmwareTask(this) {
            @Override
            protected void onPostExecute(Boolean result) {
                onFirmwareChecked(result);
            }
        };

        checkFirmwareTask.execute();
    }


    @SuppressLint("NewApi")
    private void checkDroneConnectivity() {
        if (checkDroneConnectionTask != null && checkDroneConnectionTask.getStatus() != Status
                .FINISHED) {
            checkDroneConnectionTask.cancel(true);
        }

        checkDroneConnectionTask = new CheckDroneNetworkAvailabilityTask() {

            @Override
            protected void onPostExecute(Boolean result) {
                onDroneAvailabilityChanged(result);
            }

        };

        if (Build.VERSION.SDK_INT >= 11) {
            checkDroneConnectionTask.executeOnExecutor(CheckDroneNetworkAvailabilityTask
                    .THREAD_POOL_EXECUTOR, this);
        }
        else {
            checkDroneConnectionTask.execute(this);
        }
    }


    @Override
    public void onMediaStorageMounted() {
        checkMedia();
    }


    @Override
    public void onMediaStorageUnmounted() {
        checkMedia();
    }


    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((DroneControlService.LocalBinder) service).getService();

        File mediaDir = mService.getMediaDir();
        if (mediaDir == null) {
            mediaState = EPhotoVideoState.NO_SDCARD;
            requestUpdateButtonsState();
        }

        checkMedia();
    }


    public void onServiceDisconnected(ComponentName name) {
        // Left unimplemented
    }


    private void checkMedia() {
        if (mService != null) {
            String mediaStorageState = Environment.getExternalStorageState();

            if (!mediaStorageState.equals(Environment.MEDIA_MOUNTED) &&
                    !mediaStorageState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                mediaState = EPhotoVideoState.NO_SDCARD;
                requestUpdateButtonsState();
                return;
            }
            else {
                mediaState = EPhotoVideoState.NO_MEDIA;
            }
        }
        else {
            mediaState = EPhotoVideoState.NO_SDCARD;
            requestUpdateButtonsState();
            return;
        }


        if (!taskRunning(checkMediaTask)) {
            checkMediaTask = new CheckMediaAvailabilityTask(this) {
                @Override
                protected void onPostExecute(Boolean available) {
                    if (available) {
                        mediaState = EPhotoVideoState.READY;
                    }
                    else {
                        mediaState = EPhotoVideoState.NO_MEDIA;
                    }
                    requestUpdateButtonsState();
                }

            };

            checkMediaTask.execute();
        }
    }

    private boolean taskRunning(AsyncTask<?, ?, ?> checkMediaTask2) {
        return checkMediaTask2 != null && checkMediaTask2.getStatus() != Status.FINISHED;
    }


    private void stopTasks() {
        if (taskRunning(checkMediaTask)) {
            checkMediaTask.cancel(true);
        }

        if (taskRunning(checkFirmwareTask)) {
            checkFirmwareTask.cancel(true);
        }

        if (taskRunning(checkDroneConnectionTask)) {
            checkDroneConnectionTask.cancelAnyFtpOperation();
        }

    }

    @Override
    protected boolean isFreeFlightEnabled() {
        return droneOnNetwork;
    }

    @Override
    protected EPhotoVideoState getPhotoVideoState() {
        return mediaState;
    }

    @Override
    protected boolean isFirmwareUpdateEnabled() {
        return firmwareUpdateAvailable;
    }

    private void onNotifyAboutGPSDisabled() {
        showAlertDialog(getString(R.string.Location_services_alert),
                getString(R.string
                        .If_you_want_to_store_your_location_anc_access_your_media_enable_it),
                null);
    }

    private void onNotifyAboutUSBStickRemove() {
        Runnable actionOnDismiss = new Runnable() {
            @Override
            public void run() {
                onUSBStickRemoveDialogDismissed();
            }
        };

        showAlertDialog(getString(R.string.Update_Warning), getString(R.string.Please_unplug_your_USB_key_from_the_ARDrone_now), actionOnDismiss);
    }

}
