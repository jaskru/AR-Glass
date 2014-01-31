package com.ne0fhyklabs.freeflight.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.activities.ConnectActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * This is used to automatically connect to the drone wifi network,
 * before attempting communication with the drone.
 *
 * @author Fredia Huya-Kouadio
 */
public class PreConnectionFragment extends DialogFragment {

    public static final String TAG = PreConnectionFragment.class.getSimpleName();

    /**
     * These are the possible security modes that can be returned by a scan result.
     */
    public static final String[] WIFI_SECURITY_MODES = {"WEP", "PSK", "EAP"};

    private static final IntentFilter sIntentFilter = new IntentFilter();

    static {
        sIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        sIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    private ProgressBar mScanProgressBar;
    private ListView mWifiApsView;

    private String mARParrotSsid;
    private View mARParrotApView;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                if (mWifiApsView.getAdapter() == null) {
                    //Retrieve the scan results, and update the listview.
                    final WifiManager wm = (WifiManager) context.getSystemService(Context
                            .WIFI_SERVICE);
                    List<ScanResult> scanResults = wm.getScanResults();
                    List<ScanResult> openAps = new ArrayList<ScanResult>();
                    for (ScanResult result : scanResults) {
                        boolean isApOpen = true;
                        for (String securityMode : WIFI_SECURITY_MODES) {
                            if (result.capabilities.contains(securityMode)) {
                                isApOpen = false;
                                break;
                            }
                        }

                        if (isApOpen) {
                            openAps.add(result);
                        }
                    }

                    if (openAps.isEmpty()) {
                        Toast.makeText(context, "Unable to find AR Parrot wifi access point!",
                                Toast.LENGTH_LONG).show();
                        dismiss();
                    }
                    else {

                        //Update the listview adapter with the discovered access points.
                        mWifiApsView.setAdapter(new ScanResultAdapter(context, openAps));
                    }

                    //Hide the scanning progress bar
                    mScanProgressBar.setVisibility(View.GONE);
                }
            }
            else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                if (mARParrotSsid != null) {
                    //Check if we're connected to the AR Parrot wifi ap
                    final NetworkInfo netInfo = intent.getParcelableExtra(WifiManager
                            .EXTRA_NETWORK_INFO);

                    if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        final String networkSsid = netInfo.getExtraInfo();
                        if (mARParrotSsid.equals(networkSsid)) {
                            if (netInfo.isConnected()) {
                                //Dismiss the dialog, and launch the ConnectActivity
                                dismiss();

                                Intent connectActivity = new Intent(context,
                                        ConnectActivity.class).addFlags
                                        (Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(connectActivity);
                            }
                            else {
                                //Update the connection state in the wifi ap list view
                                if (mARParrotApView != null) {
                                    String connectionState = "";
                                    switch(netInfo.getDetailedState()){
                                        case CONNECTED:
                                            connectionState = "connected";
                                            break;

                                        case CONNECTING:
                                            connectionState = "connecting...";
                                            break;

                                        case OBTAINING_IPADDR:
                                            connectionState = "obtaining ip address...";
                                            break;

                                        case FAILED:
                                            Toast.makeText(context,
                                                    "Unable to connect to " + mARParrotSsid +
                                                            "!", Toast.LENGTH_LONG).show();
                                            dismiss();
                                            break;
                                    }

                                    TextView connectionStateView = (TextView) mARParrotApView
                                            .findViewById(R.id.wifi_ap_network_state);
                                    connectionStateView.setText(connectionState);
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pre_connection_screen, container,
                false);

        mWifiApsView = (ListView) view.findViewById(R.id.wifi_aps);
        mWifiApsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Don't register the click if connection is already processing on the same view.
                if(mARParrotApView == view)
                    return;

                final ScanResult scanResult = (ScanResult) parent.getItemAtPosition(position);

                final WifiConfiguration wifiConf = new WifiConfiguration();

                //Please note the quotes. String should contain ssid in quotes.
                mARParrotApView = view;
                mARParrotSsid = "\"" + scanResult.SSID + "\"";
                wifiConf.SSID = mARParrotSsid;

                //Key management for OPEN network
                wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                final WifiManager wm = (WifiManager) getActivity().getSystemService(Context
                        .WIFI_SERVICE);
                int networkId = wm.addNetwork(wifiConf);

                //Enable it and connect to it.
                wm.disconnect();
                final boolean isWifiEnabled = wm.enableNetwork(networkId, true);
                final boolean isWifiConnecting = wm.reconnect();
                if (isWifiEnabled && isWifiConnecting) {
                    ProgressBar connectionProgress = (ProgressBar) view.findViewById(R.id
                            .wifi_ap_connection_progress);
                    if (connectionProgress != null) {
                        connectionProgress.setIndeterminate(true);
                        connectionProgress.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        mScanProgressBar = (ProgressBar) view.findViewById(R.id.wifi_scan_progress_bar);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        //Register for wifi access points scan result
        getActivity().registerReceiver(mReceiver, sIntentFilter);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Fill the listview with discovered wifi access points.
        queryWifiAps();
    }

    private void queryWifiAps() {
        final Context context = getActivity().getApplicationContext();
        final WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        //Enable the wifi network
        if (!wm.setWifiEnabled(true)) {
            Toast.makeText(context, "Unable to activate wifi!", Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }

        //Scan for access points.
        if (!wm.startScan()) {
            Toast.makeText(context, "Unable to scan for wifi networks!", Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }

        //Show the scanning progress bar
        mScanProgressBar.setVisibility(View.VISIBLE);
    }

    private static class ScanResultAdapter extends ArrayAdapter<ScanResult> {

        /**
         * This is used to inflate the item views.
         */
        private final LayoutInflater mInflater;

        public ScanResultAdapter(Context context, List<ScanResult> scanResults) {
            super(context, 0, scanResults);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View wifiApView;
            if (convertView == null) {
                wifiApView = mInflater.inflate(R.layout.wifi_ap_list_item, parent, false);
            }
            else {
                wifiApView = convertView;
            }

            TextView ssidView = (TextView) wifiApView.findViewById(R.id.wifi_ap_ssid);
            ssidView.setText(getItem(position).SSID);

            return wifiApView;
        }

    }
}