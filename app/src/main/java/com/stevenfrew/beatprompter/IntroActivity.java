package com.stevenfrew.beatprompter;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

public class IntroActivity extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        int backgroundColor=Color.parseColor("#CCCCCC");

        SliderPage page1=new SliderPage();
        page1.setTitle(SongList.mSongListInstance.getString(R.string.welcome_to_beatprompter));
        page1.setDescription(SongList.mSongListInstance.getString(R.string.welcome_to_beatprompter_description));
        page1.setImageDrawable(R.drawable.beatprompter_logo);
        page1.setBgColor(backgroundColor);
        addSlide(AppIntroFragment.newInstance(page1));

        SliderPage page2=new SliderPage();
        page2.setTitle(SongList.mSongListInstance.getString(R.string.turn_turn_turn));
        page2.setDescription(SongList.mSongListInstance.getString(R.string.works_best_in_landscape));
        page2.setImageDrawable(R.drawable.landscape_best);
        page2.setBgColor(backgroundColor);
        addSlide(AppIntroFragment.newInstance(page2));

        SliderPage page3=new SliderPage();
        page3.setTitle(SongList.mSongListInstance.getString(R.string.cloud_sync_explanation_title));
        page3.setDescription(SongList.mSongListInstance.getString(R.string.cloud_sync_explanation));
        page3.setImageDrawable(R.drawable.cloud_sync_diagram);
        page3.setBgColor(backgroundColor);
        addSlide(AppIntroFragment.newInstance(page3));

        SliderPage page4=new SliderPage();
        page4.setTitle(SongList.mSongListInstance.getString(R.string.keep_the_beat_title));
        page4.setDescription(SongList.mSongListInstance.getString(R.string.keep_the_beat));
        page4.setImageDrawable(R.drawable.keep_the_beat);
        page4.setBgColor(backgroundColor);
        addSlide(AppIntroFragment.newInstance(page4));

        SliderPage page5=new SliderPage();
        page5.setTitle(SongList.mSongListInstance.getString(R.string.not_just_text_title));
        page5.setDescription(SongList.mSongListInstance.getString(R.string.not_just_text));
        page5.setImageDrawable(R.drawable.not_just_text);
        page5.setBgColor(backgroundColor);
        addSlide(AppIntroFragment.newInstance(page5));

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
