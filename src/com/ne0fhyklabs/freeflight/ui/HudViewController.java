/*
 * HudViewController
 *
 * Created on: July 5, 2011
 * Author: Dmytro Baryskyy
 */

package com.ne0fhyklabs.freeflight.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.SparseIntArray;

import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.drone.NavData;
import com.ne0fhyklabs.freeflight.ui.hud.Sprite.Align;
import com.ne0fhyklabs.freeflight.ui.hud.Text;
import com.ne0fhyklabs.freeflight.video.VideoStageRenderer;

public class HudViewController extends GLSurfaceView {

    private static final int EMERGENCY_LABEL_ID = 13;

    private Text txtAlert;

    private VideoStageRenderer renderer;

    private SparseIntArray emergencyStringMap;

    public HudViewController(Context context) {
        this(context, null);
    }

    public HudViewController(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);

        renderer = new VideoStageRenderer(context, null);
        setRenderer(renderer);

        initNavdataStrings();

        final Resources res = context.getResources();

        txtAlert = new Text(context, "", Align.TOP_CENTER);
        txtAlert.setMargin((int) res.getDimension(R.dimen.hud_alert_text_margin_top), 0, 0, 0);
        txtAlert.setTextColor(Color.RED);
        txtAlert.setTextSize((int) res.getDimension(R.dimen.hud_alert_text_size));
        txtAlert.setBold(true);
        txtAlert.blink(true);

        renderer.addSprite(EMERGENCY_LABEL_ID, txtAlert);
    }

    private void initNavdataStrings() {
        emergencyStringMap = new SparseIntArray(17);

        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_CUTOUT, R.string.CUT_OUT_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_MOTORS, R.string.MOTORS_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_CAMERA, R.string.CAMERA_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_PIC_WATCHDOG,
                R.string.PIC_WATCHDOG_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_PIC_VERSION,
                R.string.PIC_VERSION_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_ANGLE_OUT_OF_RANGE,
                R.string.TOO_MUCH_ANGLE_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_VBAT_LOW,
                R.string.BATTERY_LOW_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_USER_EL, R.string.USER_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_ULTRASOUND,
                R.string.ULTRASOUND_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_UNKNOWN, R.string.UNKNOWN_EMERGENCY);
        emergencyStringMap.put(NavData.ERROR_STATE_NAVDATA_CONNECTION,
                R.string.CONTROL_LINK_NOT_AVAILABLE);
        emergencyStringMap.put(NavData.ERROR_STATE_START_NOT_RECEIVED, R.string.START_NOT_RECEIVED);
        emergencyStringMap.put(NavData.ERROR_STATE_ALERT_CAMERA, R.string.VIDEO_CONNECTION_ALERT);
        emergencyStringMap.put(NavData.ERROR_STATE_ALERT_VBAT_LOW, R.string.BATTERY_LOW_ALERT);
        emergencyStringMap.put(NavData.ERROR_STATE_ALERT_ULTRASOUND, R.string.ULTRASOUND_ALERT);
        emergencyStringMap.put(NavData.ERROR_STATE_ALERT_VISION, R.string.VISION_ALERT);
        emergencyStringMap.put(NavData.ERROR_STATE_EMERGENCY_UNKNOWN, R.string.UNKNOWN_EMERGENCY);
    }

    public void setEmergency(final int code) {
        final int res = emergencyStringMap.get(code);

        if (res != 0) {
            txtAlert.setText(getContext().getString(res));
            txtAlert.setVisibility(Text.VISIBLE);
            txtAlert.blink(true);
        } else {
            txtAlert.setVisibility(Text.INVISIBLE);
            txtAlert.blink(false);
        }
    }

    public void onDestroy() {
        renderer.clearSprites();
    }

}