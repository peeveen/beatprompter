package com.stevenfrew.beatprompter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Collection;

class ImageLine extends Line
{
    private ImageFile mImageFile;
    private ImageScalingMode mScalingMode;
    private Rect mSourceRect,mDestRect;
    private Bitmap mBitmap;

    ImageLine(ImageFile image, ImageScalingMode scalingMode, Collection<Tag> lineTags, int bars, ColorEvent lastColor, int bpb, int scrollbeat, int scrollbeatOffset, ScrollingMode scrollingMode, ArrayList<FileParseError> parseErrors) {
        super(lineTags, bars, lastColor, bpb, scrollbeat, scrollbeatOffset, scrollingMode, parseErrors);
        mImageFile=image;
        mScalingMode=scalingMode;
    }

    LineMeasurements doMeasurements(Paint paint, float minimumFontSize, float maximumFontSize, int screenWidth, int screenHeight, Typeface font, int highlightColour, int defaultHighlightColour, ArrayList<FileParseError> errors, ScrollingMode scrollMode,CancelEvent cancelEvent)
    {
        String path=mImageFile.mFile.getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            mBitmap=BitmapFactory.decodeFile(path, options);
        }
        catch(Exception e) {
            errors.add(new FileParseError(null,SongList.getContext().getString(R.string.could_not_read_image_file)+": "+mImageFile.mName));
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

    void recycleGraphics()
    {
        super.recycleGraphics();
        mBitmap.recycle();
    }
}