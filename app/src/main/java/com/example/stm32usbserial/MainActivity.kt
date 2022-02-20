package com.example.stm32usbserial

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var mTvDevName: TextView? = null
    private var mTvDevVendorId: TextView? = null
    private var mTvDevProductId: TextView? = null
    private var mTvRxMsg: TextView? = null
    private var mEtTxMsg: EditText? = null
    private var mBtnCnt: Button? = null
    private var mBtnSend: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mTvDevName = findViewById(R.id.tv_devName)
        mTvDevVendorId = findViewById(R.id.tv_devVendorId)
        mTvDevProductId = findViewById(R.id.tv_devProductId)
        mTvRxMsg = findViewById(R.id.tv_rxMsg)
        mEtTxMsg = findViewById(R.id.et_txMsg)
        mBtnCnt = findViewById(R.id.btn_cnt)
        mBtnSend = findViewById(R.id.btn_send)

        // set click listener
        mBtnCnt?.setOnClickListener(this)
        mBtnSend?.setOnClickListener(this)

        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbBroadcastReceiver, filter)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btn_cnt -> {
                usbStartConnection()
            }
            R.id.btn_send -> {
                usbSendData(mEtTxMsg?.text.toString())
            }
        }
    }

    /*! usb */
    lateinit var mUsbManager: UsbManager
    private var isConnected: Boolean = false
    private var mDevice: UsbDevice? = null
    private var mSerial: UsbSerialDevice? = null // from felhr library
    private var mConnection: UsbDeviceConnection? = null
    private val ACTION_USB_PERMISSION = "permission"
    private fun usbStartConnection() {
        val usbDevices: HashMap<String, UsbDevice>? = mUsbManager.deviceList
        if (!isConnected && !usbDevices?.isEmpty()!!) {
            usbDevices.forEach() { entry ->
                mDevice = entry.value

                val intent: PendingIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
                mUsbManager.requestPermission(mDevice, intent)

                return
            }
        } else
            Log.i("Serial", "No usb device or has connection")
    }

    private fun usbSendData(data: String) {
        mSerial?.write(data.toByteArray())
        Log.i("Serial", "Send data: " + data.toByteArray())
    }

    private val usbReceiveCallback: UsbSerialInterface.UsbReadCallback = object: UsbSerialInterface.UsbReadCallback {
        override fun onReceivedData(data: ByteArray?) {
            try {
                var s: String = String(data!!)
                mTvRxMsg?.text = s
            } catch (e: Exception) {
                Log.i("Serial", "Error in receiving message")
            }
        }
    }

    private fun usbEndConnection() {
        isConnected = false
        mSerial?.close()
    }

    private val usbBroadcastReceiver = object: BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                ACTION_USB_PERMISSION -> {
                    val granted: Boolean = p1.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    // p1.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)

                    if (granted) {
                        isConnected = true
                        mDevice = p1.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        mConnection = mUsbManager.openDevice(mDevice)
                        mSerial = UsbSerialDevice.createUsbSerialDevice(mDevice, mConnection)


                        mTvDevName?.text = getString(R.string.str_devName) + mDevice?.productName
                        mTvDevVendorId?.text = getString(R.string.str_devVendorId) + mDevice?.vendorId.toString()
                        mTvDevProductId?.text = getString(R.string.str_devProductId) + mDevice?.productId.toString()

                        if (mSerial!!.open()) {
                            Toast.makeText(this@MainActivity, "Device connected", Toast.LENGTH_SHORT).show()
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