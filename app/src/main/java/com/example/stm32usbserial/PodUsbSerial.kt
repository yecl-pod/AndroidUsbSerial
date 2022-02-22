package com.example.stm32usbserial

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

class PodUsbSerialService: Service() {
    var isConnected: Boolean = false
    var mRxMsg: String? = null
    var mDevName: String? = null
    var mDevVendorId: Int = 0
    var mDevProductId: Int = 0
    companion object {
        val ACTION_USB_MSGRECEIVED: String = "actionUsbMsgReceived"
        val ACTION_USB_CONNECTED: String = "actionUsbConnected"
    }

    /*! usb */
    lateinit var mUsbManager: UsbManager

    private var mDevice: UsbDevice? = null
    private var mSerial: UsbSerialDevice? = null // from felhr library
    private var mConnection: UsbDeviceConnection? = null
    private val ACTION_USB_PERMISSION = "permission"

    override fun onCreate() {
        isConnected = false
        setFilter()
        mUsbManager = getSystemService(USB_SERVICE) as UsbManager
    }

    override fun onBind(p0: Intent?): IBinder? {

        return UsbBinder()
    }

    fun usbStartConnection() {
        val usbDevices: HashMap<String, UsbDevice>? = mUsbManager.deviceList
        if (!isConnected && !usbDevices?.isEmpty()!!) {
            usbDevices.forEach() { entry ->
                mDevice = entry.value

                val intent: PendingIntent =
                    PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
                mUsbManager.requestPermission(mDevice, intent)

                return
            }
        } else
            Log.i("Serial", "No usb device or has connection")
    }

    fun usbSendData(data: String) {
        mSerial?.write(data.toByteArray())
        Log.i("Serial", "Send data: " + data.toByteArray())
    }

    inner class UsbBinder : Binder() {
        fun getService() = this@PodUsbSerialService
    }

    private fun setFilter() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(mUsbBroadcastReceiver, filter)
    }

    private val usbReceiveCallback: UsbSerialInterface.UsbReadCallback =
        UsbSerialInterface.UsbReadCallback { data ->
            try {
                mRxMsg = String(data!!)
                this.sendBroadcast(Intent(ACTION_USB_MSGRECEIVED))
            } catch (e: Exception) {
                Log.i("Serial", "Error in receiving message")
            }
        }

    fun usbEndConnection() {
        isConnected = false
        mSerial?.close()
    }

    private val mUsbBroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                ACTION_USB_PERMISSION -> {
                    val granted: Boolean =
                        p1.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    // p1.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)

                    if (granted) {
                        isConnected = true
                        mDevice = p1.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        mConnection = mUsbManager.openDevice(mDevice)
                        mSerial = UsbSerialDevice.createUsbSerialDevice(mDevice, mConnection)

                        mDevName = mDevice?.productName!!
                        mDevVendorId = mDevice?.vendorId!!
                        mDevProductId = mDevice?.productId!!
                        p0?.sendBroadcast(Intent(ACTION_USB_CONNECTED))
                        if (mSerial!!.open()) {
                            Log.i("Serial", "port open [SUCCESS]")
                            mSerial!!.setBaudRate(115200)
                            mSerial!!.setDataBits(UsbSerialDevice.DATA_BITS_8)
                            mSerial!!.setStopBits(UsbSerialDevice.STOP_BITS_1)
                            mSerial!!.setParity(UsbSerialDevice.PARITY_NONE)
                            mSerial!!.setFlowControl(UsbSerialDevice.FLOW_CONTROL_OFF)
                            mSerial!!.read(usbReceiveCallback)
                        } else {
                            Log.i("Serial", "port open [FAILED]")
                        }
                    } else {
                        Log.i("Serial", "permission not granted")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    usbStartConnection()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    usbEndConnection()
                }
            }
        }
    }
}