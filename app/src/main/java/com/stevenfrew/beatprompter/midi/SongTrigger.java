package com.stevenfrew.beatprompter.midi;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cache.FileParseError;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class SongTrigger
{
    private final static String MSB_ATTRIBUTE_NAME="bankSelectMSB";
    private final static String LSB_ATTRIBUTE_NAME="bankSelectLSB";
    private final static String TRIGGER_INDEX_ATTRIBUTE_NAME="triggerIndex";
    private final static String CHANNEL_ATTRIBUTE_NAME="channel";
    private final static String IS_SONG_SELECT_ATTRIBUTE_NAME="isSongSelect";

    private Value mBankSelectMSB;
    private Value mBankSelectLSB;
    private Value mTriggerIndex;
    private Value mChannel;
    private boolean mSongSelect;

    public static SongTrigger DEAD_TRIGGER=new SongTrigger(new NoValue(),new NoValue(),new NoValue(),new NoValue(),true);

    public SongTrigger(byte msb,byte lsb,byte triggerIndex,byte channel,boolean isSongSelect)
    {
        this(new CommandValue(msb),new CommandValue(lsb),new CommandValue(triggerIndex),new CommandValue(channel),isSongSelect);
    }
    private SongTrigger(Value bankSelectMSB, Value bankSelectLSB, Value triggerIndex, Value channel, boolean songSelect)
    {
        mBankSelectLSB=bankSelectLSB;
        mBankSelectMSB=bankSelectMSB;
        mSongSelect=songSelect;
        mTriggerIndex=triggerIndex;
        mChannel=channel;
    }
    public boolean equals(Object o)
    {
        if(o instanceof SongTrigger)
        {
            SongTrigger mst=(SongTrigger)o;
            if(mst.mBankSelectMSB.matches(mBankSelectMSB))
                if(mst.mBankSelectLSB.matches(mBankSelectLSB))
                    if(mst.mSongSelect==mSongSelect)
                        if(mst.mTriggerIndex.matches(mTriggerIndex))
                            return (mst.mChannel.matches(mChannel));
        }
        return false;
    }

    public void writeToXML(Document doc, Element parent,String tag)
    {
        Element triggerElement = doc.createElement(tag);
        triggerElement.setAttribute(MSB_ATTRIBUTE_NAME,mBankSelectMSB.toString());
        triggerElement.setAttribute(LSB_ATTRIBUTE_NAME,mBankSelectLSB.toString());
        triggerElement.setAttribute(TRIGGER_INDEX_ATTRIBUTE_NAME,mTriggerIndex.toString());
        triggerElement.setAttribute(CHANNEL_ATTRIBUTE_NAME,mChannel.toString());
        triggerElement.setAttribute(IS_SONG_SELECT_ATTRIBUTE_NAME, "" + mSongSelect);
        parent.appendChild(triggerElement);
    }

    public static SongTrigger readFromXMLElement(Element element)
    {
        String msbString=element.getAttribute(MSB_ATTRIBUTE_NAME);
        String lsbString=element.getAttribute(LSB_ATTRIBUTE_NAME);
        String triggerIndexString=element.getAttribute(TRIGGER_INDEX_ATTRIBUTE_NAME);
        String channelString=element.getAttribute(CHANNEL_ATTRIBUTE_NAME);
        String isSongSelectString=element.getAttribute(IS_SONG_SELECT_ATTRIBUTE_NAME);

        Value msbValue=Value.parseValue(msbString);
        Value lsbValue=Value.parseValue(lsbString);
        Value triggerIndexValue=Value.parseValue(triggerIndexString);
        Value channelValue=Value.parseChannelValue(channelString);
        boolean isSongSelect=Boolean.parseBoolean(isSongSelectString);

        return new SongTrigger(msbValue,lsbValue,triggerIndexValue,channelValue,isSongSelect);
    }

    public static SongTrigger parse(String descriptor, boolean songSelect, int lineNumber, List<FileParseError> errors)
    {
        if(descriptor==null)
            return null;
        String[] bits=descriptor.split(",");
        for(int f=0;f<bits.length;++f)
            bits[f]=bits[f].trim();
        Value msb=new WildcardValue();
        Value lsb=new WildcardValue();
        Value channel=new WildcardValue();
        if(bits.length>1)
            if(songSelect)
                errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.song_index_must_have_one_value)));
        if((bits.length>4)||(bits.length<1))
            if(songSelect)
                errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.song_index_must_have_one_value)));
            else
                errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.song_index_must_have_one_two_or_three_values)));

        if(bits.length>3) {
            Value value=Value.parseValue(bits[3],lineNumber,3,bits.length,errors);
            if(value instanceof ChannelValue)
                channel = value;
        }

        if(bits.length>2)
            lsb= Value.parseValue(bits[2],lineNumber,2,bits.length,errors);

        if(bits.length>1)
            msb= Value.parseValue(bits[1],lineNumber,1,bits.length,errors);

        Value index= Value.parseValue(bits[0],lineNumber,0,bits.length,errors);

        return new SongTrigger(msb,lsb,index,channel,songSelect);
    }
    public boolean isSendable()
    {
        return mTriggerIndex instanceof CommandValue
                && mBankSelectLSB instanceof CommandValue
                && mBankSelectMSB instanceof CommandValue;
    }
    public ArrayList<OutgoingMessage> getMIDIMessages(byte defaultOutputChannel) throws ResolutionException
    {
        ArrayList<OutgoingMessage> outputMessages=new ArrayList<>();
        if(mSongSelect)
            outputMessages.add(new SongSelectMessage(mTriggerIndex.resolve()));
        else
        {
            byte channel;
            if(mChannel instanceof WildcardValue)
                channel=defaultOutputChannel;
            else
                channel=mChannel.resolve();

            outputMessages.add(new ControlChangeMessage(ControlChangeMessage.BANK_SELECT_MSB_BYTE,mBankSelectMSB.resolve(),channel));
            outputMessages.add(new ControlChangeMessage(ControlChangeMessage.BANK_SELECT_LSB_BYTE,mBankSelectLSB.resolve(),channel));
            outputMessages.add(new ProgramChangeMessage(mTriggerIndex.resolve(),channel));
        }
        return outputMessages;
    }
}
