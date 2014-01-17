package com.ne0fhyklabs.freeflight.sensors;

public interface DeviceOrientationChangeDelegate
{
    public void onDeviceOrientationChanged(float[] orientation, float magneticHeading, int magnetoAccuracy);
}
