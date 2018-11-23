package com.am9.commlib;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

/**
 * Created by williamsha on 2018/11/9.
 */

public class MyBleNotifyCallback extends BleNotifyCallback {
    static String TAG = "MyBleNotifyCallback";
    public BluetoothGattCharacteristic characteristic;
    BluetoothGattService service;

    public MyBleNotifyCallback(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
        this.service = characteristic.getService();

    }


    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null){
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    @Override
    public void onNotifySuccess() {
        Log.d(TAG, service.getUuid().toString()+" characteristic:" + characteristic.getUuid().toString()+" ==onNotifySuccess");
    }

    @Override
    public void onNotifyFailure(BleException exception) {
        Log.d(TAG, service.getUuid().toString()+" characteristic:" + characteristic.getUuid().toString()+" ==onNotifyFailure:" + exception.toString());

    }

    @Override
    public void onCharacteristicChanged(byte[] data) {

        Log.d(TAG, service.getUuid().toString()+" characteristic:" + characteristic.getUuid().toString()+" data:"+   HexUtil.formatHexString(characteristic.getValue(), true));




    }
}
