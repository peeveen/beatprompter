package com.stevenfrew.beatprompter.midi;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MIDIValue {
    private final static String VALUE_ATTRIBUTE_NAME="value";
    private final static String CHANNEL_SPECIFIER_ATTRIBUTE_NAME="channel";

    public byte mValue;
    public boolean mChannelSpecifier;
    public MIDIValue(byte value)
    {
        this(value,false);
    }
    MIDIValue(byte value,boolean channel)
    {
        mValue=value;
        mChannelSpecifier=channel;
    }

    void writeToXML(Document doc, Element element, String elementName)
    {
        Element valEl=doc.createElement(elementName);
        valEl.setAttribute(VALUE_ATTRIBUTE_NAME,Byte.toString(mValue));
        valEl.setAttribute(CHANNEL_SPECIFIER_ATTRIBUTE_NAME,Boolean.toString(mChannelSpecifier));
        element.appendChild(valEl);
    }

    static MIDIValue readFromXMLElement(Element element)
    {
        String valueString=element.getAttribute(VALUE_ATTRIBUTE_NAME);
        byte value=Byte.parseByte(valueString);
        String channelSpecifierString=element.getAttribute(CHANNEL_SPECIFIER_ATTRIBUTE_NAME);
        boolean ch=Boolean.parseBoolean(channelSpecifierString);
        return new MIDIValue(value,ch);
    }
    public boolean isChannelledMessage()
    {
        byte test=(byte)(mValue&0xF0);
        return (test!=(byte)0xF0)&&((test&0x80)!=0);
    }
}
