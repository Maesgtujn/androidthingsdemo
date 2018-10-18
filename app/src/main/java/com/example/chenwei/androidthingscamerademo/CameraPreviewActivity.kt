package com.example.chenwei.androidthingscamerademo

import android.app.Activity
import android.content.ContentValues.TAG
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.TextureView
import java.util.concurrent.Semaphore
import android.widget.Button as WidgetButton

class CameraPreviewActivity : Activity() {

    private lateinit var mCameraHandler: Handler
    private lateinit var mCameraThread: HandlerThread

    private lateinit var mCamera: DemoCamera
    private val mCameraOpenCloseLock: Semaphore = Semaphore(1)

    private lateinit var btnTakePicture: WidgetButton

    private lateinit var mTextureView: AutoFitTextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        mTextureView = findViewById(R.id.texture)


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

    private val mSurfaceTextureListener = object: TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureSizeChanged, width: $width, height: $height")
            mCamera.configureTransform(this@CameraPreviewActivity, width, height)
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureAvailable, width: $width, height: $height")
            startCameraPreview(width, height)
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            Log.d(TAG, "onSurfaceTextureUpdated")
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
