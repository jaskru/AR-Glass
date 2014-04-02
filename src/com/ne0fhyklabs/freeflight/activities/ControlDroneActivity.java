/*
 * ControlDroneActivity
 *
 * Created on: May 5, 2011
 * Author: Dmytro Baryskyy
 */

package com.ne0fhyklabs.freeflight.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;

import com.ne0fhyklabs.freeflight.FreeFlightApplication;
import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.controllers.Controller;
import com.ne0fhyklabs.freeflight.drone.DroneConfig;
import com.ne0fhyklabs.freeflight.drone.DroneConfig.EDroneVersion;
import com.ne0fhyklabs.freeflight.drone.NavData;
import com.ne0fhyklabs.freeflight.receivers.DroneBatteryChangedReceiver;
import com.ne0fhyklabs.freeflight.receivers.DroneBatteryChangedReceiverDelegate;
import com.ne0fhyklabs.freeflight.receivers.DroneCameraReadyActionReceiverDelegate;
import com.ne0fhyklabs.freeflight.receivers.DroneCameraReadyChangeReceiver;
import com.ne0fhyklabs.freeflight.receivers.DroneEmergencyChangeReceiver;
import com.ne0fhyklabs.freeflight.receivers.DroneEmergencyChangeReceiverDelegate;
import com.ne0fhyklabs.freeflight.receivers.DroneFlyingStateReceiver;
import com.ne0fhyklabs.freeflight.receivers.DroneFlyingStateReceiverDelegate;
import com.ne0fhyklabs.freeflight.receivers.DroneRecordReadyActionReceiverDelegate;
import com.ne0fhyklabs.freeflight.receivers.DroneRecordReadyChangeReceiver;
import com.ne0fhyklabs.freeflight.receivers.DroneVideoRecordStateReceiverDelegate;
import com.ne0fhyklabs.freeflight.receivers.DroneVideoRecordingStateReceiver;
import com.ne0fhyklabs.freeflight.receivers.WifiSignalStrengthChangedReceiver;
import com.ne0fhyklabs.freeflight.receivers.WifiSignalStrengthReceiverDelegate;
import com.ne0fhyklabs.freeflight.sensors.DeviceOrientationManager;
import com.ne0fhyklabs.freeflight.service.DroneControlService;
import com.ne0fhyklabs.freeflight.settings.ApplicationSettings;
import com.ne0fhyklabs.freeflight.settings.ApplicationSettings.ControlMode;
import com.ne0fhyklabs.freeflight.transcodeservice.TranscodingService;
import com.ne0fhyklabs.freeflight.ui.HudViewController;
import com.ne0fhyklabs.freeflight.ui.HudViewProxy;
import com.ne0fhyklabs.freeflight.utils.GlassUtils;

import java.io.File;

@SuppressLint("NewApi")
/**
 * TODO: Map the record functionality to a menu or glass button
 * TODO: Map the take photo functionality to a menu or glass button
 * TODO: Map takeoff/land functionality to ...
 */
public class ControlDroneActivity extends FragmentActivity implements
        WifiSignalStrengthReceiverDelegate,
        DroneVideoRecordStateReceiverDelegate, DroneEmergencyChangeReceiverDelegate,
        DroneBatteryChangedReceiverDelegate, DroneFlyingStateReceiverDelegate,
        DroneCameraReadyActionReceiverDelegate, DroneRecordReadyActionReceiverDelegate {

    private static final int LOW_DISK_SPACE_BYTES_LEFT = 1048576 * 20; // 20 mebabytes
    private static final int WARNING_MESSAGE_DISMISS_TIME = 5000; // 5 seconds
    private static final float MIN_TILT_ANGLE_THRESHOLD = (1f / 6f);

    private static final String TAG = ControlDroneActivity.class.getName();

    private DroneControlService droneControlService;
    private ApplicationSettings settings;

    /**
     * Primary controller is Google Glass. Might add option for others later.
     */
    private Controller mController;

    /**
     * User Interface instance fields
     */
    private HudViewController mDroneView;
    private HudViewProxy mHudProxy;

    private WifiSignalStrengthChangedReceiver wifiSignalReceiver;
    private DroneVideoRecordingStateReceiver videoRecordingStateReceiver;
    private DroneEmergencyChangeReceiver droneEmergencyReceiver;
    private DroneBatteryChangedReceiver droneBatteryReceiver;
    private DroneFlyingStateReceiver droneFlyingStateReceiver;
    private DroneCameraReadyChangeReceiver droneCameraReadyChangedReceiver;
    private DroneRecordReadyChangeReceiver droneRecordReadyChangeReceiver;

    private SoundPool soundPool;
    private int batterySoundId;
    private int effectsStreamId;

    private boolean controlLinkAvailable;

    private boolean flying;
    private boolean recording;
    private boolean cameraReady;
    private boolean mRecordingReady;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            droneControlService = ((DroneControlService.LocalBinder) service).getService();
            onDroneServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            droneControlService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_control_drone_screen);
        mDroneView = (HudViewController) findViewById(R.id.drone_view);
        mHudProxy = new HudViewProxy(this);

        settings = getSettings();
        bindService(new Intent(this, DroneControlService.class), mConnection,
                Context.BIND_AUTO_CREATE);

        wifiSignalReceiver = new WifiSignalStrengthChangedReceiver(this);
        videoRecordingStateReceiver = new DroneVideoRecordingStateReceiver(this);
        droneEmergencyReceiver = new DroneEmergencyChangeReceiver(this);
        droneBatteryReceiver = new DroneBatteryChangedReceiver(this);
        droneFlyingStateReceiver = new DroneFlyingStateReceiver(this);
        droneCameraReadyChangedReceiver = new DroneCameraReadyChangeReceiver(this);
        droneRecordReadyChangeReceiver = new DroneRecordReadyChangeReceiver(this);

        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        batterySoundId = soundPool.load(this, R.raw.battery, 1);

        /*
         * Initialize the controller
         */
        mController = Controller.ControllerType.GOOGLE_GLASS.getImpl(this);

        DeviceOrientationManager orientationManager = getControllerOrientationManager();

        if (orientationManager != null && !orientationManager.isAcceleroAvailable()) {
            settings.setControlMode(ControlMode.NORMAL_MODE);
        }

        settings.setFirstLaunch(false);
    }

    private void initController() {
        if (mController != null)
            mController.init();
    }

    public boolean isRecording(){
        return recording;
    }

    private DeviceOrientationManager getControllerOrientationManager() {
        DeviceOrientationManager orientationManager = null;
        if (mController != null)
            orientationManager = mController.getDeviceOrientationManager();

        return orientationManager;
    }

    private void destroyController() {
        if (mController != null)
            mController.destroy();
    }

    private void resumeController() {
        if (mController != null)
            mController.resume();
    }

    private void pauseController() {
        if (mController != null)
            mController.pause();
    }

    public void setDeviceOrientation(int heading, int accuracy) {
        if (droneControlService != null)
            droneControlService.setDeviceOrientation(heading, accuracy);
    }

    /**
     * Configuration method. Set the drone max tilt angle. Used by roll, and pitch.
     *
     * @param tilt
     */
    public void setDroneTilt(int tilt) {
        if (droneControlService != null) {
            if (tilt < DroneConfig.TILT_MIN)
                tilt = DroneConfig.TILT_MIN;
            else if (tilt > DroneConfig.TILT_MAX)
                tilt = DroneConfig.TILT_MAX;

            DroneConfig droneConfig = droneControlService.getDroneConfig();
            if (droneConfig != null && droneConfig.getTilt() != tilt) {
                droneConfig.setTilt(tilt);
            }

            if (mHudProxy != null) {
                mHudProxy.setPitchMax(tilt);
                mHudProxy.setPitchMin(-tilt);
                mHudProxy.setMaxRoll(tilt);
                mHudProxy.setMinRoll(-tilt);
            }
        }
    }

    public void setDroneRoll(float roll) {
        if (droneControlService != null) {
            if (Math.abs(roll) <= MIN_TILT_ANGLE_THRESHOLD)
                roll = 0;
            droneControlService.setRoll(roll);
        }
    }

    public void setDronePitch(float pitch) {
        if (droneControlService != null)
            if (Math.abs(pitch) <= MIN_TILT_ANGLE_THRESHOLD)
                pitch = 0;
        droneControlService.setPitch(pitch);
    }

    public void setDroneGaz(float gaz) {
        if (droneControlService != null) {
            droneControlService.setGaz(gaz);
        }
    }

    public void setDroneYaw(float yaw) {
        if (droneControlService != null) {
            droneControlService.setYaw(yaw);
        }
    }

    public void setDroneProgressiveCommandEnabled(boolean enable) {
        if (droneControlService != null)
            droneControlService.setProgressiveCommandEnabled(enable);
    }

    public void setDroneProgressiveCommandCombinedYawEnabled(boolean enable) {
        if (droneControlService != null)
            droneControlService.setProgressiveCommandCombinedYawEnabled(enable);
    }

    public void switchDroneCamera() {
        if (droneControlService != null)
            droneControlService.switchCamera();
    }

    public void triggerDroneTakeOff() {
        if (droneControlService != null)
            droneControlService.triggerTakeOff();
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mController != null && mController.onGenericMotion(findViewById(android.R.id
                .content), event) || super.onGenericMotionEvent(event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mController != null && mController.onKeyLongPress(keyCode,
                event) || super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mController != null && mController.onKeyDown(keyCode, event)
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mController != null && mController.onKeyUp(keyCode, event)
                || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mController != null && mController.onTouch(findViewById(android.R.id.content),
                event) || super.onTouchEvent(event);
    }

    public void doLeftFlip() {
        if (droneControlService != null)
            droneControlService.doLeftFlip();
    }

    @Override
    protected void onDestroy() {
        destroyController();
        unbindService(mConnection);

        if (mDroneView != null) {
            mDroneView.onDestroy();
        }

        soundPool.release();
        soundPool = null;

        super.onDestroy();
        Log.d(TAG, "ControlDroneActivity destroyed");
        System.gc();
    }

    private void registerReceivers() {
        // System wide receiver
        registerReceiver(wifiSignalReceiver, new IntentFilter(
                WifiManager.RSSI_CHANGED_ACTION));

        // Local receivers
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance
                (getApplicationContext());
        localBroadcastMgr.registerReceiver(videoRecordingStateReceiver, new IntentFilter(
                DroneControlService.VIDEO_RECORDING_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneEmergencyReceiver, new IntentFilter(
                DroneControlService.DRONE_EMERGENCY_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneBatteryReceiver, new IntentFilter(
                DroneControlService.DRONE_BATTERY_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneFlyingStateReceiver, new IntentFilter(
                DroneControlService.DRONE_FLYING_STATE_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneCameraReadyChangedReceiver,
                new IntentFilter(DroneControlService.CAMERA_READY_CHANGED_ACTION));
        localBroadcastMgr.registerReceiver(droneRecordReadyChangeReceiver,
                new IntentFilter(DroneControlService.RECORD_READY_CHANGED_ACTION));
    }

    private void unregisterReceivers() {
        // Unregistering system receiver
        unregisterReceiver(wifiSignalReceiver);

        // Unregistering local receivers
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager
                .getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(videoRecordingStateReceiver);
        localBroadcastMgr.unregisterReceiver(droneEmergencyReceiver);
        localBroadcastMgr.unregisterReceiver(droneBatteryReceiver);
        localBroadcastMgr.unregisterReceiver(droneFlyingStateReceiver);
        localBroadcastMgr.unregisterReceiver(droneCameraReadyChangedReceiver);
        localBroadcastMgr.unregisterReceiver(droneRecordReadyChangeReceiver);
    }

    /**
     * Used to update the current state based on the drone config.
     */
    private void updateDroneConfig() {
        //Update the view hud widgets
        final DroneConfig droneConfig = droneControlService.getDroneConfig();
        if (droneConfig != null) {
            setDroneTilt(droneConfig.getTilt());

            if (GlassUtils.instance$.isGlassDevice()) {
                // Reduce the live video stream resolution
                droneConfig.setVideoCodec(DroneConfig.H264_360P_CODEC);
            }
        }
    }

    @Override
    protected void onResume() {
        if (mDroneView != null) {
            mDroneView.onResume();
        }

        if (droneControlService != null) {
            droneControlService.resume();
        }

        registerReceivers();
        refreshWifiSignalStrength();

        // Start tracking device orientation
        resumeController();

        super.onResume();
    }

    @Override
    protected void onPause() {
        // Land parrot if it's flying
        if (isDroneFlying())
            triggerDroneTakeOff();

        if (mDroneView != null) {
            mDroneView.onPause();
        }

        if (droneControlService != null) {
            droneControlService.pause();
        }

        unregisterReceivers();

        pauseController();

        stopEmergencySound();

        System.gc();
        super.onPause();
    }

    /**
     * Called when we connected to DroneControlService
     */
    protected void onDroneServiceConnected() {
        if (droneControlService != null) {
            droneControlService.resume();
            droneControlService.requestDroneStatus();

            updateDroneConfig();

            mHudProxy.setIsFlying(flying);
            if (!flying) {
                //Perform a flat trim for the drone
                droneControlService.flatTrim();
            }
        } else {
            Log.w(TAG, "DroneServiceConnected event ignored as DroneControlService is null");
        }

        initController();
        runTranscoding();
        Log.d(TAG, "Transcoding completed");
    }

    @Override
    public void onDroneFlyingStateChanged(boolean flying) {
        this.flying = flying;
        if (mHudProxy != null)
            mHudProxy.setIsFlying(flying);

        if (GlassUtils.instance$.isGlassDevice())
            droneControlService.setProgressiveCommandEnabled(flying);
    }

    @Override
    @SuppressLint("NewApi")
    public void onDroneRecordReadyChanged(boolean ready) {
        mRecordingReady = recording || ready;
    }

    protected void onNotifyLowDiskSpace() {
        showWarningDialog(getString(R.string.your_device_is_low_on_disk_space),
                WARNING_MESSAGE_DISMISS_TIME);
    }

    protected void onNotifyLowUsbSpace() {
        showWarningDialog(getString(R.string.USB_drive_full_Please_connect_a_new_one),
                WARNING_MESSAGE_DISMISS_TIME);
    }

    protected void onNotifyNoMediaStorageAvailable() {
        showWarningDialog(getString(R.string.Please_insert_a_SD_card_in_your_Smartphone),
                WARNING_MESSAGE_DISMISS_TIME);
    }

    @Override
    public void onCameraReadyChanged(boolean ready) {
        cameraReady = ready;
    }

    @Override
    public void onDroneEmergencyChanged(int code) {
        mDroneView.setEmergency(code);

        if (code == NavData.ERROR_STATE_EMERGENCY_VBAT_LOW || code == NavData
                .ERROR_STATE_ALERT_VBAT_LOW) {
            playEmergencySound();
        } else {
            stopEmergencySound();
        }

        controlLinkAvailable = (code != NavData.ERROR_STATE_NAVDATA_CONNECTION);
        mHudProxy.enableRecording(controlLinkAvailable);
    }

    @Override
    public void onDroneBatteryChanged(int value) {
        mHudProxy.setBatteryValue(value);
    }

    @Override
    public void onWifiSignalStrengthChanged(int strength) {
        mHudProxy.setWifiValue(strength);
    }

    @Override
    public void onDroneRecordVideoStateChanged(boolean recording, boolean usbActive,
                                               int remaining) {
        if (droneControlService == null)
            return;

        boolean prevRecording = this.recording;
        this.recording = recording;

        mHudProxy.setRecording(recording);
        mHudProxy.setUsbIndicatorEnabled(usbActive);
        mHudProxy.setUsbRemainingTime(remaining);

        if (!recording) {
            if (prevRecording != recording && droneControlService != null
                    && droneControlService.getDroneVersion() == EDroneVersion.DRONE_1) {
                runTranscoding();
                showWarningDialog(getString(R.string
                                .Your_video_is_being_processed_Please_do_not_close_application),
                        WARNING_MESSAGE_DISMISS_TIME);
            }
        }

        if (prevRecording != recording) {
            if (usbActive && droneControlService.getDroneConfig().isRecordOnUsb() &&
                    remaining == 0) {
                onNotifyLowUsbSpace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (canGoBack()) {
            super.onBackPressed();
        }
    }

    private boolean canGoBack() {
        return !((flying || recording || !cameraReady) && controlLinkAvailable);
    }

    public int getDeviceTilt() {
        if (droneControlService == null)
            return DroneConfig.DEVICE_TILTMAX_MAX;

        DroneConfig droneConfig = droneControlService.getDroneConfig();
        if (droneConfig == null)
            return DroneConfig.DEVICE_TILTMAX_MAX;

        int deviceTilt = droneConfig.getDeviceTiltMax();
        if (deviceTilt < DroneConfig.DEVICE_TILTMAX_MIN)
            return DroneConfig.DEVICE_TILTMAX_MIN;
        else if (deviceTilt > DroneConfig.DEVICE_TILTMAX_MAX)
            return DroneConfig.DEVICE_TILTMAX_MAX;
        else
            return deviceTilt;
    }

    public int getDroneTilt() {
        if (droneControlService == null)
            return DroneConfig.INVALID_TILT;

        DroneConfig droneConfig = droneControlService.getDroneConfig();
        if (droneConfig == null)
            return DroneConfig.INVALID_TILT;

        return droneConfig.getTilt();
    }

    public EDroneVersion getDroneVersion() {
        if (droneControlService != null)
            droneControlService.getDroneVersion();

        return EDroneVersion.UNKNOWN;
    }

    public int getDroneYawSpeed() {
        if (droneControlService == null)
            return DroneConfig.YAW_MIN;

        DroneConfig droneConfig = droneControlService.getDroneConfig();
        if (droneConfig == null)
            return DroneConfig.YAW_MIN;

        int yawSpeed = droneConfig.getYawSpeedMax();
        if (yawSpeed < DroneConfig.YAW_MIN)
            return DroneConfig.YAW_MIN;
        else if (yawSpeed > DroneConfig.YAW_MAX)
            return DroneConfig.YAW_MAX;
        else
            return yawSpeed;
    }

    public HudViewProxy getHudView() {
        return mHudProxy;
    }

    public boolean isDroneFlying() {
        return flying;
    }

    public void setMagntoEnabled(boolean enable) {
        if (droneControlService != null)
            droneControlService.setMagnetoEnabled(enable);
    }

    public ApplicationSettings getSettings() {
        return ((FreeFlightApplication) getApplication()).getAppSettings();
    }

    public void refreshWifiSignalStrength() {
        WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int signalStrength = WifiManager.calculateSignalLevel(manager.getConnectionInfo().getRssi
                (), 4);
        onWifiSignalStrengthChanged(signalStrength);
    }

    private void showWarningDialog(final String message, final int forTime) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(message);

        if (prev != null) {
            return;
        }

        ft.addToBackStack(null);

        // Create and show the dialog.
        WarningDialog dialog = new WarningDialog();

        dialog.setMessage(message);
        dialog.setDismissAfter(forTime);
        dialog.show(ft, message);
    }

    private void playEmergencySound() {
        if (effectsStreamId != 0) {
            soundPool.stop(effectsStreamId);
            effectsStreamId = 0;
        }

        effectsStreamId = soundPool.play(batterySoundId, 1, 1, 1, -1, 1);
    }

    private void stopEmergencySound() {
        soundPool.stop(effectsStreamId);
        effectsStreamId = 0;
    }

    private void runTranscoding() {
        if (droneControlService.getDroneVersion() == EDroneVersion.DRONE_1) {
            File mediaDir = droneControlService.getMediaDir();

            if (mediaDir != null) {
                Intent transcodeIntent = new Intent(this, TranscodingService.class);
                transcodeIntent.putExtra(TranscodingService.EXTRA_MEDIA_PATH,
                        mediaDir.toString());
                startService(transcodeIntent);
            } else {
                Log.d(TAG, "Transcoding skipped SD card is missing.");
            }
        }
    }

    private boolean isLowOnDiskSpace() {
        boolean lowOnSpace = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            DroneConfig config = droneControlService.getDroneConfig();
            if (!recording && !config.isRecordOnUsb()) {
                File mediaDir = droneControlService.getMediaDir();
                long freeSpace = 0;

                if (mediaDir != null) {
                    freeSpace = mediaDir.getUsableSpace();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
                        && freeSpace < LOW_DISK_SPACE_BYTES_LEFT) {
                    lowOnSpace = true;
                }
            }
        } else {
            // TODO: Provide alternative implementation. Probably using StatFs
        }

        return lowOnSpace;
    }

    public void onRecord() {
        if (droneControlService != null) {
            DroneConfig droneConfig = droneControlService.getDroneConfig();

            boolean sdCardMounted = droneControlService.isMediaStorageAvailable();
            boolean recordingToUsb = droneConfig.isRecordOnUsb() &&
                    droneControlService.isUSBInserted();

            if (recording) {
                // Allow to stop recording
                droneControlService.record();
            } else {
                // Start recording
                if (!sdCardMounted) {
                    if (recordingToUsb) {
                        droneControlService.record();
                    } else {
                        onNotifyNoMediaStorageAvailable();
                    }
                } else {
                    if (!recordingToUsb && isLowOnDiskSpace()) {
                        onNotifyLowDiskSpace();
                    }

                    droneControlService.record();
                }
            }
        }
    }

    public void onTakePhoto() {
        if (droneControlService.isMediaStorageAvailable()) {
            mDroneView.playSoundEffect(SoundEffectConstants.CLICK);
            droneControlService.takePhoto();
        } else {
            onNotifyNoMediaStorageAvailable();
        }
    }

}