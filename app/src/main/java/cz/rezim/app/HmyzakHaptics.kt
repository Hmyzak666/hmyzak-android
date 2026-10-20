package cz.rezim.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

internal enum class HapticResult { REQUESTED, SYSTEM_DISABLED, NO_VIBRATOR, UNAVAILABLE }

// Drive a concrete pulse rather than relying on the host View's CONFIRM mapping.
// Both API branches classify this as touch feedback and respect phone settings.
@Suppress("DEPRECATION")
internal fun performSetHaptic(context: Context, test: Boolean = false): HapticResult = try {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else context.getSystemService(Vibrator::class.java)
    when {
        vibrator == null || !vibrator.hasVibrator() -> HapticResult.NO_VIBRATOR
        Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0 ->
            HapticResult.SYSTEM_DISABLED
        else -> {
            val effect = VibrationEffect.createOneShot(if (test) 180L else 70L, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build())
            } else {
                vibrator.vibrate(effect, AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
            }
            // A successful request is not proof that the user felt the motor.
            HapticResult.REQUESTED
        }
    }
} catch (_: RuntimeException) { HapticResult.UNAVAILABLE }

internal fun openHapticSettings(context: Context): Boolean = try {
    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) { false }
catch (_: SecurityException) { false }
