package com.stevenfrew.beatprompter;

import android.content.Context;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

class SetListFile extends CachedFile
{
    final static String SETLISTFILE_ELEMENT_TAG_NAME="set";
    private final static String SET_TITLE_ATTRIBUTE_NAME="title";

    ArrayList<String> mSongTitles=new ArrayList<>();
    String mSetTitle;

    SetListFile(Context context, DownloadedFile downloadedFile) throws InvalidBeatPrompterFileException
    {
        super(downloadedFile);
        parseSetListFileInfo(context);
    }

    private SetListFile(Context context, CachedFile cachedFile, String title) throws IOException {
        super(cachedFile);
        mSetTitle=title;
        parseSetListFileInfo(context);
    }

    private String getSetNameFromLine(String line, int lineNumber)
    {
        return getTokenValue(line, lineNumber, "set");
    }

    private void parseSetListFileInfo(Context context) throws InvalidBeatPrompterFileException
    {
        BufferedReader br=null;
        try
        {
            br=new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));
            String setTitle=null;
            String line;
            int lineNumber=0;
            while((line=br.readLine())!=null)
            {
                line=line.trim();
                if(line.startsWith("#"))
                    continue;
                if((setTitle==null)||(setTitle.length()==0))
                    setTitle = getSetNameFromLine(line, lineNumber);
                else
                    mSongTitles.add(line);
                ++lineNumber;
            }

            if((setTitle==null)||(setTitle.length()==0))
                throw new InvalidBeatPrompterFileException(String.format(context.getString(R.string.not_a_valid_set_list), mStorageName));
            else
                mSetTitle=setTitle;
        }
        catch(IOException ioe)
        {
            throw new InvalidBeatPrompterFileException(String.format(context.getString(R.string.not_a_valid_set_list), mStorageName));
        }
        finally
        {
            try
            {
                if(br!=null)
                    br.close();
            }
            catch(IOException ioe)
            {
                Log.e(SongList.TAG,"Failed to close set list file",ioe);
            }
        }
    }

    void writeToXML(Document doc, Element parent)
    {
        Element setListFileElement = doc.createElement(SETLISTFILE_ELEMENT_TAG_NAME);
        super.writeToXML(setListFileElement);
        setListFileElement.setAttribute(SET_TITLE_ATTRIBUTE_NAME, mSetTitle);
        parent.appendChild(setListFileElement);
    }

    static SetListFile readFromXMLElement(Context context,Element element) throws IOException
    {
        CachedFile cf=CachedFile.readFromXMLElement(element);
        String setTitle=element.getAttribute(SET_TITLE_ATTRIBUTE_NAME);
        return new SetListFile(context,cf,setTitle);
    }
}
