package com.stevenfrew.beatprompter.comm.midi

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.Task
import java.util.concurrent.ArrayBlockingQueue
import android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Build
import android.support.annotation.RequiresApi
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.OutgoingMessage

object MIDIController {
    private var mMidiUsbRegistered = false
    private var mUsbManager: UsbManager? = null
    private var mNativeMidiManager:MidiManager?=null
    private var mMidiNativeDeviceListener:MidiNativeDeviceListener?=null

    private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private var mPermissionIntent: PendingIntent? = null

    const val MIDI_TAG = "midi"
    private const val MIDI_QUEUE_SIZE = 1024
    var mMidiBankMSBs = ByteArray(16)
    var mMidiBankLSBs = ByteArray(16)

    var mMIDIOutQueue = ArrayBlockingQueue<OutgoingMessage>(MIDI_QUEUE_SIZE)
    private val mSenderTask= SenderTask(mMIDIOutQueue)
    private val mReceiverTask= ReceiverTask()

    private val mSenderTaskThread=Thread(mSenderTask)
    private val mReceiverTaskThread=Thread(mReceiverTask)

    private fun addNativeDevice(nativeDeviceInfo:MidiDeviceInfo)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mNativeMidiManager!=null)
            nativeDeviceInfo.ports.forEach {
                mNativeMidiManager!!.openDevice(nativeDeviceInfo,mMidiNativeDeviceListener,null)
            }
    }

    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                attemptUsbMidiConnection()
            }
            else if (ACTION_USB_PERMISSION == action) {
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
                                            if (endPoint.direction == UsbConstants.USB_DIR_OUT)
                                                mSenderTask.addSender(UsbSender(conn,endPoint,device.deviceName))
                                            else if (endPoint.direction == UsbConstants.USB_DIR_IN)
                                                mReceiverTask.addReceiver(UsbReceiver(conn,endPoint,device.deviceName))
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

    private fun getDeviceMidiInterface(device: UsbDevice): UsbInterface? {
        val interfaceCount = device.interfaceCount
        var fallbackInterface: UsbInterface? = null
        for (h in 0 until interfaceCount) {
            val face = device.getInterface(h)
            val mainClass = face.interfaceClass
            val subclass = face.interfaceSubclass
            // Oh you f***in beauty, we've got a perfect compliant MIDI interface!
            if (mainClass == 1 && subclass == 3)
                return face
            else if (mainClass == 255 && fallbackInterface == null) {
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
                    allEndpointsCheckout = allEndpointsCheckout and (type == USB_ENDPOINT_XFER_BULK && (maxPacket and 3) == 0)
                }
                if (allEndpointsCheckout)
                    fallbackInterface = face
            }
            // Aw bollocks, we've got some vendor-specific pish.
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
        mSenderTaskThread.start()
        Task.resumeTask(mSenderTask)
        mReceiverTaskThread.start()
        Task.resumeTask(mReceiverTask)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            mMidiNativeDeviceListener=MidiNativeDeviceListener()
            mNativeMidiManager = application.getSystemService(Context.MIDI_SERVICE) as MidiManager
            mNativeMidiManager?.devices?.forEach { addNativeDevice(it) }
            mNativeMidiManager?.registerDeviceCallback(mMidiNativeDeviceListener, null)
        }
        mUsbManager = application.getSystemService(Context.USB_SERVICE) as UsbManager
        mPermissionIntent = PendingIntent.getBroadcast(application, 0, Intent(ACTION_USB_PERMISSION), 0)

        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        application.registerReceiver(mUsbReceiver, filter)
        mMidiUsbRegistered = true

        attemptUsbMidiConnection()
    }

    fun shutdown(app: BeatPrompterApplication) {
        if (mMidiUsbRegistered)
            app.unregisterReceiver(mUsbReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mNativeMidiManager?.unregisterDeviceCallback(mMidiNativeDeviceListener)

        Task.stopTask(mSenderTask, mSenderTaskThread)
        Task.stopTask(mReceiverTask, mReceiverTaskThread)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    class MidiNativeDeviceListener:MidiManager.OnDeviceOpenedListener,MidiManager.DeviceCallback()
    {
        override fun onDeviceAdded(deviceInfo: MidiDeviceInfo) {
            addNativeDevice(deviceInfo)
        }

        override fun onDeviceRemoved(deviceInfo: MidiDeviceInfo) {
            // Don't care. Relevant sender/receiver will throw an exception and be removed.
        }

        override fun onDeviceOpened(openedDevice: MidiDevice?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                try {
                    if(openedDevice!=null) {
                        val deviceName=""+openedDevice.info.properties[MidiDeviceInfo.PROPERTY_NAME]
                        openedDevice.info?.ports?.forEach {
                            if (it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT)
                                mReceiverTask.addReceiver(NativeReceiver(openedDevice.openOutputPort(it.portNumber),deviceName))
                            else if (it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT)
                                mSenderTask.addSender(NativeSender(openedDevice.openInputPort(it.portNumber),deviceName))
                        }
                        EventHandler.sendEventToSongList(EventHandler.CONNECTION_ADDED, deviceName)
                    }
                }
                catch(ioException:Exception)
                {
                    // Obviously not for us.
                }
        }
    }
}
