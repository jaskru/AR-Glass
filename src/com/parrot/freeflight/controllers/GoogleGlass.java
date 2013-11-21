package com.parrot.freeflight.controllers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.parrot.freeflight.activities.ControlDroneActivity;
import com.parrot.freeflight.drone.DroneConfig;
import com.parrot.freeflight.drone.DroneConfig.EDroneVersion;
import com.parrot.freeflight.sensors.DeviceOrientationManager;
import com.parrot.freeflight.settings.ApplicationSettings;
import com.parrot.freeflight.ui.hud.Sprite;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * EnhancedGestureDetector detects double tap gesture of second pointer when using multi touch in
 * addition to
 * standard GestureDetecror gestures.
 */
public class GoogleGlass extends Controller {
    private static final String TAG = GoogleGlass.class.getSimpleName();

    private static final int YAW_CONTROL_TRIGGER = 2;
    private static final int TILT_SCROLL_DAMPER = 20;
    private static final int GLASS_DRONE_TILT = 10;
    private static final int GLASS_YAW_SPEED = DroneConfig.YAW_MAX;
    private static final float RAD_TO_DEG = (float) (180f / Math.PI);

    private int mTiltMax = DroneConfig.TILT_MAX;
    private int mYawMax = DroneConfig.YAW_MAX;

    private boolean magnetoEnabled;
    private boolean mIsGlassMode;

    private final ApplicationSettings mSettings;
    private final GestureDetector mGestureDetector;
    private final AtomicBoolean mYawControlEnabled = new AtomicBoolean(false);

    private final SensorManager mSensorManager;
    private final SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (mDroneControl == null || !mIsGlassMode) {
                return;
            }

            switch (event.sensor.getType()) {
                case Sensor.TYPE_GRAVITY:
                    int referenceTilt = DroneConfig.TILT_MAX;//Math.min(mTiltMax *2,
//                        DroneConfig.TILT_MAX);

                    // Angle are in degrees
                    final float minRatio = (1f / 6f);

                    float rollAngle = RAD_TO_DEG * computeRollOrientation(event);
                    float rollRatio = rollAngle / referenceTilt;

                    if (rollRatio > 1f)
                        rollRatio = 1f;
                    else if (rollRatio < -1f)
                        rollRatio = -1f;
                    else if (Math.abs(rollRatio) <= minRatio)
                        rollRatio = 0;

                    float pitchAngle = RAD_TO_DEG * computePitchOrientation(event);
                    float pitchRatio = pitchAngle / referenceTilt;

                    if (pitchRatio > 1f)
                        pitchRatio = 1f;
                    else if (pitchRatio < -1f)
                        pitchRatio = -1f;
                    else if (Math.abs(pitchRatio) <= minRatio)
                        pitchRatio = 0;

                    // Set the tilt angle
                    mDroneControl.setDroneRoll(rollRatio);
                    mDroneControl.setDronePitch(pitchRatio);
                    break;

                case Sensor.TYPE_GYROSCOPE:
                    if(mYawControlEnabled.get()){
                        //Get the rotation speed around the y axis.
                        final int yawReference = DroneConfig.YAW_MAX;//Math.min(mYawMax * 2,
//                                DroneConfig.YAW_MAX);
                        float angRadSpeed = event.values[1];
                        float angDegSpeed = RAD_TO_DEG * angRadSpeed;

//                        Log.i(TAG, "Angular speed: " + angDegSpeed);
                        float angSpeedRatio = angDegSpeed / yawReference;

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
            // TODO Auto-generated method stub

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

        mIsGlassMode = true;
        mSensorManager = (SensorManager) droneControl
                .getSystemService(Context.SENSOR_SERVICE);

        mSettings = droneControl.getSettings();

        mGestureDetector = new GestureDetector(droneControl.getApplicationContext());
        mGestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                switch (gesture) {
                    case TWO_TAP:
                        //Do a flip
                        mDroneControl.doLeftFlip();
                        Log.i(TAG, "Double tap event");
                        return true;

                    case TAP:
                        //For now, take off, or land, but update to launch the menu instead
                        mDroneControl.triggerDroneTakeOff();
                        Log.i(TAG, "Tap event");
                        return true;
                }
                return false;
            }
        });

        mGestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                //TODO: Check this is correct.
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

        mGestureDetector.setTwoFingerScrollListener(new GestureDetector.TwoFingerScrollListener() {
            @Override
            public boolean onTwoFingerScroll(float displacement, float delta, float velocity) {
                if (Math.abs(delta) >= TILT_SCROLL_DAMPER) {

                    //Update the drone speed.
                    int currentTilt = mDroneControl.getDroneTilt();
                    if (currentTilt != DroneConfig.INVALID_TILT) {
                        int newTilt = currentTilt + (int) (delta / TILT_SCROLL_DAMPER);

                        if (newTilt > DroneConfig.TILT_MAX)
                            newTilt = DroneConfig.TILT_MAX;
                        else if (newTilt < DroneConfig.TILT_MIN)
                            newTilt = DroneConfig.TILT_MIN;

                        mDroneControl.setDroneTilt(newTilt);
                        mTiltMax = newTilt;
                    }
                }
                return true;
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
        if (mDroneControl.isInTouchMode())
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

        // Set the drone speed for google glass
        mDroneControl.setDroneTilt(GLASS_DRONE_TILT);
        mDroneControl.setDroneYawSpeed(GLASS_YAW_SPEED);

        mTiltMax = mDroneControl.getDroneTilt();
        if(mTiltMax == DroneConfig.INVALID_TILT)
            mTiltMax = DroneConfig.TILT_MAX;

        mYawMax = mDroneControl.getDroneYawSpeed();
        if(mYawMax == DroneConfig.INVALID_TILT)
            mYawMax = DroneConfig.YAW_MAX;

        return true;
    }

    @Override
    protected boolean onGenericMotionImpl(View view, MotionEvent event) {
        mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(true);
        return mGestureDetector.onMotionEvent(event);
    }

    @Override
    protected boolean onKeyDownImpl(int keyCode, KeyEvent event) {
        // TODO: check if glass sends any key events
        return false;
    }

    @Override
    protected boolean onKeyUpImpl(int keyCode, KeyEvent event) {
        // TODO: check if glass sends any key events
        return false;
    }

    @Override
    public boolean onTouchImpl(View view, MotionEvent ev) {
        return false;
    }

    @Override
    protected Sprite[] getSpritesImpl() {
        return NO_SPRITES;
    }

    /**
     * Restore the google glass drone settings
     * {@inheritDoc}
     */
    @Override
    protected void resumeImpl() {
        // Set the drone speed for google glass
        mDroneControl.setDroneTilt(GLASS_DRONE_TILT);

       //Set the drone yaw speed
        mDroneControl.setDroneYawSpeed(GLASS_YAW_SPEED);

        registerListeners();
    }

    @Override
    protected void pauseImpl() {
        unregisterListeners();
    }

    @Override
    protected void destroyImpl() {
        unregisterListeners();
    }

    /**
     * Register this activity as a listener for gravity sensor changes.
     */
    private void registerListeners() {
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_NORMAL);
//        mSensorManager.registerListener(mSensorListener,
//                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
//                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor
                .TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Unregister this activity as a listener.
     */
    private void unregisterListeners() {
        mSensorManager.unregisterListener(mSensorListener);
    }

    @Override
    protected DeviceOrientationManager getDeviceOrientationManagerImpl() {
        return null;
    }

}
