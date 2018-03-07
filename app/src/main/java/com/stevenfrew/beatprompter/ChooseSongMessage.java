package com.stevenfrew.beatprompter;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

class ChooseSongMessage extends BluetoothMessage
{
    static final byte CHOOSE_SONG_MESSAGE_ID=0;

    String mTitle;
    String mTrack;
    boolean mBeatScroll;
    boolean mSmoothScroll;
    int mOrientation;
    int mMinFontSize;
    int mMaxFontSize;
    int mScreenWidth;
    int mScreenHeight;

    ChooseSongMessage(LoadingSongFile lsf)
    {
        mTitle=lsf.mSongFile.mTitle;
        if(mTitle==null)
            mTitle="";
        mTrack=lsf.mTrack;
        mOrientation=lsf.mNativeDisplaySettings.mOrientation;
        if(mTrack==null)
            mTrack="";
        mBeatScroll=lsf.mScrollMode==ScrollingMode.Beat;
        mSmoothScroll=lsf.mScrollMode==ScrollingMode.Smooth;
        mMinFontSize=lsf.mNativeDisplaySettings.mMinFontSize;
        mMaxFontSize=lsf.mNativeDisplaySettings.mMaxFontSize;
        mScreenWidth=lsf.mNativeDisplaySettings.mScreenWidth;
        mScreenHeight=lsf.mNativeDisplaySettings.mScreenHeight;
    }

    ChooseSongMessage(byte[] bytes) throws NotEnoughBluetoothDataException
    {
        try
        {
            final ByteArrayInputStream byteArrayInputStream =
                    new ByteArrayInputStream(bytes);
            int dataRead=byteArrayInputStream.read(new byte[1]);
            if(dataRead==1) {
                int availableStart=byteArrayInputStream.available();
                final ObjectInputStream objectInputStream =
                        new ObjectInputStream(byteArrayInputStream);
                mTitle = (String) objectInputStream.readObject();
                mTrack = (String) objectInputStream.readObject();
                mBeatScroll = objectInputStream.readBoolean();
                mSmoothScroll = objectInputStream.readBoolean();
                mOrientation = objectInputStream.readInt();
                mMinFontSize = objectInputStream.readInt();
                mMaxFontSize = objectInputStream.readInt();
                mScreenWidth=objectInputStream.readInt();
                mScreenHeight=objectInputStream.readInt();
                int availableEnd=byteArrayInputStream.available();
                objectInputStream.close();
                byteArrayInputStream.close();
                mMessageLength=1+(availableStart-availableEnd);
            }
            else
                throw new NotEnoughBluetoothDataException();
        }
        catch(Exception e)
        {
            Log.e(SongList.TAG,"Couldn't read ChooseSongMessage data, assuming not enough data",e);
            throw new NotEnoughBluetoothDataException();
        }
    }

    public byte[] getBytes()
    {
        try
        {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(new byte[]{CHOOSE_SONG_MESSAGE_ID},0,1);
            final ObjectOutputStream objectOutputStream =
                    new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(mTitle);
            objectOutputStream.writeObject(mTrack);
            objectOutputStream.writeBoolean(mBeatScroll);
            objectOutputStream.writeBoolean(mSmoothScroll);
            objectOutputStream.writeInt(mOrientation);
            objectOutputStream.writeInt(mMinFontSize);
            objectOutputStream.writeInt(mMaxFontSize);
            objectOutputStream.writeInt(mScreenWidth);
            objectOutputStream.writeInt(mScreenHeight);
            objectOutputStream.flush();
            objectOutputStream.close();

            final byte[] byteArray = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            return byteArray;
        }
        catch(Exception e)
        {
            Log.e(SongList.TAG,"Couldn't write ChooseSongMessage data.",e);
        }
        return null;
    }
}
