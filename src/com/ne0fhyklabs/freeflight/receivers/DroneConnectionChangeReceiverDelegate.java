package com.ne0fhyklabs.freeflight.receivers;

public interface DroneConnectionChangeReceiverDelegate 
{
	public void onDroneConnected();
	public void onDroneDisconnected();
}
