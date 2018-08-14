package com.stevenfrew.beatprompter

enum class PlayState constructor(private val mValue: Int) {
    AtTitleScreen(0),
    Paused(1),
    Playing(2);

    fun asValue(): Int {
        return mValue
    }

    companion object {

        fun fromValue(`val`: Int): PlayState {
            return when (`val`) {
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