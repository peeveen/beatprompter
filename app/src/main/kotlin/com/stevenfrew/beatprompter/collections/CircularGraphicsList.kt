package com.stevenfrew.beatprompter.collections

import com.stevenfrew.beatprompter.LineGraphic

internal class CircularGraphicsList:ArrayList<LineGraphic>() {
    override fun add(element: LineGraphic):Boolean {
        lastOrNull()?.mNextGraphic = element
        val result=super.add(element)
        last().mNextGraphic = first()
        return result
    }
}