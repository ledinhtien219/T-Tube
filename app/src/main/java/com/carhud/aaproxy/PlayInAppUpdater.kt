package com.carhud.aaproxy

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.carhud.app.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Update ONLY the Google Play distribution. Updates are offered on the phone,
 * never as an Android Auto car-screen prompt or a direct APK download.
 *
 * One launcher per activity, registered before STARTED.
 * Flexible downloads do not restart a running Android Auto session until
 * the user explicitly chooses "Cài đặt" on the phone.
 */
internal class PlayInAppUpdater(private val activity: AppCompatActivity) {
    private val manager by lazy { AppUpdateManagerFactory.create(activity.applicationContext) }
    private val settings by lazy {
        activity.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE)
    }
    private val resultLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK &&
            result.resultCode != Activity.RESULT_CANCELED
        ) showToast("Không bắt đầu được cập nhật. Hãy kiểm tra CH Play.")
    }

    private var listenerRegistered = false
    private var checking = false
    private var prompting = false
    private var installPrompting = false

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            activity.runOnUiThread { showInstallPrompt() }
        }
    }

    fun onResume() {
        if (!BuildConfig.PLAY_DISTRIBUTION) return
        if (!listenerRegistered) {
            manager.registerListener(listener)
            listenerRegistered = true
        }
        // Reopen the install prompt after app backgrounding or activity recreation.
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) showInstallPrompt()
        }
    }

    fun checkForUpdates(manual: Boolean = false) {
        if (!BuildConfig.PLAY_DISTRIBUTION || activity.isFinishing || activity.isDestroyed) return
        if (checking || prompting) return
        if (!manual) {
            val last = settings.getLong(PREF_LAST_CHECK, 0L)
            val now = System.currentTimeMillis()
            if (now - last in 0 until AUTO_CHECK_INTERVAL_MS) return
        }
        checking = true
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                checking = false
                if (!manual) settings.edit()
                    .putLong(PREF_LAST_CHECK, System.currentTimeMillis()).apply()
                if (!canShowDialog()) return@addOnSuccessListener

                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> showInstallPrompt()
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        if (prompting) return@addOnSuccessListener
                        prompting = true
                        val dialog = AlertDialog.Builder(activity)
                            .setTitle("Có bản cập nhật T Tube")
                            .setMessage("Đã có bản mới trên CH Play. Bạn có thể tải khi đang sử dụng app và chọn cài đặt lúc thuận tiện.")
                            .setNegativeButton("Để sau", null)
                            .setPositiveButton("Cập nhật") { _, _ ->
                                try {
                                    manager.startUpdateFlowForResult(
                                        info,
                                        resultLauncher,
                                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                                    )
                                } catch (_: Exception) {
                                    showToast("Không thể mở cập nhật CH Play. Vui lòng thử lại.")
                                }
                            }.create()
                        dialog.setOnDismissListener { prompting = false }
                        dialog.show()
                    }
                    manual -> showToast("Bạn đang dùng bản mới nhất khả dụng trên CH Play.")
                }
            }
            .addOnFailureListener {
                checking = false
                if (manual && canShowDialog()) {
                    showToast("Chưa kiểm tra được CH Play. Kiểm tra kết nối mạng và tài khoản thử nghiệm.")
                }
            }
    }

    private fun showInstallPrompt() {
        if (!canShowDialog() || installPrompting) return
        installPrompting = true
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Bản cập nhật đã tải xong")
            .setMessage("Chọn Cài đặt khi xe đang đỗ. T Tube sẽ cần khởi động lại để hoàn tất cập nhật.")
            .setNegativeButton("Để sau", null)
            .setPositiveButton("Cài đặt và khởi động lại") { _, _ ->
                manager.completeUpdate()
            }.create()
        dialog.setOnDismissListener { installPrompting = false }
        dialog.show()
    }

    private fun canShowDialog(): Boolean =
        !activity.isFinishing && !activity.isDestroyed &&
            activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    private fun showToast(message: String) {
        if (!activity.isFinishing && !activity.isDestroyed) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    fun dispose() {
        if (listenerRegistered) {
            manager.unregisterListener(listener)
            listenerRegistered = false
        }
    }

    companion object {
        private const val PREF_LAST_CHECK = "play_update_last_check_at"
        private const val AUTO_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    }
}
