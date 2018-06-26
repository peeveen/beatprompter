package com.stevenfrew.beatprompter.midi;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public class MIDISongTrigger
{
    private final static String MSB_ELEMENT_NAME="bankSelectMSB";
    private final static String LSB_ELEMENT_NAME="bankSelectLSB";
    private final static String TRIGGER_ELEMENT_NAME="triggerIndex";
    private final static String CHANNEL_ELEMENT_NAME="channel";
    private final static String IS_SONG_SELECT_ATTRIBUTE_NAME="isSongSelect";

    private MIDIValue mBankSelectMSB=new MIDIValue(WILDCARD_VALUE);
    private MIDIValue mBankSelectLSB=new MIDIValue(WILDCARD_VALUE);
    private MIDIValue mTriggerIndex=new MIDIValue((byte)0);
    private MIDIValue mChannel=new MIDIValue(WILDCARD_VALUE,true);
    private boolean mSongSelect=false;

    final static byte WILDCARD_VALUE=-1;
    private final static byte NEVER_VALUE=-2;
    final static String WILDCARD_STRING="*";

    public static MIDISongTrigger DEAD_TRIGGER=new MIDISongTrigger(new MIDIValue(NEVER_VALUE),new MIDIValue(NEVER_VALUE),new MIDIValue(NEVER_VALUE),true,new MIDIValue(NEVER_VALUE,true));

    public MIDISongTrigger(byte bankSelectMSB,byte bankSelectLSB,byte triggerIndex,boolean songSelect,byte channel)
    {
        this(new MIDIValue(bankSelectMSB),new MIDIValue(bankSelectLSB),new MIDIValue(triggerIndex),songSelect,new MIDIValue(channel,true));
    }
    private MIDISongTrigger(MIDIValue bankSelectMSB,MIDIValue bankSelectLSB,MIDIValue triggerIndex,boolean songSelect,MIDIValue channel)
    {
        mBankSelectLSB=bankSelectLSB;
        mBankSelectMSB=bankSelectMSB;
        mSongSelect=songSelect;
        mTriggerIndex=triggerIndex;
        mChannel=channel;
    }
    public boolean equals(Object o)
    {
        if(o instanceof MIDISongTrigger)
        {
            MIDISongTrigger mst=(MIDISongTrigger)o;
            if((mst.mBankSelectMSB.mValue==mBankSelectMSB.mValue)||(mBankSelectMSB.mValue==WILDCARD_VALUE))
                if((mst.mBankSelectLSB.mValue==mBankSelectLSB.mValue)||(mBankSelectLSB.mValue==WILDCARD_VALUE))
                    if(mst.mSongSelect==mSongSelect)
                        if((mst.mTriggerIndex.mValue==mTriggerIndex.mValue)||(mTriggerIndex.mValue==WILDCARD_VALUE))
                            return (mst.mChannel.mValue == mChannel.mValue) || (mChannel.mValue == WILDCARD_VALUE);
        }
        return false;
    }

    public void writeToXML(Document doc, Element parent,String tag)
    {
        Element triggerElement = doc.createElement(tag);
        mBankSelectMSB.writeToXML(doc,triggerElement,MSB_ELEMENT_NAME);
        mBankSelectLSB.writeToXML(doc,triggerElement,LSB_ELEMENT_NAME);
        mTriggerIndex.writeToXML(doc,triggerElement,TRIGGER_ELEMENT_NAME);
        mChannel.writeToXML(doc,triggerElement,CHANNEL_ELEMENT_NAME);
        triggerElement.setAttribute(IS_SONG_SELECT_ATTRIBUTE_NAME, "" + mSongSelect);
        parent.appendChild(triggerElement);
    }

    public static MIDISongTrigger readFromXMLElement(Element element)
    {

        NodeList msbNodes=element.getElementsByTagName(MSB_ELEMENT_NAME);
        if(msbNodes.getLength()==1) {
            MIDIValue bankSelectMSB = MIDIValue.readFromXMLElement((Element)msbNodes.item(0));
            NodeList lsbNodes = element.getElementsByTagName(LSB_ELEMENT_NAME);
            if (lsbNodes.getLength() == 1) {
                MIDIValue bankSelectLSB = MIDIValue.readFromXMLElement((Element)lsbNodes.item(0));
                NodeList triggerNodes = element.getElementsByTagName(TRIGGER_ELEMENT_NAME);
                if (triggerNodes.getLength() == 1) {
                    MIDIValue triggerIndex = MIDIValue.readFromXMLElement((Element)triggerNodes.item(0));
                    NodeList channelNodes = element.getElementsByTagName(CHANNEL_ELEMENT_NAME);
                    if (channelNodes.getLength() == 1) {
                        MIDIValue channel = MIDIValue.readFromXMLElement((Element)channelNodes.item(0));
                        String isSongSelectString=element.getAttribute(IS_SONG_SELECT_ATTRIBUTE_NAME);
                        boolean isSongSelect=Boolean.parseBoolean(isSongSelectString);
                        return new MIDISongTrigger(bankSelectMSB,bankSelectLSB,triggerIndex,isSongSelect,channel);
                    }
                }
            }
        }
        return null;
    }

    public static MIDISongTrigger parse(String descriptor, boolean songSelect)
    {
        if(descriptor==null)
            return null;
        String[] bits=descriptor.split(",");
        for(int f=0;f<bits.length;++f)
            bits[f]=bits[f].trim();
        MIDIValue msb=new MIDIValue(WILDCARD_VALUE,false);
        MIDIValue lsb=new MIDIValue(WILDCARD_VALUE,false);
        MIDIValue channel=new MIDIValue(WILDCARD_VALUE,true);
        if(bits.length>1)
            if(songSelect)
                throw new IllegalArgumentException(SongList.mSongListInstance.getString(R.string.song_index_must_have_one_value));
        if((bits.length>4)||(bits.length<1))
            if(songSelect)
                throw new IllegalArgumentException(SongList.mSongListInstance.getString(R.string.song_index_must_have_one_value));
            else
                throw new IllegalArgumentException(SongList.mSongListInstance.getString(R.string.song_index_must_have_one_two_or_three_values));

        if(bits.length>3)
            channel=MIDIMessage.parseChannelValue(bits[3],true);

        if(bits.length>2)
            lsb=MIDIMessage.parseValue(bits[2],true);

        if(bits.length>1)
            msb=MIDIMessage.parseValue(bits[1],true);

        MIDIValue index=MIDIMessage.parseValue(bits[0],true);

        return new MIDISongTrigger(msb,lsb,index,songSelect,channel);
    }
    public boolean isSendable()
    {
        return mTriggerIndex.mValue>=0 && mBankSelectLSB.mValue>=0 && mBankSelectMSB.mValue>=0;
    }
    public ArrayList<MIDIOutgoingMessage> getMIDIMessages(byte defaultOutputChannel)
    {
        ArrayList<MIDIOutgoingMessage> outputMessages=new ArrayList<>();
        if(mSongSelect)
            outputMessages.add(new MIDISongSelectMessage(mTriggerIndex.mValue));
        else
        {
            byte channel=mChannel.mValue;
            if(channel==WILDCARD_VALUE)
                channel=defaultOutputChannel;
            outputMessages.add(new MIDIControlChangeMessage(MIDIControlChangeMessage.BANK_SELECT_MSB_BYTE,mBankSelectMSB,channel));
            outputMessages.add(new MIDIControlChangeMessage(MIDIControlChangeMessage.BANK_SELECT_LSB_BYTE,mBankSelectLSB,channel));
            outputMessages.add(new MIDIProgramChangeMessage(mTriggerIndex.mValue,channel));
        }
        return outputMessages;
    }
}
