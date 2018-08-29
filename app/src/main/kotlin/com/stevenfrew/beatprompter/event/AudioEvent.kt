package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.cache.AudioFile

class AudioEvent(eventTime: Long, val mAudioFile: AudioFile, val mVolume:Int,val mBackingTrack:Boolean) : BaseEvent(eventTime)