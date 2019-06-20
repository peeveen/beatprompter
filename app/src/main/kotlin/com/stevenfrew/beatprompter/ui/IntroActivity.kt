package com.stevenfrew.beatprompter.ui

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

class IntroActivity
    : AppIntro() {
    private data class IntroPageInfo(val caption: Int, val description: Int, val image: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pageInfo = arrayOf(
                IntroPageInfo(R.string.welcome_to_beatprompter, R.string.welcome_to_beatprompter_description, R.drawable.beatprompter_logo),
                IntroPageInfo(R.string.turn_turn_turn, R.string.works_best_in_landscape, R.drawable.landscape_best),
                IntroPageInfo(R.string.cloud_sync_explanation_title, R.string.cloud_sync_explanation, R.drawable.cloud_sync_diagram),
                IntroPageInfo(R.string.keep_the_beat_title, R.string.keep_the_beat, R.drawable.keep_the_beat),
                IntroPageInfo(R.string.not_just_text_title, R.string.not_just_text, R.drawable.not_just_text))

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        val backgroundColor = Color.parseColor("#CCCCCC")

        pageInfo.forEach {
            addSlide(AppIntroFragment.newInstance(SliderPage().apply {
                title = BeatPrompter.getResourceString(it.caption)
                description = BeatPrompter.getResourceString(it.description)
                imageDrawable = it.image
                bgColor = backgroundColor
            }))
        }

        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(Color.parseColor("#AAAAAA"))
        setSeparatorColor(Color.parseColor("#888888"))

        // Hide Skip/Done button.
        showSkipButton(true)
        isProgressButtonEnabled = true
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Do something when users tap on Skip button.
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Do something when users tap on Done button.
        finish()
    }
}
