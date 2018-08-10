package com.stevenfrew.beatprompter.cloud

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

internal class CloudBrowserItemListAdapter(items: List<CloudItemInfo>) : ArrayAdapter<CloudItemInfo>(BeatPrompterApplication.context, -1, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = BeatPrompterApplication.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = convertView ?: inflater.inflate(R.layout.cloud_browser_item, parent, false)
        val textView = rowView.findViewById<TextView>(R.id.file_or_folder_name)
        val imageView = rowView.findViewById<ImageView>(R.id.file_or_folder_icon)
        val cloudItem = this.getItem(position)
        if (cloudItem != null) {
            textView.text = cloudItem.mName
            val isFolder = cloudItem is CloudFolderInfo
            textView.isEnabled = isFolder
            imageView.isEnabled = isFolder
            //rowView.setEnabled(isFolder);
            if (isFolder)
                imageView.setImageResource(R.drawable.ic_folder)
            else
                imageView.setImageResource(R.drawable.ic_document)
        }
        return rowView
    }

    override fun isEnabled(position: Int): Boolean {
        return getItem(position) is CloudFolderInfo
    }
}
