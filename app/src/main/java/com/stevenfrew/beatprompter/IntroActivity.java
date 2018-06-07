package com.stevenfrew.beatprompter;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

public class IntroActivity extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        int backgroundColor=Color.parseColor("#CCCCCC");
        addSlide(AppIntroFragment.newInstance(SongList.getContext().getString(R.string.welcome_to_beatprompter), SongList.getContext().getString(R.string.welcome_to_beatprompter_description), R.drawable.beatprompter_logo, backgroundColor));
        addSlide(AppIntroFragment.newInstance(SongList.getContext().getString(R.string.turn_turn_turn), SongList.getContext().getString(R.string.works_best_in_landscape), R.drawable.landscape_best, backgroundColor));
        addSlide(AppIntroFragment.newInstance(SongList.getContext().getString(R.string.cloud_sync_explanation_title), SongList.getContext().getString(R.string.cloud_sync_explanation), R.drawable.cloud_sync_diagram, backgroundColor));
        addSlide(AppIntroFragment.newInstance(SongList.getContext().getString(R.string.keep_the_beat_title), SongList.getContext().getString(R.string.keep_the_beat), R.drawable.keep_the_beat, backgroundColor));
        addSlide(AppIntroFragment.newInstance(SongList.getContext().getString(R.string.not_just_text_title), SongList.getContext().getString(R.string.not_just_text), R.drawable.not_just_text, backgroundColor));

        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(Color.parseColor("#AAAAAA"));
        setSeparatorColor(Color.parseColor("#888888"));

        // Hide Skip/Done button.
        showSkipButton(true);
        setProgressButtonEnabled(true);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.
        finish();
    }
}
