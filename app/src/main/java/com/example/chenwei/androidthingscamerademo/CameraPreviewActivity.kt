package com.example.chenwei.androidthingscamerademo

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.view.TextureView
import android.widget.TextView
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


    private lateinit var bitmap: Bitmap
    private lateinit var wshelper: WebSocketHelper
    lateinit var mtcnn: MTCNN


    fun initThread() {

        /**
        - 异步线程
         */
        continua = true
        mCameraThread2 = Thread(object : Runnable {
            override fun run() {
                frameCount = 0
                t_start = System.currentTimeMillis()
                var bm: Bitmap
                while (continua) {
                    if (mTextureView !== null) {
                        Log.d(TAG, "processImage:start")

                        bitmap = mTextureView.getBitmap()
                        //Log.d("Thread",""+bitmap.height + " x "+bitmap.width)
                        bm = Utils.copyBitmap(bitmap)
                        //bm = Bitmap.createScaledBitmap(bitmap, 600, Math.round(((bitmap.height * 600 / bitmap.width).toDouble())).toInt(), false)
                        Log.d(TAG, "processImage: bitmap:" + bm.width + "," + bm.height)
                        try {
                            val boxes = mtcnn.detectFaces(bm, 300)

                            Log.d(TAG, "======== box:" + boxes.size)

                            frameCount++
                            var frameRate: Double = 1000.0 * frameCount.toFloat() / ((System.currentTimeMillis() - t_start))

                            Log.d(TAG, "======== #" + frameCount + ",frameRate(f/s):" + frameRate)
                            val msg = Message.obtain()

                            msg.obj = boxes
                            msg.what=1
                            msg.arg1 = frameCount
                            msg.arg2 = (frameRate*1000).toInt()

                            mCameraHandler2.sendMessage(msg)



                            if(boxes.size>0){
                                wshelper.sendReq(boxes,bm)

                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "[*]detect false:$e")
                        } finally {
                        }


                    }
                    Thread.sleep(10)

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



        mtcnn = MTCNN(assets)
        val handler:Handler=Handler()
        mCameraHandler2 = object : Handler() {
            override fun handleMessage(msg: Message?) {
                when (msg?.what) {
                //msg.let {
                    0 -> {
                        mTvMsg.setText(msg.obj as String)

                    }
                    1->{
                        val boxes: Vector<Box> = msg?.obj as Vector<Box>
                        val textmsg = String.format("#%d @ %.2f (fps)",msg?.arg1,msg?.arg2.toFloat()/1000)
                        //handler.postDelayed(Runnable { mTvMsg.setText(textmsg) },100)

                        //
                        mDraw.draw(boxes)
                        if(boxes.size==0){
                            mTvPerson.setText("")
                        }
                    }
                    2->{
                        val jsonPersons: JSONArray = msg?.obj as JSONArray
                        var text:String=""
                        for( i in 1..jsonPersons.length()){
                            val jsonPerson:JSONObject = jsonPersons.get(i-1) as JSONObject
                            text=text+String.format("%n %s  @ %.2f",jsonPerson.getString("name"),100*jsonPerson.getDouble("prob"))+"%"

                        }

                        Log.d(TAG,"jsonPersons:"+jsonPersons.toString())
                        mTvPerson.setText(text)

                    }
                    else ->{

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
            Log.d(TAG, "onSurfaceTextureUpdated")
            //todo 尝试此处获取图像送到MTCNN
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
        mCameraHandler = Handler(mCameraThread.looper)


    }

    private fun stopBackgroundThread() {
        mCameraThread.quitSafely()
        continua = false
        mCameraThread2.join()

        try {
            mCameraThread.join()
        } catch (ex: InterruptedException) {
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
