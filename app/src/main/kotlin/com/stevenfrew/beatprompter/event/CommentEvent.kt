package com.stevenfrew.beatprompter.event

import android.graphics.*
import com.stevenfrew.beatprompter.Comment
import com.stevenfrew.beatprompter.ScreenString

class CommentEvent(eventTime: Long, var mComment: Comment) : BaseEvent(eventTime)