package com.ne0fhyklabs.freeflight.receivers;

import android.net.NetworkInfo;


public interface NetworkChangeReceiverDelegate 
{
	public void onNetworkChanged(NetworkInfo info);
}
