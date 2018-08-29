package com.stevenfrew.beatprompter

data class BeatInfo(val mBPL:Int=0, val mBPB:Int=0, val mBPM:Double=0.0, val mScrollBeat:Int=0, val mScrollBeatOffset:Int=0, val mScrollMode: LineScrollingMode=LineScrollingMode.Beat)