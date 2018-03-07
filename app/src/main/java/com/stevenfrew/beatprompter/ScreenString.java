package com.stevenfrew.beatprompter;

import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

class ScreenString
{
    private final static int MARGIN_PIXELS=10;
    private final static boolean MASKING=true;
    private final static String MASKING_STRING="X";
    private final static String DOUBLE_MASKING_STRING=MASKING_STRING+MASKING_STRING;

    private static int boldDoubleXWidth[]=new int[(Utils.MAXIMUM_FONT_SIZE-Utils.MINIMUM_FONT_SIZE)+1];
    private static int regularDoubleXWidth[]=new int[(Utils.MAXIMUM_FONT_SIZE-Utils.MINIMUM_FONT_SIZE)+1];

    static
    {
        for(int f=Utils.MINIMUM_FONT_SIZE;f<=Utils.MAXIMUM_FONT_SIZE;++f)
            boldDoubleXWidth[f-Utils.MINIMUM_FONT_SIZE]=regularDoubleXWidth[f-Utils.MINIMUM_FONT_SIZE]=-1;
    }

    float mFontSize;
    int mWidth;
    int mHeight;
    int mDescenderOffset;
    int mColor;
    Typeface mFace;
    String mText;

    private ScreenString(String text,float fontSize,int color,int width,int height, Typeface face, int descenderOffset)
    {
        mText=text;
        mFontSize=fontSize;
        mColor=color;
        mWidth=Math.max(0,width);
        mHeight=Math.max(0,height);
        mFace=face;
        mDescenderOffset=descenderOffset;
    }

    private static void getTextRect(String str,Paint paint,Rect r)
    {
        float measureWidth=paint.measureText(str);
        paint.getTextBounds(str,0,str.length(),r);
        r.left=0;
        r.right=(int)Math.ceil(measureWidth);
    }

//    static Rect singleXRect=new Rect();
    private static Rect doubleXRect=new Rect();
    private static int getDoubleXStringLength(Paint paint,float fontSize,boolean bold)
    {
        int intFontSize=((int)fontSize)-Utils.MINIMUM_FONT_SIZE;

        // This should never happen, but let's check anyway.
        if(intFontSize<0)
            intFontSize=0;
        else if(intFontSize>=boldDoubleXWidth.length)
            intFontSize=boldDoubleXWidth.length-1;

        int size=(bold?boldDoubleXWidth:regularDoubleXWidth)[intFontSize];
        if(size==-1) {
            getTextRect(DOUBLE_MASKING_STRING,paint,doubleXRect);
            size =doubleXRect.width();
            (bold ? boldDoubleXWidth : regularDoubleXWidth)[intFontSize]=size;
        }
        return size;
    }

    private static Rect stringWidthRect=new Rect();
    static int getStringWidth(Paint paint,String str,Typeface face,boolean bold,float fontSize)
    {
        if((str==null)||(str.length()==0))
            return 0;
        paint.setTypeface(face);
        paint.setTextSize(fontSize*Utils.FONT_SCALING);
        if(MASKING)
            str=MASKING_STRING+str+MASKING_STRING;
        getTextRect(str,paint,stringWidthRect);
        int width= stringWidthRect.width() - (MASKING?getDoubleXStringLength(paint,fontSize,bold):0);
        return width;
    }

    static int getBestFontSize(String text,Paint paint,float minimumFontSize, float maximumFontSize, int maxWidth,int maxHeight,Typeface face,boolean bold)
    {
        return getBestFontSize(text,paint,minimumFontSize,maximumFontSize,maxWidth,maxHeight,face,bold,null);
    }

    static ScreenString create(String text,Paint paint,int maxWidth,int maxHeight,int color,Typeface face,boolean bold)
    {
        Rect outRect=new Rect();
        int fontSize=getBestFontSize(text,paint,Utils.MINIMUM_FONT_SIZE,Utils.MAXIMUM_FONT_SIZE,maxWidth,maxHeight,face,bold,outRect);
        return new ScreenString(text,fontSize,color,outRect.width(),outRect.height()+MARGIN_PIXELS,face,outRect.bottom);
    }

    private static Rect createRect=new Rect();
    static ScreenString create(String text,Paint paint,float fontSize,Typeface face,boolean bold,int color)
    {
        paint.setTypeface(face);
        paint.setTextSize(fontSize*Utils.FONT_SCALING);
        String measureText=text;
        if(MASKING)
            measureText=MASKING_STRING+text+MASKING_STRING;
        getTextRect(measureText,paint,createRect);
        if(MASKING)
            createRect.right-=getDoubleXStringLength(paint,fontSize,bold);
        return new ScreenString(text,fontSize,color,createRect.width(),createRect.height()+MARGIN_PIXELS,face,createRect.bottom);
    }

    private static int getBestFontSize( String text,Paint paint, float minimumFontSize, float maximumFontSize, int maxWidth, int maxHeight, Typeface face,boolean bold,Rect outRect)
    {
        if (maxWidth <= 0)
            return 0;
        float hi = maximumFontSize;
        float lo = minimumFontSize;
        final float threshold = 0.5f; // How close we have to be

        Rect rect=outRect;
        if(rect==null)
            rect=new Rect();
        if(MASKING)
            text=MASKING_STRING+text+MASKING_STRING;
        paint.setTypeface(face);
        while((hi - lo) > threshold)
        {
            float size = (float)((hi+lo)/2.0);
            int intSize = (int)Math.floor(size);
            paint.setTextSize(intSize*Utils.FONT_SCALING);
            getTextRect(text,paint,rect);
            float widthXX=MASKING?getDoubleXStringLength(paint,intSize,bold):0;
            if((rect.width()-widthXX >= maxWidth) || ((maxHeight!=-1)&& (rect.height()>=maxHeight-MARGIN_PIXELS)))
                hi = size; // too big
            else
                lo = size; // too small
        }
        // Use lo so that we undershoot rather than overshoot
        int sizeToUse=(int)Math.floor(lo);
        if(outRect!=null)
        {
            paint.setTextSize(sizeToUse*Utils.FONT_SCALING);
            getTextRect(text, paint, outRect);
            if(MASKING) {
                float widthXX = getDoubleXStringLength(paint, sizeToUse, bold);
                outRect.right -= widthXX;
            }
        }
        return sizeToUse;
    }
}
