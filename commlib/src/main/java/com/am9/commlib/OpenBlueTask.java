package com.am9.commlib;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.clj.fastble.BleManager;

/**
 * Created by williamsha on 2018/8/31.
 */
public class OpenBlueTask extends AsyncTask<Integer, Integer, String> {
    private static final String TAG = OpenBlueTask.class.getSimpleName();

    private Runnable runnable;
    private ProgressDialog dialog;
    private int delay = 100;

    public OpenBlueTask(Context context, Runnable runnable) {
        Context mContext = context;

        this.dialog = new ProgressDialog(mContext);
        this.runnable = runnable;
        dialog.setTitle("title");
        dialog.setMessage("open blue...");

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.show();
    }

    @Override
    protected String doInBackground(Integer... params) {
        //第二个执行方法,onPreExecute()执行完后执行
        if (!BleManager.getInstance().isBlueEnable()) {
            delay = 3000;
            BleManager.getInstance().enableBluetooth();
        }
        for (int i = 0; i <= 100; i++) {
            publishProgress(i);
            try {
                Thread.sleep(params[0]);
                boolean isEnable = BleManager.getInstance().isBlueEnable();
                Log.d(TAG, i + "--" + isEnable);
                if (isEnable) {
                    return "isEnable";
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return "done";
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        dialog.dismiss();
        Handler handler = new Handler();
        handler.postDelayed(runnable, delay);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        dialog.dismiss();
    }
}
