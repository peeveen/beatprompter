package com.stevenfrew.beatprompter.song.load

import android.graphics.Rect

data class SongChoiceInfo(
	val normalizedTitle: String,
	val normalizedArtist: String,
	val variation: String,
	val orientation: Int,
	val isBeatScroll: Boolean,
	val isSmoothScroll: Boolean,
	val minFontSize: Float,
	val maxFontSize: Float,
	val screenSize: Rect,
	val noAudio: Boolean,
	val audioLatency: Int
)