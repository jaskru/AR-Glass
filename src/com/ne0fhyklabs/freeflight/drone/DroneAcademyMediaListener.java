package com.ne0fhyklabs.freeflight.drone;

public interface DroneAcademyMediaListener
{
    void onNewMediaIsAvailable(String path);
    void onNewMediaToQueue(String path);
    void onQueueComplete();
}
