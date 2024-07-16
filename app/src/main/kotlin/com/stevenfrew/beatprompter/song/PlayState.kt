package com.stevenfrew.beatprompter.song

enum class PlayState(val mValue: Int) {
	AtTitleScreen(0),
	Paused(1),
	Playing(2);

	companion object {
		fun fromValue(state: Int): PlayState =
			when (state) {
				0 -> AtTitleScreen
				1 -> Paused
				else -> Playing
			}

		fun increase(state: PlayState): PlayState = if (state == AtTitleScreen) Paused else Playing
		fun reduce(state: PlayState): PlayState = if (state == Playing) Paused else AtTitleScreen
	}
}