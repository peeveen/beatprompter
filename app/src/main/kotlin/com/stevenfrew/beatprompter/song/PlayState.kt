package com.stevenfrew.beatprompter.song

enum class PlayState(private val mValue: Int) {
    AtTitleScreen(0),
    Paused(1),
    Playing(2);

    fun asValue(): Int {
        return mValue
    }

    companion object {
        fun fromValue(state: Int): PlayState {
            return when (state) {
                0 -> AtTitleScreen
                1 -> Paused
                else -> Playing
            }
        }

        fun increase(state: PlayState): PlayState {
            return if (state == AtTitleScreen) Paused else Playing
        }

        fun reduce(state: PlayState): PlayState {
            return if (state == Playing) Paused else AtTitleScreen
        }
    }
}