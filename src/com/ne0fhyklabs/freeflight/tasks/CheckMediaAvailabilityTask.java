
package com.ne0fhyklabs.freeflight.tasks;

import android.content.Context;
import android.os.AsyncTask;
import com.ne0fhyklabs.freeflight.utils.ARDroneMediaGallery;

public class CheckMediaAvailabilityTask extends AsyncTask<Void, Void, Boolean>
{
    private Context context;
    
    public CheckMediaAvailabilityTask(Context context)
    {
        this.context = context;
    }


    @Override
    protected Boolean doInBackground(final Void... params)
    {
        ARDroneMediaGallery gallery = new ARDroneMediaGallery(context);
        return gallery.countOfMedia() > 0;
    }
}
