
package com.ne0fhyklabs.freeflight.activities;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import com.ne0fhyklabs.freeflight.R;
import com.ne0fhyklabs.freeflight.receivers.MediaReadyDelegate;
import com.ne0fhyklabs.freeflight.receivers.MediaReadyReceiver;
import com.ne0fhyklabs.freeflight.receivers.MediaStorageReceiver;
import com.ne0fhyklabs.freeflight.receivers.MediaStorageReceiverDelegate;
import com.ne0fhyklabs.freeflight.service.DroneControlService;
import com.ne0fhyklabs.freeflight.tasks.GetMediaObjectsListTask;
import com.ne0fhyklabs.freeflight.tasks.GetMediaObjectsListTask.MediaFilter;
import com.ne0fhyklabs.freeflight.transcodeservice.TranscodingService;
import com.ne0fhyklabs.freeflight.ui.adapters.MediaAdapter;
import com.ne0fhyklabs.freeflight.ui.adapters.MediaSortSpinnerAdapter;
import com.ne0fhyklabs.freeflight.utils.ARDroneMediaGallery;
import com.ne0fhyklabs.freeflight.vo.MediaVO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This activity handles the photos and videos taken by the Parrot AR Drone.
 */
public class MediaActivity extends FragmentActivity implements
        OnItemClickListener,
        MediaReadyDelegate,
        MediaStorageReceiverDelegate {

    private enum ActionBarState {
        BROWSE, EDIT;
    }

    private static final String TAG = MediaActivity.class.getSimpleName();

    private final ArrayList<MediaVO> mediaList = new ArrayList<MediaVO>();
    private final ArrayList<MediaVO> selectedItems = new ArrayList<MediaVO>();

    private MediaFilter currentFilter = MediaFilter.ALL;

    private GridView gridView;
    private ARDroneMediaGallery mediaGallery;

    private MediaReadyReceiver mediaReadyReceiver;    // Detects when drone created new media file
    private MediaStorageReceiver mediaStorageReceiver; // Detects when SD Card becomes unmounted

    /**
     * The action mode is triggered by the 'Edit' menu button.
     */
    private ActionMode mActionMode;

    /**
     * Callback logic used by the action mode.
     */
    private final AbsListView.MultiChoiceModeListener mActionModeCallback = new AbsListView
            .MultiChoiceModeListener() {


        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {

        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            //Store the passed action mode
            mActionMode = mode;
            mode.getMenuInflater().inflate(R.menu.menu_media_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            final MenuItem selectClearItem = menu.findItem(R.id.menu_select_clear);
            if (selectClearItem != null) {
                selectClearItem.setTitle(selectedItems.size() <= 0
                        ? R.string.menu_select_all
                        : R.string.menu_clear_all);
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    onDeleteMediaClicked();
                    return true;

                case R.id.menu_select_clear:
                    switchAllItemState(selectedItems.size() <= 0);

                    ((MediaAdapter) gridView.getAdapter()).notifyDataSetChanged();
                    mode.invalidate();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            //Deselect any selected items
            switchAllItemState(false);
            final MediaAdapter adapter = (MediaAdapter) gridView.getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            //Unset the action mode instance
            mActionMode = null;
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaGallery = new ARDroneMediaGallery(this);
        setContentView(R.layout.media_screen);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);

            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(new MediaSortSpinnerAdapter
                    (getApplicationContext()),
                    new android.app.ActionBar.OnNavigationListener() {
                        @Override
                        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                            switch ((int)itemId) {
                                case R.id.rbtn_images:
                                    currentFilter = MediaFilter.IMAGES;
                                    break;

                                case R.id.rbtn_videos:
                                    currentFilter = MediaFilter.VIDEOS;
                                    break;

                                case R.id.rbtn_all:
                                default:
                                    currentFilter = MediaFilter.ALL;
                                    break;
                            }

                            onApplyMediaFilter(currentFilter);
                            return true;
                        }
                    });
        }

        mediaReadyReceiver = new MediaReadyReceiver(this);
        mediaStorageReceiver = new MediaStorageReceiver(this);
        initGallery();

        onApplyMediaFilter(currentFilter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        MediaAdapter adapter = (MediaAdapter) gridView.getAdapter();
        if (adapter != null) {
            adapter.stopThumbnailLoading();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_media_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit:
                startActionMode(mActionModeCallback);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initGallery() {
        int columnCount = getResources().getInteger(R.integer.media_gallery_columns_count);
        gridView = (GridView) findViewById(R.id.grid);
        gridView.setNumColumns(columnCount);
        gridView.setOnItemClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance
                (getApplicationContext());

        IntentFilter mediaReadyFilter = new IntentFilter();
        mediaReadyFilter.addAction(DroneControlService.NEW_MEDIA_IS_AVAILABLE_ACTION);
        mediaReadyFilter.addAction(TranscodingService.NEW_MEDIA_IS_AVAILABLE_ACTION);
        broadcastManager.registerReceiver(mediaReadyReceiver, mediaReadyFilter);
    }


    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance
                (getApplicationContext());
        broadcastManager.unregisterReceiver(mediaReadyReceiver);
    }


    @Override
    protected void onPause() {
        super.onPause();
        mediaStorageReceiver.unregisterFromEvents(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mediaStorageReceiver.registerForEvents(this);
    }


    @SuppressLint("NewApi")
    protected synchronized void onApplyMediaFilter(MediaFilter filter) {
        GetMediaObjectsListTask mediaWorkerTask = new GetMediaObjectsListTask(this, filter) {
            @Override
            protected void onPostExecute(final List<MediaVO> result) {
                mediaList.clear();
                mediaList.addAll(result);

                MediaAdapter adapter = (MediaAdapter) gridView.getAdapter();

                if (adapter == null) {
                    adapter = new MediaAdapter(MediaActivity.this, mediaList);
                    gridView.setAdapter(adapter);
                }
                else {
                    adapter.setFileList(mediaList);
                }
            }
        };

        try {

            mediaWorkerTask.executeOnExecutor(GetMediaObjectsListTask.THREAD_POOL_EXECUTOR).get();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    protected void onDeleteMediaClicked() {
        if (selectedItems.size() > 0) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("");
            alert.setMessage(R.string.delete_popup);

            alert.setPositiveButton(getString(R.string.delete_media),
                    new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            onDeleteSelectedMediaItems();
                        }
                    });
            alert.setNegativeButton(getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            dialog.cancel();
                        }
                    });

            alert.show();
        }
    }


    private void onDeleteSelectedMediaItems() {
        int size = selectedItems.size();

        List<MediaVO> photosToDelete = new ArrayList<MediaVO>();
        List<MediaVO> videosToDelete = new ArrayList<MediaVO>();

        for (int i = 0; i < size; ++i) {
            MediaVO media = selectedItems.get(i);

            if (media.isSelected()) {

                if (media.isVideo()) {
                    videosToDelete.add(media);
                }
                else {
                    photosToDelete.add(media);
                }
            }
        }

        // Deleting photos
        int countOfPhotos = photosToDelete.size();
        if (countOfPhotos > 0) {
            int[] idsToDelete = new int[countOfPhotos];

            for (int i = 0; i < countOfPhotos; ++i) {
                idsToDelete[i] = photosToDelete.get(i).getId();
            }

            mediaGallery.deleteImages(idsToDelete);
            mediaList.removeAll(photosToDelete);
        }

        // Deleting videos
        int countOfVideos = videosToDelete.size();
        if (countOfVideos > 0) {
            int[] idsToDelete = new int[countOfVideos];

            for (int i = 0; i < countOfVideos; ++i) {
                idsToDelete[i] = videosToDelete.get(i).getId();
            }

            mediaGallery.deleteVideos(idsToDelete);
            mediaList.removeAll(videosToDelete);
        }

        selectedItems.clear();

        if (mActionMode != null && (countOfPhotos > 0 || countOfVideos > 0)) {
            mActionMode.finish();
        }
    }


    protected void onPlayMediaItem(int position) {
        final Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtra(GalleryActivity.SELECTED_ELEMENT, position);
        intent.putExtra(GalleryActivity.MEDIA_FILTER, currentFilter.ordinal());

        startActivity(intent);
    }

    private void switchAllItemState(final boolean isSelected) {
        if (isSelected) {
            selectedItems.addAll(mediaList);
        }
        else {
            selectedItems.clear();
        }

        final int size = mediaList.size();

        for (int i = 0; i < size; i++) {
            final MediaVO imageDetailVO = mediaList.get(i);

            imageDetailVO.setSelected(isSelected);
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view,
                            final int position, final long id) {
        if (mActionMode == null) {
            onPlayMediaItem(position);
        }
        else {
            final LinearLayout selectedHolder = (LinearLayout) view.findViewById(R.id
                    .selected_holder);

            if (selectedHolder.isShown()) {
                selectedHolder.setVisibility(View.GONE);
                MediaVO media = mediaList.get(position);
                media.setSelected(false);

                selectedItems.remove(media);

            }
            else {
                selectedHolder.setVisibility(View.VISIBLE);
                MediaVO media = mediaList.get(position);
                media.setSelected(true);

                selectedItems.add(media);
                mActionMode.invalidate();
            }
        }
    }

    @Override
    public void onMediaReady(File mediaFile) {
        Log.d(TAG, "New file available " + mediaFile.getAbsolutePath());
        onApplyMediaFilter(currentFilter);
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Low memory warning received. Trying to cleanum cacne");

        MediaAdapter adapter = (MediaAdapter) gridView.getAdapter();
        adapter.onLowMemory();
    }


    @Override
    public void onMediaStorageMounted() {
        // Nothing to do
    }


    @Override
    public void onMediaStorageUnmounted() {

    }


    @Override
    public void onMediaEject() {
        mediaGallery.onDestroy();
        finish();
    }
}
