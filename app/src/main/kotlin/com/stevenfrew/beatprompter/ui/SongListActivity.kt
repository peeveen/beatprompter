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
		if (savedInstanceState == null) {
			val fragmentManager: FragmentManager = supportFragmentManager
			val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
			val songListFragment = SongListFragment()
			fragmentTransaction.replace(
				android.R.id.content,
				songListFragment,
				"SongListFragment" + (++mSongListFragmentCounter)
			)
			fragmentTransaction.commit()
		}

		supportActionBar?.apply {
			displayOptions = ActionBar.DISPLAY_SHOW_HOME
			setIcon(R.drawable.ic_beatprompter)
			title = ""
		}
	}

	companion object {
		var mSongListFragmentCounter: Int = 0
	}
}