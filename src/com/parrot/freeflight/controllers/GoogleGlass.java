package com.parrot.freeflight.controllers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.parrot.freeflight.activities.ControlDroneActivity;
import com.parrot.freeflight.drone.DroneConfig;
import com.parrot.freeflight.drone.DroneConfig.EDroneVersion;
import com.parrot.freeflight.sensors.DeviceOrientationChangeDelegate;
import com.parrot.freeflight.sensors.DeviceOrientationManager;
import com.parrot.freeflight.sensors.DeviceSensorManagerWrapper;
import com.parrot.freeflight.settings.ApplicationSettings;
import com.parrot.freeflight.ui.hud.Sprite;

/**
 * EnhancedGestureDetector detects double tap gesture of second pointer when using multi touch in
 * addition to
 * standard GestureDetecror gestures.
 */
public class GoogleGlass extends Controller implements
        DeviceOrientationChangeDelegate {
    private static final String TAG = GoogleGlass.class.getSimpleName();

    private static final float RAD_TO_DEG = (float) (180f / Math.PI);

    private float[] mOrientation;
    private float[] mRotationMatrix;
    private boolean magnetoEnabled;

    private final ApplicationSettings mSettings;
    private final GestureDetector mGestureDetector;
    private final DeviceOrientationManager mOrientationManager;

    private final SensorManager mSensorManager;
    private final SensorEventListener mSensorListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if ( mDroneControl == null ) {
                return;
            }

            switch (event.sensor.getType()) {
                case Sensor.TYPE_GRAVITY:
                    // Angle are in degrees
                    final float minRatio = (1f / 6f);

                    float rollAngle = RAD_TO_DEG * computeRollOrientation(event);
                    float rollRatio = rollAngle / DroneConfig.TILT_MAX;

                    if ( rollRatio > 1f )
                        rollRatio = 1f;
                    else if ( rollRatio < -1f )
                        rollRatio = -1f;
                    else if ( Math.abs(rollRatio) <= minRatio )
                        rollRatio = 0;

                    float pitchAngle = RAD_TO_DEG * computePitchOrientation(event);
                    float pitchRatio = pitchAngle / DroneConfig.TILT_MAX;

                    if ( pitchRatio > 1f )
                        pitchRatio = 1f;
                    else if ( pitchRatio < -1f )
                        pitchRatio = -1f;
                    else if ( Math.abs(pitchRatio) <= minRatio )
                        pitchRatio = 0;

                    // Set the tilt angle
                    mDroneControl.setDroneRoll(rollRatio);
                    mDroneControl.setDronePitch(pitchRatio);
                    break;

                case Sensor.TYPE_ROTATION_VECTOR:
                    SensorManager.getRotationMatrixFromVector(mRotationMatrix,
                            event.values);
                    SensorManager.remapCoordinateSystem(mRotationMatrix,
                            SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
                    SensorManager.getOrientation(mRotationMatrix, mOrientation);

                    float heading = (float) Math.toDegrees(mOrientation[0]);

                    // TODO: experiment with the heading to control yaw
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
            float angle = (float) -Math.atan(
                    event.values[0] / Math.sqrt(event.values[1] * event.values[1] +
                                                event.values[2] * event.values[2]));
            return angle;
        }

        private float computePitchOrientation(SensorEvent event) {
            float angle = (float) -Math.atan(
                    event.values[2] / Math.sqrt(event.values[1] * event.values[1] +
                                                event.values[0] * event.values[0]));

            return angle;
        }
    };

    GoogleGlass(final ControlDroneActivity droneControl) {
        super(droneControl);

        mOrientation = new float[3];
        mRotationMatrix = new float[16];
        mSensorManager = (SensorManager) droneControl
                .getSystemService(Context.SENSOR_SERVICE);

        mSettings = droneControl.getSettings();

        mOrientationManager = new DeviceOrientationManager(
                new DeviceSensorManagerWrapper(
                        droneControl.getApplicationContext()), this);

        mGestureDetector = new GestureDetector(droneControl.getApplicationContext(),
                new SimpleOnGestureListener() {
                    @Override
                    public void onShowPress(MotionEvent e) {
                        // Land the drone if it's flying
                        if ( mDroneControl.isDroneFlying() )
                            mDroneControl.triggerDroneTakeOff();
                    }

                    @Override
                    public boolean
                            onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                    float distanceY) {

                        if ( distanceX >= 0 )
                            mDroneControl.setDroneGaz(-1f);
                        else {
                            mDroneControl.setDroneGaz(1f);
                        }

                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        // Take photo
                        mDroneControl.doLeftFlip();
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent event) {
                        // Trigger take off/landing
                        mDroneControl.triggerDroneTakeOff();
                        return true;
                    }
                });
    }

    @Override
    protected boolean initImpl() {
        Log.v(TAG, "Initializing Google Glass controller");
        if ( mDroneControl.isInTouchMode() )
            return false;

        magnetoEnabled = mSettings.isAbsoluteControlEnabled();
        if ( magnetoEnabled ) {
            if ( mDroneControl.getDroneVersion() == EDroneVersion.DRONE_1 ||
                 !mOrientationManager.isMagnetoAvailable() ) {
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
    protected DeviceOrientationManager getDeviceOrientationManagerImpl() {
        return mOrientationManager;
    }

    @Override
    protected boolean onGenericMotionImpl(View view, MotionEvent event) {
        if ( event.getPointerCount() >= 1 )
            onDoubleDownStarted();
        else
            onDoubleDownEnded();

        if ( event.getAction() == MotionEvent.ACTION_UP ) {
            // Set the gaz back to zero
            mDroneControl.setDroneGaz(0);
        }

        return mGestureDetector.onTouchEvent(event);
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

    @Override
    protected void resumeImpl() {
        registerListeners();
        // mOrientationManager.resume();
    }

    @Override
    protected void pauseImpl() {
        unregisterListeners();
        // mOrientationManager.pause();
    }

    @Override
    protected void destroyImpl() {
        unregisterListeners();
        // mOrientationManager.destroy();
    }

    /**
     * Register this activity as a listener for gravity sensor changes.
     */
    private void registerListeners() {
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Unregister this activity as a listener.
     */
    private void unregisterListeners() {
        mSensorManager.unregisterListener(mSensorListener);
    }

    @Override
    public void onDeviceOrientationChanged(float[] orientation, float magneticHeading,
            int magnetoAccuracy) {
        // TODO: complete
    }

    private void onDoubleDownStarted() {
        // Enable controls
        mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(true);
    }

    private void onDoubleDownEnded() {
        // Disable controls
        mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(false);
        mDroneControl.setDroneGaz(0);
    }

}
