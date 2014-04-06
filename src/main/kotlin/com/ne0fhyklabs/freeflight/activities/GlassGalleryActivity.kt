package com.ne0fhyklabs.freeflight.activities

import android.support.v4.app.FragmentActivity
import android.os.Bundle
import com.ne0fhyklabs.freeflight.R
import com.ne0fhyklabs.freeflight.tasks.GetMediaObjectsListTask.MediaFilter
import com.ne0fhyklabs.freeflight.tasks.GetMediaObjectsListTask
import android.os.AsyncTask.Status
import com.ne0fhyklabs.freeflight.vo.MediaVO
import android.util.Log
import java.util.concurrent.ExecutionException
import java.util.ArrayList
import com.google.android.glass.widget.CardScrollView
import com.google.android.glass.widget.CardScrollAdapter
import android.view.View
import android.view.ViewGroup
import android.content.Context
import kotlin.properties.Delegates
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View.OnKeyListener
import android.widget.TextView
import com.ne0fhyklabs.freeflight.utils.ARDroneMediaGallery
import com.ne0fhyklabs.freeflight.utils.ShareUtils
import android.content.Intent
import android.widget.ImageView
import android.view.LayoutInflater
import com.ne0fhyklabs.freeflight.tasks.LoadMediaThumbTask
import android.widget.ImageView.ScaleType
import com.google.android.glass.media.Sounds
import android.media.AudioManager

/**
 * Created by fhuya on 3/19/14.
 * TODO: Fix the default screen when there's no more pictures.
 */
public class GlassGalleryActivity : FragmentActivity() {

    object Static {
        val TAG = javaClass<GlassGalleryActivity>().getName()
    }

    object IntentExtras {
        val SELECTED_ELEMENT = "SELECTED_ELEMENT"
        val MEDIA_FILTER = "MEDIA_FILTER"
    }

    private var mInitMediaTask: GetMediaObjectsListTask? = null
    private var mMediaFilter: MediaFilter? = null

    private val mMediaGallery : ARDroneMediaGallery by Delegates.lazy {
        ARDroneMediaGallery(getApplicationContext())
    }

    private val mAdapter: GlassGalleryAdapter by Delegates.lazy {
        val mediaList = ArrayList<MediaVO>()
        val galleryAdapter = GlassGalleryAdapter(mediaList, getApplicationContext()!!)
        galleryAdapter
    }

    private val mCardsScroller: CardScrollView by Delegates.lazy {
        val cardsScroller = findViewById(R.id.glass_gallery) as CardScrollView
        cardsScroller.setAdapter(mAdapter)
        cardsScroller.setHorizontalScrollBarEnabled(true)
        cardsScroller.setOnKeyListener(object: OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                when(keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER -> {
                        openOptionsMenu()
                        return true
                    }

                    else -> return cardsScroller.onKeyDown(keyCode, event)
                }
            }
        })

        cardsScroller.setOnItemClickListener { parent, view, position, id -> openOptionsMenu() }

        cardsScroller
    }

    private var mDisablePauseSound = false

    private val mNoMediaView : TextView by Delegates.lazy {
        findViewById(R.id.glass_gallery_no_media) as TextView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow()?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_glass_gallery)

        val intent = getIntent()
        val selectedItem = if (savedInstanceState != null)
            savedInstanceState.getInt(IntentExtras.SELECTED_ELEMENT, 0)
        else intent?.getIntExtra(IntentExtras.SELECTED_ELEMENT, 0) ?: 0

        val mediaFilterType = intent?.getIntExtra(IntentExtras.MEDIA_FILTER,
                MediaFilter.IMAGES.ordinal()) as Int
        mMediaFilter = MediaFilter.values()[mediaFilterType]

        initMediaTask(mMediaFilter!!, selectedItem)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mInitMediaTask != null && mInitMediaTask!!.getStatus() == Status.RUNNING) {
            mInitMediaTask?.cancel(false)
        }
    }

    override fun onPause() {
        super.onPause()
        if(!mDisablePauseSound) {
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.playSoundEffect(Sounds.DISMISSED);
        }
        mDisablePauseSound = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu_glass_gallery, menu)

        if (mMediaFilter == MediaFilter.IMAGES) {
            //Hide the play menu button
            val playMenu = menu?.findItem(R.id.menu_video_play)
            playMenu?.setEnabled(false)
            playMenu?.setVisible(false)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        //Get the currently viewed media item
        val selectedMedia = mAdapter.getItem(mCardsScroller.getSelectedItemPosition()) as MediaVO

        when(item?.getItemId()) {
            R.id.menu_video_play -> {
                playVideo(selectedMedia)
                return true
            }

        //GDK doesn't yet support share intent.
        /*R.id.menu_media_share -> {
            shareMedia(selectedMedia)
            return true
        }*/

            R.id.menu_media_delete -> {
                deleteMedia(selectedMedia)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun playVideo(video: MediaVO){
        if(!video.isVideo()){
            Log.e(Static.TAG, "Error: trying to play image as video")
            return
        }

        mDisablePauseSound = true
        val videoIntent = Intent("com.google.glass.action.VIDEOPLAYER").putExtra("video_url",
                video.getUri().toString())
        startActivity(videoIntent)
    }

    private fun shareMedia(media: MediaVO){
        if(media.isVideo()){
            ShareUtils.shareVideo(this, media.getPath())
        }
        else{
            ShareUtils.sharePhoto(this, media.getPath())
        }
    }

    private fun deleteMedia(media: MediaVO){
        val mediaId = IntArray(1)
        mediaId.set(0, media.getId())

        if(media.isVideo()){
            mMediaGallery.deleteVideos(mediaId)
        }
        else{
            mMediaGallery.deleteImages(mediaId)
        }

        mAdapter.remove(media)
        updateView(null)
    }

    private fun initMediaTask(filter: MediaFilter, selectedElem: Int) {
        if (mInitMediaTask == null || mInitMediaTask!!.getStatus() != Status.RUNNING) {
            mInitMediaTask = object : GetMediaObjectsListTask(this, filter) {
                override fun onPostExecute(result: List<MediaVO>) = onMediaScanCompleted(selectedElem, result)
            }

            try {
                mInitMediaTask!!.execute().get()
            } catch(e: InterruptedException) {
                Log.e(Static.TAG, e.getMessage(), e)
            } catch(e: ExecutionException) {
                Log.e(Static.TAG, e.getMessage(), e)
            }
        }
    }

    private fun onMediaScanCompleted(selectedElem: Int, result: List<MediaVO>) {
        initView(selectedElem, result)
        mInitMediaTask = null
    }

    private fun initView(selectedElem: Int, result: List<MediaVO>) {
        mAdapter.clear()
        mAdapter.addAll(result)

        updateView(selectedElem)
    }

    private fun updateView(selectedElem: Int?) {
        if (mAdapter.getCount() > 0) {
            mNoMediaView.setVisibility(View.GONE)

            mCardsScroller.setVisibility(View.VISIBLE)
            if (selectedElem != null)
                mCardsScroller.setSelection(selectedElem)

            mCardsScroller.updateViews(true)
            if (!mCardsScroller.isActivated())
                mCardsScroller.activate()
        } else {
            mNoMediaView.setVisibility(View.VISIBLE)

            mCardsScroller.deactivate()
            mCardsScroller.setVisibility(View.GONE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val cardsScroller = findViewById(R.id.glass_gallery) as CardScrollView
        outState.putInt(IntentExtras.SELECTED_ELEMENT, cardsScroller.getSelectedItemPosition())
    }

    private class GlassGalleryAdapter(private val mediaList: ArrayList<MediaVO>,
                                      private val context: Context) : CardScrollAdapter() {

        val mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        fun clear() {
            mediaList.clear()
            notifyDataSetChanged()
        }

        fun addAll(items: List<MediaVO>) {
            mediaList.addAll(items)
            notifyDataSetChanged()
        }

        fun remove(media: MediaVO){
            mediaList.remove(media)
            notifyDataSetChanged()
        }

        override fun getCount(): Int = mediaList.size()

        override fun getItem(position: Int): Any? = mediaList.get(position)

        override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View? {
            val mediaDetail = getItem(position) as MediaVO

            val mediaCard = mInflater.inflate(R.layout.item_video, viewGroup, false)
            val imageView = mediaCard?.findViewById(R.id.image) as ImageView
            val imageIndicatorView = mediaCard?.findViewById(R.id.btn_play)

            if(!mediaDetail.isVideo()){
                imageIndicatorView?.setVisibility(View.INVISIBLE)
                imageView.setScaleType(ScaleType.CENTER_CROP)
            }

            LoadMediaThumbTask(mediaDetail, imageView).execute()
            return mediaCard
        }

        override fun findIdPosition(p0: Any?): Int = -1

        override fun findItemPosition(item: Any?): Int {
            if (item !is MediaVO)
                return -1

            val mediaVO = item as MediaVO
            for (i in 0..(getCount() - 1)) {
                if (mediaVO?.equals(getItem(i)))
                    return i
            }

            return -1
        }

    }
}