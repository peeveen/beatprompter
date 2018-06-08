package com.stevenfrew.beatprompter.cache;

public class FileParseError
{
    private int mLineNumber=-1;
    private String mMessage;

    public FileParseError(Tag tag,String message)
    {
        this(tag==null?-1:tag.mLineNumber,message);
    }

    public FileParseError(int lineNumber,String message)
    {
       mLineNumber=lineNumber;
        mMessage=message;
    }

    public String getErrorMessage()
    {
        return (mLineNumber!=-1?""+mLineNumber+": ":"")+mMessage;
    }
}
