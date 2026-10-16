package com.carhud.aaproxy

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CarHostModeRuntimeTest {
    private lateinit var context: Context
    private lateinit var prefs: android.content.SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
        CarDisconnectWatchdog.resetForTests()
        CarMediaManager.setCarConnectionState(false, resumePhoneOnDisconnect = false)
    }

    @After
    fun tearDown() {
        prefs.edit().putBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false).commit()
        CarDisconnectWatchdog.resetForTests()
        CarMediaManager.setCarConnectionState(false, resumePhoneOnDisconnect = false)
        CarHostModeManager.requestMode(context, false)
    }

    @Test
    fun switchesBetweenExactlyOneEnabledHost() {
        prefs.edit().putBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, false).commit()
        CarHostModeManager.requestMode(context, false)
        assertTrue(isEnabled(CarHudProjectionService::class.java, manifestDefault = true))
        assertFalse(isEnabled(CarHudAutoService::class.java, manifestDefault = false))

        prefs.edit().putBoolean(SettingsActivity.KEY_ULTRAWIDE_FULL_SURFACE, true).commit()
        CarHostModeManager.requestMode(context, true)
        assertFalse(isEnabled(CarHudProjectionService::class.java, manifestDefault = true))
        assertTrue(isEnabled(CarHudAutoService::class.java, manifestDefault = false))
    }

    private fun isEnabled(cls: Class<*>, manifestDefault: Boolean): Boolean {
        val pm = context.packageManager
        return when (pm.getComponentEnabledSetting(ComponentName(context, cls))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
            else -> manifestDefault
        }
    }
}
