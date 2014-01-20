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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;
import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.receivers.MediaStorageReceiver;
import com.ne0fhyklabs.freeflight.receivers.MediaStorageReceiverDelegate;

@SuppressLint("Registered")
// There is no need to register this activity in the manifest as this is a base activity for others.
public abstract class DashboardActivityBase extends FragmentActivity implements OnClickListener,
        MediaStorageReceiverDelegate {
	protected static final String TAG = "DashboardActivity";

	public enum EPhotoVideoState {
	    UNKNOWN,
	    READY,
	    NO_MEDIA,
	    NO_SDCARD
	}

	private CheckedTextView btnFreeFlight;
	private CheckedTextView btnPhotosVideos;
	private CheckedTextView btnSettings;

    private AlertDialog alertDialog;

    private MediaStorageReceiver externalStorageStateReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dashboard_screen);

		initUI();
		initListeners();
		initBroadcastReceivers();
	}

	private void initBroadcastReceivers()    {
       externalStorageStateReceiver = new MediaStorageReceiver(this);
    }


    private void initUI()	{
        btnFreeFlight = (CheckedTextView) findViewById(R.id.btnFreeFlight);
		btnPhotosVideos   = (CheckedTextView) findViewById(R.id.btnPhotosVideos);
		btnSettings = (CheckedTextView) findViewById(R.id.btnSettings);
	}


	private void initListeners()	{
		btnFreeFlight.setOnClickListener(this);
		btnPhotosVideos.setOnClickListener(this);
		btnSettings.setOnClickListener(this);
	}


	@Override
	protected void onPause()
	{
		super.onPause();

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        externalStorageStateReceiver.unregisterFromEvents(this);
	}


	@Override
	protected void onResume()	{
		super.onResume();
		requestUpdateButtonsState();

		externalStorageStateReceiver.registerForEvents(this);
	}


	public void requestUpdateButtonsState()	{
		if (Looper.myLooper() == null)
			throw new IllegalStateException("Should be called from UI thread");

		btnFreeFlight.setChecked(isFreeFlightEnabled());
		btnPhotosVideos.setChecked(getPhotoVideoState().equals(EPhotoVideoState.READY));
		btnSettings.setChecked(true);
	}


	@Override
    public void onClick(View v)	{
		switch (v.getId())
		{
			case R.id.btnFreeFlight:
				// Open freeflight
				if (/*!isFreeFlightEnabled() ||*/ !onStartFreeflight())
				{
					showErrorMessageForTime(v, getString(R.string.wifi_not_available_please_connect_device_to_drone), 2000);
				}

				break;

			case R.id.btnPhotosVideos:
				// Open photos/videos
			    EPhotoVideoState state = getPhotoVideoState();
			    switch (state) {
			    case READY:
			        onStartPhotosVideos();
			        break;
			    case NO_MEDIA:
			        showErrorMessageForTime(v, getString(R.string.there_is_no_flight_photos_or_videos_saved_in_your_phone), 2000);
			        break;
			    case NO_SDCARD:
			        showErrorMessageForTime(v, getString(R.string.NO_SD_CARD_INSERTED), 2000);
			        break;
		        default:
		            Log.w(TAG, "Unknown media state " + state.name());
			        break;
			    }
				break;

            case R.id.btnSettings:
				// Open drone update
				onStartSettings();
				break;

		}
	}

	protected abstract boolean isFreeFlightEnabled();

	protected abstract EPhotoVideoState getPhotoVideoState();

	protected abstract boolean onStartFreeflight();

	protected abstract boolean onStartPhotosVideos();

    protected abstract boolean onStartSettings();

	private void showErrorMessageForTime(View v, String string, int i)
	{
		final View oldView = v;
		final ViewGroup parent = (ViewGroup) v.getParent();
		final int index = parent.indexOfChild(v);

		TextView buttonNok = (TextView) v.getTag();

		if (buttonNok == null)
		{
            final LayoutInflater inflater = (LayoutInflater) getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);

			buttonNok = (TextView) inflater.inflate(R.layout.dashboard_button_nok, parent, false);
			buttonNok.setLayoutParams(v.getLayoutParams());
			v.setTag(buttonNok);
		}

		buttonNok.setText(string);

		parent.removeView(v);
		parent.addView(buttonNok, index);

		Runnable runnable = new Runnable()
		{
			@Override
            public void run()
			{
				parent.removeViewAt(index);
				parent.addView(oldView, index);
			}
		};

		parent.postDelayed(runnable, i);
	}


    protected void showAlertDialog(String title, String message, final Runnable actionOnDismiss)
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialog = alertDialogBuilder.setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(getString(android.R.string.ok), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.dismiss();
                        if (actionOnDismiss != null) {
                            actionOnDismiss.run();
                        }
                    }
                }).create();

        alertDialog.show();
    }

    @Override
    public void onMediaEject()
    {
        // Left unimplemented
    }

}
