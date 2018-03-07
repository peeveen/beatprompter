package com.stevenfrew.beatprompter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Collection;

public class ImageLine extends Line
{
    ImageFile mImageFile;
    boolean mScaling=false;
    ImageScalingMode mScalingMode;
    Rect mSourceRect,mDestRect;
    Bitmap mBitmap;

    ImageLine(ImageFile image, ImageScalingMode scalingMode, Context context, Collection<Tag> lineTags, int bars, ColorEvent lastColor, int bpb, int scrollbeat, int scrollbeatOffset, ArrayList<FileParseError> parseErrors) {
        super(context, lineTags, bars, lastColor, bpb, scrollbeat, scrollbeatOffset, parseErrors);
        mImageFile=image;
        mScalingMode=scalingMode;
    }

    LineMeasurements doMeasurements(Context context, Paint paint, float minimumFontSize, float maximumFontSize, int screenWidth, int screenHeight, Typeface font, int highlightColour, int defaultHighlightColour, ArrayList<FileParseError> errors, ScrollingMode scrollMode,CancelEvent cancelEvent)
    {
        String path=mImageFile.mFile.getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            mBitmap=BitmapFactory.decodeFile(path, options);
        }
        catch(Exception e) {
            errors.add(new FileParseError(null,context.getString(R.string.could_not_read_image_file)+": "+mImageFile.mTitle));
            return null;
        }

        int imageHeight=mBitmap.getHeight();
        int imageWidth=mBitmap.getWidth();
        int scaledImageHeight=imageHeight;
        int scaledImageWidth=imageWidth;

        if ((imageWidth > screenWidth)||(mScalingMode==ImageScalingMode.Stretch))
        {
            scaledImageHeight = (int) (imageHeight * ((double) screenWidth / (double) imageWidth));
            scaledImageWidth = screenWidth;
            mScaling=true;
        }

        ArrayList<Integer> graphicHeights = new ArrayList<>();
        graphicHeights.add(scaledImageHeight);
        mSourceRect=new Rect(0,0,imageWidth,imageHeight);
        mDestRect=new Rect(0,0,scaledImageWidth,scaledImageHeight);
        return new LineMeasurements(1, mDestRect.width(), mDestRect.height(), graphicHeights, highlightColour, mLineEvent, mNextLine, mYStartScrollTime,scrollMode);
    }

    Collection<LineGraphic> getGraphics(boolean allocate)
    {
        for(int f=0;f<mLineMeasurements.mLines;++f)
        {
            LineGraphic graphic=mGraphics.get(f);
            if ((graphic.mLastDrawnLine != this) &&(allocate))
            {
                Paint paint = new Paint();
                Canvas c = new Canvas(graphic.mBitmap);
                c.drawBitmap(mBitmap,mSourceRect,mDestRect,paint);
                graphic.mLastDrawnLine = this;
            }
        }
        return mGraphics;
    }

    boolean hasOwnGraphics()
    {
        return true;
    }

    void recycleGraphics()
    {
        super.recycleGraphics();
        mBitmap.recycle();
    }
}