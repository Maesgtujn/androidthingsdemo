package com.example.chenwei.androidthingscamerademo

import android.os.Message
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()

        val msg: Message = Message.obtain()
       Log.d("TAG+++", ""+msg.arg1)

        assertEquals("com.example.chenwei.androidthingscamerademo", appContext.packageName)

    }
}
