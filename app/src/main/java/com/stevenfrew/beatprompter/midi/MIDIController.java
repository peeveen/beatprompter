package com.stevenfrew.beatprompter.midi;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.Task;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import static android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK;

public class MIDIController {
    private static boolean mMidiUsbRegistered = false;
    private static UsbManager mUsbManager;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static PendingIntent mPermissionIntent;

    public static final String MIDI_TAG="midi";
    private static final int MIDI_QUEUE_SIZE=1024;
    public static ArrayBlockingQueue<OutgoingMessage> mMIDIOutQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    public static ArrayBlockingQueue<IncomingMessage> mMIDISongDisplayInQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    public static ArrayBlockingQueue<IncomingMessage> mMIDISongListInQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    public static byte[] mMidiBankMSBs=new byte[16];
    public static byte[] mMidiBankLSBs=new byte[16];

    private static USBInTask mMidiUsbInTask = null;
    private static USBOutTask mMidiUsbOutTask = new USBOutTask();
    private static InTask mMidiInTask = new InTask();
    private static SongDisplayInTask mMidiSongDisplayInTask = new SongDisplayInTask();
    private static Thread mMidiUsbInTaskThread = null;
    private static Thread mMidiUsbOutTaskThread = new Thread(mMidiUsbOutTask);
    private static Thread mMidiInTaskThread = new Thread(mMidiInTask);
    private static Thread mMidiSongDisplayInTaskThread = new Thread(mMidiSongDisplayInTask);

    private static final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                attemptUsbMidiConnection();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                mMidiUsbOutTask.setConnection(null, null);
                Task.stopTask(mMidiUsbInTask, mMidiUsbInTaskThread);
                mMidiUsbInTask = null;
                mMidiUsbInTaskThread = null;
            }
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            UsbInterface midiInterface = getDeviceMidiInterface(device);
                            if (midiInterface != null) {
                                UsbDeviceConnection conn = mUsbManager.openDevice(device);
                                if (conn != null) {
                                    if (conn.claimInterface(midiInterface, true)) {
                                        int endpointCount = midiInterface.getEndpointCount();
                                        for (int f = 0; f < endpointCount; ++f) {
                                            UsbEndpoint endPoint = midiInterface.getEndpoint(f);
                                            if (endPoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                                                mMidiUsbOutTask.setConnection(conn, endPoint);
                                            } else if (endPoint.getDirection() == UsbConstants.USB_DIR_IN) {
                                                if (mMidiUsbInTask == null) {
                                                    mMidiUsbInTask = new USBInTask(conn, endPoint, getIncomingMIDIChannelsPref());
                                                    (mMidiUsbInTaskThread = new Thread(mMidiUsbInTask)).start();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    private static UsbInterface getDeviceMidiInterface(UsbDevice device) {
        int interfacecount = device.getInterfaceCount();
        UsbInterface fallbackInterface = null;
        for (int h = 0; h < interfacecount; ++h) {
            UsbInterface face = device.getInterface(h);
            int mainclass = face.getInterfaceClass();
            int subclass = face.getInterfaceSubclass();
            // Oh you f***in beauty, we've got a perfect compliant MIDI interface!
            if ((mainclass == 1) && (subclass == 3))
                return face;
                // Aw bollocks, we've got some vendor-specific pish.
                // Still worth trying.
            else if ((mainclass == 255) && (fallbackInterface == null)) {
                // Basically, go with this if:
                // It has all endpoints of type "bulk transfer"
                // and
                // The endpoints have a max packet size that is a mult of 4.
                int endPointCount = face.getEndpointCount();
                boolean allEndpointsCheckout = true;
                for (int g = 0; g < endPointCount; ++g) {
                    UsbEndpoint ep = face.getEndpoint(g);
                    int maxPacket = ep.getMaxPacketSize();
                    int type = ep.getType();
                    allEndpointsCheckout &= ((type == USB_ENDPOINT_XFER_BULK) && ((maxPacket & 3) == 0));
                }
                if (allEndpointsCheckout)
                    fallbackInterface = face;
            }
        }
        return fallbackInterface;
    }

    private static void attemptUsbMidiConnection()
    {
        HashMap<String, UsbDevice> list = mUsbManager.getDeviceList();
        if ((list != null) && (list.size() > 0)) {
            Object[] devObjs = list.values().toArray();
            for (Object devObj : devObjs) {
                UsbDevice dev = (UsbDevice) devObj;
                if (getDeviceMidiInterface(dev) != null) {
                    mUsbManager.requestPermission(dev, mPermissionIntent);
                    break;
                }
            }
        }
    }

    public static void initialise(BeatPrompterApplication application)
    {
        mMidiInTaskThread.start();
        Task.resumeTask(mMidiInTask);
        mMidiUsbOutTaskThread.start();
        Task.resumeTask(mMidiUsbOutTask);

        mUsbManager = (UsbManager) application.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(application, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        application.registerReceiver(mUsbReceiver, filter);
        mMidiUsbRegistered = true;

        attemptUsbMidiConnection();

        PreferenceManager.getDefaultSharedPreferences(application).registerOnSharedPreferenceChangeListener(mPrefListener);
    }

    public static void shutdown(BeatPrompterApplication app)
    {
        Task.stopTask(mMidiInTask,mMidiInTaskThread);
        Task.stopTask(mMidiSongDisplayInTask,mMidiSongDisplayInTaskThread);
        Task.stopTask(mMidiUsbInTask,mMidiUsbInTaskThread);
        Task.stopTask(mMidiUsbOutTask,mMidiUsbOutTaskThread);
        if(mMidiUsbRegistered)
            app.unregisterReceiver(mUsbReceiver);
    }

    private static int getIncomingMIDIChannelsPref() {
        SharedPreferences sharedPrefs = BeatPrompterApplication.getPreferences();
        return sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_midiIncomingChannels_key), 65535);
    }

    private static void setIncomingMIDIChannels()
    {
        if(mMidiUsbInTask!=null)
            mMidiUsbInTask.setIncomingChannels(getIncomingMIDIChannelsPref());
    }

    private static SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
            (prefs, key) -> {
                if(key.equals(BeatPrompterApplication.getResourceString(R.string.pref_midiIncomingChannels_key)))
                {
                    setIncomingMIDIChannels();
                }
            };

    public static void pauseDisplayInTask()
    {
        Task.pauseTask(mMidiSongDisplayInTask,mMidiSongDisplayInTaskThread);
    }

    public static void resumeDisplayInTask()
    {
        Task.resumeTask(mMidiSongDisplayInTask);
    }
}
