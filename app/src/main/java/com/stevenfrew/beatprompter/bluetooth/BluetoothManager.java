package com.stevenfrew.beatprompter.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.EventHandler;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothManager {

    public final static String BLUETOOTH_TAG="bpbt";
    private static BluetoothAdapter mBluetoothAdapter;

    private static final Object mBluetoothSocketsLock=new Object();
    private static List<BluetoothSocket> mOutputBluetoothSockets=new ArrayList<>();
    private static BluetoothSocket mInputBluetoothSocket=null;

    private static final Object mBluetoothThreadsLock=new Object();
    private static AcceptConnectionsFromClientsThread mServerBluetoothThread=null;
    private static List<ConnectToServerThread> mConnectToServerThreads=new ArrayList<>();

    private static final BroadcastReceiver mAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action!=null)
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            BluetoothManager.onStopBluetooth();
                            break;
                        case BluetoothAdapter.STATE_ON:
                            BluetoothMode bluetoothMode=getBluetoothMode();
                            if(bluetoothMode!=BluetoothMode.None)
                                BluetoothManager.onStartBluetooth(bluetoothMode);
                            break;
                    }
                }
        }
    };

    private static final BroadcastReceiver mDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action!=null)
                if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
                {
                    // Something has disconnected.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    synchronized(mBluetoothSocketsLock) {
                        for (int f = mOutputBluetoothSockets.size() - 1; f >= 0; --f) {
                            BluetoothSocket socket = mOutputBluetoothSockets.get(f);
                            if (socket.getRemoteDevice().getAddress().equals(device.getAddress())) {
                                mOutputBluetoothSockets.remove(f);
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(BLUETOOTH_TAG, "Error closing socket after client disconnection notification.", e);
                                }
                                EventHandler.sendEventToSongList(EventHandler.CLIENT_DISCONNECTED, device.getName());
                            }
                        }
                        if (mInputBluetoothSocket != null) {
                            if (mInputBluetoothSocket.getRemoteDevice().getAddress().equals(device.getAddress())) {
                                try {
                                    mInputBluetoothSocket.close();
                                } catch (IOException e) {
                                    Log.e(BLUETOOTH_TAG, "Error closing socket after server disconnection notification.", e);
                                } finally {
                                    mInputBluetoothSocket = null;
                                }
                                synchronized(mBluetoothThreadsLock) {
                                    for (ConnectToServerThread thread : mConnectToServerThreads)
                                        if (thread.isForDevice(device))
                                            thread.closeSocket();
                                }
                                EventHandler.sendEventToSongList(EventHandler.SERVER_DISCONNECTED, device.getName());
                            }
                        }
                    }
                }
        }
    };

    public static void initialise(BeatPrompterApplication application)
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        application.registerReceiver(mAdapterReceiver, filter);

        IntentFilter deviceFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        application.registerReceiver(mDeviceReceiver, deviceFilter);

        PreferenceManager.getDefaultSharedPreferences(application).registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    private static void onStopBluetooth()
    {
        shutDownBluetoothServer();
        shutDownBluetoothClient();
    }
    private static void shutDownBluetoothServer()
    {
        synchronized(mBluetoothThreadsLock) {
            if (mServerBluetoothThread != null)
            {
                try
                {
                    mServerBluetoothThread.stopListening();
                    mServerBluetoothThread.interrupt();
                    mServerBluetoothThread.join();
                } catch (Exception e) {
                    Log.e(BLUETOOTH_TAG, "Error stopping BlueTooth server connection accepting thread, on thread join.", e);
                }
                finally {
                    mServerBluetoothThread = null;
                }
            }
        }
        synchronized(mBluetoothSocketsLock) {
            for (BluetoothSocket socket : mOutputBluetoothSockets)
                try {
                    if (socket != null)
                        if (socket.isConnected())
                            socket.close();
                } catch (IOException e) {
                    Log.e(BLUETOOTH_TAG, "Error stopping BlueTooth, on socket close.", e);
                }
            mOutputBluetoothSockets.clear();
        }
    }

    private static void shutDownBluetoothClient()
    {
        synchronized(mBluetoothThreadsLock) {
            if(mConnectToServerThreads.size()>0)
            {
                for(ConnectToServerThread thread:mConnectToServerThreads)
                    try
                    {
                        thread.stopTrying();
                        thread.interrupt();
                        thread.join();
                    } catch (Exception e) {
                        Log.e(BLUETOOTH_TAG, "Error stopping BlueTooth client connection thread, on thread join.", e);
                    }
                mConnectToServerThreads.clear();
            }
        }
        synchronized(mBluetoothSocketsLock) {
            if (mInputBluetoothSocket != null) {
                if (mInputBluetoothSocket.isConnected())
                    try {
                        mInputBluetoothSocket.close();
                    } catch (IOException ioe) {
                        Log.e(BLUETOOTH_TAG, "Error stopping BlueTooth, on input socket close.", ioe);
                    }
                mInputBluetoothSocket = null;
            }
        }
    }

    private static void onStartBluetooth(BluetoothMode mode)
    {
        if(mBluetoothAdapter!=null)
            if(mBluetoothAdapter.isEnabled())
            {
                synchronized(mBluetoothThreadsLock)
                {
                    if (mode == BluetoothMode.Server)
                    {
                        shutDownBluetoothClient();
                        if (mServerBluetoothThread == null) {
                            mServerBluetoothThread = new AcceptConnectionsFromClientsThread(mBluetoothAdapter);
                            mServerBluetoothThread.start();
                        }
                    } else if(mode==BluetoothMode.Client)
                    {
                        shutDownBluetoothServer();
                        if (mConnectToServerThreads.size() == 0) {
                            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                            for (BluetoothDevice device : pairedDevices)
                            {
                                try {
                                    ConnectToServerThread thread = new ConnectToServerThread(device);
                                    mConnectToServerThreads.add(thread);
                                }
                                catch(Exception e)
                                {
                                    Log.e(BLUETOOTH_TAG,"Failed to create ConnectToServerThread for bluetooth device "+device.getName(),e);
                                }
                            }
                            for (ConnectToServerThread thread : mConnectToServerThreads)
                                try {
                                    thread.start();
                                }
                                catch(Exception e)
                                {
                                    Log.e(BLUETOOTH_TAG,"Failed to start a ConnectToServerThread",e);
                                }
                        }
                    }
                }
            }
    }

    public static BluetoothMode getBluetoothMode()
    {
        try
        {
            return BluetoothMode.valueOf(PreferenceManager.getDefaultSharedPreferences(SongList.mSongListInstance).
                    getString(SongList.mSongListInstance.getString(R.string.pref_bluetoothMode_key),
                            SongList.mSongListInstance.getString(R.string.bluetoothModeNoneValue)));
        }
        catch(Exception e)
        {
            // Backwards compatibility with old shite values.
            return BluetoothMode.None;
        }
    }

    public static void broadcastMessageToClients(BluetoothMessage message)
    {
        if(getBluetoothMode()==BluetoothMode.Server)
        {
            byte[] bytes = message.getBytes();
            synchronized(mBluetoothSocketsLock) {
                for (BluetoothSocket outputSocket : mOutputBluetoothSockets) {
                    try {
                        if (outputSocket.isConnected()) {
                            Log.d(BLUETOOTH_TAG, "Broadcasting message " + message + " to listening apps.");
                            outputSocket.getOutputStream().write(bytes);
                        }
                    } catch (IOException e) {
                        Log.e(BLUETOOTH_TAG, "Failed to send Bluetooth message.", e);
                    }
                }
            }
        }
    }

    static void handleConnectionFromClient(BluetoothSocket socket)
    {
        if(getBluetoothMode()==BluetoothMode.Server)
        {
            EventHandler.sendEventToSongList(EventHandler.CLIENT_CONNECTED, socket.getRemoteDevice().getName());
            synchronized(mBluetoothSocketsLock) {
                mOutputBluetoothSockets.add(socket);
            }
        }
    }

    private static SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
            (prefs, key) -> {
                if (key.equals(SongList.mSongListInstance.getString(R.string.pref_bluetoothMode_key)))
                {
                    BluetoothMode mode=getBluetoothMode();
                    if(mode==BluetoothMode.None)
                        onStopBluetooth();
                    else
                        onStartBluetooth(mode);
                }
            };

    public static void startBluetooth()
    {
        BluetoothMode bluetoothMode=getBluetoothMode();
        if(getBluetoothMode()!=BluetoothMode.None)
            onStartBluetooth(bluetoothMode);
    }

    static void setServerConnection(BluetoothSocket socket)
    {
        EventHandler.sendEventToSongList(EventHandler.SERVER_CONNECTED, socket.getRemoteDevice().getName());
        synchronized (mBluetoothSocketsLock) {
            mInputBluetoothSocket = socket;
        }
    }

    public static boolean isConnectedToServer()
    {
        synchronized(mBluetoothSocketsLock) {
            return mInputBluetoothSocket != null;
        }
    }

    public static void routeBluetoothMessage(BluetoothMessage btm) {
        if (btm instanceof ChooseSongMessage)
                EventHandler.sendEventToSongList(EventHandler.BLUETOOTH_MESSAGE_RECEIVED, btm);
        else
                EventHandler.sendEventToSongDisplay(EventHandler.BLUETOOTH_MESSAGE_RECEIVED, btm);
    }

    public static int getBluetoothClientCount()
    {
        synchronized(mBluetoothSocketsLock) {
            return mOutputBluetoothSockets.size();
        }
    }

    public static void shutdown(BeatPrompterApplication application)
    {
        PreferenceManager.getDefaultSharedPreferences(application).unregisterOnSharedPreferenceChangeListener(mPrefListener);
        application.unregisterReceiver(mAdapterReceiver);
        application.unregisterReceiver(mDeviceReceiver);
        shutDownBluetoothClient();
        shutDownBluetoothServer();
    }
}
