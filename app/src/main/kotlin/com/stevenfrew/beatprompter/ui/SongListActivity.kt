package com.stevenfrew.beatprompter.ui

import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.stevenfrew.beatprompter.R

class SongListActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val fragmentManager: FragmentManager = supportFragmentManager
		val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
		fragmentTransaction.replace(android.R.id.content, SongListFragment())
		fragmentTransaction.commit()

		supportActionBar?.apply {
			displayOptions = ActionBar.DISPLAY_SHOW_HOME
			setIcon(R.drawable.ic_beatprompter)
			title = ""
		}
	}
}