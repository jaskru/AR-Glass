package com.ne0fhyklabs.freeflight.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.activities.SettingsActivity;
import com.ne0fhyklabs.freeflight.service.DroneControlService;
import com.ne0fhyklabs.freeflight.ui.controls.SeekBarPreference;

/**
 * AR Glass preference fragment.
 */
public class SettingsFragment extends PreferenceFragment {

    /**
     * Provides access to the parent activity.
     */
    private SettingsActivity mParent;

    /**
     * Handle to the drone configuration.
     */
    private DroneControlService mDroneService;

    /**
     * Preference change listener used to handle preference updates.
     */
    private final Preference.OnPreferenceChangeListener mPrefChangeListener = new Preference
            .OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final String prefKey = preference.getKey();
            if(prefKey.equals(getString(R.string.key_abs_control))){

            }
            return false;
        }
    };

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        if(!(activity instanceof SettingsActivity)){
            throw new IllegalStateException("Parent activity must be an instance of" +
                    SettingsActivity.class                    .getName());
        }

        mParent = (SettingsActivity) activity;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        mParent = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mDroneService = mParent.getDroneControlService();
        setupPreferences();
    }

    private void setupPreferences(){
        final PreferenceManager prefs = getPreferenceManager();

        /*
        Piloting mode section
         */
        final CheckBoxPreference absControlPref = (CheckBoxPreference) prefs.findPreference
                (getString(R.string.key_abs_control));
        if(absControlPref != null){
            absControlPref.setOnPreferenceChangeListener(mPrefChangeListener);
        }

        final Preference calibrationPref = prefs.findPreference(getString(R.string
                .key_abs_control_calibration));
        if(calibrationPref != null){
            calibrationPref.setOnPreferenceChangeListener(mPrefChangeListener);
        }

        final SeekBarPreference deviceTilt = (SeekBarPreference) prefs.findPreference(getString(R
                .string.key_device_tilt));
        if(deviceTilt != null){
            deviceTilt.setOnPreferenceChangeListener(mPrefChangeListener);
        }

        /*
        Status section
         */
        final Preference arHardwareVersion = prefs.findPreference(getString(R.string
                .key_ar_hardware_version));
        if(arHardwareVersion != null){

        }

        final Preference arSoftwareVersion = prefs.findPreference(getString(R.string
                .key_ar_software_version));
        if(arSoftwareVersion != null){

        }

        /*
        Personal settings
         */
        final Preference networkPref = prefs.findPreference(getText(R.string.key_network_name));
        if(networkPref != null){

        }

        final CheckBoxPreference pairingPref = (CheckBoxPreference) prefs.findPreference(getText
                (R.string.key_network_pairing));
        if(pairingPref != null){

        }
		
		final CheckBoxPreference usbRecordPref = (CheckBoxPreference) prefs.findPreference(null);
		if(usbRecordPref != null){
			
		}
		
		final CheckBoxPreference flipPref = (CheckBoxPreference) prefs.findPreference(null);
		if(flipPref != null){
			
		}
		
		/*
		Flight section
		*/
		final SeekBarPreference altPref = (SeekBarPreference) prefs.findPreference(null);
		if(altPref!= null){
			
		}
		
		final SeekBarPreference vertSpeedPref = (SeekBarPreference) prefs.findPreference(null);
		if(vertSpeedPref != null){
			
		}
		
		final SeekBarPreference yawSpeedPref = (SeekBarPreference) prefs.findPreference(null);
		if(yawSpeedPref != null){
			
		}
		
		final SeekBarPreference tiltPref = (SeekBarPreference) prefs.findPreference(null);
		if(tiltPref != null){
			
		}
		
		final CheckBoxPreference outdoorHullPref = (CheckBoxPreference) prefs.findPreference(null);
		if(outdoorHullPref != null){
			
		}
		
		final CheckBoxPreference outdoorFlightPref = (CheckBoxPreference) prefs.findPreference(null);
		if(outdoorFlightPref != null){
			
		}
    }

}
