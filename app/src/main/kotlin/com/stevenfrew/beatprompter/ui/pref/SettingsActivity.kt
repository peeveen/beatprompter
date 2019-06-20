package com.stevenfrew.beatprompter.ui.pref

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stevenfrew.beatprompter.storage.googledrive.GoogleDriveStorage

class SettingsActivity : AppCompatActivity() {
    private val mFragment = SettingsFragment()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the fragment as the main content.
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, mFragment)
                .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GoogleDriveStorage.REQUEST_CODE_GOOGLE_SIGN_IN && resultCode == Activity.RESULT_OK) {
            GoogleDriveStorage.completeAction(this)
        }
    }
}
