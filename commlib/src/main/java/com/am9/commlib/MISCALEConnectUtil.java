package com.am9.commlib;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.text.TextUtils;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.utils.HexUtil;

import java.util.UUID;

/**
 * Created by williamsha on 2018/8/31.
 */

public class MISCALEConnectUtil {

    /**
     * 体重秤传回数据解析为体重数值（公斤）
     *
     * @param data
     * @return weight
     */
    static public double convertToWeight(byte[] data) {
        if (data == null || data.length < 1)
            return -1;  //数据异常返回-1
        double weight = ((((data[2] & 0xFF) << 8) + (data[1] & 0xFF)) / 200.0);
        //mWeigthText.setText(getString(R.string.show_kg, weight));
        String TAG = "MISCALEConnectUtil";
        Log.d(TAG, HexUtil.formatHexString(data));
        Log.d(TAG, "data[0]:" + byte2bits(data[0]));
        int aa = (data[0] & 0xFF);
        if (aa == 0x22) {
            Log.d(TAG, "== " + weight);
        } else {
            Log.d(TAG, "~~ " + weight);
        }
        return weight;

    }

    static public int getWeightState(byte[] data) {
        /*
* 02 -> 有负载，非稳定结果，测量中
* 22 -> 有负载，稳定结果
* 82 -> 离开，此次测量无稳定结果
* a2 -> 离开，此次测量有稳定结果
* */

        return data[0] & 0xFF;
    }

    public static String byte2bits(byte b) {

        int z = b;
        z |= 256;

        String str = Integer.toBinaryString(z);

        int len = str.length();

        return str.substring(len - 8, len);

    }

    public static BluetoothGattCharacteristic getCharacteristic(final BluetoothGatt bluetoothGatt) {
        String service_uuid = "0000181d-0000-1000-8000-00805f9b34fb";
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(service_uuid));
        String characteristic_uuid = "00002a9d-0000-1000-8000-00805f9b34fb";
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristic_uuid));
        return characteristic;
    }

    /**
     * @param str_name
     * @param isAutoConnect Whether to directly connect to the remote device (false)
     *                      or to automatically connect as soon as the remote
     *                      device becomes available (true).
     */
    public static void setScanRule(String str_name, boolean isAutoConnect) {

        String[] names;
        if (TextUtils.isEmpty(str_name)) {
            names = null;
        } else {
            names = str_name.split(",");
        }


        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()

                .setDeviceName(true, names)   // 只扫描指定广播名的设备，可选

                .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(60000)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);

    }

}
