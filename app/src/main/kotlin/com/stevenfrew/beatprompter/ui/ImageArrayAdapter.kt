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
                        objects: Array<CharSequence>, private val resourceIds: IntArray, private val index: Int) : ArrayAdapter<CharSequence>(context, textViewResourceId, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = (context as Activity).layoutInflater
        val row = convertView ?: inflater.inflate(R.layout.imagelistitem, parent, false)

        val imageView = row.findViewById<ImageView>(R.id.image)
        imageView.setImageResource(resourceIds[position])

        val checkedTextView = row.findViewById<CheckedTextView>(
                R.id.check)

        checkedTextView.text = getItem(position)
        checkedTextView.isChecked = position == index

        return row
    }
}