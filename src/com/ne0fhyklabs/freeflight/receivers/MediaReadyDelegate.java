package com.ne0fhyklabs.freeflight.receivers;

import java.io.File;

public interface MediaReadyDelegate
{
    public void onMediaReady(File mediaFile);
}
