package com.example.airsense

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.airsense.notifications.NotificationHelper
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationModuleInstrumentedTest {

    @Test
    fun notificationHelper_initializesAndCreatesChannelSuccessfully() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val notificationHelper = NotificationHelper(appContext)

        assertNotNull(notificationHelper)

        // Verificamos que la creación del canal no lance excepciones en el entorno Android
        notificationHelper.createNotificationChannel()
    }
}
