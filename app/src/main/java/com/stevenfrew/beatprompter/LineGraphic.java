package com.stevenfrew.beatprompter;

import android.graphics.Bitmap;
import android.graphics.Rect;

class LineGraphic
{
    Line mLastDrawnLine;
    Bitmap mBitmap;
    LineGraphic mNextGraphic;
    LineGraphic mPrevGraphic;

    LineGraphic(Rect size)
    {
        mBitmap=Bitmap.createBitmap(size.width(),size.height(),Bitmap.Config.ARGB_8888);
        mLastDrawnLine=null;
    }

    void recycle()
    {
        if(mBitmap!=null)
            mBitmap.recycle();
    }
}
