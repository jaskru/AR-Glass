package com.ne0fhyklabs.freeflight.ui

import com.ne0fhyklabs.freeflight.activities.ControlDroneActivity
import com.ne0fhyklabs.freeflight.R
import com.ne0fhyklabs.androhud.widget.SimpleYaw
import android.graphics.Color
import android.widget.TextView
import android.view.View
import com.ne0fhyklabs.androhud.widget.SimplePitchRoll
import android.widget.ImageView
import android.util.Log

/**
 * Created by fhuya on 3/9/14.
 */
public class HudViewProxy(private val activity: ControlDroneActivity) {

    private val TAG = javaClass<HudViewProxy>().getSimpleName()

    private val mHudPitchRoll: SimplePitchRoll = view(R.id.hud_pitch_roll_widget)
    private val mHudBatteryInfo: TextView = view(R.id.hud_battery_info)
    private val mHudWifiInfo: ImageView = view(R.id.hud_wifi_info)
    private val mHudStatusInfo: TextView = view(R.id.hud_status_info)
    private val mHudRecInfo: TextView = view(R.id.hud_rec_info)
    private val mHudUsbInfo: TextView = view(R.id.hud_usb_info)

    /**
     * Used to track the last time the pitch and roll value were updated.
     */
    private var mLastPitchRollUpdate = System.currentTimeMillis()

    /**
     * Update rate for the pitch and roll value.
     */
    private val UPDATE_RATE = 15 //fps

    /**
     * Delay between successive updates in order to attain the desired update rate.
     */
    private val UPDATE_DELAY = 1000 / UPDATE_RATE

    private fun view<T : View>(id: Int): T {
        val view: View? = activity.findViewById(id)
        if (view == null)
            throw IllegalArgumentException("Given id could not be found in current layout!")
        return view as T
    }

    /**
     * Enable/Disable video recording status
     * TODO: maybe propagate to the control
     */
    fun enableRecording(enable: Boolean){
        mHudRecInfo.setEnabled(enable)
    }

    /**
     * Enable/Disable video recording from the drone's camera.
     * @param inProgress true to enable recording, false to disable
     */
    fun setRecording(inProgress: Boolean) {
        if (inProgress) {
            mHudRecInfo.setTextColor(Color.RED)
            mHudRecInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.activated_btn_record,
                    0, 0, 0)
        }
        else {
            mHudRecInfo.setTextColor(Color.WHITE)
            mHudRecInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_record, 0, 0, 0)
        }
    }

    /**
     * Enable/Disable usb indicator.
     * @param usbActivate true to enable usb indicator.
     */
    fun setUsbIndicatorEnabled(usbActivate: Boolean): Unit {
        mHudUsbInfo.setVisibility(if (usbActivate) View.VISIBLE else View.INVISIBLE)
    }

    /**
     * Updates remaining video time that can be stored on the usb stick.
     * @param seconds amount of time remaining in seconds
     */
    fun setUsbRemainingTime(seconds: Int): Unit {
        val remainingTime: String? = when {
            seconds > 3600 -> "> 1h"
            seconds > 2700 -> "45m"
            seconds > 1800 -> "30m"
            seconds > 900 -> "15m"
            seconds > 600 -> "10m"
            seconds > 300 -> "5m"
            else -> {
                val remMin = seconds / 60
                val remSec = seconds % 60
                if (0 == remSec && 0 == remMin) "FULL"
                else "$remMin: ${if (remSec >= 10) remSec else '0' + remSec}"
            }
        }

        val textColor = if (seconds < 30) 0xffAA0000.toInt() else Color.WHITE
        mHudUsbInfo.setText(remainingTime)
        mHudUsbInfo.setTextColor(textColor)
    }

    /**
     * Updates the status info based on the flying state of the drone.
     */
    fun setIsFlying(flying: Boolean){
        mHudStatusInfo.setText(if(flying) R.string.status_flying else R.string.status_landed);
    }

    /**
     * Updates the drone's battery level
     */
    fun setBatteryValue(percent: Int){
        if(percent > 100 || percent < 0){
            Log.w(TAG, "Can't set battery value. Invalid value $percent")
        }
        else{
            val imgNum = Math.min(3, Math.max(0, Math.round(percent / 100.0f * 3.0f)))
            mHudBatteryInfo.setText("$percent%")
            val compoundDrawables = mHudBatteryInfo.getCompoundDrawables()
            if(compoundDrawables != null && compoundDrawables[0] != null){
                compoundDrawables[0].setLevel(imgNum)
            }
        }
    }

    /**
     * Updates the drone's wifi connection level
     */
    fun setWifiValue(wifiLevel: Int){
        mHudWifiInfo.setImageLevel(wifiLevel)
    }

    /**
     * Sets the max value for the roll angle.
     * @param rollMax max roll value in degrees
     */
    fun setMaxRoll(rollMax: Float){
        mHudPitchRoll.setRollMax(rollMax)
    }

    /**
     * Sets the min value for the roll angle.
     * @param rollMin min roll value in degrees
     */
    fun setMinRoll(rollMin: Float){
        mHudPitchRoll.setRollMin(rollMin)
    }

    /**
     * Sets the max value for the pitch angle.
     * @param pitchMax max pitch value in degrees
     */
    fun setPitchMax(pitchMax: Float){
        mHudPitchRoll.setPitchMax(pitchMax)
    }

    /**
     * Sets the min value for the pitch angle.
     * @param pitchMin min pitch value in degrees
     */
    fun setPitchMin(pitchMin: Float){
        mHudPitchRoll.setPitchMin(pitchMin)
    }

    /**
     * Sets the pitch and roll angle. Both values are between -1 and 1,
     * so they needs to be converted back to degrees.
     * @param pitch pitch value
     * @param roll roll value
     */
    fun setPitchRoll(pitch: Float, roll: Float) {
        val rollMax = mHudPitchRoll.getRollMax()
        val pitchMax = mHudPitchRoll.getPitchMax()

        //Limit the rate of updates
        val timeSinceLastUpdate = System.currentTimeMillis() - mLastPitchRollUpdate
        if (timeSinceLastUpdate > UPDATE_DELAY) {
            val actualRoll = roll * rollMax
            val actualPitch = pitch * pitchMax
            mHudPitchRoll.setPitchRoll(actualPitch, actualRoll)
        }
    }
}