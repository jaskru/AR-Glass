package com.ne0fhyklabs.freeflight.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.ne0fhyklabs.freeflight.R;

/**
 * This adapter is used to populate the action bar spinner menu for the MediaActivity class.
 *
 * @author Fredia Huya-Kouadio
 */
public class MediaSortSpinnerAdapter extends BaseAdapter {

    private final static int[] sSortOptionsId = {
            R.id.rbtn_all,
            R.id.rbtn_images,
            R.id.rbtn_videos
    };

    private final static int[] sSortOptionsLabelRes = {
            R.string.media_sort_photos_and_videos,
            R.string.media_sort_photos,
            R.string.media_sort_videos
    };

    private final static int[] sSortOptionsIconRes = {
            R.drawable.rbtn_photo_video,
            R.drawable.rbtn_photo,
            R.drawable.rbtn_video
    };

    /**
     * This is used to inflate the spinner item views.
     */
    private final LayoutInflater mInflater;

    public MediaSortSpinnerAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return sSortOptionsId.length;
    }

    @Override
    public Object getItem(int position) {
        return sSortOptionsId[position];
    }

    @Override
    public long getItemId(int position) {
        return sSortOptionsId[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView spinnerItemView;
        if(convertView == null){
            spinnerItemView = (TextView) mInflater.inflate(R.layout.media_sort_spinner_item,
                    parent, false);
        }
        else spinnerItemView = (TextView) convertView;

        spinnerItemView.setId(sSortOptionsId[position]);
        spinnerItemView.setText(sSortOptionsLabelRes[position]);
        spinnerItemView.setCompoundDrawablesWithIntrinsicBounds(sSortOptionsIconRes[position], 0,
         0, 0);
        return spinnerItemView;
    }

}
