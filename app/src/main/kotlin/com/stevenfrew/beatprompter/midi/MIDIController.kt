package com.stevenfrew.beatprompter.midi

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Task
import java.util.concurrent.ArrayBlockingQueue
import android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK

object MIDIController:SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        if (key == BeatPrompterApplication.getResourceString(R.string.pref_midiIncomingChannels_key))
            setIncomingMIDIChannels()
    }

    private var mMidiUsbRegistered = false
    private var mUsbManager: UsbManager? = null
    private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private var mPermissionIntent: PendingIntent? = null

    const val MIDI_TAG = "midi"
    private const val MIDI_QUEUE_SIZE = 1024
    var mMIDIOutQueue = ArrayBlockingQueue<OutgoingMessage>(MIDI_QUEUE_SIZE)
    var mMIDISongDisplayInQueue = ArrayBlockingQueue<IncomingMessage>(MIDI_QUEUE_SIZE)
    var mMIDISongListInQueue = ArrayBlockingQueue<IncomingMessage>(MIDI_QUEUE_SIZE)
    var mMidiBankMSBs = ByteArray(16)
    var mMidiBankLSBs = ByteArray(16)

    private var mMidiUsbInTask: USBInTask? = null
    private val mMidiUsbOutTask = USBOutTask()
    private val mMidiInTask = InTask()
    private val mMidiSongDisplayInTask = SongDisplayInTask()
    private var mMidiUsbInTaskThread: Thread? = null
    private val mMidiUsbOutTaskThread = Thread(mMidiUsbOutTask)
    private val mMidiInTaskThread = Thread(mMidiInTask)
    private val mMidiSongDisplayInTaskThread = Thread(mMidiSongDisplayInTask)

    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                attemptUsbMidiConnection()
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                mMidiUsbOutTask.setConnection(null, null)
                Task.stopTask(mMidiUsbInTask, mMidiUsbInTaskThread)
                mMidiUsbInTask = null
                mMidiUsbInTaskThread = null
            }
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            val midiInterface = getDeviceMidiInterface(device)
                            if (midiInterface != null) {
                                val conn = mUsbManager!!.openDevice(device)
                                if (conn != null) {
                                    if (conn.claimInterface(midiInterface, true)) {
                                        val endpointCount = midiInterface.endpointCount
                                        for (f in 0 until endpointCount) {
                                            val endPoint = midiInterface.getEndpoint(f)
                                            if (endPoint.direction == UsbConstants.USB_DIR_OUT) {
                                                mMidiUsbOutTask.setConnection(conn, endPoint)
                                            } else if (endPoint.direction == UsbConstants.USB_DIR_IN) {
                                                if (mMidiUsbInTask == null) {
                                                    mMidiUsbInTask = USBInTask(conn, endPoint, incomingMIDIChannelsPref)
                                                    val taskThread=Thread(mMidiUsbInTask)
                                                    mMidiUsbInTaskThread =taskThread
                                                    taskThread.start()
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
    }

    private val incomingMIDIChannelsPref: Int
        get() {
            val sharedPrefs = BeatPrompterApplication.preferences
            return sharedPrefs.getInt(BeatPrompterApplication.getResourceString(R.string.pref_midiIncomingChannels_key), 65535)
        }

    private fun getDeviceMidiInterface(device: UsbDevice): UsbInterface? {
        val interfacecount = device.interfaceCount
        var fallbackInterface: UsbInterface? = null
        for (h in 0 until interfacecount) {
            val face = device.getInterface(h)
            val mainclass = face.interfaceClass
            val subclass = face.interfaceSubclass
            // Oh you f***in beauty, we've got a perfect compliant MIDI interface!
            if (mainclass == 1 && subclass == 3)
                return face
            else if (mainclass == 255 && fallbackInterface == null) {
                // Basically, go with this if:
                // It has all endpoints of type "bulk transfer"
                // and
                // The endpoints have a max packet size that is a mult of 4.
                val endPointCount = face.endpointCount
                var allEndpointsCheckout = true
                for (g in 0 until endPointCount) {
                    val ep = face.getEndpoint(g)
                    val maxPacket = ep.maxPacketSize
                    val type = ep.type
                    allEndpointsCheckout = allEndpointsCheckout and (type == USB_ENDPOINT_XFER_BULK && maxPacket and 3 == 0)
                }
                if (allEndpointsCheckout)
                    fallbackInterface = face
            }// Aw bollocks, we've got some vendor-specific pish.
            // Still worth trying.
        }
        return fallbackInterface
    }

    private fun attemptUsbMidiConnection() {
        val list = mUsbManager!!.deviceList
        if (list != null && list.size > 0) {
            val devObjs = list.values
            for (devObj in devObjs) {
                val dev = devObj as UsbDevice
                if (getDeviceMidiInterface(dev) != null) {
                    mUsbManager!!.requestPermission(dev, mPermissionIntent)
                    break
                }
            }
        }
    }

    fun initialise(application: BeatPrompterApplication) {
        mMidiInTaskThread.start()
        Task.resumeTask(mMidiInTask)
        mMidiUsbOutTaskThread.start()
        Task.resumeTask(mMidiUsbOutTask)

        mUsbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
        mPermissionIntent = PendingIntent.getBroadcast(application, 0, Intent(ACTION_USB_PERMISSION), 0)
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        application.registerReceiver(mUsbReceiver, filter)
        mMidiUsbRegistered = true

        attemptUsbMidiConnection()

        BeatPrompterApplication.preferences.registerOnSharedPreferenceChangeListener(this)
    }

    fun shutdown(app: BeatPrompterApplication) {
        Task.stopTask(mMidiInTask, mMidiInTaskThread)
        Task.stopTask(mMidiSongDisplayInTask, mMidiSongDisplayInTaskThread)
        Task.stopTask(mMidiUsbInTask, mMidiUsbInTaskThread)
        Task.stopTask(mMidiUsbOutTask, mMidiUsbOutTaskThread)
        if (mMidiUsbRegistered)
            app.unregisterReceiver(mUsbReceiver)
    }

    private fun setIncomingMIDIChannels() {
        if (mMidiUsbInTask != null)
            mMidiUsbInTask!!.setIncomingChannels(incomingMIDIChannelsPref)
    }

    fun pauseDisplayInTask() {
        Task.pauseTask(mMidiSongDisplayInTask, mMidiSongDisplayInTaskThread)
    }

    fun resumeDisplayInTask() {
        Task.resumeTask(mMidiSongDisplayInTask)
    }
}
