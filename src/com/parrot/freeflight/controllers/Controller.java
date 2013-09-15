/**
 * @author Fredia Huya-Kouadio
 * @date Sep 14, 2013
 */
package com.parrot.freeflight.controllers;

import android.view.MotionEvent;
import android.view.View;

import com.parrot.freeflight.activities.ControlDroneActivity;
import com.parrot.freeflight.sensors.DeviceOrientationManager;
import com.parrot.freeflight.ui.hud.Sprite;

public abstract class Controller {

    public enum ControllerType {
        GAMEPAD {
            @Override
            public Controller getImpl(final ControlDroneActivity droneControl) {
                return null;
            }
        },
        VIRTUAL_JOYSTICK {
            @Override
            public Controller getImpl(final ControlDroneActivity droneControl) {
                return new VirtualJoystick(droneControl);
            }
        },
        GOOGLE_GLASS {
            @Override
            public Controller getImpl(final ControlDroneActivity droneControl) {
                return null;
            }
        };

        public abstract Controller getImpl(final ControlDroneActivity droneControl);
    }

    protected ControlDroneActivity mDroneControl;

    public Controller(final ControlDroneActivity droneControl) {
        mDroneControl = droneControl;
    }

    public abstract boolean init();

    public abstract Sprite[] getSprites();

    public abstract DeviceOrientationManager getDeviceOrientationManager();

    public abstract boolean onEvent(View view, MotionEvent event);

    public abstract void resume();

    public abstract void pause();

    // TODO: Remove the controller sprites on destroy
    public abstract void destroy();
}
