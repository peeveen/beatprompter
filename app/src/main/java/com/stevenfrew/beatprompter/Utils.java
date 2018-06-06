package com.stevenfrew.beatprompter;

import android.graphics.Color;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Utils
{
    static double[] mSineLookup=new double[91];
//    private static double[] mCosineLookup=new double[91];
//    private static double[] mInverseSineLookup=new double[91];
//    private static double[] mInverseCosineLookup=new double[91];
//    private static double[] mReverseSineLookup=new double[91];
//    private static double[] mReverseCosineLookup=new double[91];
//    private static double[] mReverseInverseSineLookup=new double[91];
//    private static double[] mReverseInverseCosineLookup=new double[91];

    // Set by onCreate() in SongList.java
    static int MAXIMUM_FONT_SIZE;
    static int MINIMUM_FONT_SIZE;
    static float FONT_SCALING;

    static
    {
        for(int f=0;f<=90;++f) {
            double radians=Math.toRadians(f);
//            double reverseRadians=Math.toRadians(90-f);
            mSineLookup[f] = Math.sin(radians);
//            mCosineLookup[f] = Math.cos(radians);
//            mInverseSineLookup[f] = 1.0-mSineLookup[f];
//            mInverseCosineLookup[f] = 1.0-mCosineLookup[f];
//            mReverseSineLookup[f] = Math.sin(reverseRadians);
//            mReverseCosineLookup[f] = Math.cos(reverseRadians);
//            mReverseInverseSineLookup[f] = 1.0-mReverseSineLookup[f];
//            mReverseInverseCosineLookup[f] = 1.0-mReverseCosineLookup[f];
        }
    }

    // Special token.
    static final int TRACK_AUDIO_LENGTH_VALUE=-93781472;

    static long nanosecondsPerBeat(double bpm)
    {
        return (long)(60000000000.0/bpm);
    }
    static int nanoToMilli(long nano) { return (int)(nano/1000000);}
    static long milliToNano(int milli) { return ((long)milli)*1000000;}
    static long milliToNano(long milli) { return milli*1000000;}
    static double bpmToMIDIClockNanoseconds(double bpm){return 60000000000.0/(bpm*24.0);}
    static int makeHighlightColour(int colour)
    {
        colour&=0x00ffffff;
        colour|=0x44000000;
        return colour;
    }
    static int makeHighlightColour(int colour,byte opacity)
    {
        colour&=0x00ffffff;
        colour|=opacity << 24;
        return colour;
    }
    static int makeContrastingColour(int colour)
    {
        int r=(colour>>16) &0x000000FF;
        int g=(colour>>8) &0x000000FF;
        int b=colour &0x000000FF;
        return ((r * 0.299) + (g * 0.587) + (b * 0.114) > 186 ? Color.BLACK : Color.WHITE);
    }
    private static List<String> splitters=new ArrayList<>(Arrays.asList(" ", "-"));
    static int countWords(String[] words)
    {
        int wordCount=0;
        for(String w:words)
            if(!splitters.contains(w))
                wordCount++;
        return wordCount;
    }
    static String stitchBits(String[] bits,int nonWhitespaceBitsToJoin)
    {
        String result="";
        int nonWhitespaceBitsJoined=0;
        for (String bit : bits) {
            boolean whitespace = (bit.trim().length() == 0);
            if ((!whitespace) && (nonWhitespaceBitsJoined == nonWhitespaceBitsToJoin))
                break;
            result += bit;
            if (!whitespace)
                ++nonWhitespaceBitsJoined;
        }
        return result;
    }
    static String[] splitText(String str)
    {
        ArrayList<String> bits=new ArrayList<>();
        int bestSplitIndex;
        for(;;) {
            bestSplitIndex = str.length();
            String bestSplitter=null;
            for (String splitter : splitters) {
                int splitIndex = str.indexOf(splitter);
                if (splitIndex != -1) {
                    bestSplitIndex = Math.min(splitIndex, bestSplitIndex);
                    if(bestSplitIndex==splitIndex)
                        bestSplitter=splitter;
                }
            }
            String bit = str.substring(0, bestSplitIndex);
            if(bit.length()>0)
                bits.add(bit);
            if(bestSplitter!=null) {
                bits.add(bestSplitter);
                str = str.substring(bestSplitIndex+1);
            }
            else
                break;
        }
        return bits.toArray(new String[bits.size()]);
    }
    static String[] splitIntoLetters(String str)
    {
        char[] chars=str.toCharArray();
        String[] charsAsStrings=new String[chars.length];
        for(int f=0;f<chars.length;++f)
            charsAsStrings[f]=""+chars[f];
        return charsAsStrings;
    }
    static int parseDuration(String str, boolean trackLengthAllowed)
    {
        if((str.equalsIgnoreCase("track")) && (trackLengthAllowed))
            return Utils.TRACK_AUDIO_LENGTH_VALUE;
        try
        {
            double totalsecs = Double.parseDouble(str);
            return (int)Math.floor(totalsecs*1000.0);
        }
        catch(NumberFormatException nfe)
        {
            // Might be mm:ss
            int colonIndex=str.indexOf(":");
            if((colonIndex!=-1)&&(colonIndex<str.length()-1))
            {
                String strmins=str.substring(0,colonIndex);
                String strsecs=str.substring(colonIndex+1);
                int mins=Integer.parseInt(strmins);
                int secs=Integer.parseInt(strsecs);
                return (secs+(mins*60))*1000;
            }
            throw nfe;
        }
    }


    private static final String REGEXP =
            "^([\\s ]*[\\(\\/]{0,2})" //spaces, opening parenthesis, /
                    +"(([ABCDEFG])([b\u266D#\u266F\u266E])?)" //note name + accidental
                    //\u266D = flat, \u266E = natural, \u266F = sharp
                    +"([mM1234567890abdijnsu��o�\u00D8\u00F8\u00B0\u0394\u2206\\-\\+]*)"
                    //handles min(or), Maj/maj(or), dim, sus, Maj7, mb5...
                    // but not #11 (may be ok for Eb7#11,
                    // but F#11 will disturb...)
                    //\u00F8 = slashed o, \u00D8 = slashed O, \u00B0 = degree
                    //(html ø, Ø, °)
                    //delta = Maj7, maths=\u2206, greek=\u0394
                    +"((\\/)(([ABCDEFG])([b\u266D#\u266F\u266E])?))?" // /bass
                    +"(\\)?[ \\s]*)$"; //closing parenthesis, spaces

    private static Pattern pattern = Pattern.compile(REGEXP);

    static boolean isChord(String text) {
        if (text != null)
            text = text.trim();
        return !((text == null) || (text.length() == 0)) && pattern.matcher(text).matches();
    }

    static void streamToStream(InputStream is, OutputStream os) throws IOException
    {
        byte[] buffer = new byte[2048];
        int bytesRead;
        while ((bytesRead = is.read(buffer, 0, buffer.length)) != -1)
            os.write(buffer, 0, bytesRead);
    }

    private static final String ReservedChars = "|\\?*<\":>+[]/'";
    static String makeSafeFilename(String str)
    {
        StringBuilder builder=new StringBuilder();
        for(char c:str.toCharArray())
            if(ReservedChars.contains(""+c))
                builder.append("_");
            else
                builder.append(c);
        return builder.toString();
    }
}
