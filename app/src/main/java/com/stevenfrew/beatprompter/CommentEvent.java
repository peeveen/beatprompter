package com.stevenfrew.beatprompter;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;

class CommentEvent extends BaseEvent {
    Comment mComment;
    ScreenString mScreenString;
    PointF mTextDrawLocation;
    RectF mPopupRect;

    CommentEvent(long eventTime, Comment comment)
    {
        super(eventTime);
        mComment=comment;
    }

    void doMeasurements(int screenWidth, int screenHeight, Paint paint, Typeface face)
    {
        int maxCommentBoxHeight=(int)(screenHeight/4.0);
        int maxTextWidth=(int)(screenWidth*0.9);
        int maxTextHeight=(int)(maxCommentBoxHeight*0.9);
        mScreenString=ScreenString.create(mComment.mText,paint,maxTextWidth,maxTextHeight, Color.BLACK,face,false);
        float rectWidth=(float)(mScreenString.mWidth*1.1);
        float rectHeight=(float)(mScreenString.mHeight*1.1);
        float heightDiff=(float)((rectHeight-mScreenString.mHeight)/2.0);
        float rectX=(float)((screenWidth-rectWidth)/2.0);
        float rectY=(screenHeight-rectHeight)-10;
        int textWidth=mScreenString.mWidth;
        float textX=(float)((screenWidth-textWidth)/2.0);
        float textY=(rectY+rectHeight)-(mScreenString.mDescenderOffset+heightDiff);
        mPopupRect=new RectF(rectX,rectY,rectX+rectWidth,rectY+rectHeight);
        mTextDrawLocation=new PointF(textX,textY);
    }
}
