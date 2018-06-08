package com.stevenfrew.beatprompter.bluetooth;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class SetSongTimeMessage extends BluetoothMessage
{
    static final byte SET_SONG_TIME_MESSAGE_ID=2;

    private static final int LONG_BUFFER_SIZE=Long.SIZE/Byte.SIZE;

    public long mTime;

    public SetSongTimeMessage(long time)
    {
        mTime=time;
    }

    SetSongTimeMessage(byte[] bytes) throws NotEnoughBluetoothDataException
    {
        try
        {
            final ByteArrayInputStream byteArrayInputStream =
                    new ByteArrayInputStream(bytes);
            int bytesRead=byteArrayInputStream.read(new byte[1]);
            if(bytesRead==1) {
                mTime = 0;
                byte[] longBytes = new byte[LONG_BUFFER_SIZE];
                bytesRead=byteArrayInputStream.read(longBytes);
                if(bytesRead==LONG_BUFFER_SIZE) {
                    for (int f = LONG_BUFFER_SIZE - 1; f >= 0; --f) {
                        mTime <<= 8;
                        mTime |= (((long) longBytes[f]) & 0x00000000000000FFL);
                    }
                }
                else
                    throw new NotEnoughBluetoothDataException();
            }
            else
                throw new NotEnoughBluetoothDataException();
            byteArrayInputStream.close();
            mMessageLength=1+LONG_BUFFER_SIZE;
        }
        catch(NotEnoughBluetoothDataException nebde)
        {
            throw nebde;
        }
        catch(Exception e)
        {
            Log.e(BeatPrompterApplication.TAG,"Failed to read SetSongTimeMessage, assuming insufficient data.",e);
            throw new NotEnoughBluetoothDataException();
        }
    }

    public byte[] getBytes()
    {
        try
        {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(new byte[]{SET_SONG_TIME_MESSAGE_ID});
            byte[] longBytes=new byte[LONG_BUFFER_SIZE];
            long time=mTime;
            for(int f=0;f<LONG_BUFFER_SIZE;++f)
            {
                longBytes[f]=(byte)(time&0x00000000000000FFL);
                time>>=8;
            }
            byteArrayOutputStream.write(longBytes);
            final byte[] byteArray = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            return byteArray;
        }
        catch(Exception e)
        {
            Log.e(BeatPrompterApplication.TAG,"Failed to write SetSongTimeMessage.",e);
        }
        return null;
    }
}
