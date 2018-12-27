package com.example.chenwei.androidthingscamerademo.peripheralIO;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import com.example.chenwei.androidthingscamerademo.CameraPreviewActivity;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

public class PeripheralHelper {
    private static final String TAG = PeripheralHelper.class.getSimpleName();

    private static final Queue<Music.Note> SONG = new ArrayDeque<>();

    private Gpio mButtonGpio;
    private Gpio ledGpio;
    private Pwm bus;
    private Handler buzzerSongHandler;
    private CameraPreviewActivity cameraPreviewActivity;

    public Gpio getLedGpio() {
        return ledGpio;
    }
    
    public PeripheralHelper(Context context) {
        PeripheralManager service = PeripheralManager.getInstance();
        cameraPreviewActivity = (CameraPreviewActivity) context;
        //  initialize LED and Button
        try {
            ledGpio = service.openGpio(BoardDefaults.getGPIOForLED());
            ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            mButtonGpio = service.openGpio(BoardDefaults.getGPIOForButton());
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            mButtonGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    Log.i(TAG, "GPIO changed, button pressed");
//                    cameraPreviewActivity.startImageCapture();
                    return true;
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pins", e);
        }
        //  initialize speakerPwn
        try {
            bus = service.openPwm(BoardDefaults.getPWMForSpeaker());
        } catch (IOException e) {
            throw new IllegalStateException("PWM1 bus cannot be opened.", e);
        }

        try {
            bus.setPwmDutyCycle(50);
        } catch (IOException e) {
            throw new IllegalStateException("PWM1 bus cannot be configured.", e);
        }

        HandlerThread handlerThread = new HandlerThread("BackgroundThread");
        handlerThread.start();
        buzzerSongHandler = new Handler(handlerThread.getLooper());

    }


    public void setLedValue(boolean value) {
        try {
            ledGpio.setValue(value);
        } catch (IOException e) {
            Log.e(TAG, "Error updating GPIO value", e);
        }
    }

    public void setSpeakerValue(boolean value) {
        if (value) {
            SONG.addAll(Music.POKEMON_ANIME_THEME);
            buzzerSongHandler.post(playSong);
        }
    }
    private final Runnable playSong = new Runnable() {
        @Override
        public void run() {
            if (SONG.isEmpty()) {
                return;
            }

            Music.Note note = SONG.poll();

            if (note.isRest()) {
                SystemClock.sleep(note.getPeriod());
            }
            else {
                try {
                    bus.setPwmFrequencyHz(note.getFrequency());
                    bus.setEnabled(true);
                    SystemClock.sleep(note.getPeriod());
                    bus.setEnabled(false);
                } catch (IOException e) {
                    throw new IllegalStateException("PWM1 bus cannot play note.", e);
                }
            }
            buzzerSongHandler.post(this);
        }
    };
}




