package com.ne0fhyklabs.freeflight.receivers;

public interface DroneAvailabilityDelegate 
{
	public void onDroneAvailabilityChanged(boolean isDroneOnNetwork);
}
