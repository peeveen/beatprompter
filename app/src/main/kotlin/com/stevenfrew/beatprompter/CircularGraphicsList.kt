package com.stevenfrew.beatprompter

internal class CircularGraphicsList:ArrayList<LineGraphic>() {
    override fun add(element:LineGraphic):Boolean {
        if (!isEmpty()) {
            last().mNextGraphic = element
            element.mNextGraphic = first()
        }
        return super.add(element)
    }
}