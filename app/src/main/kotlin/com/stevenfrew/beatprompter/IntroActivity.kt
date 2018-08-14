package com.stevenfrew.beatprompter

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage

class IntroActivity : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        val backgroundColor = Color.parseColor("#CCCCCC")

        val page1 = SliderPage()
        page1.title = BeatPrompterApplication.getResourceString(R.string.welcome_to_beatprompter)
        page1.description = BeatPrompterApplication.getResourceString(R.string.welcome_to_beatprompter_description)
        page1.imageDrawable = R.drawable.beatprompter_logo
        page1.bgColor = backgroundColor
        addSlide(AppIntroFragment.newInstance(page1))

        val page2 = SliderPage()
        page2.title = BeatPrompterApplication.getResourceString(R.string.turn_turn_turn)
        page2.description = BeatPrompterApplication.getResourceString(R.string.works_best_in_landscape)
        page2.imageDrawable = R.drawable.landscape_best
        page2.bgColor = backgroundColor
        addSlide(AppIntroFragment.newInstance(page2))

        val page3 = SliderPage()
        page3.title = BeatPrompterApplication.getResourceString(R.string.cloud_sync_explanation_title)
        page3.description = BeatPrompterApplication.getResourceString(R.string.cloud_sync_explanation)
        page3.imageDrawable = R.drawable.cloud_sync_diagram
        page3.bgColor = backgroundColor
        addSlide(AppIntroFragment.newInstance(page3))

        val page4 = SliderPage()
        page4.title = BeatPrompterApplication.getResourceString(R.string.keep_the_beat_title)
        page4.description = BeatPrompterApplication.getResourceString(R.string.keep_the_beat)
        page4.imageDrawable = R.drawable.keep_the_beat
        page4.bgColor = backgroundColor
        addSlide(AppIntroFragment.newInstance(page4))

        val page5 = SliderPage()
        page5.title = BeatPrompterApplication.getResourceString(R.string.not_just_text_title)
        page5.description = BeatPrompterApplication.getResourceString(R.string.not_just_text)
        page5.imageDrawable = R.drawable.not_just_text
        page5.bgColor = backgroundColor
        addSlide(AppIntroFragment.newInstance(page5))

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
