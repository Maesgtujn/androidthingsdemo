package com.example.chenwei.androidthingscamerademo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class TestActivity extends Activity {
    private static String TAG = "TestActivity";
    private Context mContext;

    private void checkpermission(Context mContext) {

        int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {

            Intent intent = new Intent(mContext, CameraPreviewActivity.class);
            startActivity(intent);
            Log.d("", "PERMISSION_GRANTED");
        } else {
            requestCameraPermission(1);
            Log.d("", "NOT_PERMISSION_GRANTED");

        }

    }

    /**
     * Check if this device has a camera
     */
//
//    private boolean checkCameraHardware(Context context)
//    {
//        if (context.getPackageManager().hasSystemFeature(
//                PackageManager.FEATURE_CAMERA_ANY))
//        {
//            // this device has a camera
//            return true;
//        }
//        else
//        {
//            // no camera on this device
//            return false;
//        }
//    }
//
//    private int getDefaultCameraId()
//    {
//        int defaultId = -1;
//
//        // Find the total number of cameras available
//        int mNumberOfCameras = Camera.getNumberOfCameras();
//
//        // Find the ID of the default camera
//        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//        for (int i = 0; i < mNumberOfCameras; i++)
//        {
//            Camera.getCameraInfo(i, cameraInfo);
//            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
//            {
//                defaultId = i;
//            }
//        }
//        if (-1 == defaultId)
//        {
//            if (mNumberOfCameras > 0)
//            {
//                // 如果没有后向摄像头
//                defaultId = 0;
//            }
//            else
//            {
//                // 没有摄像头
//                Toast.makeText(getApplicationContext(), "NO CAMERA",
//                        Toast.LENGTH_LONG).show();
//            }
//        }
//        return defaultId;
//    }
    private void requestCameraPermission(final int RC_HANDLE_CAMERA_PERM) {
        Log.w("", "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        mContext = this;
        checkpermission(mContext);
        //Log.d("checkCameraHardware",""+checkCameraHardware(mContext));
        //Log.d("getDefaultCameraId",""+getDefaultCameraId());


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == 1) {

            Intent intent = new Intent(mContext, CameraPreviewActivity.class);
            startActivity(intent);
            return;
        }


        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
    }

}
