package com.example.chenwei.androidthingscamerademo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
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
    private final Paint mPaint,mPaint2;
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

        mPaint2 = new Paint();
        mPaint2.setColor(Color.RED);
        mPaint2.setAntiAlias(true);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setStrokeWidth(2f);


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
    public void clear() {
        try {
            mCanvas = mSurfaceHolder.lockCanvas();

            /* draw background and clear the previous frame */
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        } catch (Exception e){

        }finally {
            if (mCanvas != null){
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }
        }
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

    /**
     * 识别框{@code rect}镜像反转
     * @param rect
     * @return
     */
    private Rect flipping(Rect rect){
        Rect rect1 = new Rect();
        rect1.left = mCanvas.getWidth() - rect.right;
        rect1.right = mCanvas.getWidth() - rect.left;
        rect1.top =rect.top;
        rect1.bottom = rect.bottom;
        return rect1;
    }
    public void draw(Vector<Box> boxes) {
        try {
            mCanvas = mSurfaceHolder.lockCanvas();
            /* draw background and clear the previous frame */
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for(Box box :boxes){
                Rect rect = flipping(box.transform2Rect());
//                Rect rect = box.transform2Rect();
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


    public void drawRectQR() {
        try {
            mCanvas = mSurfaceHolder.lockCanvas();
            float crop=0.7f;
            int edge = (int) (Math.min(mCanvas.getHeight(),mCanvas.getWidth())*crop);
            int top = (mCanvas.getHeight()-edge) /2;
            int bottom = (mCanvas.getHeight()+edge) /2;
            int left = (mCanvas.getWidth()-edge) /2;

            int right = (mCanvas.getWidth()+edge) /2;


            Rect rect = new Rect(left,top,right,bottom);

            /* draw background and clear the previous frame */
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mCanvas.drawRect(rect, mPaint2);

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
