package com.tcar.tmirror

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Phone control panel for a fresh, independently authored mirroring app.
 * Never tries to intercept Google Maps or simulate clicks on the vehicle UI.
 */
class MainActivity : Activity() {
    private val requestCapture = 371
    private val requestNotification = 372
    private val prefs by lazy { getSharedPreferences("tmirror", MODE_PRIVATE) }
    private lateinit var state: TextView
    private lateinit var appChoice: Button
    private fun dp(d: Int) = (d * resources.displayMetrics.density + .5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
            setBackgroundColor(0xFF101722.toInt())
        }
        fun text(value: String, size: Float): TextView = TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, dp(14))
        }
        fun button(value: String, onClick: () -> Unit): Button = Button(this).apply {
            text = value
            setOnClickListener { onClick() }
            isAllCaps = false
        }
        container.addView(text("T-Mirror · Thử nghiệm Android Auto", 22f))
        container.addView(text("Phản chiếu màn hình điện thoại thật. Google Maps vẫn do Android Auto quản lý. Legacy chỉ xuất hiện cạnh Maps nếu head unit hỗ trợ.", 14f))
        state = text("Trạng thái: Chưa phản chiếu", 15f)
        container.addView(state)
        container.addView(button("▶ Bắt đầu phản chiếu") { beginCapture() })
        container.addView(button("■ Dừng phản chiếu") {
            startService(Intent(this, CaptureService::class.java).setAction(CaptureService.ACTION_STOP))
            state.text = "Đã yêu cầu dừng."
        })
        appChoice = button("Chọn ứng dụng tự mở") { chooseApp() }
        container.addView(appChoice)
        container.addView(button("Mở ứng dụng đã chọn") { openSelectedApp() })
        container.addView(text("Chỉ dùng phản chiếu video khi xe đã đỗ. Yêu cầu đồng ý ghi màn hình mỗi phiên theo Android.", 12f))
        container.addView(text("Nếu không thấy T-Mirror (Legacy) trong Android Auto, kiểm tra nguồn cài đặt và loại màn hình. Cài từ Play Testing không bảo đảm được host chấp nhận.", 12f))
        val scroll = android.widget.ScrollView(this)
        scroll.addView(container, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)
        updateState()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestNotification)
        }
    }

    private fun updateState() {
        state.text = if (CaptureService.running) "● Đang phản chiếu" else "○ Chưa phản chiếu"
        appChoice.text = "App tự mở: " + prefs.getString("app_name", "Chưa chọn")
    }
    override fun onResume() { super.onResume(); if (::state.isInitialized) updateState() }

    private fun beginCapture() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), requestCapture)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != requestCapture || resultCode != RESULT_OK || data == null) return
        val command = Intent(this, CaptureService::class.java)
            .setAction(CaptureService.ACTION_START)
            .putExtra(CaptureService.EXTRA_CODE, resultCode)
            .putExtra(CaptureService.EXTRA_DATA, data)
        startForegroundService(command)
        updateState()
        // Launch from foreground user-initiated activity (not a background service).
        if (prefs.getBoolean("auto_launch", true)) {
            window.decorView.postDelayed({ openSelectedApp() }, 550L)
        }
    }

    private fun chooseApp() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        if (apps.isEmpty()) {
            AlertDialog.Builder(this).setMessage("Không tìm thấy ứng dụng khả dụng").setPositiveButton("OK", null).show()
            return
        }
        val names = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Ứng dụng tự mở khi phản chiếu")
            .setItems(names) { _, which ->
                val choice = apps[which]
                prefs.edit().putString("app_package", choice.activityInfo.packageName)
                    .putString("app_name", names[which]).apply()
                updateState()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun openSelectedApp() {
        val selected = prefs.getString("app_package", null) ?: return
        val launch = packageManager.getLaunchIntentForPackage(selected)
        if (launch != null) {
            try { startActivity(launch) } catch (_: Exception) {
                AlertDialog.Builder(this).setMessage("Không thể mở ứng dụng đã chọn")
                    .setPositiveButton("OK", null).show()
            }
        }
    }
}
