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
import com.example.lovetester.ui.common.applySystemBarsPadding
import com.example.lovetester.ui.common.enableLightEdgeToEdge
import android.provider.Settings
import com.example.lovetester.ui.home.HomeActivity
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.example.lovetester.data.local.model.OnboardingStep
import com.example.lovetester.data.local.repository.LoveSettingsRepository
import com.example.lovetester.ui.intro.IntroActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PermissionActivity : AppCompatActivity(){

    private lateinit var binding: ActivityPermissionBinding

    @Inject
    lateinit var repository: LoveSettingsRepository
    private var isNavigating = false
    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            syncCameraPermissionSwitch()

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableLightEdgeToEdge()
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

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

        binding.btnContinue.setOnClickListener {
            navigateToHome()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBackToIntro()
                }
            }
        )
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
                R.color.primary
            )

            binding.switchAllowAccess.thumbTintList =
                ColorStateList.valueOf(thumbColor)

            val trackColor = ContextCompat.getColor(
                this,
                R.color.switch_track_on
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
            .setTitle(R.string.permission_camera_dialog_title)
            .setMessage(R.string.permission_camera_dialog_message)
            .setPositiveButton(R.string.permission_camera_dialog_open_settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }
    private fun hasCameraPermission(): Boolean {
        val permissionStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )

        return permissionStatus == PackageManager.PERMISSION_GRANTED
    }

    private fun navigateToHome() {
        if (isNavigating) return

        isNavigating = true
        binding.btnContinue.isEnabled = false

        lifecycleScope.launch {
            try {
                repository.setOnboardingStep(OnboardingStep.COMPLETED)

                val intent = Intent(
                    this@PermissionActivity,
                    HomeActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isNavigating = false
                binding.btnContinue.isEnabled = true

                Toast.makeText(
                    this@PermissionActivity,
                    getString(R.string.permission_save_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun navigateBackToIntro() {
        if (isNavigating) return

        isNavigating = true
        binding.btnContinue.isEnabled = false

        lifecycleScope.launch {
            try {
                repository.setOnboardingStep(OnboardingStep.INTRO)

                val intent = Intent(
                    this@PermissionActivity,
                    IntroActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isNavigating = false
                binding.btnContinue.isEnabled = true

                Toast.makeText(
                    this@PermissionActivity,
                    getString(R.string.permission_back_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}