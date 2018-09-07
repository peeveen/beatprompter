package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.cache.AudioFile

/**
 * An AudioEvent signals the event processor to start playing an audio file.
 */
class AudioEvent(eventTime: Long, val mAudioFile: AudioFile, val mVolume:Int,val mBackingTrack:Boolean) : BaseEvent(eventTime)