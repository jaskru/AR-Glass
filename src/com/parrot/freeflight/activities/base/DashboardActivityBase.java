package com.parrot.freeflight.activities.base;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.parrot.freeflight.R;
import com.parrot.freeflight.receivers.MediaStorageReceiver;
import com.parrot.freeflight.receivers.MediaStorageReceiverDelegate;
import com.parrot.freeflight.ui.StatusBar;

@SuppressLint("Registered")
// There is no need to register this activity in the manifest as this is a base activity for others.
public class DashboardActivityBase extends ParrotActivity implements OnClickListener,
        MediaStorageReceiverDelegate
{
	protected static final String TAG = "DashboardActivity";

	public enum EPhotoVideoState
	{
	    UNKNOWN,
	    READY,
	    NO_MEDIA,
	    NO_SDCARD
	}

	private StatusBar header = null;

	private CheckedTextView btnFreeFlight;
	private CheckedTextView btnPhotosVideos;
	private CheckedTextView btnFirmwareUpdate;

    private AlertDialog alertDialog;

    private MediaStorageReceiver externalStorageStateReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dashboard_screen);

		View headerView = findViewById(R.id.header_preferences);

		header = new StatusBar(this, headerView);

		initUI();
		initListeners();
		initBroadcastReceivers();
	}

	private void initBroadcastReceivers()
    {
       externalStorageStateReceiver = new MediaStorageReceiver(this);
    }


    private void initUI()
	{
        btnFreeFlight = (CheckedTextView) findViewById(R.id.btnFreeFlight);
		btnPhotosVideos   = (CheckedTextView) findViewById(R.id.btnPhotosVideos);
		btnFirmwareUpdate = (CheckedTextView) findViewById(R.id.btnFirmwareUpdate);
	}


	private void initListeners()
	{
		btnFreeFlight.setOnClickListener(this);
		btnPhotosVideos.setOnClickListener(this);
		btnFirmwareUpdate.setOnClickListener(this);
	}


	@Override
	protected void onDestroy()
	{
		super.onDestroy();
	}


	@Override
	protected void onPause()
	{
		super.onPause();
		header.stopUpdating();

        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }

        externalStorageStateReceiver.unregisterFromEvents(this);
	}


	@Override
	protected void onResume()
	{
		super.onResume();
		header.startUpdating();
		requestUpdateButtonsState();

		externalStorageStateReceiver.registerForEvents(this);
	}


	public void requestUpdateButtonsState()
	{
		if (Looper.myLooper() == null)
			throw new IllegalStateException("Should be called from UI thread");

		btnFreeFlight.setChecked(isFreeFlightEnabled());
		btnPhotosVideos.setChecked(getPhotoVideoState().equals(EPhotoVideoState.READY));
		btnFirmwareUpdate.setChecked(isFirmwareUpdateEnabled());
	}


	@Override
    public void onClick(View v)
	{
		switch (v.getId())
		{
			case R.id.btnFreeFlight:
				// Open freeflight
				if (!isFreeFlightEnabled() || !onStartFreeflight())
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

            case R.id.btnFirmwareUpdate:
				// Open drone update
				if (!isFirmwareUpdateEnabled() || !onStartFirmwareUpdate())
				{
					showErrorMessageForTime(v, getString(R.string.your_ar_drone_is_up_to_date), 2000);
				}
				break;

		}
	}

	protected boolean isFreeFlightEnabled()
	{
		return false;
	}


	protected EPhotoVideoState getPhotoVideoState()
	{
	    return EPhotoVideoState.NO_SDCARD;
	}


	@Deprecated
	protected boolean isPhotoVideosEnabled()
	{
		return false;
	}

	protected boolean isARDroneUpdateEnabled()
	{
		return false;
	}

	protected boolean isFirmwareUpdateEnabled()
	{
		return false;
	}

	protected boolean onStartFreeflight()
	{
		return false;
	}

	protected boolean onStartPhotosVideos()
	{
		return false;
	}

	protected boolean onStartFirmwareUpdate()
	{
		return false;
	}

	private void showErrorMessageForTime(View v, String string, int i)
	{
		final View oldView = v;
		final ViewGroup parent = (ViewGroup) v.getParent();
		final int index = parent.indexOfChild(v);

		TextView buttonNok = (TextView) v.getTag();

		if (buttonNok == null)
		{
			buttonNok = (TextView) inflateView(R.layout.dashboard_button_nok, parent, false);
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
    public void onMediaStorageMounted()
    {
        // Left unimplemented
    }


    @Override
    public void onMediaStorageUnmounted()
    {
       // Left unimplemented
    }


    @Override
    public void onMediaEject()
    {
        // Left unimplemented
    }

}
