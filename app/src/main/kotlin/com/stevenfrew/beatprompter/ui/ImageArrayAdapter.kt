package com.stevenfrew.beatprompter.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import com.stevenfrew.beatprompter.R

class ImageArrayAdapter(context: Context, textViewResourceId: Int,
                        objects: Array<CharSequence>,
                        private val resourceIds: IntArray,
                        private val index: Int)
    : ArrayAdapter<CharSequence>(context, textViewResourceId, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return (convertView
                ?: (context as Activity).layoutInflater.inflate(R.layout.imagelistitem, parent, false)).also {
            it.findViewById<ImageView>(R.id.image).setImageResource(resourceIds[position])
            it.findViewById<CheckedTextView>(R.id.check).apply {
                text = getItem(position)
                isChecked = position == index
            }
        }
    }
}