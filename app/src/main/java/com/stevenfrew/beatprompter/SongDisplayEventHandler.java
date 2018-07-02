package com.stevenfrew.beatprompter;

import android.app.Activity;
import android.os.Message;
import android.util.Log;

import com.stevenfrew.beatprompter.bluetooth.BluetoothMessage;

public class SongDisplayEventHandler extends EventHandler {
    private SongView mSongView;
    private SongDisplayActivity mActivity;

    SongDisplayEventHandler(SongDisplayActivity activity,SongView songView)
    {
        mActivity=activity;
        mSongView=songView;
    }
    public void handleMessage(Message msg)
    {
        switch (msg.what)
        {
            case EventHandler.BLUETOOTH_MESSAGE_RECEIVED:
                mActivity.processBluetoothMessage((BluetoothMessage)msg.obj);
                break;
            case EventHandler.MIDI_SET_SONG_POSITION:
                if(mSongView!=null)
                    mSongView.setSongBeatPosition(msg.arg1,true);
                else
                    Log.d(BeatPrompterApplication.TAG,"MIDI song position pointer received by SongDisplay before view was created.");
                break;
            case EventHandler.MIDI_START_SONG:
                if(mSongView!=null)
                    mSongView.startSong(true,true);
                else
                    Log.d(BeatPrompterApplication.TAG,"MIDI start signal received by SongDisplay before view was created.");
                break;
            case EventHandler.MIDI_CONTINUE_SONG:
                if(mSongView!=null)
                    mSongView.startSong(true,false);
                else
                    Log.d(BeatPrompterApplication.TAG,"MIDI continue signal received by SongDisplay before view was created.");
                break;
            case EventHandler.MIDI_STOP_SONG:
                if(mSongView!=null)
                    mSongView.stopSong(true);
                else
                    Log.d(BeatPrompterApplication.TAG,"MIDI stop signal received by SongDisplay before view was created.");
                break;
            case EventHandler.END_SONG:
                mActivity.setResult(Activity.RESULT_OK);
                mActivity.finish();
                break;
            case EventHandler.MIDI_LSB_BANK_SELECT:
                BeatPrompterApplication.mMidiBankLSBs[msg.arg1]=(byte)msg.arg2;
                break;
            case EventHandler.MIDI_MSB_BANK_SELECT:
                BeatPrompterApplication.mMidiBankMSBs[msg.arg1]=(byte)msg.arg1;
                break;
        }
    }
}
