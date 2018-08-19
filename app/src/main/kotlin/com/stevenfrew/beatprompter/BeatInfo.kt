package com.stevenfrew.beatprompter

data class BeatInfo(val mBPL:Int=4, val mBPB:Int=4, val mBPM:Double=120.0, val mScrollBeat:Int=4, val mScrollBeatOffset:Int=0, val mScrollingMode: ScrollingMode=ScrollingMode.Beat)