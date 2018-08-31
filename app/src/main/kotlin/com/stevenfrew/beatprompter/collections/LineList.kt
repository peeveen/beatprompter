package com.stevenfrew.beatprompter.collections

import com.stevenfrew.beatprompter.Line

internal class LineList:ArrayList<Line>() {
    override fun add(element: Line):Boolean {
        val lastOrNull=lastOrNull()
        lastOrNull?.mNextLine=element
        element.mPrevLine=lastOrNull
        return super.add(element)
    }
}