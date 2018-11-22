package com.example.chenwei.androidthingscamerademo

import android.app.Activity
import android.app.ProgressDialog
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
import android.widget.Toast
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
    private lateinit var mCameraThread2: CaptureThread
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
    //1->MTCNN&FACENET , 2->QRCODE ,3->face reg


    fun initThread(context: Context) {
        //mCameraThread2.run()
        /**
        - 异步线程
         */
        mCameraThread2 = CaptureThread(context, mCameraHandler2, mTextureView, wshelper)

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        var pd: ProgressDialog = ProgressDialog(this@CameraPreviewActivity)
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
        //mtcnn = MTCNN(assets)

        mCameraHandler2 = object : Handler() {
            override fun handleMessage(msg: Message?) {
                val state = msg!!.arg1
                val count = msg.arg2
                when (msg.what) {

                    R.id.what_qrcode -> {
                        when (state) {

                            R.id.state_start -> {
                                //画扫扫描框

                                mDraw.drawRectQR()

                                mTvHint.setText(R.string.qrcode_start)
                                mTvMsg.setText("")
                                mTvPerson.setText("")
                                mTvMode.setText("qrcode")

                            }
                            R.id.state_succ -> {
                                mTvHint.setText(R.string.qrcode_succ)
                                mTvMsg.text = msg.obj as String
                                mCameraThread2.setdetectorType(R.id.what_facenet_regadd)

                            }
                            R.id.state_fail -> {
                                mTvHint.setText(R.string.qrcode_fail)
                                mCameraThread2.setdetectorType(R.id.what_facenet_identify)
                                mTvMsg.setText("")

                            }

                            R.id.state_progress -> mTvMsg.setText(getString(R.string.qrcode_progress) + count+"/25")

                        }

                        //detectorType = R.id.what_facenet_identify

                    }
                    R.id.what_mtcnn -> {
                        if (state == R.id.state_succ) {
                            val boxes: Vector<Box> = msg.obj as Vector<Box>
                            mDraw.draw(boxes)
                        }

                    }
                    R.id.what_facenet_identify -> {
                        try {
                            if (mCameraThread2.getdetectorType() == R.id.what_facenet_identify)
                                when (state) {
                                    R.id.state_start -> {
                                        //unknowCount = 0
                                        mTvHint.setText(R.string.facenet_identify_start)
                                        mTvMode.setText("facenet_identify")

                                        //Toast.makeText(this@CameraPreviewActivity, R.string.facenet_identify_start, Toast.LENGTH_SHORT).show()


                                    }
                                    R.id.state_succ -> {
                                        mTvHint.setText(R.string.facenet_identify_succ)
                                        val jsonPersons: JSONArray = msg.obj as JSONArray
                                        var text = ""
                                        for (i in 1..jsonPersons.length()) {
                                            val jsonPerson: JSONObject = jsonPersons.get(i - 1) as JSONObject
                                            val name = jsonPerson.getString("name")
                                            if (name == UNKOWN_NAME) {
                                                unknowCount++
                                            } else {
                                                unknowCount = 0
                                            }

                                            text = text + String.format("%n %s  @ %.2f %s", name, 100 * jsonPerson.getDouble("prob"),  jsonPerson.getString("emotion"))
                                        }

                                        mTvPerson.text = text
                                        if (unknowCount > 0)
                                            mTvMsg.setText("Unknow:" + unknowCount)
                                        else
                                            mTvMsg.setText("")
                                        if (unknowCount > 4) {
                                            unknowCount = 0
                                            mCameraThread2.setdetectorType(R.id.what_qrcode)
                                        }
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

                    R.id.what_facenet_regadd -> {
                        when (state) {
                            R.id.state_start -> {
                                mTvHint.setText(R.string.facenet_add_start)
                                mTvMode.setText("facenet_regadd")
                                mTvMsg.setText("")
                                mTvPerson.setText("")

                            }
                            R.id.state_succ -> {
                                mTvHint.setText(R.string.facenet_add_succ)

                            }

                            R.id.state_fail -> {
                            }
                            R.id.state_progress -> mTvMsg.setText("已采集图像：" + count)
                        }

                    }
                    R.id.what_facenet_validate -> {
                        when (state) {

                            R.id.state_start -> {
                                mCameraThread2.setdetectorType(R.id.what_idle)
                                mTvMode.setText("facenet_validate")
                                mTvMsg.setText("")
                                mTvPerson.setText("")
                                pd = ProgressDialog.show(this@CameraPreviewActivity, "facenet_validate", getString(R.string.facenet_validate_start));
                                mTvHint.setText(R.string.facenet_validate_start)
                            }
                            R.id.state_succ -> {
                                pd.dismiss()
                                mTvHint.setText(R.string.facenet_validate_succ)
                                mCameraThread2.setdetectorType(R.id.what_facenet_identify)
                                Toast.makeText(this@CameraPreviewActivity, "注册成功", Toast.LENGTH_LONG).show()

                            }
                            R.id.state_fail -> {
                                pd.dismiss()
                                mTvHint.setText(R.string.facenet_validate_fail)
                                mCameraThread2.setdetectorType(R.id.what_facenet_identify)
                                Toast.makeText(this@CameraPreviewActivity, "注册失败", Toast.LENGTH_LONG).show()

                            }
                            R.id.state_progress -> {
                                mTvHint.setText(R.string.facenet_validate_progress)
                            }

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
