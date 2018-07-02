package com.stevenfrew.beatprompter;

import java.util.ArrayList;
import java.util.Collections;

public class Comment
{
    public String mText;
    private ArrayList<String> commentAudience=new ArrayList<>();

    Comment(String text,String audience)
    {
        mText=text;
        if(audience!=null)
        {
            String[] bits=audience.split("@");
            Collections.addAll(commentAudience, bits);
        }
    }

    public boolean isIntendedFor(String audience)
    {
        if(commentAudience.isEmpty())
            return true;
        if((audience==null)||(audience.isEmpty()))
            return true;
        String[] bits=audience.split(",");
        for(String bit:bits)
            for(String a:commentAudience)
                if(bit.trim().equalsIgnoreCase(a.trim()))
                    return true;
        return false;
    }
}
