package com.stevenfrew.beatprompter;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

public class BeatPrompterApplication extends Application {
    private static String bluetoothPrefNone;
    private static String bluetoothPrefServer;
    private static String bluetoothPrefClient;

    public static final String TAG = "beatprompter";
    public static final String MIDI_TAG = "midi";
    public static final String AUTOLOAD_TAG = "autoload";
    private final static String BLUETOOTH_TAG="bpbt";

    public final static String APP_NAME="BeatPrompter";
    public final static String SHARED_PREFERENCES_ID="beatPrompterSharedPreferences";

    private static final String APP_BLUETOOTH_NAME="BeatPrompter";
    private static final UUID APP_BLUETOOTH_UUID=new UUID(0x49ED8190882ADC90L,0x93FC920912D3DD2EL);
    private static final int HANDLER_MESSAGE_BASE_ID=1834739585;

    public static final int BLUETOOTH_MESSAGE_RECEIVED=HANDLER_MESSAGE_BASE_ID;
    public static final int CLIENT_CONNECTED=HANDLER_MESSAGE_BASE_ID+1;
    public static final int SERVER_CONNECTED=HANDLER_MESSAGE_BASE_ID+2;
    public static final int MIDI_START_SONG=HANDLER_MESSAGE_BASE_ID+3;
    public static final int MIDI_CONTINUE_SONG=HANDLER_MESSAGE_BASE_ID+4;
    public static final int MIDI_STOP_SONG=HANDLER_MESSAGE_BASE_ID+5;
    public static final int END_SONG=HANDLER_MESSAGE_BASE_ID+6;
    public static final int CLOUD_SYNC_ERROR=HANDLER_MESSAGE_BASE_ID+7;
    public static final int FOLDER_CONTENTS_FETCHING=HANDLER_MESSAGE_BASE_ID+8;
    public static final int MIDI_MSB_BANK_SELECT=HANDLER_MESSAGE_BASE_ID+9;
    public static final int MIDI_LSB_BANK_SELECT=HANDLER_MESSAGE_BASE_ID+10;
    public static final int MIDI_SONG_SELECT=HANDLER_MESSAGE_BASE_ID+11;
    public static final int MIDI_PROGRAM_CHANGE=HANDLER_MESSAGE_BASE_ID+12;
    public static final int SONG_LOAD_CANCELLED=HANDLER_MESSAGE_BASE_ID+13;
    public static final int SONG_LOAD_FAILED=HANDLER_MESSAGE_BASE_ID+14;
    public static final int SONG_LOAD_LINE_READ=HANDLER_MESSAGE_BASE_ID+15;
    public static final int SONG_LOAD_LINE_PROCESSED=HANDLER_MESSAGE_BASE_ID+16;
    public static final int SONG_LOAD_COMPLETED=HANDLER_MESSAGE_BASE_ID+17;
    public static final int MIDI_SET_SONG_POSITION=HANDLER_MESSAGE_BASE_ID+18;
    public static final int POWERWASH=HANDLER_MESSAGE_BASE_ID+19;
    public static final int SET_CLOUD_PATH=HANDLER_MESSAGE_BASE_ID+20;
    public static final int CLEAR_CACHE=HANDLER_MESSAGE_BASE_ID+21;
    public static final int FOLDER_CONTENTS_FETCHED=HANDLER_MESSAGE_BASE_ID+22;
    public static final int CLIENT_DISCONNECTED=HANDLER_MESSAGE_BASE_ID+23;
    public static final int SERVER_DISCONNECTED=HANDLER_MESSAGE_BASE_ID+24;
    public static final int CACHE_UPDATED=HANDLER_MESSAGE_BASE_ID+25;

    public static final int MIDI_QUEUE_SIZE=1024;

    static ArrayBlockingQueue<MIDIOutgoingMessage> mMIDIOutQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    static ArrayBlockingQueue<MIDIIncomingMessage> mMIDISongDisplayInQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    static ArrayBlockingQueue<MIDIIncomingMessage> mMIDISongListInQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);

    private BluetoothAdapter mBluetoothAdapter;

    private final Object mBluetoothSocketsLock=new Object();
    private List<BluetoothSocket> mOutputBluetoothSockets=new ArrayList<>();
    BluetoothSocket mInputBluetoothSocket=null;

    private final Object mBluetoothThreadsLock=new Object();
    AcceptConnectionsFromClientsThread mServerBluetoothThread=null;
    List<ConnectToServerThread> mConnectToServerThreads=new ArrayList<>();

    static Handler mSongListHandler=null;
    static Handler mSongDisplayHandler=null;
    static Handler mSettingsHandler=null;
    static byte[] mMidiBankMSBs=new byte[16];
    static byte[] mMidiBankLSBs=new byte[16];

    private static Song mCurrentSong=null;
    private final static Object mCurrentSongSync=new Object();

    static Song getCurrentSong()
    {
        synchronized (mCurrentSongSync)
        {
            return mCurrentSong;
        }
    }
    static void setCurrentSong(Song song)
    {
        synchronized (mCurrentSongSync)
        {
            mCurrentSong=song;
            System.gc();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    int getBluetoothClientCount()
    {
        synchronized(mBluetoothSocketsLock) {
         return mOutputBluetoothSockets.size();
     }
    }

    static boolean cancelCurrentSong(SongFile songWeWantToInterruptWith)
    {
        Song loadedSong=getCurrentSong();
        if(loadedSong!=null)
            if(SongDisplayActivity.mSongDisplayActive)
                if(!loadedSong.mTitle.equals(songWeWantToInterruptWith.mTitle))
                    if(SongDisplayActivity.mSongDisplayInstance.canYieldToMIDITrigger()) {
                        loadedSong.mCancelled = true;
                        mSongDisplayHandler.obtainMessage(END_SONG).sendToTarget();
                    }
                    else
                        return false;
                else
                    // Trying to interrupt a song with itself is pointless!
                    return false;
        return true;
    }

    public void setSongListHandler(Handler h)
    {
        mSongListHandler=h;
    }

    public void setSongDisplayHandler(Handler h)
    {
        mSongDisplayHandler=h;
    }

    public void setSettingsHandler(Handler h)
    {
        mSettingsHandler=h;
    }

    private final BroadcastReceiver mAdapterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        onStopBluetooth();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        BluetoothMode bluetoothMode=getBluetoothMode();
                        if(bluetoothMode!=BluetoothMode.None)
                            onStartBluetooth(bluetoothMode);
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver mDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

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
                            mSongListHandler.obtainMessage(CLIENT_DISCONNECTED, device.getName()).sendToTarget();
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
                            mSongListHandler.obtainMessage(SERVER_DISCONNECTED, device.getName()).sendToTarget();
                        }
                    }
                }
            }

        }
    };


    public void onStopBluetooth()
    {
        shutDownBluetoothServer();
        shutDownBluetoothClient();
    }

    public void shutDownBluetoothServer()
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

    public void shutDownBluetoothClient()
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

    public void onStartBluetooth(BluetoothMode mode)
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
                            mServerBluetoothThread = new AcceptConnectionsFromClientsThread();
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

    private class ConnectToServerThread extends Thread
    {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mDevice;
        private boolean mStop=false;
        private final Object mSocketNullLock=new Object();

        ConnectToServerThread(BluetoothDevice device) {
            mDevice=device;
        }

        public boolean isForDevice(BluetoothDevice device)
        {
            return mDevice.getAddress().equals(device.getAddress());
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            //            mBluetoothAdapter.cancelDiscovery();
            while (!mStop) {
                boolean alreadyConnected = false;
                synchronized (mBluetoothSocketsLock) {
                    alreadyConnected = mInputBluetoothSocket != null;
                }
                if (!alreadyConnected)
                    try {
                        // Connect the device through the socket. This will block
                        // until it succeeds or throws an exception, which can happen
                        // if it doesn't find anything to connect to within about 4 seconds.
                        BluetoothSocket socket = null;
                        synchronized (mSocketNullLock) {
                            if (mmSocket == null) {
                                try {
                                    // MY_UUID is the app's UUID string, also used by the server code
                                    mmSocket = mDevice.createRfcommSocketToServiceRecord(APP_BLUETOOTH_UUID);
                                } catch (IOException e) {
                                    Log.e(BLUETOOTH_TAG, "Error creating Bluetooth socket.", e);
                                }
                            }
                            socket = mmSocket;
                        }
                        if (socket != null) {
                            socket.connect();

                            // Do work to manage the connection (in a separate thread)
                            mSongListHandler.obtainMessage(SERVER_CONNECTED, mmSocket.getRemoteDevice().getName())
                                    .sendToTarget();
                            synchronized (mBluetoothSocketsLock) {
                                mInputBluetoothSocket = mmSocket;
                            }
                            ConnectedClientThread connectedClientThread = new ConnectedClientThread(mmSocket);
                            connectedClientThread.start();
                        }
                    } catch (IOException connectException) {
                        Log.e(BLUETOOTH_TAG, "Failed to connect to a BeatPrompter band leader (there probably isn't one!)", connectException);
                    }
                    //                if(!mStop)
                else
                    // Already connected. Wait a bit and try/check again.
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Log.w(BLUETOOTH_TAG, "Thread that maintains connection to the server was interrupted while waiting.");
                    }
            }
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void stopTrying()
        {
            mStop=true;
            closeSocket();
        }

        public void closeSocket()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e) {
                Log.e(BLUETOOTH_TAG, "Error closing Bluetooth socket.",e);
            }
            finally
            {
                synchronized (mSocketNullLock) {
                    mmSocket = null;
                }
            }
        }
    }

    public BluetoothMode getBluetoothMode()
    {
        String bluetoothPref=PreferenceManager.getDefaultSharedPreferences(this).
                getString(getString(R.string.pref_bluetoothMode_key), bluetoothPrefNone);
       return bluetoothPrefServer.equals(bluetoothPref)?BluetoothMode.Server:
               (bluetoothPrefClient.equals(bluetoothPref)?BluetoothMode.Client:BluetoothMode.None);
    }

    public void broadcastMessageToClients(BluetoothMessage message)
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

    private class AcceptConnectionsFromClientsThread extends Thread
    {
        private BluetoothServerSocket mmServerSocket=null;
        private boolean mStop=false;
        private final Object mSocketNullLock=new Object();

        public void run() {
            // Keep listening until exception occurs or a socket is returned
            while (!mStop)
            {
                try {
                    BluetoothServerSocket serverSocket=null;
                    BluetoothSocket acceptedSocket=null;
                    synchronized(mSocketNullLock)
                    {
                        if(mmServerSocket==null)
                        {
                            try {
                                // MY_UUID is the app's UUID string, also used by the server code
                                mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_BLUETOOTH_NAME, APP_BLUETOOTH_UUID);
                            } catch (IOException e) {
                                Log.e(BLUETOOTH_TAG, "Error creating Bluetooth socket.",e);
                            }
                        }
                        serverSocket=mmServerSocket;
                    }

                    if(serverSocket!=null) {
                        acceptedSocket = serverSocket.accept(2000);
                        // If a connection was accepted
                        if (acceptedSocket != null) {
                            // Do work to manage the connection (in a separate thread)
                            handleConnectionFromClient(acceptedSocket);
                        }
                    }
                } catch (IOException e) {
                    //Log.e(BLUETOOTH_TAG, "Failed to accept new Bluetooth connection.",e);
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        void stopListening()
        {
            mStop=true;
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(BLUETOOTH_TAG, "Failed to close Bluetooth listener socket.",e);
            }
            finally
            {
                synchronized (mSocketNullLock)
                {
                    mmServerSocket=null;
                }
            }
        }
    }

    private class ConnectedClientThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        ConnectedClientThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(BLUETOOTH_TAG, "Failed to open Bluetooth input stream.",e);
            }

            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[2048];  // buffer store for the stream
            int bytes; // bytes returned from read()
            int bufferContentsLength=0;

            // Keep listening to the InputStream until an exception occurs
            while (mmSocket.isConnected())
            {
                try
                {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer,bufferContentsLength,2048-bufferContentsLength);
                    // Send the obtained bytes to the UI activity
                    if(bytes>0) {
                        bufferContentsLength += bytes;
                        while (bufferContentsLength > 0) {
                            try {
                                BluetoothMessage btm = BluetoothMessage.fromBytes(buffer);
                                int messageLength = btm.mMessageLength;
                                bufferContentsLength -= messageLength;
                                System.arraycopy(buffer, messageLength, buffer, 0, bufferContentsLength);
                                routeBluetoothMessage(btm);
                            } catch (NotEnoughBluetoothDataException nebde) {
                                // Read again!
                                Log.d(BLUETOOTH_TAG, "Not enough data in the Bluetooth buffer to create a fully formed message, waiting for more data.");
                                break;
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    Log.e(BLUETOOTH_TAG, "Failed to read or route the received Bluetooth message.",e);
                }
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(BLUETOOTH_TAG, "Failed to close the Bluetooth input socket.",e);
            }
        }
    }

    public void routeBluetoothMessage(BluetoothMessage btm)
    {
        if(btm instanceof ChooseSongMessage) {
            if (mSongListHandler != null)
                mSongListHandler.obtainMessage(BLUETOOTH_MESSAGE_RECEIVED, btm)
                        .sendToTarget();
        }
        else {
            if (mSongDisplayHandler != null)
                mSongDisplayHandler.obtainMessage(BLUETOOTH_MESSAGE_RECEIVED, btm)
                        .sendToTarget();
        }
    }

    public void handleConnectionFromClient(BluetoothSocket socket)
    {
        if(getBluetoothMode()==BluetoothMode.Server)
        {
            mSongListHandler.obtainMessage(CLIENT_CONNECTED, socket.getRemoteDevice().getName())
                    .sendToTarget();
            synchronized(mBluetoothSocketsLock) {
                mOutputBluetoothSockets.add(socket);
            }
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener()
            {
                public void onSharedPreferenceChanged(SharedPreferences prefs,
                                                      String key) {
                    if (key.equals(getString(R.string.pref_bluetoothMode_key)))
                    {
                        BluetoothMode mode=getBluetoothMode();
                        if(mode==BluetoothMode.None)
                            onStopBluetooth();
                        else
                            onStartBluetooth(mode);
                    }
                }
            };

    public void onCreate() {
        super.onCreate();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothPrefClient=getString(R.string.bluetoothModeBandMemberValue);
        bluetoothPrefServer=getString(R.string.bluetoothModeBandLeaderValue);
        bluetoothPrefNone=getString(R.string.bluetoothModeNoneValue);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mAdapterReceiver, filter);

        IntentFilter deviceFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mDeviceReceiver, deviceFilter);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    public void startBluetooth()
    {
        BluetoothMode bluetoothMode=getBluetoothMode();
        if(getBluetoothMode()!=BluetoothMode.None)
            onStartBluetooth(bluetoothMode);
    }

    public void onTerminate()
    {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mPrefListener);
        unregisterReceiver(mAdapterReceiver);
        unregisterReceiver(mDeviceReceiver);
        shutDownBluetoothClient();
        shutDownBluetoothServer();

        super.onTerminate();
    }

    boolean isConnectedToServer()
    {
        synchronized(mBluetoothSocketsLock) {
            return mInputBluetoothSocket != null;
        }
    }
}
