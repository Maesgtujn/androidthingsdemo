package com.example.chenwei.androidthingscamerademo.peripheralIO;
/**
 * @Description set the peripheral IO pin number and return
 *
 * @author Jimmy Li
 * @
 * @date 18-10-8
 */
class BoardDefaults {

    private static final String BUZZER_PIN = "PWM1";
    private static final String LED_PIN = "BCM6";
    private static final String BUTTON_PIN = "BCM21";
    /**
     * @return the GPIO pin that the LED is connected
     * @author Jimmy Li
     * @date 18-10-8 下午1:50
     */
    public static String getGPIOForLED() {
        return LED_PIN;
    }

    public static String getPWMForSpeaker() {
        return BUZZER_PIN;
    }

    public static String getGPIOForButton(){
        return BUTTON_PIN;
    }
}
