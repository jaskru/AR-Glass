package com.parrot.freeflight.controllers;

import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.parrot.freeflight.activities.ControlDroneActivity;
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
public class GoogleGlassController extends Controller implements DeviceOrientationChangeDelegate {
    private static final String TAG = GoogleGlassController.class.getSimpleName();

    private short mPitchValue = 0;
    private short mGazValue = 0;

    private boolean running;
    private boolean magnetoEnabled;

    private final ApplicationSettings mSettings;
    private final GestureDetector mGestureDetector;
    private final DeviceOrientationManager mOrientationManager;

    GoogleGlassController(final ControlDroneActivity droneControl) {
        super(droneControl);

        mSettings = droneControl.getSettings();

        mOrientationManager = new DeviceOrientationManager(new DeviceSensorManagerWrapper(
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

                        short value = running ? mGazValue : mPitchValue;

                        value += (distanceX / 20);
                        if ( value > 100 )
                            value = 100;

                        if ( value < -100 )
                            value = -100;

                        if ( running ) {
                            mGazValue = value;
                            mDroneControl.setDroneGaz(mGazValue / 100f);
                        }
                        else {
                            mPitchValue = value;
                            mDroneControl.getHudView().setPitchValue(-1 * mPitchValue);

                            mDroneControl.setDronePitch(mPitchValue / 100f);
                        }
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        // Take photo
                        mDroneControl.onTakePhoto();
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
        if ( event.getPointerCount() >= 2 )
            onDoubleDownStarted();
        else
            onDoubleDownEnded();

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
        return new Sprite[0];
    }

    @Override
    protected void resumeImpl() {
        mOrientationManager.resume();
    }

    @Override
    protected void pauseImpl() {
        mOrientationManager.pause();
    }

    @Override
    protected void destroyImpl() {
        mOrientationManager.destroy();
    }

    @Override
    public void onDeviceOrientationChanged(float[] orientation, float magneticHeading,
            int magnetoAccuracy) {
        // TODO: complete
    }

    private void onDoubleDownStarted() {
        // Enable controls
        running = true;
        mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(true);
    }

    private void onDoubleDownEnded() {
        // Disable controls
        running = false;
        mGazValue = 0;
        mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(false);
        mDroneControl.setDroneGaz(0);
    }

}
