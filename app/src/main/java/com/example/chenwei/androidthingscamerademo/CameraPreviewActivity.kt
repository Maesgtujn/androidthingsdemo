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
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.example.chenwei.androidthingscamerademo.Utils.sendMessage
import kotlinx.android.synthetic.main.activity_camera_preview.*
import okhttp3.*

import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

import java.util.*
import java.util.concurrent.TimeUnit
import android.widget.Button as WidgetButton


class CameraPreviewActivity : Activity() {

    private lateinit var mBackgroundHandler: Handler
    private lateinit var mBackgroundThread: HandlerThread

    private lateinit var messageHandler: Handler

    lateinit var captureThread: CaptureThread

    private lateinit var mCamera: DemoCamera

    private lateinit var wsHelper: WebSocketHelper

//    private lateinit var mPeripheralHelper: PeripheralHelper

    private var client: OkHttpClient? = null

//    private var continua: Boolean = false
    lateinit var pd: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        pd = ProgressDialog(this@CameraPreviewActivity)

        setContentView(R.layout.activity_camera_preview)

        client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS).build()

        BleManager.getInstance().init(application)                  //  start bluetooth service

        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000).operateTimeout = 5000

        BleManager.getInstance().enableBluetooth()



        /* Initialize a handler to handle message */
        messageHandler = MessageHandler(this)

        wsHelper = WebSocketHelper("ws://app.mxic.com.cn:8011", messageHandler)

        wsHelper.initWs()                                          //   start webSocket service

//        mPeripheralHelper = PeripheralHelper(this)

    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        startBackgroundThread()                                    //   start background camera preview thread
        mCamera = DemoCamera(mBackgroundHandler, textureView)

        if (textureView.isAvailable) {
            Log.d(TAG, "texture.isAvailable? ${textureView.isAvailable}")
            startCameraPreview(textureView.width, textureView.height)

        } else {
            textureView.surfaceTextureListener = mSurfaceTextureListener
        }
        // captureThread = CaptureThread(baseContext, messageHandler, texture, wsHelper)

    }

    override fun onPostResume() {
        super.onPostResume()

        Log.d(TAG, "onPostResume")

        val runnable = Runnable {
            Log.d(TAG, "RUN=======")
            MISCALEConnectUtil.setScanRule("MI_SCALE", false)
            startScan()
        }
        //addText(txt, "OpenBlueTask")
        runnable.run()
//        val dTask = OpenBlueTask(this@CameraPreviewActivity, runnable)
//        dTask.execute(20)

    }

    override fun onPause() {
        Log.d(TAG, "onPause")
//        stopBackgroundThread()
        Log.d(TAG, "stopBackgroundThread")

        super.onPause()
    }

    private fun startScan() {
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
            }

            override fun onScanning(bleDevice: BleDevice) {
                BleManager.getInstance().cancelScan()
                connect(bleDevice)
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {
//                Log.d(TAG, "==remain_count$remain_count")
//                if(scanResultList.isEmpty() && remain_count>0){
//                    startScan(remain_count-1)
//                }
                if (scanResultList.isEmpty()){
                    sendMessage(messageHandler, R.id.what_bt_con, null, R.id.state_funcfail, 0)
                }

            }

        })
    }

    /**
     * Connect the specific [bleDevice] and send message to handler to update the UI.
     * Eg: When onStartConnect, set [tv_bt] text as connecting.
     *
     */
    private fun connect(bleDevice: BleDevice) {
        Log.d(TAG, "BLE connect")
        BleManager.getInstance().connect(bleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                Log.d(TAG, "BLE onStartConnect")
                //addText(tv_msg, "connect.onStartConnect")
                sendMessage(messageHandler, R.id.what_bt_con, bleDevice, R.id.state_start, 0)

            }

            override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                //addText(tv_msg, "connect.onConnectFail")
                //pd.dismiss()
                //Toast.makeText(this@CameraPreviewActivity, getString(R.string.connect_fail), Toast.LENGTH_LONG).show()
                sendMessage(messageHandler, R.id.what_bt_con, bleDevice, R.id.state_fail, 0)

            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                //addText(tv_msg, "connect.onConnectSuccess")
                //pd.dismiss()
                val characteristic = MISCALEConnectUtil.getCharacteristic(gatt)
                regNotify(false, bleDevice, characteristic)                                        //  recognise the notify message

                //regAll_Notify(bleDevice,gatt)
                //sendMessage(messageHandler, R.id.what_bt_con, bleDevice, R.id.state_succ, 0)
                sendMessage(messageHandler, R.id.what_bt_con, bleDevice, R.id.state_progress, 0)

            }

            override fun onDisConnected(isActiveDisConnected: Boolean, bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                //pd.dismiss()
                //addText(tv_msg, "connect.onDisConnected:isActiveDisConnected:$isActiveDisConnected")
                sendMessage(messageHandler, R.id.what_bt_con, bleDevice, R.id.state_discon, 0)

                connect(bleDevice)


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
                                sendMessage(messageHandler, R.id.what_bt_con, bleDevice, R.id.state_succ, 0)

                            }

                            override fun onNotifyFailure(exception: BleException) {
                                //runOnUiThread { addText(tv_msg, exception.toString()) }
                                sendMessage(messageHandler, R.id.what_bt_con, bleDevice, R.id.state_funcfail, 0)

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
                                            sendMessage(messageHandler, R.id.what_weight, weight, R.id.state_start, 0)
                                            isScaleEmpty = false
                                        } else {
                                            sendMessage(messageHandler, R.id.what_weight, weight, R.id.state_progress, 0)
                                        }
                                    }
                                    0x22 -> {
                                        sendMessage(messageHandler, R.id.what_weight, weight, R.id.state_succ, 0)
                                    }
                                    0x82 -> {
                                        sendMessage(messageHandler, R.id.what_weight, weight, R.id.state_leave_without_stable_value, 0)
                                    }
                                    0xa2 -> {
                                        isScaleEmpty = true
                                        sendMessage(messageHandler, R.id.what_weight, weight, R.id.state_leave, 0)
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

    /**
     * When [TextureView] is available, start CameraPreview and [captureThread].
     */
    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureSizeChanged, width: $width, height: $height")
            mCamera.configureTransform(this@CameraPreviewActivity, width, height)
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureAvailable, width: $width, height: $height")
            startCameraPreview(width, height)

            /* Initialize the capture thread(face recognition and scan QR code) */

            initThread(this@CameraPreviewActivity)

        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            Log.d(TAG, "onSurfaceTextureDestroyed")
            return true
        }
    }

    /**
     * Initialize the [CaptureThread]
     */
    fun initThread(context: Context) {

        captureThread = CaptureThread(context, messageHandler, textureView, wsHelper)
        captureThread.start()
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

    }

//    private fun stopBackgroundThread() {
//
//        try {
//            continua = false
//            mBackgroundThread.quitSafely()
//            captureThread.join()
//
//            mBackgroundThread.join()
//            captureThread.stop()
//        } catch (ex: InterruptedException) {
//            Log.d("stopBackgroundThread", "InterruptedException")
//            ex.printStackTrace()
//        }
//    }



    /**
     * Post [weight] and [employeeNo] to the server through webSocket.
     * Receiving parameters :
     *      type                    figure
     *      employeeNo
     *      date    (insert time)
     *      weight
     *
     * Return parameters:
     *      fail0   (no such employee number)
     *      fail1   (no employee name corresponds to the employee number)
     *      succ    (insert success)
     *      fail    (insert failure)
     *
     */
    fun postWeight(weight: Double, employeeNo: String) {

        val url = "http://app.mxic.com.cn:9000/ehealth/php/scales/create.php"

        val request = Request.Builder()
                .url(url)

        val bodyBuilder = FormBody.Builder()
        val timeMillis = Calendar.getInstance().timeInMillis

        bodyBuilder
                .add("type", "figure")              //  fixed mark
                .add("employeeNo", employeeNo)
                .add("date", timeMillis.toString())
                .add("weight", weight.toString())

        request.post(bodyBuilder.build())

        val call = client!!.newCall(request.build())
        Thread(Runnable {
            try {
                val response = call.execute()
                val respText = response.body().string()
                sendMessage(messageHandler, R.id.what_postWeight, respText, 0, 0)
                Log.d(TAG, "上传体重和工号: $respText")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }).start()

    }


    /**
     * Get employee name from the server through webSocket
     */
    fun reqName(employeeNo: String, prob: Double) {
        val url = "http://app.mxic.com.cn:9000/ehealth/php/scales/query.php?searchtype=" +
                "employeeNo&employeeNo=$employeeNo"
        val request = Request.Builder()
                .url(url)
                .build()

        Thread(Runnable {
            try {
                val response = client!!.newCall(request)?.execute()

                val jsonArray = JSONArray(response?.body()?.string())
                Log.d(TAG, "getJsonArray: " + jsonArray.toString())
                val name = jsonArray.getJSONObject(0).getString("name")
                val text = String.format("%s  %.2f %s", name, prob,"%")
                sendMessage(messageHandler, R.id.what_got_uname, text, 0, 0)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException){
                e.printStackTrace()
            }
        }).start()
    }

    /**
     * Clear the text [tv_person] content after 5 seconds delay.
     */
    fun delayClearText(){
        Thread(Runnable {
            try {
                Thread.sleep(5000)
                sendMessage(messageHandler, R.id.what_clear_text, "", 0, 0)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }).start()
    }

    companion object {
        private val TAG = CameraPreviewActivity::class.java.simpleName
    }
}
