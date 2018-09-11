package com.stevenfrew.beatprompter.ui.pref

import android.app.AlertDialog
import android.content.Context
import android.preference.ListPreference
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.ImageArrayAdapter

class ImageListPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {
    private lateinit var resourceIds: IntArray

    private var inForceUpdate = false

    init {
        val typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.ImageListPreference)

        val indexCount = typedArray.indexCount
        val resources = context.resources
        val packageName = context.packageName
        if (indexCount > 0) {
            val imageNames = resources.getStringArray(
                    typedArray.getResourceId(indexCount - 1, -1))

            resourceIds = IntArray(imageNames.size)

            for (i in imageNames.indices) {
                val imageName = imageNames[i].substring(
                        imageNames[i].lastIndexOf('/') + 1,
                        imageNames[i].lastIndexOf('.'))

                resourceIds[i] = resources.getIdentifier(imageName,
                        "drawable", packageName)
            }

            typedArray.recycle()
        }
    }

    public override fun onBindView(view: View) {
        super.onBindView(view)
        val imageView = view.findViewById<ImageView>(R.id.iconImageView)
        val sharedPrefs = BeatPrompterApplication.preferences
        val value = sharedPrefs.getString(key, "")

        when (value) {
            BeatPrompterApplication.getResourceString(R.string.googleDriveValue) -> imageView.setImageResource(R.drawable.ic_google_drive)
            BeatPrompterApplication.getResourceString(R.string.dropboxValue) -> imageView.setImageResource(R.drawable.ic_dropbox)
            BeatPrompterApplication.getResourceString(R.string.oneDriveValue) -> imageView.setImageResource(R.drawable.ic_onedrive)
            BeatPrompterApplication.getResourceString(R.string.localStorageValue) -> imageView.setImageResource(R.drawable.ic_device)
            else -> imageView.setImageResource(R.drawable.blank_icon)
        }
    }

    fun forceUpdate() {
        if (!inForceUpdate) {
            try {

                inForceUpdate = true
                notifyChanged()
            } finally {
                inForceUpdate = false
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        val index = findIndexOfValue(sharedPreferences.getString(
                key, "1"))

        val listAdapter = ImageArrayAdapter(context,
                R.layout.imagelistitem, entries, resourceIds, index)

        // Order matters.
        builder.setAdapter(listAdapter, this)
        super.onPrepareDialogBuilder(builder)
    }
}
