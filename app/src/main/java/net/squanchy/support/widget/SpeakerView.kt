package net.squanchy.support.widget

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_event_details.view.*
import net.squanchy.R
import net.squanchy.imageloader.ImageLoader
import net.squanchy.imageloader.imageLoaderComponent
import net.squanchy.speaker.domain.view.Speaker
import net.squanchy.support.kotlin.children
import net.squanchy.support.lang.Optional
import net.squanchy.support.unwrapToActivityContext

abstract class SpeakerView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private lateinit var imageLoader: ImageLoader
    private lateinit var speakerPhotoContainer: ViewGroup
    val layoutInflater = LayoutInflater.from(context)

    init {
        if (!isInEditMode) {
            val activity = unwrapToActivityContext(context)
            imageLoader = imageLoaderComponent(activity).imageLoader()
        }
        super.setOrientation(LinearLayout.VERTICAL)
    }

    override fun setOrientation(orientation: Int): Nothing {
        throw UnsupportedOperationException("SpeakerView doesn't support changing orientation")
    }

    fun updateWith(speakers: List<Speaker>, listener: Optional<OnSpeakerClickListener>) {
        speakerName.text = toCommaSeparatedNames(speakers)
        updateSpeakerPhotos(speakers, listener)
    }

    private fun toCommaSeparatedNames(speakers: List<Speaker>): String {
        return TextUtils.join(", ", speakers.map { it.name })
    }

    private fun updateSpeakerPhotos(speakers: List<Speaker>, listener: Optional<OnSpeakerClickListener>) {
        if (!::imageLoader.isInitialized) {
            throw IllegalStateException("Unable to access the ImageLoader, it hasn't been initialized yet")
        }

        val photoViews: List<ImageView>
        if (speakerPhotoContainer.childCount > 0) {
            photoViews = getAllImageViewsContainedIn(speakerPhotoContainer)
            speakerPhotoContainer.removeAllViews()
        } else {
            photoViews = mutableListOf()
        }

        for (speaker in speakers) {
            val photoView = recycleOrInflatePhotoView(photoViews)
            speakerPhotoContainer.addView(photoView)
            setClickListenerOrNotClickable(photoView, listener, speaker)

            if (speaker.photoUrl.isPresent) {
                loadSpeakerPhoto(photoView, speaker.photoUrl.get(), imageLoader)
            } else {
                photoView.setImageResource(R.drawable.ic_no_avatar)
            }
        }
    }

    private fun setClickListenerOrNotClickable(photoView: ImageView, listener: Optional<OnSpeakerClickListener>, speaker: Speaker) {
        if (listener.isPresent) {
            photoView.setOnClickListener { listener.get().onSpeakerClicked(speaker) }
            photoView.isClickable = true
        } else {
            photoView.setOnClickListener(null)
            photoView.isClickable = false
        }
    }

    private fun recycleOrInflatePhotoView(photoViews: MutableList<ImageView>): ImageView {
        return if (photoViews.isEmpty()) {
            inflatePhotoView(speakerPhotoContainer)
        } else {
            photoViews.removeAt(0)
        }
    }

    protected abstract fun inflatePhotoView(speakerPhotoContainer: ViewGroup): ImageView

    private fun loadSpeakerPhoto(photoView: ImageView, photoUrl: String, imageLoader: ImageLoader) {
        photoView.setImageDrawable(null)
        imageLoader.load(photoUrl)
            .into(photoView)
    }

    private fun getAllImageViewsContainedIn(container: ViewGroup): MutableList<ImageView> {
        return container.children
            .map { it as ImageView }
            .toMutableList()
    }

    interface OnSpeakerClickListener {

        fun onSpeakerClicked(speaker: Speaker)
    }
}
