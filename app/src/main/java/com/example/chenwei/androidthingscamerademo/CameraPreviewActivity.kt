package com.example.chenwei.androidthingscamerademo

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.TextView
import com.am9.commlib.MISCALEConnectUtil
import com.am9.commlib.MISCALEConnectUtil.convertToWeight
import com.am9.commlib.MISCALEConnectUtil.getWeightStat
import com.am9.commlib.MyBleNotifyCallback
import com.am9.commlib.OpenBlueTask
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.example.chenwei.androidthingscamerademo.Utils.sendMessage
import com.google.zxing.activity.CaptureActivity
import kotlinx.android.synthetic.main.activity_camera_preview.*
import java.util.concurrent.Semaphore
import android.widget.Button as WidgetButton


class CameraPreviewActivity : Activity() {
    val TAG: String = "CameraPreviewActivity"
    private lateinit var mCameraHandler: Handler
    private lateinit var mCameraThread: HandlerThread

    public lateinit var mCameraHandler2: Handler
    public lateinit var mCameraThread2: CaptureThread
    private var continua: Boolean = false
    private var frameCount: Int = 0
    private var t_start = System.currentTimeMillis()
    public lateinit var pd: ProgressDialog

    private lateinit var mCamera: DemoCamera
    private val mCameraOpenCloseLock: Semaphore = Semaphore(1)

    public lateinit var mTextureView: AutoFitTextureView
    public lateinit var mDraw: SurfaceViewDraw
    public lateinit var mTvMsg: TextView
    public lateinit var mTvPerson: TextView
    public lateinit var mTvMode: TextView
    public lateinit var mTvbtCon: TextView

    public lateinit var mTvWsCon: TextView

    public lateinit var mTvHint: TextView


    private lateinit var bitmap: Bitmap
    private lateinit var wshelper: WebSocketHelper
    lateinit var mtcnn: MTCNN
    private var unknowCount: Int = 0
    //1->MTCNN&FACENET , 2->QRCODE ,3->face reg


    override fun onCreate(savedInstanceState: Bundle?) {

        pd = ProgressDialog(this@CameraPreviewActivity)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        mTextureView = findViewById(R.id.texture)
        mDraw = findViewById(R.id.draw)
        mTvMsg = findViewById(R.id.tv_msg)
        mTvPerson = findViewById(R.id.tv_person)
        mTvMode = findViewById(R.id.tv_mode)
        mTvbtCon = findViewById(R.id.tv_bt)

        mTvWsCon = findViewById(R.id.tv_ws)

        mTvHint = findViewById(R.id.tv_hint)


        mTvMsg.setOnClickListener {
            val intent = Intent(it.context, CaptureActivity::class.java)
            startActivity(intent)
        }
        //mtcnn = MTCNN(assets)

        tv_mode.setText(R.string.mode_idle)
        tv_weight.text = "..."

        BleManager.getInstance().init(application)
        BleManager.getInstance().enableBluetooth()

        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000).operateTimeout = 5000

        mCameraHandler2 = CamHandler(this)

        wshelper = WebSocketHelper("ws://192.168.164.196:8011", mCameraHandler2)

        wshelper.initWs()


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

    private fun startScan() {
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                //addText(mTvMsg, "scan.onScanStarted")
            }

            override fun onScanning(bleDevice: BleDevice) {
                //addText(mTvMsg, "scan.onScanning;name:" + bleDevice.name + "\n" + bleDevice.mac)
                BleManager.getInstance().cancelScan()
                connect(bleDevice)
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {

                //addText(mTvMsg, "scan.onScanFinished:" + scanResultList.size)

            }
        })
    }

    private fun connect(bleDevice: BleDevice) {

        BleManager.getInstance().connect(bleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                //addText(mTvMsg, "connect.onStartConnect")
                sendMessage(mCameraHandler2, R.id.what_bt_con, bleDevice, R.id.state_start, 0)

            }

            override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                //addText(mTvMsg, "connect.onConnectFail")
                //pd.dismiss()
                //Toast.makeText(this@CameraPreviewActivity, getString(R.string.connect_fail), Toast.LENGTH_LONG).show()
                sendMessage(mCameraHandler2, R.id.what_bt_con, bleDevice, R.id.state_fail, 0)

            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                //addText(mTvMsg, "connect.onConnectSuccess")
                //pd.dismiss()
                val characteristic = MISCALEConnectUtil.getCharacteristic(gatt)
                regNotify(false, bleDevice, characteristic)

                //regAll_Notify(bleDevice,gatt)
                //sendMessage(mCameraHandler2, R.id.what_bt_con, bleDevice, R.id.state_succ, 0)
                sendMessage(mCameraHandler2, R.id.what_bt_con, bleDevice, R.id.state_progress, 0)

            }

            override fun onDisConnected(isActiveDisConnected: Boolean, bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                //pd.dismiss()
                //addText(mTvMsg, "connect.onDisConnected:isActiveDisConnected:$isActiveDisConnected")
                sendMessage(mCameraHandler2, R.id.what_bt_con, bleDevice, R.id.state_discon, 0)

                connect(bleDevice)

//                if (isActiveDisConnected) {
//                    Toast.makeText(this@CameraPreviewActivity, getString(R.string.active_disconnected), Toast.LENGTH_LONG).show()
//                } else {
//                    Toast.makeText(this@CameraPreviewActivity, getString(R.string.disconnected), Toast.LENGTH_LONG).show()
//                }

            }
        })
    }

    fun regAll_Notify(bleDevice: BleDevice, bluetoothGatt: BluetoothGatt) {
        for (service in bluetoothGatt.services) {
            for (characteristic in service.characteristics) {

                val charaProp = characteristic.properties

                if (charaProp and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    //@todo Notify

                    Log.d("TAG", service.uuid.toString() + " >> " + characteristic.uuid.toString())
                    BleManager.getInstance().notify(
                            bleDevice,
                            service.uuid.toString(),
                            characteristic.uuid.toString(), MyBleNotifyCallback(characteristic))

                }

            }
        }

    }

    internal fun regNotify(isStop: Boolean, bleDevice: BleDevice, characteristic: BluetoothGattCharacteristic) =
            if (!isStop) {
                BleManager.getInstance().notify(
                        bleDevice,
                        characteristic.service.uuid.toString(),
                        characteristic.uuid.toString(),
                        object : BleNotifyCallback() {
                            var isScaleEmpty = true
                            override fun onNotifySuccess() {
                                sendMessage(mCameraHandler2, R.id.what_bt_con, bleDevice, R.id.state_succ, 0)

                            }

                            override fun onNotifyFailure(exception: BleException) {
                                //runOnUiThread { addText(tv_msg, exception.toString()) }
                                sendMessage(mCameraHandler2, R.id.what_bt_con, bleDevice, R.id.state_funcfail, 0)

                            }

                            override fun onCharacteristicChanged(data: ByteArray) {
                                val weight = convertToWeight(characteristic.value)
                                val state = getWeightStat(characteristic.value)

                                when (state) {
                                /*
                                * 02 -> 有负载，非稳定结果，测量中
                                * 22 -> 有负载，稳定结果
                                * 82 -> 离开，此次测量无稳定结果
                                * a2 -> 离开，此次测量有稳定结果
                                * */
                                    0x02 -> {
                                        if (isScaleEmpty) {
                                            sendMessage(mCameraHandler2, R.id.what_weight, weight, R.id.state_start, 0)
                                            isScaleEmpty = false
                                        } else {
                                            sendMessage(mCameraHandler2, R.id.what_weight, weight, R.id.state_progress, 0)
                                        }
                                    }
                                    0x22 -> {
                                        sendMessage(mCameraHandler2, R.id.what_weight, weight, R.id.state_succ, 0)
                                    }
                                    0x82 -> {
                                        sendMessage(mCameraHandler2, R.id.what_weight, weight, R.id.state_leave_without_stable_value, 0)
                                    }
                                    0xa2 -> {
                                        isScaleEmpty = true
                                        sendMessage(mCameraHandler2, R.id.what_weight, weight, R.id.state_leave, 0)
                                    }
                                }


                                //addText(tv_msg, weight.toString())
                                //pd.dismiss();

//                                if (weight > 0) {
//                                    runOnUiThread {
//                                        Toast.makeText(this@CameraPreviewActivity, weight.toString(), Toast.LENGTH_LONG).show()
//                                        pd.dismiss();
//
//                                    }
//                                }


                            }
                        })
            } else {
                BleManager.getInstance().stopNotify(
                        bleDevice,
                        characteristic.service.uuid.toString(),
                        characteristic.uuid.toString())
                pd.dismiss()

            }

    internal fun addText(txt: TextView, text: String) {
        txt.append(text + "\n")

        // pd.setMessage(text)
        // pd.show()
        Log.d(TAG, text)


    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        startBackgroundThread()
        mCamera = DemoCamera(mOnImageAvailableListener, mCameraHandler, mTextureView)

        if (mTextureView.isAvailable) {
            startCameraPreview(mTextureView.width, mTextureView.height)

        } else {
            mTextureView.surfaceTextureListener = mSurfaceTextureListener
        }
        // mCameraThread2 = CaptureThread(baseContext, mCameraHandler2, mTextureView, wshelper)

    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        stopBackgroundThread()
        Log.d(TAG, "stopBackgroundThread")

        super.onPause()
    }

    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureSizeChanged, width: $width, height: $height")
            mCamera.configureTransform(this@CameraPreviewActivity, width, height)
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureAvailable, width: $width, height: $height")
            startCameraPreview(width, height)

            initThread(this@CameraPreviewActivity)
            mCameraThread2.start()
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            //Log.d(TAG, "onSurfaceTextureUpdated")
            //var bitmap: Bitmap = mTextureView.getBitmap()
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
        mCameraThread = HandlerThread("CameraBackground")
        mCameraThread.start()
        mCameraHandler = object : Handler(mCameraThread.looper) {
            override fun handleMessage(msg: Message) {

                Log.d("mCameraHandler", "+++>>>" + msg.toString())

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
            mCameraThread.quitSafely()
            mCameraThread2.join()

            mCameraThread.join()
            mCameraThread2.stop();
        } catch (ex: InterruptedException) {
            Log.d("stopBackgroundThread", "InterruptedException")
            ex.printStackTrace()
        }
    }

    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        Log.d(TAG, "Image available now")
        // Do whatever you want here with the new still picture.
        /*
        val image = reader.acquireLatestImage()
        val imageBuf = image.planes[0].buffer
        val imageBytes = ByteArray(imageBuf.remaining())
        imageBuf.get(imageBytes)
        image.close()
        Log.d(TAG, "Still image size: ${imageBytes.size}")
        */
    }

    /**
     * Initialize the {@code CaptureThread}
     */
    fun initThread(context: Context) {
        //mCameraThread2.run()
        mCameraThread2 = CaptureThread(context, mCameraHandler2, mTextureView, wshelper)

    }
}
