package com.stevenfrew.beatprompter.comm.bluetooth.message

import com.stevenfrew.beatprompter.PlayState

data class StartStopToggleInfo(val mStartState: PlayState, val mTime: Long)