package com.example.chenwei.androidthingscamerademo

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.TextureView
import com.am9.commlib.MISCALEConnectUtil
import com.am9.commlib.MISCALEConnectUtil.convertToWeight
import com.am9.commlib.MISCALEConnectUtil.getWeightState
import com.am9.commlib.OpenBlueTask
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.example.chenwei.androidthingscamerademo.Utils.sendMessage
import kotlinx.android.synthetic.main.activity_camera_preview.*
import android.widget.Button as WidgetButton


class CameraPreviewActivity : Activity() {

    private lateinit var mBackgroundHandler: Handler
    private lateinit var mBackgroundThread: HandlerThread

    lateinit var mMessageHandler: Handler
    lateinit var mCaptureThread: CaptureThread
    private var continua: Boolean = false
    //    private var frameCount: Int = 0
//    private var t_start = System.currentTimeMillis()
    lateinit var pd: ProgressDialog

    private lateinit var mCamera: DemoCamera
//    private val mCameraOpenCloseLock: Semaphore = Semaphore(1)


    //    private lateinit var bitmap: Bitmap
    private lateinit var wshelper: WebSocketHelper

//    private var unknowCount: Int = 0
    //1->MTCNN&FACENET , 2->QRCODE ,3->face reg


    override fun onCreate(savedInstanceState: Bundle?) {

        pd = ProgressDialog(this@CameraPreviewActivity)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        tv_mode.setText(R.string.mode_idle)
        tv_weight.text = "..."

        BleManager.getInstance().init(application)                  //  start bluetooth service
        BleManager.getInstance().enableBluetooth()

        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000).operateTimeout = 5000

        /* Initialize a handler to handle message */
        mMessageHandler = MessageHandler(this)

        wshelper = WebSocketHelper("ws://192.168.164.196:8011", mMessageHandler)

        wshelper.initWs()                                          //   start webSocket service


    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        startBackgroundThread()                                    //   start background camera preview thread
        mCamera = DemoCamera(mBackgroundHandler, texture)

        if (texture.isAvailable) {
            Log.d(TAG, "texture.isAvailable? ${texture.isAvailable}")
            startCameraPreview(texture.width, texture.height)

        } else {
            texture.surfaceTextureListener = mSurfaceTextureListener
        }
        // mCaptureThread = CaptureThread(baseContext, mMessageHandler, texture, wshelper)

    }

    override fun onPostResume() {
        super.onPostResume()

        Log.d(TAG, "onPostResume")

        val runa = Runnable {
            Log.d(TAG, "RUN=======")
            MISCALEConnectUtil.setScanRule("MI_SCALE", false)
            startScan()
        }
        //addText(txt, "OpenBlueTask")

        val dTask = OpenBlueTask(this@CameraPreviewActivity, runa)
        dTask.execute(20)

    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        stopBackgroundThread()
        Log.d(TAG, "stopBackgroundThread")

        super.onPause()
    }

    private fun startScan() {
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                //addText(tv_msg, "scan.onScanStarted")
            }

            override fun onScanning(bleDevice: BleDevice) {
                //addText(tv_msg, "scan.onScanning;name:" + bleDevice.name + "\n" + bleDevice.mac)
                BleManager.getInstance().cancelScan()
                connect(bleDevice)
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {

                //addText(tv_msg, "scan.onScanFinished:" + scanResultList.size)

            }
        })
    }

    /**
     * Connect the specific [bleDevice] and send message to handler to update the UI.
     * Eg: When onStartConnect, set [tv_bt] text as connecting.
     *
     */
    private fun connect(bleDevice: BleDevice) {

        BleManager.getInstance().connect(bleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                //addText(tv_msg, "connect.onStartConnect")
                sendMessage(mMessageHandler, R.id.what_bt_con, bleDevice, R.id.state_start, 0)

            }

            override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                //addText(tv_msg, "connect.onConnectFail")
                //pd.dismiss()
                //Toast.makeText(this@CameraPreviewActivity, getString(R.string.connect_fail), Toast.LENGTH_LONG).show()
                sendMessage(mMessageHandler, R.id.what_bt_con, bleDevice, R.id.state_fail, 0)

            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                //addText(tv_msg, "connect.onConnectSuccess")
                //pd.dismiss()
                val characteristic = MISCALEConnectUtil.getCharacteristic(gatt)
                regNotify(false, bleDevice, characteristic)                                        //  recognise the notify message

                //regAll_Notify(bleDevice,gatt)
                //sendMessage(mMessageHandler, R.id.what_bt_con, bleDevice, R.id.state_succ, 0)
                sendMessage(mMessageHandler, R.id.what_bt_con, bleDevice, R.id.state_progress, 0)

            }

            override fun onDisConnected(isActiveDisConnected: Boolean, bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                //pd.dismiss()
                //addText(tv_msg, "connect.onDisConnected:isActiveDisConnected:$isActiveDisConnected")
                sendMessage(mMessageHandler, R.id.what_bt_con, bleDevice, R.id.state_discon, 0)

                connect(bleDevice)

//                if (isActiveDisConnected) {
//                    Toast.makeText(this@CameraPreviewActivity, getString(R.string.active_disconnected), Toast.LENGTH_LONG).show()
//                } else {
//                    Toast.makeText(this@CameraPreviewActivity, getString(R.string.disconnected), Toast.LENGTH_LONG).show()
//                }

            }
        })
    }

//    }

    /**
     * Identify the notification received from the BLE device, and parse the weight from the transmitted data.
     *
     * With the help of [convertToWeight] function, the weight can be calculated.
     * With the help of [getWeightState] function, the scale state can be obtained.
     *
     */
    internal fun regNotify(isStop: Boolean, bleDevice: BleDevice, characteristic: BluetoothGattCharacteristic) =
            if (!isStop) {
                BleManager.getInstance().notify(
                        bleDevice,
                        characteristic.service.uuid.toString(),
                        characteristic.uuid.toString(),
                        object : BleNotifyCallback() {
                            var isScaleEmpty = true
                            override fun onNotifySuccess() {
                                sendMessage(mMessageHandler, R.id.what_bt_con, bleDevice, R.id.state_succ, 0)

                            }

                            override fun onNotifyFailure(exception: BleException) {
                                //runOnUiThread { addText(tv_msg, exception.toString()) }
                                sendMessage(mMessageHandler, R.id.what_bt_con, bleDevice, R.id.state_funcfail, 0)

                            }

                            override fun onCharacteristicChanged(data: ByteArray) {
                                val weight = convertToWeight(characteristic.value)
                                val state = getWeightState(characteristic.value)

                                when (state) {
                                /*
                                * 02 -> 有负载，非稳定结果，测量中
                                * 22 -> 有负载，稳定结果
                                * 82 -> 离开，此次测量无稳定结果
                                * a2 -> 离开，此次测量有稳定结果
                                * */
                                    0x02 -> {
                                        if (isScaleEmpty) {
                                            sendMessage(mMessageHandler, R.id.what_weight, weight, R.id.state_start, 0)
                                            isScaleEmpty = false
                                        } else {
                                            sendMessage(mMessageHandler, R.id.what_weight, weight, R.id.state_progress, 0)
                                        }
                                    }
                                    0x22 -> {
                                        sendMessage(mMessageHandler, R.id.what_weight, weight, R.id.state_succ, 0)
                                    }
                                    0x82 -> {
                                        sendMessage(mMessageHandler, R.id.what_weight, weight, R.id.state_leave_without_stable_value, 0)
                                    }
                                    0xa2 -> {
                                        isScaleEmpty = true
                                        sendMessage(mMessageHandler, R.id.what_weight, weight, R.id.state_leave, 0)
                                    }
                                }


                            }
                        })
            } else {
                BleManager.getInstance().stopNotify(
                        bleDevice,
                        characteristic.service.uuid.toString(),
                        characteristic.uuid.toString())
                pd.dismiss()

            }

//    }

    /**
     * When [TextureView] is available, start CameraPreview and [mCaptureThread].
     */
    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureSizeChanged, width: $width, height: $height")
            mCamera.configureTransform(this@CameraPreviewActivity, width, height)
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureAvailable, width: $width, height: $height")
            startCameraPreview(width, height)

            initThread(this@CameraPreviewActivity)
            mCaptureThread.start()                                   //  start the capture thread.
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            //Log.d(TAG, "onSurfaceTextureUpdated")
            //var bitmap: Bitmap = texture.getBitmap()
            //Log.d("onSurfaceTextureUpdated",""+bitmap.height)


            //faceDetector(bitmap,2)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            Log.d(TAG, "onSurfaceTextureDestroyed")
            return true
        }
    }

    private fun startCameraPreview(width: Int, height: Int) {
        mCamera.setUpCameraOutputs(this, width, height)
        mCamera.configureTransform(this, width, height)
        mCamera.openCamera(this)
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread.start()
        mBackgroundHandler = object : Handler(mBackgroundThread.looper) {
            override fun handleMessage(msg: Message) {

                Log.d("mBackgroundHandler", "+++>>>" + msg.toString())

            }
        }

//
//        val mHandlerThread: HandlerThread
//        //子线程中的handler
//        val mThreadHandler: Handler
//        //UI线程中的handler
//        val mMainHandler = Handler()
//
//
//        mHandlerThread = HandlerThread("check-message-coming")
//        mHandlerThread.start()
//        mThreadHandler = object : Handler(mHandlerThread.looper) {
//            override fun handleMessage(msg: Message) {
//                //update();//模拟数据更新
//
//                //if (isUpdateInfo)
//                //    mThreadHandler.sendEmptyMessage(MSG_UPDATE_INFO);
//            }
//        }


    }

    private fun stopBackgroundThread() {

        try {
            continua = false
            mBackgroundThread.quitSafely()
            mCaptureThread.join()

            mBackgroundThread.join()
            mCaptureThread.stop()
        } catch (ex: InterruptedException) {
            Log.d("stopBackgroundThread", "InterruptedException")
            ex.printStackTrace()
        }
    }

//    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
//        Log.d(TAG, "Image available now")
//        // Do whatever you want here with the new still picture.
//        /*
//        val image = reader.acquireLatestImage()
//        val imageBuf = image.planes[0].buffer
//        val imageBytes = ByteArray(imageBuf.remaining())
//        imageBuf.get(imageBytes)
//        image.close()
//        Log.d(TAG, "Still image size: ${imageBytes.size}")
//        */
//    }

    /**
     * Initialize the [CaptureThread]
     */
    fun initThread(context: Context) {
        //mCaptureThread.run()
        mCaptureThread = CaptureThread(context, mMessageHandler, texture, wshelper)

    }

    companion object {
        private val TAG = CameraPreviewActivity::class.java.simpleName
    }
}
