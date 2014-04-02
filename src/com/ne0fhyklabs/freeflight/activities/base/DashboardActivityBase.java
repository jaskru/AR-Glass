package com.ne0fhyklabs.freeflight.activities.base;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.receivers.MediaStorageReceiver;
import com.ne0fhyklabs.freeflight.receivers.MediaStorageReceiverDelegate;
import com.ne0fhyklabs.freeflight.utils.GlassUtils;

@SuppressLint("Registered")
// There is no need to register this activity in the manifest as this is a base activity for others.
public abstract class DashboardActivityBase extends FragmentActivity implements MediaStorageReceiverDelegate {
    protected static final String TAG = "DashboardActivity";

    public enum EPhotoVideoState {
        UNKNOWN,
        READY,
        NO_MEDIA,
        NO_SDCARD
    }

    /**
     * Set of dashboard screen names.
     */
    private static final int[] sDashboardScreenNames = {R.string.PILOTING,
            R.string.PHOTOS_VIDEOS, R.string.ar_settings};

    private CheckedTextView btnFreeFlight;
    private ImageView mBtnFreeFlightShadow;

    private CheckedTextView btnPhotosVideos;
    private ImageView mBtnPhotosVideosShadow;

    private CheckedTextView btnSettings;
    private ImageView mBtnSettingsShadow;

    private AlertDialog alertDialog;

    private MediaStorageReceiver externalStorageStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(GlassUtils.instance$.isGlassDevice()){
            setupGlassDashboard();
        }
        else{
            setupRegularDashboard();
        }

        initBroadcastReceivers();
    }

    private void initBroadcastReceivers() {
        externalStorageStateReceiver = new MediaStorageReceiver(this);
    }


    private void initUI() {
        btnFreeFlight = (CheckedTextView) findViewById(R.id.btnFreeFlight);
        btnFreeFlight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open freeflight
                if (!onStartFreeflight()) {
                    showErrorMessageForTime(v,
                            getString(R.string.wifi_not_available_please_connect_device_to_drone)
                            , 2000);
                }
            }
        });
        mBtnFreeFlightShadow = (ImageView) findViewById(R.id.btnFreeFlightShadow);

        btnPhotosVideos = (CheckedTextView) findViewById(R.id.btnPhotosVideos);
        btnPhotosVideos.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open photos/videos
                EPhotoVideoState state = getPhotoVideoState();
                switch (state) {
                    case READY:
                        onStartPhotosVideos();
                        break;
                    case NO_MEDIA:
                        showErrorMessageForTime(v,
                                getString(R.string
                                        .there_is_no_flight_photos_or_videos_saved_in_your_phone)
                                , 2000);
                        break;
                    case NO_SDCARD:
                        showErrorMessageForTime(v, getString(R.string.NO_SD_CARD_INSERTED), 2000);
                        break;
                    default:
                        Log.w(TAG, "Unknown media state " + state.name());
                        break;
                }
            }
        });
        mBtnPhotosVideosShadow = (ImageView) findViewById(R.id.btnPhotosVideosShadow);

        btnSettings = (CheckedTextView) findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open drone settings
                onStartSettings();
            }
        });
        mBtnSettingsShadow = (ImageView) findViewById(R.id.btnSettingsShadow);
    }

    private void setupRegularDashboard(){
        setContentView(R.layout.dashboard_screen);
        initUI();
    }

    private void setupGlassDashboard(){
        setContentView(R.layout.activity_glass_dashboard);

        final CardScrollView cardsView = (CardScrollView) findViewById(R.id.glass_dashboard);
        cardsView.setAdapter(new DashboardAdapter(getApplicationContext()));
        cardsView.setHorizontalScrollBarEnabled(true);
        cardsView.activate();
        cardsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.playSoundEffect(SoundEffectConstants.CLICK);

                final int screenNameRes = (Integer) parent.getItemAtPosition(position);
                switch(screenNameRes){
                    case R.string.PILOTING:
                        onStartFreeflight();
                        break;

                    case R.string.PHOTOS_VIDEOS:
                        onStartPhotosVideos();
                        break;

                    case R.string.ar_settings:
                        onStartSettings();
                        break;
                }
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        externalStorageStateReceiver.unregisterFromEvents(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        requestUpdateButtonsState();

        externalStorageStateReceiver.registerForEvents(this);
    }


    public void requestUpdateButtonsState() {
        if (Looper.myLooper() == null)
            throw new IllegalStateException("Should be called from UI thread");

        if(!GlassUtils.instance$.isGlassDevice()) {
            if (isFreeFlightEnabled()) {
                btnFreeFlight.setChecked(true);
                mBtnFreeFlightShadow.setVisibility(View.VISIBLE);
            } else {
                btnFreeFlight.setChecked(false);
                mBtnFreeFlightShadow.setVisibility(View.INVISIBLE);
            }

            if (getPhotoVideoState().equals(EPhotoVideoState.READY)) {
                btnPhotosVideos.setChecked(true);
                mBtnPhotosVideosShadow.setVisibility(View.VISIBLE);
            } else {
                btnPhotosVideos.setChecked(false);
                mBtnPhotosVideosShadow.setVisibility(View.INVISIBLE);
            }

            btnSettings.setChecked(true);
            mBtnSettingsShadow.setVisibility(View.VISIBLE);
        }
        else{
            //TODO: complete
        }
    }

    protected abstract boolean isFreeFlightEnabled();

    protected abstract EPhotoVideoState getPhotoVideoState();

    protected abstract boolean onStartFreeflight();

    protected abstract boolean onStartPhotosVideos();

    protected abstract boolean onStartSettings();

    private void showErrorMessageForTime(View v, String string, int i) {
        final View oldView = v;
        final ViewGroup parent = (ViewGroup) v.getParent();
        final int index = parent.indexOfChild(v);

        TextView buttonNok = (TextView) v.getTag();

        if (buttonNok == null) {
            final LayoutInflater inflater = (LayoutInflater) getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);

            buttonNok = (TextView) inflater.inflate(R.layout.dashboard_button_nok, parent, false);
            buttonNok.setLayoutParams(v.getLayoutParams());
            v.setTag(buttonNok);
        }

        buttonNok.setText(string);

        parent.removeView(v);
        parent.addView(buttonNok, index);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                parent.removeViewAt(index);
                parent.addView(oldView, index);
            }
        };

        parent.postDelayed(runnable, i);
    }


    protected void showAlertDialog(String title, String message, final Runnable actionOnDismiss) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialog = alertDialogBuilder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        if (actionOnDismiss != null) {
                            actionOnDismiss.run();
                        }
                    }
                }).create();

        alertDialog.show();
    }

    @Override
    public void onMediaEject() {
        // Left unimplemented
    }

    private static class DashboardAdapter extends CardScrollAdapter {

        private final Context mContext;

        public DashboardAdapter(Context context){
            super();
            mContext = context;
        }

        @Override
        public int getCount() {
            return sDashboardScreenNames.length;
        }

        @Override
        public Object getItem(int i) {
            return sDashboardScreenNames[i];
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            final Card card = new Card(mContext);
            card.setText(sDashboardScreenNames[i]);

            return card.toView();
        }

        @Override
        public int findIdPosition(Object o) {
            return -1;
        }

        @Override
        public int findItemPosition(Object o) {
            int defaultId = -1;
            if(!(o instanceof Integer))
                return defaultId;

            int screenNameRes = (Integer) o;
            for(int i = 0; i < getCount(); i++){
                if(screenNameRes == getItem(i))
                    return i;
            }

            return defaultId;
        }
    }

}
