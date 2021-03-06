package com.ne0fhyklabs.freeflight.controllers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.activities.ControlDroneActivity;
import com.ne0fhyklabs.freeflight.drone.DroneConfig.EDroneVersion;
import com.ne0fhyklabs.freeflight.settings.ApplicationSettings;
import com.ne0fhyklabs.freeflight.ui.HudViewProxy;
import com.ne0fhyklabs.freeflight.utils.GlassUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EnhancedGestureDetector detects double tap gesture of second pointer when using multi touch in
 * addition to standard GestureDetector gestures.
 */
public class GoogleGlass extends Controller {
    private static final String TAG = GoogleGlass.class.getSimpleName();

    /*
    Only applicable from Android 2.3+ (https://developer.android
     .com/reference/android/hardware/SensorManager.html#registerListener(android.hardware
     .SensorEventListener, android.hardware.Sensor, int))
     */
    private static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL; //microseconds

    private static final int YAW_CONTROL_TRIGGER = 2;
    private static final float RAD_TO_DEG = (float) (180f / Math.PI);

    private boolean magnetoEnabled;
    private boolean mIsGlassMode;

    private final SoundPool mSoundPool;
    private final int mShutterSound;
    private final int mVideoStartSound;
    private final int mVideoStopSound;

    private final ApplicationSettings mSettings;
    private final GestureDetector mGestureDetector;
    private final AtomicBoolean mYawControlEnabled = new AtomicBoolean(false);

    private HudViewProxy mHudView;

    private int mDeviceTiltMax;
    private int mDroneYawSpeed;

    private final SensorManager mSensorManager;
    private final SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (mDroneControl == null || !mIsGlassMode || !isActive()) {
                return;
            }

            switch (event.sensor.getType()) {
                case Sensor.TYPE_GRAVITY:
                    // Angle are in degrees
                    float rollAngle = RAD_TO_DEG * computeRollOrientation(event);
                    float rollRatio = rollAngle / mDeviceTiltMax;

                    if (rollRatio > 1f)
                        rollRatio = 1f;
                    else if (rollRatio < -1f)
                        rollRatio = -1f;

                    float pitchAngle = RAD_TO_DEG * computePitchOrientation(event);
                    float pitchRatio = pitchAngle / mDeviceTiltMax;

                    if (pitchRatio > 1f)
                        pitchRatio = 1f;
                    else if (pitchRatio < -1f)
                        pitchRatio = -1f;

                    // Set the tilt angle
                    mDroneControl.setDroneRoll(rollRatio);
                    mDroneControl.setDronePitch(pitchRatio);

                    mHudView.setPitchRoll(pitchRatio, -rollRatio);
                    break;

                case Sensor.TYPE_GYROSCOPE:
                    if(mYawControlEnabled.get()){
                        //Get the rotation speed around the y axis.
                        float angRadSpeed = event.values[1];
                        float angDegSpeed = RAD_TO_DEG * angRadSpeed;

                        float angSpeedRatio = (angDegSpeed *4) / mDroneYawSpeed;

                        if(angSpeedRatio > 1f)
                            angSpeedRatio = 1f;
                        else if(angSpeedRatio < -1f)
                            angSpeedRatio = -1f;

                        //Set the yaw speed
                        mDroneControl.setDroneYaw(-angSpeedRatio);
                    }
                    break;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        /**
         * Compute the orientation angle.
         *
         * @param event
         *            Gravity values.
         */
        private float computeRollOrientation(SensorEvent event) {
            return (float) -Math.atan(
                    event.values[0] / Math.sqrt(event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]));
        }

        private float computePitchOrientation(SensorEvent event) {
            return (float) -Math.atan(
                    event.values[2] / Math.sqrt(event.values[1] * event.values[1] +
                            event.values[0] * event.values[0]));
        }

    };

    GoogleGlass(final ControlDroneActivity droneControl) {
        super(droneControl);

        final Context context = droneControl.getApplicationContext();

        mSoundPool = new SoundPool(3, AudioManager.STREAM_NOTIFICATION, 0);
        mShutterSound = mSoundPool.load(context, R.raw.sound_photo_shutter, 1);
        mVideoStartSound = mSoundPool.load(context, R.raw.sound_video_start, 1);
        mVideoStopSound = mSoundPool.load(context, R.raw.sound_video_stop, 1);

        mIsGlassMode = true;
        mSensorManager = (SensorManager) droneControl.getSystemService(Context.SENSOR_SERVICE);

        mSettings = droneControl.getSettings();

        mGestureDetector = new GestureDetector(context);
        mGestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                switch (gesture) {
                    case SWIPE_UP:
                        if (mSettings.isFlipEnabled()) {
                            //Do a flip
                            mDroneControl.doLeftFlip();
                        }
                        return true;

                    case TAP:
                        //For now, take off, or land, but update to launch the menu instead
                        mDroneControl.openOptionsMenu();
                        return true;
                }
                return false;
            }
        });

        mGestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                if (delta >= 0)
                    mDroneControl.setDroneGaz(1f);
                else
                    mDroneControl.setDroneGaz(-1f);
                return true;
            }
        });

        mGestureDetector.setFingerListener(new GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {
                Log.i(TAG, "Current count: " + currentCount);
                //Activate yaw control when two fingers
                if (currentCount == YAW_CONTROL_TRIGGER){
                    mYawControlEnabled.set(true);
                }
                else {
                    mYawControlEnabled.set(false);
                    mDroneControl.setDroneYaw(0);

                    if (currentCount == 0) {
                        //Set the gaz back to zero
                        mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(false);
                        mDroneControl.setDroneGaz(0);
                    }
                }
            }
        });
    }

    public void setGlassMode(boolean glassMode) {
        mIsGlassMode = glassMode;
    }

    public boolean getGlassMode() {
        return mIsGlassMode;
    }

    @Override
    protected boolean initImpl() {
        Log.v(TAG, "Initializing Google Glass controller");
        if (!GlassUtils.instance$.isGlassDevice())
            return false;

        magnetoEnabled = mSettings.isAbsoluteControlEnabled();
        if (magnetoEnabled) {
            if (mDroneControl.getDroneVersion() == EDroneVersion.DRONE_1) {
                // Drone 1 doesn't have compass, so we need to switch magneto
                // off.
                magnetoEnabled = false;
                mSettings.setAbsoluteControlEnabled(false);
            }
        }

        mDroneControl.setMagntoEnabled(magnetoEnabled);
        return true;
    }

    @Override
    protected boolean onGenericMotionImpl(View view, MotionEvent event) {
        mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(true);
        return mGestureDetector.onMotionEvent(event);
    }

    @Override
    protected boolean onKeyDownImpl(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            event.startTracking();
            return true;
        }

        return false;
    }

    @Override
    protected boolean onKeyLongPressImpl(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            mDroneControl.onRecord();
            mSoundPool.play(mVideoStartSound, 1f, 1f, 0, 0, 1);
            return true;
        }

        return false;
    }

    @Override
    protected boolean onKeyUpImpl(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            final boolean isLongPress = (event.getFlags() & KeyEvent.FLAG_CANCELED_LONG_PRESS) == KeyEvent
                    .FLAG_CANCELED_LONG_PRESS;
            if (!isLongPress) {
                if(mDroneControl.isRecording()) {
                    mDroneControl.onRecord();
                    mSoundPool.play(mVideoStopSound, 1f, 1f, 0, 0, 1);
                }
                else {
                    mSoundPool.play(mShutterSound, 1f, 1f, 0, 0, 1);
                    mDroneControl.onTakePhoto();
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchImpl(View view, MotionEvent ev) {
        return false;
    }

    /**
     * Restore the google glass drone settings
     * {@inheritDoc}
     */
    @Override
    protected void resumeImpl() {
        resetControls(false);

        mHudView = mDroneControl.getHudView();
        mDeviceTiltMax = mDroneControl.getDeviceTilt();
        mDroneYawSpeed = mDroneControl.getDroneYawSpeed();
        registerListeners();
    }

    @Override
    protected void pauseImpl() {
        unregisterListeners();
        resetControls(true);
    }

    /**
     * Used to reset the drone control.
     */
    private void resetControls(boolean cutGaz) {
        if(mDroneControl != null){
            mDroneControl.setDronePitch(0);
            mDroneControl.setDroneRoll(0);
            mDroneControl.setDroneYaw(0);
            if (cutGaz)
                mDroneControl.setDroneGaz(0);

            final HudViewProxy hud = mDroneControl.getHudView();
            if(hud != null){
                hud.resetPitchRoll();
            }
        }
    }

    @Override
    protected void destroyImpl() {
        unregisterListeners();
        resetControls(true);
    }

    /**
     * Register this activity as a listener for gravity sensor changes.
     */
    private void registerListeners() {
        final int sensorDelay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
                ? SENSOR_DELAY
                : SensorManager.SENSOR_DELAY_NORMAL;

        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), sensorDelay);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sensorDelay);
    }

    /**
     * Unregister this activity as a listener.
     */
    private void unregisterListeners() {
        mSensorManager.unregisterListener(mSensorListener);
    }

}