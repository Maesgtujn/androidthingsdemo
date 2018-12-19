package com.example.chenwei.androidthingscamerademo;

/**
 * Created by williamsha on 2018/10/23.
 */

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import static com.example.chenwei.androidthingscamerademo.StaticValues.ACTION_TYPE_identify_no_mtcnn;
import static com.example.chenwei.androidthingscamerademo.StaticValues.ACTION_TYPE_validate;
import static com.example.chenwei.androidthingscamerademo.Utils.sendMessage;

/**
 * The {@code WebSocketHelper} class help to initialize the WebSocket service.
 * Including a server and a client.
 */
public class WebSocketHelper {
    private static String TAG = "WebSocketHelper";
    private OkHttpClient client;
    private WebSocketListener webSocketListener;
    private WebSocket mWebSocket;
    private Request request;
    private String HOST_URL;
    private Handler mHandler;

    public WebSocketHelper(String HOST_URL, Handler mHandler) {
        this.HOST_URL = HOST_URL;
        this.mHandler = mHandler;       //  message handler
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
            JSONObject jsonObject;

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
//                mWebSocket = webSocket;
                Log.d("WEBSOCKET", "==ONOPEN:==" + response);
                sendMessage(mHandler, R.id.what_ws_con, response, R.id.state_succ, 0);


            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                try {
                    jsonObject = new JSONObject(text);
                    Log.d(TAG, "onMessage:" + text);
                    String action_type = jsonObject.getString("action_type");
                    if (action_type.equals(ACTION_TYPE_identify_no_mtcnn)) {
                        JSONArray jsonPersons = jsonObject.getJSONArray("data");

                        sendMessage(mHandler, R.id.what_facenet_identify, jsonPersons, R.id.state_succ, jsonPersons.length());


                    }
                    if (action_type.equals(ACTION_TYPE_validate)) {
                        //{"action_type": "/validate", "data": {"accu": 0.9736842105263158, "succ": true, "wrong": [0.0, 0.0]}, "dur": 185261, "req_id": 1230770643302}
                        //{"action_type": "/validate", "dur": 987489, "data": {"accu": 0.9741935483870968, "wrong": [0.3333333333333333, 0.0], "succ": false}, "req_id": 1230786685456}
                        JSONObject jsData = jsonObject.getJSONObject("data");
                        boolean isSucceed = jsData.getBoolean("succ");
                        if (isSucceed)
                            sendMessage(mHandler, R.id.what_facenet_validate, jsData, R.id.state_succ, 1);
                        else
                            sendMessage(mHandler, R.id.what_facenet_validate, jsData, R.id.state_fail, 0);


                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d("WEBSOCKET", "==onMessage:==" + text);
            }


            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                sendMessage(mHandler, R.id.what_ws_con, reason, R.id.state_discon, code);

                Log.d("WEBSOCKET", "==onClosed:==" + reason + "#" + code);
//                mWebSocket = null;
            }


            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                sendMessage(mHandler, R.id.what_ws_con, response, R.id.state_fail, 0);


                Log.d("WEBSOCKET", "==onFailure:==" + response);
                t.printStackTrace();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(10000);
                            sendMessage(mHandler, R.id.what_ws_con, null, R.id.state_start, 0);

                            mWebSocket = client.newWebSocket(request, webSocketListener);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        };
        sendMessage(mHandler, R.id.what_ws_con, null, R.id.state_start, 0);

        mWebSocket = client.newWebSocket(request, webSocketListener);


    }

    public void sendReq(List<Bitmap> bitmaps, String actionType, String name) {
        try {
            JSONObject jsonObject = new JSONObject();
//设定API通道
            jsonObject.put("action_type", actionType);

            if (name != null) {
                jsonObject.put("name", name);

            }
//请求id用来匹配发送请求和返回信息
            jsonObject.put("req_id", System.currentTimeMillis());
//多个人脸数据按Base64编码为String
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < bitmaps.size(); i++) {
                String b64Img = Utils.bitmapToBase64(bitmaps.get(i));
                jsonArray.put(b64Img);
            }
            jsonObject.put("data", jsonArray);

            String strJSONreq = jsonObject.toString();
            Log.d(TAG, "strJSONreq:" + strJSONreq);
            try {
//                返回数据将在mWebSocket.onMessage获得如果需要和请求匹配，则依据req_id即可
                mWebSocket.send(strJSONreq);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
