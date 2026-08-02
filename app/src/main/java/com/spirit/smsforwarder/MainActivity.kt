package com.spirit.smsforwarder

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.spirit.smsforwarder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSIONS_REQUEST_CODE = 1

    private val permissions = mutableListOf<String>().apply {
        add(android.Manifest.permission.RECEIVE_SMS)
        add(android.Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_dashboard, R.id.navigation_configuration)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        ContextCompat.startForegroundService(
            this,
            Intent(this, AllNotificationService::class.java)
        )

        // درخواست همه دسترسی‌ها پشت سر هم
        startPermissionFlow()
    }

    override fun onResume() {
        super.onResume()
        // وقتی از تنظیمات برگشت، بقیه دسترسی‌ها را چک کن
        if (isNotificationServiceEnabled()) {
            requestBatteryOptimizationIfRequired()
        }
    }

    private fun startPermissionFlow() {
        // ۱) دسترسی‌های معمولی (SMS و ...)
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("دسترسی‌های لازم")
                .setMessage("برای کار کردن اپ، چند دسترسی لازم است. لطفاً همه را تأیید کنید.")
                .setPositiveButton("ادامه") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        missing.toTypedArray(),
                        PERMISSIONS_REQUEST_CODE
                    )
                }
                .setCancelable(false)
                .show()
        } else {
            askNotificationAccess()
        }
    }

    private fun askNotificationAccess() {
        if (!isNotificationServiceEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("دسترسی به نوتیفیکیشن‌ها")
                .setMessage(
                    "برای فوروارد همه نوتیفیکیشن‌ها باید دسترسی Notification Listener را روشن کنید.\n\n" +
                    "در صفحه بعدی، اپ SMSForwarder را پیدا کنید و سوئیچ آن را روشن کنید."
                )
                .setPositiveButton("برو به تنظیمات") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setCancelable(false)
                .show()
        } else {
            requestBatteryOptimizationIfRequired()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            // بعد از تأیید دسترسی‌های معمولی، سراغ Notification برو
            askNotificationAccess()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (enabledListeners.isNullOrEmpty()) return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':').apply {
            setString(enabledListeners)
        }
        val componentName = ComponentName(this, NotificationListener::class.java)
        return colonSplitter.any { it == componentName.flattenToString() }
    }

    private fun requestBatteryOptimizationIfRequired() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("بهینه‌سازی باتری")
                    .setMessage(
                        "برای اینکه اپ در پس‌زمینه خاموش نشود، بهینه‌سازی باتری را غیرفعال کنید."
                    )
                    .setPositiveButton("ادامه") { _, _ ->
                        val intent =
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        startActivity(intent)
                    }
                    .setNegativeButton("بعداً", null)
                    .show()
            }
        }
    }
}
