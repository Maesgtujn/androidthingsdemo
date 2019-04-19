package com.example.chenwei.androidthingscamerademo;
/*
  MTCNN For Android
  by cjf@xmu 20180625
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.util.Vector;

public class Utils {
    //复制图片，并设置isMutable=true
    public static Bitmap copyBitmap(Bitmap bitmap) {
        return bitmap.copy(bitmap.getConfig(), true);
    }

    //在bitmap中画矩形
    private static void drawRect(Bitmap bitmap, Rect rect) {
        try {
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            int r = 255;//(int)(Math.random()*255);
            int g = 0;//(int)(Math.random()*255);
            int b = 0;//(int)(Math.random()*255);
            paint.setColor(Color.rgb(r, g, b));
            paint.setStrokeWidth(1 + bitmap.getWidth() / 500);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(rect, paint);
        } catch (Exception e) {
            Log.i("Utils", "[*] error" + e);
        }
    }

    //在图中画点
    public static void drawPoints(Bitmap bitmap, Point[] landmark) {
        for (Point aLandmark : landmark) {
            int x = aLandmark.x;
            int y = aLandmark.y;
            //Log.i("Utils","[*] landmarkd "+x+ "  "+y);
            drawRect(bitmap, new Rect(x - 1, y - 1, x + 1, y + 1));
        }
    }

    //Flip alone diagonal
    //对角线翻转。data大小原先为h*w*stride，翻转后变成w*h*stride
    public static void flip_diag(float[] data, int h, int w, int stride) {
        float[] tmp = new float[w * h * stride];
        for (int i = 0; i < w * h * stride; i++) tmp[i] = data[i];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < stride; z++)
                    data[(x * h + y) * stride + z] = tmp[(y * w + x) * stride + z];
            }
    }

    //src转为二维存放到dst中
    public static void expand(float[] src, float[][] dst) {
        int idx = 0;
        for (int y = 0; y < dst.length; y++)
            for (int x = 0; x < dst[0].length; x++)
                dst[y][x] = src[idx++];
    }

    //src转为三维存放到dst中
    public static void expand(float[] src, float[][][] dst) {
        int idx = 0;
        for (int y = 0; y < dst.length; y++)
            for (int x = 0; x < dst[0].length; x++)
                for (int c = 0; c < dst[0][0].length; c++)
                    dst[y][x][c] = src[idx++];

    }

    //dst=src[:,:,1]
    public static void expandProb(float[] src, float[][] dst) {
        int idx = 0;
        for (int y = 0; y < dst.length; y++)
            for (int x = 0; x < dst[0].length; x++)
                dst[y][x] = src[idx++ * 2 + 1];
    }

    //box转化为rect
    public static Rect[] boxes2rects(Vector<Box> boxes) {
        int cnt = 0;
        for (int i = 0; i < boxes.size(); i++) if (!boxes.get(i).deleted) cnt++;
        Rect[] r = new Rect[cnt];
        int idx = 0;
        for (int i = 0; i < boxes.size(); i++)
            if (!boxes.get(i).deleted)
                r[idx++] = boxes.get(i).transform2Rect();
        return r;
    }

    //删除做了delete标记的box
    public static Vector<Box> updateBoxes(Vector<Box> boxes) {
        Vector<Box> b = new Vector<Box>();
        for (int i = 0; i < boxes.size(); i++)
            if (!boxes.get(i).deleted)
                b.addElement(boxes.get(i));
        return b;
    }

    //
    static public void showPixel(int v) {
        Log.i("MainActivity", "[*]Pixel:R" + ((v >> 16) & 0xff) + "G:" + ((v >> 8) & 0xff) + " B:" + (v & 0xff));
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        /*Convert image into a byte array.*/
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] byteArrayImage = baos.toByteArray();

        //        Log.d(TAG, "Base64: " + encodedImage);

        return Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
    }

    public static String readQRImage(Bitmap bMap) {
        String contents = null;

        int[] intArray = new int[bMap.getWidth() * bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Reader reader = new MultiFormatReader();// use this otherwise ChecksumException
        try {
            Result result = reader.decode(bitmap);
            contents = result.getText();
            //byte[] rawBytes = result.getRawBytes();
            //BarcodeFormat format = result.getBarcodeFormat();
            //ResultPoint[] points = result.getResultPoints();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        return contents;
    }

    public static Bitmap getMarginBitmap(Box box, Bitmap bitmap) {
        return Bitmap.createBitmap(bitmap,
                box.marginLeft(),
                box.marginTop(),
                Math.min(box.marginWidth(), bitmap.getWidth() - box.marginLeft()),
                Math.min(box.marginHeight(), bitmap.getHeight() - box.marginTop()));

    }

    public static void test() {

        HandlerThread uIhandlerThread = new HandlerThread("update");
        uIhandlerThread.start();
//Handler UIhandler = new Handler(uIhandlerThread.getLooper());
        Handler uIhandler = new Handler(uIhandlerThread.getLooper(), new Handler.Callback() {
            public boolean handleMessage(Message msg) {
                Bundle b = msg.getData();
                int age = b.getInt("age");
                String name = b.getString("name");
                System.out.println("age is " + age + ", name is" + name);
                System.out.println("Handler--->" + Thread.currentThread().getId());
                System.out.println("handlerMessage");
                return true;
            }
        });



    }
    public static  void sendMessage(Handler handler, int what, Object object, int arg1, int arg2) {
        Message msg = Message.obtain();
        msg.obj = object;
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        handler.sendMessage(msg);
    }

    public static void faceDetector(Bitmap bmp, int mac_face) {

        FaceDetector fdet = new android.media.FaceDetector(bmp.getWidth(), bmp.getHeight(), mac_face);

        android.media.FaceDetector.Face[] fullResults = new android.media.FaceDetector.Face[mac_face];
        fdet.findFaces(bmp, fullResults);

        Log.d("faceDetector", "" + fullResults.length);

        for (int i = 0; i < mac_face; i++) {
            if (fullResults[i] == null) {
                //faces[i].clear();
            } else {
                PointF mid = new PointF();
                fullResults[i].getMidPoint(mid);

                float eyesDis = fullResults[i].eyesDistance();
                float confidence = fullResults[i].confidence();
                float pose = fullResults[i].pose(android.media.FaceDetector.Face.EULER_Y);
                Log.d("faceDetector", ",eyesDis:" + eyesDis + ",confidence:" + confidence + ",pose:" + pose);

            }
        }

    }

}
