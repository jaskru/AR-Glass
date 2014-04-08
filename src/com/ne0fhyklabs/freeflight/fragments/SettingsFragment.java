package com.ne0fhyklabs.freeflight.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.glass.media.Sounds;
import com.ne0fhyklabs.freeflight.FreeFlightApplication;
import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.controllers.Controller;
import com.ne0fhyklabs.freeflight.drone.DroneConfig;
import com.ne0fhyklabs.freeflight.receivers.DroneConfigChangedReceiver;
import com.ne0fhyklabs.freeflight.receivers.DroneConfigChangedReceiverDelegate;
import com.ne0fhyklabs.freeflight.receivers.DroneConnectionChangeReceiverDelegate;
import com.ne0fhyklabs.freeflight.receivers.DroneConnectionChangedReceiver;
import com.ne0fhyklabs.freeflight.receivers.NetworkChangeReceiver;
import com.ne0fhyklabs.freeflight.receivers.NetworkChangeReceiverDelegate;
import com.ne0fhyklabs.freeflight.service.DroneControlService;
import com.ne0fhyklabs.freeflight.settings.ApplicationSettings;
import com.ne0fhyklabs.freeflight.ui.controls.SeekBarPreference;

/**
 * AR Glass preference fragment.
 */
public class SettingsFragment extends PreferenceFragment {

    /**
     * Used as tag for logging.
     */
    private static final String TAG = SettingsFragment.class.getSimpleName();

    /**
     * Activities hosting this fragment must implement this interface.
     */
    public interface OnSettingsHandler {
        public DroneControlService getDroneControlService();

        public Controller getController();

    }

    private static final String NULL_MAC = "00:00:00:00:00:00";

    /**
     * Provides access to the parent activity.
     */
    private OnSettingsHandler mParent;

    /**
     * Handle to the drone configuration.
     */
    private DroneControlService mDroneService;

    /**
     * App device wifi mac address.
     */
    private String mAppMac;

    /**
     * Receiver object for the drone config update broadcasts.
     */
    private final DroneConfigChangedReceiver mConfigChangedReceiver = new DroneConfigChangedReceiver(new DroneConfigChangedReceiverDelegate() {

            @Override
            public void onDroneConfigChanged() {
                setupDronePreferences();
            }
        });
		
	private final DroneConnectionChangedReceiver mConnChangedReceiver = new DroneConnectionChangedReceiver(new DroneConnectionChangeReceiverDelegate(){
		@Override
		public void onDroneConnected(){
			setupDronePreferences();
		}
		
		@Override
		public void onDroneDisconnected(){
			setupDronePreferences();
		}
	});

    private final NetworkChangeReceiver mNetChangeReceiver = new NetworkChangeReceiver(new NetworkChangeReceiverDelegate() {

        @Override
        public void onNetworkChanged(NetworkInfo info) {
            setupDronePreferences();
        }
    });

    /**
     * Drone preference change listener used to handle drone preference updates.
     */
    private final Preference.OnPreferenceChangeListener mDronePrefChangeListener = new Preference
            .OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final DroneConfig droneConfig = mDroneService.getDroneConfig();
            if (droneConfig == null)
                return false;

            final String prefKey = preference.getKey();
            if (prefKey == null)
                return false;

            if (prefKey.equals(getString(R.string.key_alt_limit))) {
                droneConfig.setAltitudeLimit((Integer) newValue);
            } else if (prefKey.equals(getString(R.string.key_device_tilt))) {
                droneConfig.setDeviceTiltMax((Integer) newValue);
            } else if (prefKey.equals(getString(R.string.key_yaw_speed))) {
                droneConfig.setYawSpeedMax((Integer) newValue);
            } else if (prefKey.equals(getString(R.string.key_vert_speed))) {
                droneConfig.setVertSpeedMax((Integer) newValue);
            } else if (prefKey.equals(getString(R.string.key_drone_tilt))) {
                droneConfig.setTilt((Integer) newValue);
            } else if (prefKey.equals(getString(R.string.key_usb_record))) {
                droneConfig.setRecordOnUsb((Boolean) newValue);
            } else if (prefKey.equals(getString(R.string.key_network_pairing))) {
                final boolean isChecked = (Boolean) newValue;
                droneConfig.setOwnerMac(isChecked ? mAppMac : NULL_MAC);
            } else if (prefKey.equals(getString(R.string.key_outdoor_hull))) {
                droneConfig.setOutdoorHull((Boolean) newValue);
            } else if (prefKey.equals(getString(R.string.key_outdoor_flight))) {
                droneConfig.setOutdoorFlight((Boolean) newValue);
                mDroneService.triggerConfigUpdate();
            }
            return true;
        }
    };

    /**
     * Preference change listener used to update the app preferences.
     */
    private final Preference.OnPreferenceChangeListener mAppPrefChangeListener = new Preference
            .OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            final ApplicationSettings appSettings = ((FreeFlightApplication) getActivity()
                    .getApplicationContext()).getAppSettings();
            if (appSettings == null)
                return false;

            final String prefKey = preference.getKey();
            if (prefKey == null)
                return false;

            if (prefKey.equals(getString(R.string.key_flip_enabled))) {
                appSettings.setFlipEnabled((Boolean) newValue);
            }
            return true;
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof OnSettingsHandler)) {
            throw new IllegalStateException("Parent activity must be an instance of" +
                    OnSettingsHandler.class.getName());
        }

        mParent = (OnSettingsHandler) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mParent = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().addFlags(android.view.WindowManager.LayoutParams
                .FLAG_KEEP_SCREEN_ON);
        addPreferencesFromResource(R.xml.settings);

        mDroneService = mParent.getDroneControlService();

        setupPreferences();
    }

    @Override
    public void onStart() {
        super.onStart();

        final Context context = getActivity().getApplicationContext();
        mAppMac = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE))
                .getConnectionInfo().getMacAddress();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
		lbm.registerReceiver(mConfigChangedReceiver, new IntentFilter(DroneControlService
																	  .DRONE_CONFIG_STATE_CHANGED_ACTION));
		lbm.registerReceiver(mConnChangedReceiver, new IntentFilter(DroneControlService.DRONE_CONNECTION_CHANGED_ACTION));

        getActivity().registerReceiver(mNetChangeReceiver, new IntentFilter(WifiManager
                .NETWORK_STATE_CHANGED_ACTION));

        Controller controller = mParent.getController();
        if(controller != null){
            controller.pause();
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        final Context context = getActivity().getApplicationContext();
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
                lbm.unregisterReceiver(mConfigChangedReceiver);
				lbm.unregisterReceiver(mConnChangedReceiver);
        getActivity().unregisterReceiver(mNetChangeReceiver);

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audio.playSoundEffect(Sounds.DISMISSED);
    }

    private void setupPreferences() {
        setupAppPreferences();
        setupDronePreferences();
    }

    /**
     * Setup the app preferences.
     */
    private void setupAppPreferences() {
        final Context context = getActivity().getApplicationContext();
        final ApplicationSettings appSettings = ((FreeFlightApplication) context).getAppSettings();
        final PreferenceManager prefs = getPreferenceManager();

        final CheckBoxPreference flipPref = (CheckBoxPreference) prefs.findPreference(getText(R
                .string.key_flip_enabled));
        if (flipPref != null) {
            flipPref.setChecked(appSettings.isFlipEnabled());
            flipPref.setOnPreferenceChangeListener(mAppPrefChangeListener);
        }

        final Preference appVersion = prefs.findPreference(getText(R.string.key_app_version));
        if(appVersion != null){
            try {
                final PackageInfo pInfo = context.getPackageManager().getPackageInfo(context
                        .getPackageName(), 0);
                appVersion.setSummary(pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Unable to get app version.", e);
            }
        }
    }

    /**
     * Setup the drone preferences.
     */
    private void setupDronePreferences() {
        final DroneConfig droneConfig = mDroneService.getDroneConfig();
        final PreferenceManager prefs = getPreferenceManager();

        final boolean isDroneConnected = mDroneService.isDroneConnected();

        final Preference networkPref = prefs.findPreference(getText(R.string.key_network_name));
        if (networkPref != null) {
            networkPref.setSummary(isDroneConnected ? droneConfig.getNetworkName() : "---");
            networkPref.setEnabled(isDroneConnected);
        }

        final Preference arHardwareVersion = prefs.findPreference(getString(R.string
                .key_ar_hardware_version));
        if (arHardwareVersion != null) {
            arHardwareVersion.setSummary(isDroneConnected ? droneConfig.getHardwareVersion():
                    "--");
        }

        final Preference arSoftwareVersion = prefs.findPreference(getString(R.string
                .key_ar_software_version));
        if (arSoftwareVersion != null) {
            arSoftwareVersion.setSummary(isDroneConnected ? droneConfig.getSoftwareVersion():
                    "--");
        }

        final Preference flatTrimPref = prefs.findPreference(getText(R.string.key_flat_trim));
        if (flatTrimPref != null) {
            flatTrimPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (mDroneService == null)
                        return false;

                    mDroneService.flatTrim();
                    return true;
                }
            });
        }

        final CheckBoxPreference usbRecordPref = (CheckBoxPreference) prefs.findPreference
                (getText(R.string.key_usb_record));
        if (usbRecordPref != null) {
            usbRecordPref.setChecked(droneConfig.isRecordOnUsb());
            usbRecordPref.setOnPreferenceChangeListener(mDronePrefChangeListener);
        }

        final SeekBarPreference deviceTilt = (SeekBarPreference) prefs.findPreference(getString(R
                .string.key_device_tilt));
        if (deviceTilt != null) {
            deviceTilt.setValue(droneConfig.getDeviceTiltMax());
            deviceTilt.setOnPreferenceChangeListener(mDronePrefChangeListener);
        }

        final CheckBoxPreference pairingPref = (CheckBoxPreference) prefs.findPreference(getText
                (R.string.key_network_pairing));
        if (pairingPref != null) {
            final String ownerMac = droneConfig.getOwnerMac();
            if (ownerMac != null && !ownerMac.equalsIgnoreCase(NULL_MAC))
                pairingPref.setChecked(true);
            else
                pairingPref.setChecked(false);
            pairingPref.setOnPreferenceChangeListener(mDronePrefChangeListener);
        }

        final SeekBarPreference altPref = (SeekBarPreference) prefs.findPreference(getText(R
                .string.key_alt_limit));
        if (altPref != null) {
            altPref.setValue(droneConfig.getAltitudeLimit());
            altPref.setOnPreferenceChangeListener(mDronePrefChangeListener);
        }

        final SeekBarPreference vertSpeedPref = (SeekBarPreference) prefs.findPreference(getText(R
                .string.key_vert_speed));
        if (vertSpeedPref != null) {
            vertSpeedPref.setValue(droneConfig.getVertSpeedMax());
            vertSpeedPref.setOnPreferenceChangeListener(mDronePrefChangeListener);
        }

        final SeekBarPreference yawSpeedPref = (SeekBarPreference) prefs.findPreference(getText(R
                .string.key_yaw_speed));
        if (yawSpeedPref != null) {
            yawSpeedPref.setValue(droneConfig.getYawSpeedMax());
            yawSpeedPref.setOnPreferenceChangeListener(mDronePrefChangeListener);
        }

        final SeekBarPreference tiltPref = (SeekBarPreference) prefs.findPreference(getText(R
                .string.key_drone_tilt));
        if (tiltPref != null) {
            tiltPref.setValue(droneConfig.getTilt());
            tiltPref.setOnPreferenceChangeListener(mDronePrefChangeListener);
        }

        final CheckBoxPreference outdoorHullPref = (CheckBoxPreference) prefs.findPreference(getText(R
                .string.key_outdoor_hull));
        if (outdoorHullPref != null) {
            outdoorHullPref.setChecked(droneConfig.isOutdoorHull());
            outdoorHullPref.setOnPreferenceChangeListener(mDronePrefChangeListener);
        }

        final CheckBoxPreference outdoorFlightPref = (CheckBoxPreference) prefs.findPreference(getText(R
                .string.key_outdoor_flight));
        if (outdoorFlightPref != null) {
            outdoorFlightPref.setChecked(droneConfig.isOutdoorFlight());
            outdoorFlightPref.setOnPreferenceChangeListener(mDronePrefChangeListener);
        }
    }

}
