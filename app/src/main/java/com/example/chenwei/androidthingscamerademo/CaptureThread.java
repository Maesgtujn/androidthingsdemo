package com.example.chenwei.androidthingscamerademo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Vector;

import static com.example.chenwei.androidthingscamerademo.StaticValues.ACTION_TYPE_identify_no_mtcnn;
import static com.example.chenwei.androidthingscamerademo.StaticValues.ACTION_TYPE_validate;
import static com.example.chenwei.androidthingscamerademo.Utils.getMarginBitmap;
import static com.example.chenwei.androidthingscamerademo.Utils.readQRImage;

/**
 * Created by williamsha on 2018/11/5.
 */

public class CaptureThread extends Thread {
    static String TAG = "CaptureThread";
    private Handler mHandler;
    private Context mContext;
    private WebSocketHelper wshelper;
    private MTCNN mtcnn;
    private int detectorType =R.id.what_facenet_identify;

    private int count = 0;
    private int frameCount = 0;
    private int minFaceSize = 300;
    private TextureView mTextureView;
    boolean continua = false;

    public CaptureThread(Context context, Handler handler, TextureView textureView, WebSocketHelper wshelper) {
        this.mContext = context;
        this.mHandler = handler;
        this.mTextureView = textureView;
        this.wshelper = wshelper;
        mtcnn = new MTCNN(context.getAssets());

    }

    void sendMessage(Handler handler, int what, Object object, int arg1, int arg2) {
        Message msg = Message.obtain();
        msg.obj = object;
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        handler.sendMessage(msg);
    }

    public void setdetectorType(int type) {
        if (detectorType != type) {
            detectorType = type;
            count = 0;
        }
    }

    @Override
    public void run() {
        //super.run();

        continua = true;

        long t_start = System.currentTimeMillis();
        Bitmap bm, bitmap;
        Vector<Box> boxes;
        ArrayList<Bitmap> regBitmaps = new ArrayList<>();
        String qr_code = "";
        while (continua) {
            if (mTextureView != null) {
                Log.d(TAG, "processImage:start");

                bitmap = mTextureView.getBitmap();

                //Log.d("Thread",""+bitmap.height + " x "+bitmap.width)
                bm = Utils.copyBitmap(bitmap);
                //bm = Bitmap.createScaledBitmap(bitmap, 600, Math.round(((bitmap.height * 600 / bitmap.width).toDouble())).toInt(), false)
                Log.d(TAG, "processImage: bitmap:" + bm.getWidth() + "," + bm.getHeight());
                frameCount++;
                Double frameRate = 1000.0 * frameCount / ((System.currentTimeMillis() - t_start));

                Log.d(TAG, "======== #" + frameCount + ",frameRate(f/s):" + frameRate);
                try {

                    switch (detectorType) {
                        case R.id.what_qrcode:
                            if (count == 0) {
                                sendMessage(mHandler, R.id.what_qrcode, null, R.id.state_start, count);
                            }
                            String rawResult = readQRImage(bm);
                            count++;
                            if (rawResult != null) {
                                qr_code = rawResult;
                                Log.d(TAG, ">>>qr_code:$qr_code");
                                sendMessage(mHandler, R.id.what_qrcode, rawResult, R.id.state_succ, count);
                                setdetectorType(R.id.what_facenet_regadd);
                            } else {
                                Log.d(TAG, ">>>qr_code:null");
                                if (count < 20) {
                                    sendMessage(mHandler, R.id.what_qrcode, null, R.id.state_progress, count);
                                } else {
                                    sendMessage(mHandler, R.id.what_qrcode, null, R.id.state_fail, count);
                                    setdetectorType(R.id.what_facenet_identify);
                                }
                            }
                            break;

                        case R.id.what_facenet_identify:
                            boxes = mtcnn.detectFaces(bm, minFaceSize);
                            sendMessage(mHandler, R.id.what_mtcnn, boxes, R.id.state_succ, boxes.size());
                            if (boxes.size() > 0) {
                                ArrayList<Bitmap> bitmaps = new ArrayList<>();
                                //获取经MTCNN人脸检测之后产生的边框位置坐标,并添加margin后将原始图片进行裁剪,输出marginBitmap;
                                for (int i = 0; i < boxes.size(); i++) {
                                    bitmaps.add(getMarginBitmap(boxes.get(i), bm));
                                }
                                sendMessage(mHandler, R.id.what_facenet_identify, boxes, R.id.state_start, boxes.size());
                                wshelper.sendReq(bitmaps, ACTION_TYPE_identify_no_mtcnn, null);
                                //what_facenet_identify  state_succ,在wshelper的onMessage中主动发送给 mCameraHandler2
                            }
                            break;
                        case R.id.what_facenet_regadd:
                            if (count == 0) {
                                sendMessage(mHandler, R.id.what_facenet_regadd, null, R.id.state_start, count);
                                regBitmaps.clear();
                            }
                            boxes = mtcnn.detectFaces(bm, minFaceSize);
                            sendMessage(mHandler, R.id.what_mtcnn, boxes, R.id.state_succ, boxes.size());
                            if (boxes.size() == 1) {
                                count++;
                                regBitmaps.add(getMarginBitmap(boxes.get(0), bm));
                                sendMessage(mHandler, R.id.what_facenet_regadd, null, R.id.state_progress, count);

                                if (regBitmaps.size() == 9) {
                                    wshelper.sendReq(regBitmaps, ACTION_TYPE_validate, qr_code);

                                }
                            }
                            break;
                        default:
                            Log.d(TAG, "default");
                    }
                    Thread.sleep(50);

                } catch (Exception e) {
                    Log.e(TAG, "[*]detect false:$e");
                } finally {
                }

            }

        }
    }


}

