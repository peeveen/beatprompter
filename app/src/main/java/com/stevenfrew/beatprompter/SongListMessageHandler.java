package com.stevenfrew.beatprompter;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.stevenfrew.beatprompter.bluetooth.BluetoothMessage;
import com.stevenfrew.beatprompter.cache.CachedCloudFileCollection;

class SongListMessageHandler extends Handler {
    private SongList mSongList;
    SongListMessageHandler(SongList songList)
    {
        mSongList=songList;
    }
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case BeatPrompterApplication.BLUETOOTH_MESSAGE_RECEIVED:
                mSongList.processBluetoothMessage((BluetoothMessage) msg.obj);
                break;
            case BeatPrompterApplication.CLIENT_CONNECTED:
                Toast.makeText(mSongList, msg.obj + " " + SongList.mSongListInstance.getString(R.string.hasConnected), Toast.LENGTH_LONG).show();
                mSongList.updateBluetoothIcon();
                break;
            case BeatPrompterApplication.CLIENT_DISCONNECTED:
                Toast.makeText(mSongList, msg.obj + " " + SongList.mSongListInstance.getString(R.string.hasDisconnected), Toast.LENGTH_LONG).show();
                mSongList.updateBluetoothIcon();
                break;
            case BeatPrompterApplication.SERVER_DISCONNECTED:
                Toast.makeText(mSongList, SongList.mSongListInstance.getString(R.string.disconnectedFromBandLeader) + " " + msg.obj, Toast.LENGTH_LONG).show();
                mSongList.updateBluetoothIcon();
                break;
            case BeatPrompterApplication.SERVER_CONNECTED:
                Toast.makeText(mSongList, SongList.mSongListInstance.getString(R.string.connectedToBandLeader) + " " + msg.obj, Toast.LENGTH_LONG).show();
                mSongList.updateBluetoothIcon();
                break;
            case BeatPrompterApplication.CLOUD_SYNC_ERROR:
                AlertDialog.Builder adb = new AlertDialog.Builder(mSongList);
                adb.setMessage(String.format(SongList.mSongListInstance.getString(R.string.cloudSyncErrorMessage), (String) msg.obj));
                adb.setTitle(SongList.mSongListInstance.getString(R.string.cloudSyncErrorTitle));
                adb.setPositiveButton("OK", (dialog, id) -> dialog.cancel());
                AlertDialog ad = adb.create();
                ad.setCanceledOnTouchOutside(true);
                ad.show();
                break;
            case BeatPrompterApplication.SONG_LOAD_FAILED:
                Toast.makeText(mSongList, msg.obj.toString(), Toast.LENGTH_LONG).show();
                break;
            case BeatPrompterApplication.MIDI_LSB_BANK_SELECT:
                BeatPrompterApplication.mMidiBankLSBs[msg.arg1] = (byte) msg.arg2;
                break;
            case BeatPrompterApplication.MIDI_MSB_BANK_SELECT:
                BeatPrompterApplication.mMidiBankMSBs[msg.arg1] = (byte) msg.arg2;
                break;
            case BeatPrompterApplication.MIDI_PROGRAM_CHANGE:
                mSongList.startSongViaMidiProgramChange(BeatPrompterApplication.mMidiBankMSBs[msg.arg1], BeatPrompterApplication.mMidiBankLSBs[msg.arg1], (byte) msg.arg2, (byte) msg.arg1);
                break;
            case BeatPrompterApplication.MIDI_SONG_SELECT:
                mSongList.startSongViaMidiSongSelect((byte) msg.arg1);
                break;
            case BeatPrompterApplication.SONG_LOAD_COMPLETED:
                mSongList.startSongActivity();
                break;
            case BeatPrompterApplication.CLEAR_CACHE:
                mSongList.clearCache();
                break;
            case BeatPrompterApplication.CACHE_UPDATED:
                CachedCloudFileCollection cache = (CachedCloudFileCollection) msg.obj;
                mSongList.onCacheUpdated(cache);
                break;
        }
    }

}
