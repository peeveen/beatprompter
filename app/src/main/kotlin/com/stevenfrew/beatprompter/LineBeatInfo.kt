package com.stevenfrew.beatprompter

data class LineBeatInfo(val mBeats:Int,val mBPL:Int, val mBPB:Int, val mBPM:Double, val mScrollBeat:Int, val mScrollBeatOffset:Int, val mScrollMode: LineScrollingMode=LineScrollingMode.Beat)
{
    constructor(songBeatInfo:SongBeatInfo):this(songBeatInfo.mBPB*songBeatInfo.mBPL,songBeatInfo.mBPL,songBeatInfo.mBPB,songBeatInfo.mBPM,songBeatInfo.mScrollBeat,0,songBeatInfo.mScrollMode.toLineScrollMode())
}