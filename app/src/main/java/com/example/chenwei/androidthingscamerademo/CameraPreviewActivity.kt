package com.example.chenwei.androidthingscamerademo

import android.app.Activity
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
import com.example.chenwei.androidthingscamerademo.StaticValues.ACTION_TYPE_identify_no_mtcnn
import com.example.chenwei.androidthingscamerademo.StaticValues.ACTION_TYPE_validate
import com.example.chenwei.androidthingscamerademo.Utils.getMarginBitmap
import com.example.chenwei.androidthingscamerademo.Utils.readQRImage
import com.google.zxing.activity.CaptureActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Semaphore
import android.widget.Button as WidgetButton


class CameraPreviewActivity : Activity() {
    val TAG: String = "CameraPreviewActivity"
    private lateinit var mCameraHandler: Handler
    private lateinit var mCameraThread: HandlerThread

    private lateinit var mCameraHandler2: Handler
    private lateinit var mCameraThread2: Thread
    private var continua: Boolean = false
    private var frameCount: Int = 0
    private var t_start = System.currentTimeMillis()


    private lateinit var mCamera: DemoCamera
    private val mCameraOpenCloseLock: Semaphore = Semaphore(1)

    private lateinit var mTextureView: AutoFitTextureView
    private lateinit var mDraw: SurfaceViewDraw
    private lateinit var mTvMsg: TextView
    private lateinit var mTvPerson: TextView
    private lateinit var mTvMode: TextView
    private lateinit var mTvHint: TextView


    private lateinit var bitmap: Bitmap
    private lateinit var wshelper: WebSocketHelper
    lateinit var mtcnn: MTCNN
    private var unknowCount: Int = 0
    private val UNKOWN_NAME = "Unknown"
    private var detectorType = R.id.what_facenet_identify
    //1->MTCNN&FACENET , 2->QRCODE ,3->face reg
    fun sendMessage(handler: Handler, what: Int, objects: Any?, arg1: Int, arg2: Int) {
        val msg = Message.obtain()
        msg.obj = objects
        msg.what = what
        msg.arg1 = arg1
        msg.arg2 = arg2
        handler.sendMessage(msg)
    }

    fun initThread() {

        /**
        - 异步线程
         */

        mCameraThread2 = Thread(object : Runnable {
            var detectorType: Int = R.id.what_facenet_identify
            var count: Int = 0
            var frameCount = 0
            val minFaceSize = 300


            fun setdetectorType(type: Int) {
                if (detectorType !== type) {
                    detectorType = type
                    count = 0
                }
            }

            override fun run() {
                continua = true

                t_start = System.currentTimeMillis()
                var bm: Bitmap
                var regbitmaps: ArrayList<Bitmap> = ArrayList<Bitmap>()
                var qr_code = ""
                while (continua) {
                    if (mTextureView !== null) {
                        Log.d(TAG, "processImage:start")

                        bitmap = mTextureView.bitmap

                        //Log.d("Thread",""+bitmap.height + " x "+bitmap.width)
                        bm = Utils.copyBitmap(bitmap)
                        //bm = Bitmap.createScaledBitmap(bitmap, 600, Math.round(((bitmap.height * 600 / bitmap.width).toDouble())).toInt(), false)
                        Log.d(TAG, "processImage: bitmap:" + bm.width + "," + bm.height)
                        frameCount++
                        var frameRate: Double = 1000.0 * frameCount.toFloat() / ((System.currentTimeMillis() - t_start))

                        Log.d(TAG, "======== #" + frameCount + ",frameRate(f/s):" + frameRate)
                        try {

                            when (detectorType) {
                                R.id.what_qrcode -> {
                                    if (count == 0) {
                                        sendMessage(mCameraHandler2, R.id.what_qrcode, null, R.id.state_start, count)
                                    }
                                    val rawResult = readQRImage(bm)
                                    count++
                                    if (rawResult !== null) {
                                        qr_code = rawResult
                                        Log.d(TAG, ">>>qr_code:$qr_code")
                                        sendMessage(mCameraHandler2, R.id.what_qrcode, rawResult, R.id.state_succ, count)
                                        setdetectorType(R.id.what_facenet_regadd)
                                    } else {
                                        Log.d(TAG, ">>>qr_code:null")
                                        if (count < 20) {
                                            sendMessage(mCameraHandler2, R.id.what_qrcode, null, R.id.state_progress, count)
                                        } else {
                                            sendMessage(mCameraHandler2, R.id.what_qrcode, null, R.id.state_fail, count)
                                            setdetectorType(R.id.what_facenet_identify)
                                        }
                                    }
                                }

                                R.id.what_facenet_identify -> {
                                    val boxes = mtcnn.detectFaces(bm, minFaceSize)
                                    sendMessage(mCameraHandler2, R.id.what_mtcnn, boxes, R.id.state_succ, boxes.size)
                                    if (boxes.size > 0) {
                                        val bitmaps = ArrayList<Bitmap>()
                                        //获取经MTCNN人脸检测之后产生的边框位置坐标,并添加margin后将原始图片进行裁剪,输出marginBitmap;
                                        for (i in boxes.indices) {
                                            bitmaps.add(getMarginBitmap(boxes[i], bm))
                                        }
                                        sendMessage(mCameraHandler2, R.id.what_facenet_identify, boxes, R.id.state_start, boxes.size)
                                        wshelper.sendReq(bitmaps, ACTION_TYPE_identify_no_mtcnn, null)
                                        //what_facenet_identify  state_succ,在wshelper的onMessage中主动发送给 mCameraHandler2
                                    }
                                }
                                R.id.what_facenet_regadd -> {
                                    if (count == 0) {
                                        sendMessage(mCameraHandler2, R.id.what_facenet_regadd, null, R.id.state_start, count)
                                        regbitmaps.clear()
                                    }
                                    val boxes = mtcnn.detectFaces(bm, minFaceSize)
                                    sendMessage(mCameraHandler2, R.id.what_mtcnn, boxes, R.id.state_succ, boxes.size)
                                    if (boxes.size == 1) {
                                        count++
                                        regbitmaps.add(getMarginBitmap(boxes[0], bm))
                                        sendMessage(mCameraHandler2, R.id.what_facenet_regadd, null, R.id.state_progress, count)

                                        if (regbitmaps.size == 9) {
                                            wshelper.sendReq(regbitmaps, ACTION_TYPE_validate, qr_code)

                                        }
                                    }
                                }
                                else -> println("default")
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "[*]detect false:$e")
                        } finally {
                        }

                    }
                    Thread.sleep(50)

                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        mTextureView = findViewById(R.id.texture)
        mDraw = findViewById(R.id.draw)
        mTvMsg = findViewById(R.id.tv_msg)
        mTvPerson = findViewById(R.id.tv_person)
        mTvMode = findViewById(R.id.tv_mode)
        mTvHint = findViewById(R.id.tv_hint)


        mTvMsg.setOnClickListener(View.OnClickListener {

            val intent = Intent(it.context, CaptureActivity::class.java)
            startActivity(intent)
        })
        mtcnn = MTCNN(assets)

        mCameraHandler2 = object : Handler() {
            override fun handleMessage(msg: Message?) {
                val state = msg!!.arg1
                val count = msg.arg2
                when (msg.what) {
                //msg.let {
                    StaticValues.WHAT_TEXT -> {
                        mTvMsg.text = msg.obj as String

                    }

                    R.id.what_qrcode -> {
                        when (state) {
                            R.id.state_succ ->
                                mTvMsg.text = msg.obj as String
                            R.id.state_start -> {
                                //画扫扫描框
                                mTvHint.setText(R.string.qrcode_start)
                            }
                            R.id.state_fail -> {
                                mTvHint.setText(R.string.qrcode_fail)
                            }
                            R.id.state_progress -> {
                                mTvMsg.setText(R.string.qrcode_progress)
                            }
                        }

                        detectorType = R.id.what_facenet_identify

                    }
                    R.id.what_mtcnn -> {
                        if (state == R.id.state_succ) {
                            val boxes: Vector<Box> = msg.obj as Vector<Box>
                            mDraw.draw(boxes)
                        }

                    }
                    R.id.what_facenet_identify -> {
                        try {
                            when (state) {

                                R.id.state_succ -> {
                                    mTvHint.setText(R.string.facenet_identify_succ)

                                    val jsonPersons: JSONArray = msg.obj as JSONArray
                                    var text: String = ""
                                    for (i in 1..jsonPersons.length()) {
                                        val jsonPerson: JSONObject = jsonPersons.get(i - 1) as JSONObject
                                        val name = jsonPerson.getString("name")
                                        if (name == UNKOWN_NAME) {
                                            unknowCount++
                                        } else {
                                            unknowCount = 0
                                        }

                                        text = text + String.format("%n %s  @ %.2f", name, 100 * jsonPerson.getDouble("prob")) + "%"
                                    }
                                    mTvPerson.text = text
                                    if (unknowCount > 4) {
mCameraThread2.setde                                    }
                                }
                                R.id.state_start -> {
                                    unknowCount = 0
                                    mTvHint.setText(R.string.facenet_identify_start)
                                }
                                R.id.state_fail -> {
                                    mTvHint.setText(R.string.facenet_identify_fail)

                                }
                                R.id.state_progress -> {
                                    mTvMsg.text = "..."
                                }

                            }


                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    else -> {

                    }
                //}
                }
            }
        }

        wshelper = WebSocketHelper("ws://192.168.164.196:8011", mCameraHandler2)

        wshelper.initWs()
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

            initThread()
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
}
