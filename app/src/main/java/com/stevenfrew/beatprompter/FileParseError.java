package com.stevenfrew.beatprompter;

class FileParseError
{
    private int mLineNumber=-1;
    private String mMessage;

    FileParseError(Tag tag,String message)
    {
        this(tag==null?-1:tag.mLineNumber,message);
    }

    FileParseError(int lineNumber,String message)
    {
       mLineNumber=lineNumber;
        mMessage=message;
    }

    String getErrorMessage()
    {
        return (mLineNumber!=-1?""+mLineNumber+": ":"")+mMessage;
    }
}
