package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.Comment

class CommentEvent(eventTime: Long, var mComment: Comment) : BaseEvent(eventTime)