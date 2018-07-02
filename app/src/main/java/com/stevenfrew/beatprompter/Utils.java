package com.stevenfrew.beatprompter;

import android.graphics.Color;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Utils
{
    static double[] mSineLookup=new double[91];

    // Set by onCreate() in SongList.java
    static int MAXIMUM_FONT_SIZE;
    static int MINIMUM_FONT_SIZE;
    public static float FONT_SCALING;

    static
    {
        for(int f=0;f<=90;++f) {
            double radians=Math.toRadians(f);
            mSineLookup[f] = Math.sin(radians);
        }
    }

    // Special token.
    public static final int TRACK_AUDIO_LENGTH_VALUE=-93781472;

    public static long nanosecondsPerBeat(double bpm)
    {
        return (long)(60000000000.0/bpm);
    }
    public static int nanoToMilli(long nano) { return (int)(nano/1000000);}
    public static long milliToNano(int milli) { return ((long)milli)*1000000;}
    public static long milliToNano(long milli) { return milli*1000000;}
    public static double bpmToMIDIClockNanoseconds(double bpm){return 60000000000.0/(bpm*24.0);}
    public static int makeHighlightColour(int colour)
    {
        colour&=0x00ffffff;
        colour|=0x44000000;
        return colour;
    }
    public static int makeHighlightColour(int colour,byte opacity)
    {
        colour&=0x00ffffff;
        colour|=opacity << 24;
        return colour;
    }
    public static int makeContrastingColour(int colour)
    {
        int r=(colour>>16) &0x000000FF;
        int g=(colour>>8) &0x000000FF;
        int b=colour &0x000000FF;
        return ((r * 0.299) + (g * 0.587) + (b * 0.114) > 186 ? Color.BLACK : Color.WHITE);
    }
    private static List<String> splitters=new ArrayList<>(Arrays.asList(" ", "-"));
    public static int countWords(String[] words)
    {
        int wordCount=0;
        for(String w:words)
            if(!splitters.contains(w))
                wordCount++;
        return wordCount;
    }
    public static String stitchBits(String[] bits,int nonWhitespaceBitsToJoin)
    {
        StringBuilder result= new StringBuilder();
        int nonWhitespaceBitsJoined=0;
        for (String bit : bits) {
            boolean whitespace = (bit.trim().length() == 0);
            if ((!whitespace) && (nonWhitespaceBitsJoined == nonWhitespaceBitsToJoin))
                break;
            result.append(bit);
            if (!whitespace)
                ++nonWhitespaceBitsJoined;
        }
        return result.toString();
    }
    public static String[] splitText(String str)
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
    public static String[] splitIntoLetters(String str)
    {
        char[] chars=str.toCharArray();
        String[] charsAsStrings=new String[chars.length];
        for(int f=0;f<chars.length;++f)
            charsAsStrings[f]=""+chars[f];
        return charsAsStrings;
    }
    public static int parseDuration(String str, boolean trackLengthAllowed)
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

    public static boolean isChord(String text) {
        if (text != null)
            text = text.trim();
        return !((text == null) || (text.length() == 0)) && pattern.matcher(text).matches();
    }

    public static void streamToStream(InputStream is, OutputStream os) throws IOException
    {
        byte[] buffer = new byte[2048];
        int bytesRead;
        while ((bytesRead = is.read(buffer, 0, buffer.length)) != -1)
            os.write(buffer, 0, bytesRead);
    }

    private static final String ReservedChars = "|\\?*<\":>+[]/'";
    public static String makeSafeFilename(String str)
    {
        StringBuilder builder=new StringBuilder();
        for(char c:str.toCharArray())
            if(ReservedChars.contains(""+c))
                builder.append("_");
            else
                builder.append(c);
        return builder.toString();
    }

    public static void appendToTextFile(File file,String str) throws IOException
    {
        try(FileWriter fw = new FileWriter(file.getAbsolutePath(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(str);
        }
    }
    private static String stripHexSignifiers(String str)
    {
        str = str.toLowerCase();
        if (str.startsWith("0x"))
            str = str.substring(2);
        else if (str.endsWith("h"))
            str = str.substring(0, str.length() - 1);
        return str;
    }
    public static byte parseHexByte(String str)
    {
        return parseByte(stripHexSignifiers(str),16);
    }
    public static byte parseByte(String str)
    {
        return parseByte(str,10);
    }
    private static byte parseByte(String str,int radix)
    {
        int val=Integer.parseInt(str,radix);
        return (byte)(val&0x000000FF);
    }
    public static boolean looksLikeHex(String str) {
        if (str == null)
            return false;
        str = str.toLowerCase();
        boolean signifierFound = false;
        if (str.startsWith("0x")) {
            signifierFound = true;
            str = str.substring(2);
        } else if (str.endsWith("h"))
        {
            signifierFound = true;
            str = str.substring(0, str.length() - 1);
        }
        // Hex values for this app are two-chars long, max.
        if(str.length()>2)
            return false;
        try
        {
            //noinspection ResultOfMethodCallIgnored
            Integer.parseInt(str);
            // non-hex integer
            return signifierFound;
        }
        catch(Exception ignored)
        {
        }
        for(int f=0;f<str.length();++f) {
            char c=str.charAt(f);
            if ((!Character.isDigit(c))&&(c != 'a')&&(c != 'b')&&(c != 'c')&&(c != 'd')&&(c != 'e')&&(c != 'f'))
                return false;
        }
        return true;
    }
}
