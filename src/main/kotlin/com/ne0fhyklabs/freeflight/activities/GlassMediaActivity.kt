package com.ne0fhyklabs.freeflight.activities

import android.support.v4.app.FragmentActivity
import android.os.Bundle
import com.ne0fhyklabs.freeflight.R
import com.google.android.glass.widget.CardScrollView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Adapter
import android.widget.AdapterView
import android.view.View
import android.view.SoundEffectConstants
import com.google.android.glass.widget.CardScrollAdapter
import android.view.ViewGroup
import com.google.android.glass.app.Card
import android.content.Intent
import com.ne0fhyklabs.freeflight.tasks.GetMediaObjectsListTask.MediaFilter

/**
 * Created by fhuya on 3/18/14.
 */
public class GlassMediaActivity : FragmentActivity() {

    val mCardViewAdapter = object : CardScrollAdapter() {

        override fun getCount(): Int = 2

        override fun getItem(i: Int): Any? {
            when(i) {
                0 -> return R.string.media_sort_photos
                1 -> return R.string.media_sort_videos
                else -> throw IllegalStateException("Invalid item index ($i)")
            }
        }

        override fun getView(i: Int, p1: View?, p2: ViewGroup): View? {
            val card = Card(getApplicationContext())
            card.setText(getItem(i) as Int)

            return card.toView()
        }

        override fun findIdPosition(p0: Any?): Int = -1

        override fun findItemPosition(o: Any?): Int {
            if (o !is Int)
                return -1;

            val screenNameRes = o as Int
            when(screenNameRes) {
                R.string.media_sort_photos -> return 0
                R.string.media_sort_videos -> return 1
                else -> return -1
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glass_media)

        val cardsView = findViewById(R.id.glass_media) as CardScrollView

        cardsView.setAdapter(mCardViewAdapter)
        cardsView.setHorizontalScrollBarEnabled(true)
        cardsView.activate()
        cardsView.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClick(parent: AdapterView<out Adapter?>, view: View, position: Int, id: Long) {
                view?.playSoundEffect(SoundEffectConstants.CLICK)

                val screenNameRes = parent?.getItemAtPosition(position)
                when(screenNameRes) {
                    R.string.media_sort_photos -> launchPhotoGallery()
                    R.string.media_sort_videos -> launchVideoGallery()
                    else -> throw IllegalStateException("Invalid screen name resource.")
                }

            }
        })
    }

    private fun launchPhotoGallery() = startActivity(Intent(this, javaClass<GlassGalleryActivity>())
            .putExtra(GlassGalleryActivity.IntentExtras.MEDIA_FILTER, MediaFilter.IMAGES.ordinal()))

    private fun launchVideoGallery() = startActivity(Intent(this, javaClass<GlassGalleryActivity>())
            .putExtra(GlassGalleryActivity.IntentExtras.MEDIA_FILTER, MediaFilter.VIDEOS.ordinal()))

}