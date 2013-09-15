/*
 * HudViewController
 *
 * Created on: July 5, 2011
 * Author: Dmytro Baryskyy
 */

package com.parrot.freeflight.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnTouchListener;
import android.widget.TextView;

import com.parrot.freeflight.R;
import com.parrot.freeflight.controllers.Controller;
import com.parrot.freeflight.drone.NavData;
import com.parrot.freeflight.gestures.EnhancedGestureDetector;
import com.parrot.freeflight.ui.hud.Button;
import com.parrot.freeflight.ui.hud.Image;
import com.parrot.freeflight.ui.hud.Image.SizeParams;
import com.parrot.freeflight.ui.hud.Indicator;
import com.parrot.freeflight.ui.hud.Sprite;
import com.parrot.freeflight.ui.hud.Sprite.Align;
import com.parrot.freeflight.ui.hud.Text;
import com.parrot.freeflight.ui.hud.ToggleButton;
import com.parrot.freeflight.utils.FontUtils.TYPEFACE;
import com.parrot.freeflight.video.VideoStageRenderer;

public class HudViewController implements OnTouchListener, OnGenericMotionListener
{
    private static final String TAG = HudViewController.class.getName();

    private static final int ALERT_ID = 3;
    private static final int TAKE_OFF_ID = 4;
    private static final int TOP_BAR_ID = 5;
    private static final int BOTTOM_BAR_ID = 6;
    private static final int CAMERA_ID = 7;
    private static final int RECORD_ID = 8;
    private static final int PHOTO_ID = 9;
    private static final int SETTINGS_ID = 10;
    private static final int BATTERY_INDICATOR_ID = 11;
    private static final int WIFI_INDICATOR_ID = 12;
    private static final int EMERGENCY_LABEL_ID = 13;
    private static final int BATTERY_STATUS_LABEL_ID = 14;
    private static final int RECORD_LABEL_ID = 15;
    private static final int USB_INDICATOR_ID = 16;
    private static final int USB_INDICATOR_TEXT_ID = 17;
    private static final int BACK_BTN_ID = 18;
    private static final int LAND_ID = 19;
    private static final int PITCH_VALUE_ID = 20;
    private static final int CONTROLLER_IDS = 21;

    private Image bottomBarBg;

    private Button btnSettings;
    private Button btnTakeOff;
    private Button btnLand;
    private Button btnEmergency;
    private Button btnCameraSwitch;
    private Button btnPhoto;
    private Button btnBack;
    private ToggleButton btnRecord;

    private Button[] buttons;

    private Indicator batteryIndicator;
    private Indicator wifiIndicator;
    private Image usbIndicator;
    // private TextView txtVideoFps;
    private TextView txtSceneFps;

    private Text txtBatteryStatus;
    private Text txtAlert;
    private Text txtRecord;
    private Text txtUsbRemaining;
    private Text txtPitchValue;

    private GLSurfaceView glView;

    private Controller mController;

    private EnhancedGestureDetector gestureDetector;

    private VideoStageRenderer renderer;
    private Activity context;

    private int prevRemainingTime;

    private SparseIntArray emergencyStringMap;

    public HudViewController(Activity context, OnGestureListener gestureListener)
    {
        this.context = context;
        gestureDetector = new EnhancedGestureDetector(context, gestureListener);

        glView = new GLSurfaceView(context);
        glView.setEGLContextClientVersion(2);

        context.setContentView(glView);

        renderer = new VideoStageRenderer(context, null);

        initNavdataStrings();
        initGLSurfaceView();

        Resources res = context.getResources();

        Image topBarBg = new Image(res, R.drawable.barre_haut, Align.TOP_CENTER);
        topBarBg.setSizeParams(SizeParams.FILL_SCREEN, SizeParams.NONE);
        topBarBg.setAlphaEnabled(false);

        bottomBarBg = new Image(res, R.drawable.barre_bas, Align.BOTTOM_CENTER);
        bottomBarBg.setSizeParams(SizeParams.FILL_SCREEN, SizeParams.NONE);
        bottomBarBg.setAlphaEnabled(false);

        renderer.addSprite(TOP_BAR_ID, topBarBg);
        renderer.addSprite(BOTTOM_BAR_ID, bottomBarBg);

        if ( isInTouchMode() ) {
            btnSettings = new Button(res, R.drawable.btn_settings, R.drawable.btn_settings_pressed,
                    Align.TOP_LEFT);
            btnSettings.setMargin(0, 0, 0,
                    (int) res.getDimension(R.dimen.hud_btn_settings_margin_left));

            btnBack = new Button(res, R.drawable.btn_back, R.drawable.btn_back_pressed,
                    Align.TOP_LEFT);
            btnBack.setMargin(0, 0, 0,
                    res.getDimensionPixelOffset(R.dimen.hud_btn_back_margin_left));

            btnEmergency = new Button(res, R.drawable.btn_emergency_normal,
                    R.drawable.btn_emergency_pressed, Align.TOP_CENTER);
            btnTakeOff = new Button(res, R.drawable.btn_take_off_normal,
                    R.drawable.btn_take_off_pressed, Align.BOTTOM_CENTER);
            btnLand = new Button(res, R.drawable.btn_landing, R.drawable.btn_landing_pressed,
                    Align.BOTTOM_CENTER);
            btnLand.setVisible(false);

            btnPhoto = new Button(res, R.drawable.btn_photo, R.drawable.btn_photo_pressed,
                    Align.TOP_RIGHT);
            btnRecord = new ToggleButton(res, R.drawable.btn_record, R.drawable.btn_record_pressed,
                    R.drawable.btn_record1, R.drawable.btn_record1_pressed,
                    R.drawable.btn_record2, Align.TOP_RIGHT);
            btnRecord.setMargin(0, res.getDimensionPixelOffset(R.dimen.hud_btn_rec_margin_right),
                    0, 0);

            txtRecord = new Text(context, "REC", Align.TOP_RIGHT);
            txtRecord.setMargin((int) res.getDimension(R.dimen.hud_rec_text_margin_top),
                    (int) res.getDimension(R.dimen.hud_rec_text_margin_right), 0, 0);
            txtRecord.setTextColor(Color.WHITE);
            txtRecord.setTypeface(TYPEFACE.Helvetica(context));
            txtRecord.setTextSize(res.getDimensionPixelSize(R.dimen.hud_rec_text_size));

            usbIndicator = new Image(res, R.drawable.picto_usb_actif, Align.TOP_RIGHT);
            usbIndicator.setMargin(0,
                    res.getDimensionPixelOffset(R.dimen.hud_usb_indicator_margin_right), 0, 0);

            prevRemainingTime = -1;
            txtUsbRemaining = new Text(context, "KO", Align.TOP_RIGHT);
            txtUsbRemaining.setMargin(
                    res.getDimensionPixelOffset(R.dimen.hud_usb_indicator_text_margin_top),
                    res.getDimensionPixelOffset(R.dimen.hud_usb_indicator_text_margin_right), 0, 0);
            txtUsbRemaining.setTypeface(TYPEFACE.Helvetica(context));
            txtUsbRemaining.setTextSize(res
                    .getDimensionPixelSize(R.dimen.hud_usb_indicator_text_size));

            btnCameraSwitch = new Button(res, R.drawable.btn_camera, R.drawable.btn_camera_pressed,
                    Align.TOP_RIGHT);
            btnCameraSwitch.setMargin(0,
                    res.getDimensionPixelOffset(R.dimen.hud_btn_camera_switch_margin_right), 0, 0);

            buttons = new Button[8];
            buttons[0] = btnSettings;
            buttons[1] = btnEmergency;
            buttons[2] = btnTakeOff;
            buttons[3] = btnLand;
            buttons[4] = btnPhoto;
            buttons[5] = btnRecord;
            buttons[6] = btnCameraSwitch;
            buttons[7] = btnBack;

            renderer.addSprite(SETTINGS_ID, btnSettings);
            renderer.addSprite(BACK_BTN_ID, btnBack);
            renderer.addSprite(PHOTO_ID, btnPhoto);
            renderer.addSprite(RECORD_ID, btnRecord);
            renderer.addSprite(CAMERA_ID, btnCameraSwitch);
            renderer.addSprite(ALERT_ID, btnEmergency);
            renderer.addSprite(TAKE_OFF_ID, btnTakeOff);
            renderer.addSprite(LAND_ID, btnLand);
            renderer.addSprite(RECORD_LABEL_ID, txtRecord);
            renderer.addSprite(USB_INDICATOR_ID, usbIndicator);
            renderer.addSprite(USB_INDICATOR_TEXT_ID, txtUsbRemaining);
        }
        else {
            txtPitchValue = new Text(context, "0", Align.TOP_RIGHT);
            txtPitchValue.setMargin((int) res.getDimension(R.dimen.hud_rec_text_margin_top),
                    (int) res.getDimension(R.dimen.hud_rec_text_margin_right), 0, 0);
            txtPitchValue.setTextColor(Color.WHITE);
            txtPitchValue.setTypeface(TYPEFACE.Helvetica(context));
            txtPitchValue.setTextSize(res.getDimensionPixelSize(R.dimen.hud_rec_text_size));

            renderer.addSprite(PITCH_VALUE_ID, txtPitchValue);
        }

        int batteryIndicatorRes[] = { R.drawable.btn_battery_0,
                                     R.drawable.btn_battery_1,
                                     R.drawable.btn_battery_2,
                                     R.drawable.btn_battery_3
        };

        batteryIndicator = new Indicator(res, batteryIndicatorRes, Align.TOP_LEFT);
        batteryIndicator.setMargin(0, 0, 0,
                (int) res.getDimension(R.dimen.hud_battery_indicator_margin_left));

        txtBatteryStatus = new Text(context, "0%", Align.TOP_LEFT);
        txtBatteryStatus.setMargin((int) res.getDimension(R.dimen.hud_battery_text_margin_top),
                0, 0,
                (int) res.getDimension(R.dimen.hud_battery_indicator_margin_left) +
                        batteryIndicator.getWidth());
        txtBatteryStatus.setTextColor(Color.WHITE);
        txtBatteryStatus.setTypeface(TYPEFACE.Helvetica(context));
        txtBatteryStatus.setTextSize((int) res.getDimension(R.dimen.hud_battery_text_size));

        int wifiIndicatorRes[] = {
                                  R.drawable.btn_wifi_0,
                                  R.drawable.btn_wifi_1,
                                  R.drawable.btn_wifi_2,
                                  R.drawable.btn_wifi_3
        };

        wifiIndicator = new Indicator(res, wifiIndicatorRes, Align.TOP_LEFT);
        wifiIndicator.setMargin(0, 0, 0,
                (int) res.getDimension(R.dimen.hud_wifi_indicator_margin_left));

        txtAlert = new Text(context, "", Align.TOP_CENTER);
        txtAlert.setMargin((int) res.getDimension(R.dimen.hud_alert_text_margin_top), 0, 0, 0);
        txtAlert.setTextColor(Color.RED);
        txtAlert.setTextSize((int) res.getDimension(R.dimen.hud_alert_text_size));
        txtAlert.setBold(true);
        txtAlert.blink(true);

        renderer.addSprite(BATTERY_INDICATOR_ID, batteryIndicator);
        renderer.addSprite(WIFI_INDICATOR_ID, wifiIndicator);
        renderer.addSprite(EMERGENCY_LABEL_ID, txtAlert);
        renderer.addSprite(BATTERY_STATUS_LABEL_ID, txtBatteryStatus);
    }

    private void initNavdataStrings()
    {
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

    private void initGLSurfaceView() {
        if ( glView != null ) {
            glView.setRenderer(renderer);
            glView.setOnTouchListener(this);
        }
    }

    public boolean isInTouchMode() {
        return glView.isInTouchMode();
    }

    public EnhancedGestureDetector getGestureDetector() {
        return gestureDetector;
    }

    public void setController(Controller controller) {
        if ( mController != null ) {
            // Remove the sprites of the previous controller
            Sprite[] controllerSprites = mController.getSprites();
            int spritesCount = controllerSprites.length;
            if ( spritesCount > 0 ) {
                for ( int i = 0; i < spritesCount; i++ )
                    renderer.removeSprite(CONTROLLER_IDS + i);
            }
        }

        mController = controller;

        Sprite[] controllerSprites = controller.getSprites();
        final int spritesCount = controllerSprites.length;
        if ( spritesCount == 0 )
            return;

        final int margin = context.getResources().getDimensionPixelSize(R.dimen.hud_joy_margin);
        for ( int i = 0; i < spritesCount; i++ ) {
            Sprite sprite = controllerSprites[i];
            sprite.setMargin(0, margin, bottomBarBg.getHeight() + margin, margin);

            renderer.addSprite(CONTROLLER_IDS + i, sprite);
        }
    }

    public void setIsFlying(final boolean isFlying)
    {
        if ( !isInTouchMode() )
            return;

        if ( isFlying ) {
            btnTakeOff.setVisible(false);
            btnLand.setVisible(true);
        }
        else {
            btnTakeOff.setVisible(true);
            btnLand.setVisible(false);
        }

    }

    public void setBatteryValue(final int percent)
    {
        if ( percent > 100 || percent < 0 ) {
            Log.w(TAG, "Can't set battery value. Invalid value " + percent);
            return;
        }

        int imgNum = Math.round(percent / 100.0f * 3.0f);

        txtBatteryStatus.setText(percent + "%");

        if ( imgNum < 0 )
            imgNum = 0;

        if ( imgNum > 3 )
            imgNum = 3;

        if ( batteryIndicator != null ) {
            batteryIndicator.setValue(imgNum);
        }
    }

    public void setWifiValue(final int theNum)
    {
        if ( wifiIndicator != null ) {
            wifiIndicator.setValue(theNum);
        }
    }

    public void setUsbRemainingTime(int seconds)
    {
        if ( !isInTouchMode() )
            return;

        boolean needColor = false;
        String remainingTime = null;

        if ( seconds != prevRemainingTime ) {
            if ( 3600 < seconds ) {
                remainingTime = "> 1h";
            }
            else if ( 2700 < seconds ) {
                remainingTime = "45m";
            }
            else if ( 1800 < seconds ) {
                remainingTime = "30m";
            }
            else if ( 900 < seconds ) {
                remainingTime = "15m";
            }
            else if ( 600 < seconds ) {
                remainingTime = "10m";
            }
            else if ( 300 < seconds ) {
                remainingTime = "5m";
            }
            else {
                if ( 30 > seconds ) {
                    needColor = true;
                } // No else

                int remMin = seconds / 60;
                int remSec = seconds % 60;

                if ( 0 == remSec && 0 == remMin ) {
                    remainingTime = "FULL";
                }
                else {
                    remainingTime = "" + remMin + ":" + (remSec >= 10 ? remSec : ("0" + remSec));
                }
            }

            prevRemainingTime = seconds;
            txtUsbRemaining.setText(remainingTime);

            if ( needColor ) {
                txtUsbRemaining.setTextColor(0xffAA0000);
            }
            else {
                txtUsbRemaining.setTextColor(Color.WHITE);
            }
        }
    }

    public void setUsbIndicatorEnabled(boolean enabled)
    {
        if ( !isInTouchMode() )
            return;

        if ( enabled ) {
            usbIndicator.setAlpha(1.0f);
            txtUsbRemaining.setAlpha(1.0f);
        }
        else {
            usbIndicator.setAlpha(0.0f);
            txtUsbRemaining.setAlpha(0.0f);
        }
    }

    public void setBackButtonVisible(boolean visible)
    {
        if ( !isInTouchMode() )
            return;

        if ( visible ) {
            btnBack.setEnabled(true);
            btnBack.setAlpha(1.0f);
        }
        else {
            btnBack.setEnabled(false);
            btnBack.setAlpha(0.0f);
        }
    }

    public void setSettingsButtonEnabled(boolean enabled)
    {
        if ( !isInTouchMode() )
            return;

        btnSettings.setEnabled(enabled);
    }

    public void setSwitchCameraButtonEnabled(boolean enabled)
    {
        if ( !isInTouchMode() )
            return;

        btnCameraSwitch.setEnabled(enabled);
    }

    public void setRecordButtonEnabled(boolean enabled)
    {
        if ( !isInTouchMode() )
            return;

        btnRecord.setEnabled(enabled);
        txtRecord.setEnabled(enabled);
    }

    public void setCameraButtonEnabled(boolean enabled)
    {
        if ( !isInTouchMode() )
            return;

        btnPhoto.setEnabled(enabled);
    }

    public void setFpsVisible(final boolean visible)
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if ( visible ) {
                    // txtVideoFps.setVisibility(View.VISIBLE);
                    txtSceneFps.setVisibility(View.VISIBLE);
                }
                else {
                    // txtVideoFps.setVisibility(View.INVISIBLE);
                    txtSceneFps.setVisibility(View.INVISIBLE);
                }
            }
        };

        context.runOnUiThread(runnable);
    }

    public void setEmergency(final int code)
    {
        int res = emergencyStringMap.get(code);

        if ( res != 0 ) {
            txtAlert.setText(context.getString(res));
            txtAlert.setVisibility(Text.VISIBLE);
            txtAlert.blink(true);
        }
        else {
            txtAlert.setVisibility(Text.INVISIBLE);
            txtAlert.blink(false);
        }
    }

    public void setPitchValue(int value) {
        if ( isInTouchMode() )
            return;

        if ( txtPitchValue != null )
            txtPitchValue.setText(Integer.toString(value));
    }

    public void setRecording(boolean inProgress)
    {
        if ( !isInTouchMode() )
            return;

        btnRecord.setChecked(inProgress);

        if ( txtRecord != null ) {
            if ( inProgress ) {
                txtRecord.setTextColor(Color.RED);
            }
            else {
                txtRecord.setTextColor(Color.WHITE);
            }
        }
    }

    public void setBtnTakeOffClickListener(OnClickListener listener)
    {
        if ( !isInTouchMode() )
            return;

        this.btnTakeOff.setOnClickListener(listener);
        this.btnLand.setOnClickListener(listener);
    }

    public void setBtnEmergencyClickListener(OnClickListener listener)
    {
        if ( !isInTouchMode() )
            return;

        this.btnEmergency.setOnClickListener(listener);
    }

    public void setBtnPhotoClickListener(OnClickListener listener)
    {
        if ( !isInTouchMode() )
            return;

        this.btnPhoto.setOnClickListener(listener);
    }

    public void setBtnRecordClickListener(OnClickListener listener)
    {
        if ( !isInTouchMode() )
            return;

        this.btnRecord.setOnClickListener(listener);
    }

    public void setSettingsButtonClickListener(OnClickListener listener)
    {
        if ( !isInTouchMode() )
            return;

        this.btnSettings.setOnClickListener(listener);
    }

    public void setBtnCameraSwitchClickListener(OnClickListener listener)
    {
        if ( !isInTouchMode() )
            return;

        this.btnCameraSwitch.setOnClickListener(listener);
    }

    public void setDoubleTapClickListener(OnDoubleTapListener listener)
    {
        if ( !isInTouchMode() )
            return;

        gestureDetector.setOnDoubleTapListener(listener);
    }

    public void onPause()
    {
        glView.onPause();
    }

    public void onResume()
    {
        glView.onResume();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        boolean result = false;

        for ( int i = 0; i < buttons.length; ++i ) {
            if ( buttons[i].processTouch(v, event) ) {
                result = true;
                break;
            }
        }

        if ( result != true ) {
            // TODO: figure out what to do with gesture detector
            gestureDetector.onTouchEvent(event);

            if ( mController != null && mController.onEvent(v, event) )
                result = true;
        }

        return result;
    }

    public void onDestroy()
    {
        renderer.clearSprites();
    }

    public void setBtnBackClickListener(OnClickListener listener)
    {
        if ( !isInTouchMode() )
            return;

        btnBack.setOnClickListener(listener);
    }

    public View getRootView()
    {
        return glView;
    }

    public void setEmergencyButtonEnabled(boolean enabled)
    {
        if ( !isInTouchMode() )
            return;

        btnEmergency.setEnabled(enabled);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View.OnGenericMotionListener#onGenericMotion(android.view.View,
     * android.view.MotionEvent)
     */
    @Override
    public boolean onGenericMotion(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        return false;
    }
}
