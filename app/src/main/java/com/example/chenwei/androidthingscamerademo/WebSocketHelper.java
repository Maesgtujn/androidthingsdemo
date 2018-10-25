package com.example.chenwei.androidthingscamerademo;

/**
 * Created by williamsha on 2018/10/23.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketHelper {
    private static String TAG = "WebSocketHelper";
    private OkHttpClient client;
    private WebSocketListener webSocketListener;
    private WebSocket mWebSocket;
    private Request request;
    private String HOST_URL;
    private Handler mHandler;
    private Context mContext;

    public WebSocketHelper(String HOST_URL, Handler mHandler) {
        this.HOST_URL = HOST_URL;
        this.mHandler = mHandler;
    }

    public void initWs() {
        client = new OkHttpClient.Builder().
                connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS).build();
//        .readTimeout(3, TimeUnit.SECONDS)
        request = new Request.Builder()
                .url(HOST_URL)
                .build();
        webSocketListener = new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
//                mWebSocket = webSocket;
                Log.d("WEBSOCKET", "==ONOPEN:==" + response);

            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                try {
                    JSONObject jsonObject = new JSONObject(text);
                    Log.d(TAG, "onMessage:" + text);

                    JSONArray jsonPersons = jsonObject.getJSONArray("data");

                    Message msg = Message.obtain();
                    msg.what = 2;
                    msg.obj = jsonPersons;
                    mHandler.sendMessage(msg);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d("WEBSOCKET", "==onMessage:==" + text);
            }


            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);

                Log.d("WEBSOCKET", "==onMessagebytes:==" + bytes.toString());

            }


            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);

                Log.d("WEBSOCKET", "==onClosing:==" + reason + "#" + code);
            }


            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);

                Log.d("WEBSOCKET", "==onClosed:==" + reason + "#" + code);
//                mWebSocket = null;
            }


            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);


                Log.d("WEBSOCKET", "==onFailure:==" + response);
                t.printStackTrace();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(10000);
                            mWebSocket = client.newWebSocket(request, webSocketListener);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        };
        mWebSocket = client.newWebSocket(request, webSocketListener);


    }

    public void sendReq(Vector<Box> boxes, Bitmap bitmap) {
        List<Bitmap> marginBitmap = new ArrayList<>();
            /*
            获取经MTCNN人脸检测之后产生的边框位置坐标,并添加margin后将原始图片进行裁剪,输出marginBitmap;
             */
        try {

            for (int i = 0; i < boxes.size(); i++) {
                marginBitmap.add(Bitmap.createBitmap(bitmap,
                        boxes.get(i).marginLeft(),
                        boxes.get(i).marginTop(),
                        Math.min(boxes.get(i).marginWidth(), bitmap.getWidth() - boxes.get(i).marginLeft()),
                        Math.min(boxes.get(i).marginHeight(), bitmap.getHeight() - boxes.get(i).marginTop())));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "[*]detect false: " + e);
        }
            /*
            将marginBitmap转换为Base64编码并通过json对象传到后台;
             */
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("action_type", "/identify_no_mtcnn");
            jsonObject.put("req_id", System.currentTimeMillis());
            JSONArray jsonArray=new JSONArray();
            for (int i = 0; i < marginBitmap.size(); i++) {
                String b64Img = Utils.bitmapToBase64(marginBitmap.get(i));

                jsonArray.put(b64Img);


            }
            jsonObject.put("data", jsonArray);

            String strJSONreq = jsonObject.toString();
            Log.d(TAG, "strJSONreq:" + strJSONreq);
            Log.d(TAG, "marginBitmap.size()" + marginBitmap.size());
            try {
                mWebSocket.send(strJSONreq);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
