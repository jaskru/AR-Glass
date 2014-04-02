/**
 * @author Fredia Huya-Kouadio
 * @date Sep 14, 2013
 */
package com.ne0fhyklabs.freeflight.controllers;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.ne0fhyklabs.freeflight.activities.ControlDroneActivity;
import com.ne0fhyklabs.freeflight.drone.DroneConfig.EDroneVersion;
import com.ne0fhyklabs.freeflight.sensors.DeviceOrientationChangeDelegate;
import com.ne0fhyklabs.freeflight.sensors.DeviceOrientationManager;
import com.ne0fhyklabs.freeflight.sensors.DeviceSensorManagerWrapper;
import com.ne0fhyklabs.freeflight.settings.ApplicationSettings;
import com.ne0fhyklabs.freeflight.settings.ApplicationSettings.ControlMode;
import com.ne0fhyklabs.freeflight.ui.hud.AcceleroJoystick;
import com.ne0fhyklabs.freeflight.ui.hud.AnalogueJoystick;
import com.ne0fhyklabs.freeflight.ui.hud.JoystickBase;
import com.ne0fhyklabs.freeflight.ui.hud.JoystickFactory;
import com.ne0fhyklabs.freeflight.ui.hud.JoystickListener;
import com.ne0fhyklabs.freeflight.ui.hud.Sprite;
import com.ne0fhyklabs.freeflight.ui.hud.Sprite.Align;
import com.ne0fhyklabs.freeflight.utils.GlassUtils;

public class VirtualJoystick extends Controller implements DeviceOrientationChangeDelegate {

    static final String TAG = VirtualJoystick.class.getName();

    public enum JoystickType {
        NONE,
        ANALOGUE,
        ACCELERO,
        COMBINED,
        MAGNETO
    }

    private static final float ACCELERO_TRESHOLD = (float) Math.PI / 180.0f * 2.0f;

    private static final int YAW = 0;
    private static final int PITCH = 1;
    private static final int ROLL = 2;

    private int screenRotationIndex;

    private float pitchGazBase;
    private float rollYawBase;

    private boolean running;

    private boolean acceleroEnabled;
    private boolean magnetoEnabled;

    private boolean rightJoyPressed;
    private boolean leftJoyPressed;

    private JoystickBase mJoystickLeft, mJoystickRight;

    private JoystickListener rollPitchListener;
    private JoystickListener gazYawListener;

    private final DeviceOrientationManager mOrientationManager;

    private final ApplicationSettings mSettings;

    // This is minimal time interval between touches
    private static final int DOUBLE_TAP_TIMESTAMP_DELTA = 200;
    // This is minimal distance between two touches.
    private static final int COORDINATE_DELTA = 50;

    // Time stamp of previous touch
    private long timestampLast;
    // Coordinates of previous touch
    private float xLast;
    private float yLast;

    VirtualJoystick(final ControlDroneActivity droneControl) {
        super(droneControl);

        screenRotationIndex = droneControl.getWindow().getWindowManager().getDefaultDisplay()
                .getRotation();

        initJoysticListeners();

        mOrientationManager = new DeviceOrientationManager(new DeviceSensorManagerWrapper(
                droneControl.getApplicationContext()), this);
        mOrientationManager.onCreate();

        mSettings = mDroneControl.getSettings();
    }

    @Override
    protected boolean initImpl() {

        if (GlassUtils.instance$.isGlassDevice())
            return false;

        magnetoEnabled = mSettings.isAbsoluteControlEnabled();

        if (magnetoEnabled) {
            if (mDroneControl.getDroneVersion() == EDroneVersion.DRONE_1 ||
                    !mOrientationManager.isMagnetoAvailable()) {
                // Drone 1 doesn't have compass, so we need to switch magneto
                // off.
                magnetoEnabled = false;
                mSettings.setAbsoluteControlEnabled(false);
            }
        }

        mDroneControl.setMagntoEnabled(magnetoEnabled);

        applyJoypadConfig(mSettings.getControlMode(), mSettings.isLeftHanded());

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.parrot.freeflight.controllers.Controller#onEvent(android.view.MotionEvent)
     */
    @Override
    protected boolean onGenericMotionImpl(View view, MotionEvent event) {
        return false;
    }

    @Override
    protected boolean onKeyDownImpl(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    protected boolean onKeyUpImpl(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    protected boolean onTouchImpl(View view, MotionEvent event) {
        boolean result = false;

        if (mJoystickLeft != null && mJoystickLeft.processTouch(view, event))
            result = true;

        if (mJoystickRight != null && mJoystickRight.processTouch(view, event))
            result = true;

        // check for double tap
        if (mSettings.isFlipEnabled() &&
                event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
            long currTimestamp = event.getEventTime();

            if (event.getPointerCount() > 1) {
                if (currTimestamp - timestampLast < DOUBLE_TAP_TIMESTAMP_DELTA &&
                        Math.abs(event.getX(1) - xLast) < COORDINATE_DELTA &&
                        Math.abs(event.getY(1) - yLast) < COORDINATE_DELTA) {
                    // Double tap detected.
                    mDroneControl.doLeftFlip();
                    result = true;
                }
            }
        }

        return result;
    }

    protected Sprite[] getSpritesImpl() {
        final float joypadOpacity = mSettings.getInterfaceOpacity() / 100f;

        if (mJoystickLeft != null) {
            mJoystickLeft.setAlign(Align.BOTTOM_LEFT);
            mJoystickLeft.setAlpha(joypadOpacity);
            mJoystickLeft.setInverseYWhenDraw(true);
        }
        if (mJoystickRight != null) {
            mJoystickRight.setAlign(Align.BOTTOM_RIGHT);
            mJoystickRight.setAlpha(joypadOpacity);
            mJoystickRight.setInverseYWhenDraw(true);
        }

        JoystickBase[] joysticks = new JoystickBase[2];
        joysticks[0] = mJoystickLeft;
        joysticks[1] = mJoystickRight;
        return joysticks;
    }

    private void applyJoypadConfig(ControlMode controlMode, boolean isLeftHanded) {
        switch (controlMode) {
            case NORMAL_MODE:
                initVirtualJoysticks(JoystickType.ANALOGUE, JoystickType.ANALOGUE, isLeftHanded);
                acceleroEnabled = false;
                break;
            case ACCELERO_MODE:
                initVirtualJoysticks(JoystickType.ACCELERO, JoystickType.ANALOGUE, isLeftHanded);
                acceleroEnabled = true;
                break;
            case ACE_MODE:
                initVirtualJoysticks(JoystickType.NONE, JoystickType.COMBINED, isLeftHanded);
                acceleroEnabled = true;
                break;
        }
    }

    private void initJoysticListeners() {
        rollPitchListener = new JoystickListener() {

            @Override
            public void onChanged(JoystickBase joy, float x, float y) {
                if (running) {
                    mDroneControl.setDroneRoll(x);
                    mDroneControl.setDronePitch(-y);
                }
            }

            @Override
            public void onPressed(JoystickBase joy) {
                leftJoyPressed = true;

                mDroneControl.setDroneProgressiveCommandEnabled(true);

                if (rightJoyPressed) {
                    mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(true);
                }
                else {
                    mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(false);
                }

                running = true;
            }

            @Override
            public void onReleased(JoystickBase joy) {
                leftJoyPressed = false;

                mDroneControl.setDroneProgressiveCommandEnabled(false);

                mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(false);

                running = false;
            }
        };

        gazYawListener = new JoystickListener() {
            @Override
            public void onChanged(JoystickBase joy, float x, float y) {
                mDroneControl.setDroneGaz(y);
                mDroneControl.setDroneYaw(x);
            }

            @Override
            public void onPressed(JoystickBase joy) {
                rightJoyPressed = true;

                if (leftJoyPressed) {
                    mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(true);
                }
                else {
                    mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(false);
                }
            }

            @Override
            public void onReleased(JoystickBase joy) {
                rightJoyPressed = false;
                mDroneControl.setDroneProgressiveCommandCombinedYawEnabled(false);
            }
        };
    }

    private void initVirtualJoysticks(JoystickType leftType, JoystickType rightType,
                                      boolean isLeftHanded) {
        JoystickBase joystickLeft = (!isLeftHanded ? mJoystickLeft : mJoystickRight);
        JoystickBase joystickRight = (!isLeftHanded ? mJoystickRight : mJoystickLeft);

        if (leftType == JoystickType.ANALOGUE) {
            if (joystickLeft == null || !(joystickLeft instanceof AnalogueJoystick) ||
                    joystickLeft.isAbsoluteControl() != mSettings.isAbsoluteControlEnabled()) {
                joystickLeft = JoystickFactory.createAnalogueJoystick(mDroneControl,
                        mSettings.isAbsoluteControlEnabled(), rollPitchListener);
            }
            else {
                joystickLeft.setOnAnalogueChangedListener(rollPitchListener);
                joystickRight.setAbsolute(mSettings.isAbsoluteControlEnabled());
            }
        }
        else if (leftType == JoystickType.ACCELERO) {
            if (joystickLeft == null || !(joystickLeft instanceof AcceleroJoystick) ||
                    joystickLeft.isAbsoluteControl() != mSettings.isAbsoluteControlEnabled()) {
                joystickLeft = JoystickFactory.createAcceleroJoystick(mDroneControl,
                        mSettings.isAbsoluteControlEnabled(), rollPitchListener);
            }
            else {
                joystickLeft.setOnAnalogueChangedListener(rollPitchListener);
                joystickRight.setAbsolute(mSettings.isAbsoluteControlEnabled());
            }
        }

        if (rightType == JoystickType.ANALOGUE) {
            if (joystickRight == null || !(joystickRight instanceof AnalogueJoystick) ||
                    joystickRight.isAbsoluteControl() != mSettings.isAbsoluteControlEnabled()) {
                joystickRight = JoystickFactory.createAnalogueJoystick(mDroneControl, false,
                        gazYawListener);
            }
            else {
                joystickRight.setOnAnalogueChangedListener(gazYawListener);
                joystickRight.setAbsolute(false);
            }
        }
        else if (rightType == JoystickType.ACCELERO) {
            if (joystickRight == null || !(joystickRight instanceof AcceleroJoystick) ||
                    joystickRight.isAbsoluteControl() != mSettings.isAbsoluteControlEnabled()) {
                joystickRight = JoystickFactory.createAcceleroJoystick(mDroneControl, false,
                        gazYawListener);
            }
            else {
                joystickRight.setOnAnalogueChangedListener(gazYawListener);
                joystickRight.setAbsolute(false);
            }
        }

        if (!isLeftHanded) {
            mJoystickLeft = joystickLeft;
            mJoystickRight = joystickRight;
        }
        else {
            mJoystickLeft = joystickRight;
            mJoystickRight = joystickLeft;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.parrot.freeflight.controllers.Controller#resume()
     */
    @Override
    protected void resumeImpl() {
        mOrientationManager.resume();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.parrot.freeflight.controllers.Controller#pause()
     */
    @Override
    protected void pauseImpl() {
        mOrientationManager.pause();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.parrot.freeflight.controllers.Controller#destroy()
     */
    @Override
    protected void destroyImpl() {
        mOrientationManager.destroy();
    }

    static long timeCounter = -1;

    @Override
    public void onDeviceOrientationChanged(float[] orientation, float magneticHeading,
                                           int magnetoAccuracy) {

        if (magnetoEnabled && mOrientationManager.isMagnetoAvailable()) {
            float heading = magneticHeading * 57.2957795f;

            if (screenRotationIndex == 1) {
                heading += 90.f;
            }

            mDroneControl.setDeviceOrientation((int) heading, 0);
        }
        else {
            mDroneControl.setDeviceOrientation(0, 0);
        }

        final boolean isInTouchMode = !GlassUtils.instance$.isGlassDevice();

        if (!running) {
            pitchGazBase = orientation[PITCH];
            rollYawBase = orientation[ROLL];

            if (isInTouchMode) {
                mDroneControl.setDronePitch(0);
                mDroneControl.setDroneRoll(0);
            }
            else {
                mDroneControl.setDroneGaz(0);
                mDroneControl.setDroneYaw(0);
            }
        }
        else {

            float x = orientation[PITCH] - pitchGazBase;
            float y = (orientation[ROLL] - rollYawBase);

            // if ( timeCounter > -1 && (System.currentTimeMillis() - timeCounter >= 500l) ) {
            // Log.d(TAG, "Diffs: [x: " + x + ", y: " + y + "]");
            // timeCounter = System.currentTimeMillis();
            // }

            if (!isInTouchMode) {
                if (acceleroEnabled &&
                        (/* Math.abs(x) > ACCELERO_TRESHOLD || */Math.abs(y) > ACCELERO_TRESHOLD)) {
                    mDroneControl.setDroneYaw(y);
                    // mDroneControl.setDroneGaz(x);
                }
            }
            else {
                if (screenRotationIndex == 0) {
                    // Xoom
                    if (acceleroEnabled &&
                            (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD)) {
                        x *= -1;
                        mDroneControl.setDronePitch(x);
                        mDroneControl.setDroneRoll(y);
                    }
                }
                else if (screenRotationIndex == 1) {
                    if (acceleroEnabled &&
                            (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD)) {
                        x *= -1;
                        y *= -1;

                        mDroneControl.setDronePitch(y);
                        mDroneControl.setDroneRoll(x);
                    }
                }
                else if (screenRotationIndex == 3) {
                    // google tv
                    if (acceleroEnabled &&
                            (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD)) {

                        mDroneControl.setDronePitch(y);
                        mDroneControl.setDroneRoll(x);
                    }
                }
            }

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.parrot.freeflight.controllers.Controller#getDeviceOrientationManager()
     */
    @Override
    protected DeviceOrientationManager getDeviceOrientationManagerImpl() {
        return mOrientationManager;
    }
}
