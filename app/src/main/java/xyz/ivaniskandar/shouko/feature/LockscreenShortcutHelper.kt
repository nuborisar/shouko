package xyz.ivaniskandar.shouko.feature

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.logcat
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.getPreferences
import xyz.ivaniskandar.shouko.util.canWriteSecureSettings

/**
 * A feature module to apply custom lockscreen shortcut
 *
 * Since the right side lockscreen shortcut is tied to the
 * "Double tap power button to launch camera" gesture, we need to
 * keep the values default when the keyguard is locked. This module
 * does all the heavy lifting.
 *
 * Custom values inside local preferences ([getPreferences]) will
 * be applied when the keyguard is shown to user. Otherwise, those
 * will be set to system default a.k.a null.
 *
 * The caveat is the power button double tap gesture will be
 * "broken" if it's triggered when the keyguard is showing and
 * custom shortcut is applied. But hey, we can't have all nice
 * things in the world, can we?
 */
class LockscreenShortcutHelper(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
) : DefaultLifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {
    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!context.canWriteSecureSettings) {
                return
            }
            val keyguardLocked = context.getSystemService<KeyguardManager>()!!.isKeyguardLocked
            val screenOn = intent.action != Intent.ACTION_SCREEN_OFF
            if (screenOn && keyguardLocked) {
                lifecycleOwner.lifecycleScope.launch {
                    delay(75) // 5 II camera button action fix
                    logcat { "Set camera lockscreen shortcuts to custom ${intent.action}" }
                    // Keyguard is showing
                    val localSettings = getPreferences(context)
                    Settings.Secure.putString(
                        context.contentResolver,
                        LOCKSCREEN_LEFT_BUTTON,
                        localSettings.getString(LOCKSCREEN_LEFT_BUTTON, null)
                    )
                    Settings.Secure.putString(
                        context.contentResolver,
                        LOCKSCREEN_RIGHT_BUTTON,
                        localSettings.getString(LOCKSCREEN_RIGHT_BUTTON, null)
                    )
                }
            } else {
                logcat { "Set lockscreen shortcuts to system default" }
                Settings.Secure.putString(context.contentResolver, LOCKSCREEN_LEFT_BUTTON, null)
                Settings.Secure.putString(context.contentResolver, LOCKSCREEN_RIGHT_BUTTON, null)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val localSettings = getPreferences(context)
        val ready = localSettings.getString(LOCKSCREEN_LEFT_BUTTON, null) != null ||
            localSettings.getString(LOCKSCREEN_RIGHT_BUTTON, null) != null
        updateReceiverState(ready)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        updateReceiverState(false)
    }

    private fun updateReceiverState(state: Boolean) {
        if (state) {
            if (!receiverRegistered) {
                logcat { "Registering receiver" }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                    priority = 999
                }
                context.registerReceiver(receiver, filter)
                receiverRegistered = true
            }
        } else if (receiverRegistered) {
            logcat { "Unregistering receiver" }
            context.unregisterReceiver(receiver)
            receiverRegistered = false
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        onStart(lifecycleOwner)
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        getPreferences(context).registerOnSharedPreferenceChangeListener(this)
    }

    companion object {
        /**
         * System's Secure Settings key
         */
        const val LOCKSCREEN_LEFT_BUTTON = "sysui_keyguard_left"
        const val LOCKSCREEN_RIGHT_BUTTON = "sysui_keyguard_right"

        fun getPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("secure_settings", Context.MODE_PRIVATE)
        }
    }
}
