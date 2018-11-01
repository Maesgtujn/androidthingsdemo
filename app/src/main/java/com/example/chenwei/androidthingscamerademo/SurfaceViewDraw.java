package com.example.chenwei.androidthingscamerademo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Vector;

public class SurfaceViewDraw extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private SurfaceHolder mSurfaceHolder;
    private Canvas mCanvas;
    /*  mark the thread state */
    private boolean mIsDrawing;
//    private int x = 0, y = 0;
    private Paint mPaint;
    public SurfaceViewDraw(Context context){
        this(context, null);
    }

    public SurfaceViewDraw(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    public SurfaceViewDraw(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2f);

        initView();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mIsDrawing = true;

        new Thread(this).start();   //  start the thread
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsDrawing = false;
    }

    @Override
    public void run() {
//        while (mIsDrawing){
//            long start = System.currentTimeMillis();
//            draw();
//            long end = System.currentTimeMillis();
//            if (end - start < 100) {
//                try {
//                    Thread.sleep(100 - (end - start));
//                } catch (InterruptedException e){
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    /**
     * initiate view
     */
    private void initView(){
        setZOrderOnTop(true);               //  SurfaceView at the top

        getHolder().setFormat(PixelFormat.TRANSPARENT);      //  set surface transparent

        mSurfaceHolder = getHolder();

        mSurfaceHolder.addCallback(this);   //  register callback
    }

    public void draw(Rect rect) {
        try {
            mCanvas = mSurfaceHolder.lockCanvas();

            /* draw background and clear the previous frame */
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            //Rect rect = new Rect(0, 0, 100, 100);
            mCanvas.drawRect(rect, mPaint);
        } catch (Exception e){

        }finally {
            if (mCanvas != null){
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }
    public void draw(Vector<Box> boxes) {
        try {
            mCanvas = mSurfaceHolder.lockCanvas();
            /* draw background and clear the previous frame */
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for(Box box :boxes){
                Rect rect = box.transform2Rect();
                mCanvas.drawRect(rect, mPaint);

                //mCanvas.drawText(" " + box.area(), rect.left, rect.top + mPaint.getTextSize(), mPaint);


            }
        } catch (Exception e){
        }finally {
            if (mCanvas != null){
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    public void draw() {
        try {
            mCanvas = mSurfaceHolder.lockCanvas();

            /* draw background and clear the previous frame */
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);


            //Rect rect = new Rect(0, 0, 100, 100);
        } catch (Exception e){

        }finally {
            if (mCanvas != null){
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

}
