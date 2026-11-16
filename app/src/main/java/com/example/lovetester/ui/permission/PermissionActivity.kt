package com.example.lovetester.ui.permission

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lovetester.R
import com.example.lovetester.databinding.ActivityPermissionBinding
import android.provider.Settings
class PermissionActivity : AppCompatActivity(){

    private lateinit var binding: ActivityPermissionBinding

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            syncCameraPermissionSwitch()

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        syncCameraPermissionSwitch()

        binding.switchAllowAccess.setOnClickListener {

            if (hasCameraPermission()) {
                showDisableCameraDialog()
            } else {
                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }

            syncCameraPermissionSwitch()
        }
    }

    override fun onResume() {
        super.onResume()

        val hasPermission = hasCameraPermission()

        binding.switchAllowAccess.isChecked = hasPermission

        updateSwitchColors(hasPermission)
    }

    private fun updateSwitchColors(isChecked: Boolean) {
        if(isChecked){
            val thumbColor = ContextCompat.getColor(
                this,
                R.color.color_ff0845
            )

            binding.switchAllowAccess.thumbTintList =
                ColorStateList.valueOf(thumbColor)

            val trackColor = ContextCompat.getColor(
                this,
                R.color.color_4dff4081
            )

            binding.switchAllowAccess.trackTintList =
                ColorStateList.valueOf(trackColor)
        }else{
            val thumbColor = ContextCompat.getColor(
                this,
                R.color.white
            )

            binding.switchAllowAccess.thumbTintList =
                ColorStateList.valueOf(thumbColor)

            val trackColor = ContextCompat.getColor(
                this,
                R.color.splash_ads_text
            )

            binding.switchAllowAccess.trackTintList =
                ColorStateList.valueOf(trackColor)
        }
    }

    private fun syncCameraPermissionSwitch() {
        val hasPermission = hasCameraPermission()

        binding.switchAllowAccess.isChecked = hasPermission

        updateSwitchColors(hasPermission)
    }

    private fun openAppSettings() {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )

        startActivity(intent)
    }

    private fun showDisableCameraDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tắt quyền Camera")
            .setMessage(
                "Để tắt quyền Camera, bạn cần vào Cài đặt ứng dụng và thu hồi quyền Camera."
            )
            .setPositiveButton("Mở cài đặt") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    private fun hasCameraPermission(): Boolean {
        val permissionStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )

        return permissionStatus == PackageManager.PERMISSION_GRANTED
    }
}