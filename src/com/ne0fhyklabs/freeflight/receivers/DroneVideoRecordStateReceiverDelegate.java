package com.ne0fhyklabs.freeflight.receivers;

public interface DroneVideoRecordStateReceiverDelegate 
{
	public void onDroneRecordVideoStateChanged(boolean recording, boolean usbActive, int remainingTime);
}
