package com.jacekpietras.zoo.tracking.permissions

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.jacekpietras.zoo.tracking.R
import com.jacekpietras.zoo.tracking.utils.getAppCompatActivity
import com.jacekpietras.zoo.tracking.utils.getApplicationSettingsIntent
import com.jacekpietras.zoo.tracking.utils.isGpsEnabled
import com.jacekpietras.zoo.tracking.utils.observeReturn

class GpsPermissionRequester(private val fragment: Fragment) {

    private lateinit var activity: AppCompatActivity
    private val permissionResult =
        fragment.registerForActivityResult(RequestMultiplePermissions()) { isGranted ->
            if (isGranted.filter { it.value }.isNotEmpty()) checkPermissionsAgain()
            else callbacks.onFailed()
        }
    private val resolutionResult =
        fragment.registerForActivityResult(StartIntentSenderForResult()) { isGranted ->
            if (isGranted.resultCode == RESULT_OK) checkPermissionsAgain()
            else callbacks.onFailed()
        }
    private lateinit var callbacks: Callback

    fun checkPermissions(
        onGranted: () -> Unit,
        onDenied: () -> Unit = {},
    ) {
        this.activity = fragment.requireActivity().getAppCompatActivity()

        this.callbacks = Callback(
            onFailed = onDenied,
            onPermission = onGranted,
        )
        checkPermissionsAgain()
    }

    private fun checkPermissionsAgain() {
        when {
            havePermissions() -> {
                resetFirstTimeAsking()

                if (activity.isGpsEnabled()) {
                    callbacks.onPermission()
                } else {
                    EnableGpsUseCase().run(
                        activity = activity,
                        lifecycleOwner = activity,
                        onRequestSth = { intent ->
                            resolutionResult.launch(IntentSenderRequest.Builder(intent).build())
                        },
                        onFreshRequestRequired = { checkPermissionsAgain() },
                        onGpsEnabled = {
                            callbacks.onPermission()
                        },
                        onDenied = {
                            callbacks.onFailed()
                        },
                    )
                }
            }
            allPermissions.any { shouldDescribe(it) } -> {
                // do nothing, dialog is shown
            }
            else -> {
                askedForPermission()
                askForPermissions()
            }
        }
    }

    private fun shouldDescribe(permission: String): Boolean =
        when {
            shouldShowRequestPermissionRationale(activity, permission) -> {
                showRationale()
                true
            }
            isFirstTimeAskingPermission(permission) -> {
                firstTimeAskingPermission(permission, false)
                false
            }
            else -> {
                showDenied()
                true
            }
        }

    private fun askForPermissions() {
        permissionResult.launch(allPermissions.toTypedArray())
    }

    private fun showRationale() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.gps_permission_rationale_title))
            .setMessage(activity.getString(R.string.gps_permission_rationale_content))
            .setPositiveButton(activity.getString(android.R.string.ok)) { dialog, _ ->
                askForPermissions()
                dialog.dismiss()
            }
            .setNegativeButton(activity.getString(android.R.string.cancel)) { dialog, _ ->
                callbacks.onFailed()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showDenied() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.gps_permission_denied_title))
            .setMessage(activity.getString(R.string.gps_permission_denied_content))
            .setPositiveButton(activity.getString(android.R.string.ok)) { dialog, _ ->
                dialog.dismiss()
                activity.startActivity(activity.getApplicationSettingsIntent())
                activity.observeReturn { checkPermissionsAgain() }
            }
            .setNegativeButton(activity.getString(android.R.string.cancel)) { dialog, _ ->
                callbacks.onFailed()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun havePermissions(): Boolean =
        neededPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun resetFirstTimeAsking() {
        allPermissions.forEach {
            firstTimeAskingPermission(it, false)
        }
    }

    private fun askedForPermission() {
        allPermissions.forEach {
            firstTimeAskingPermission(it, true)
        }
    }

    private fun firstTimeAskingPermission(permission: String, firstTime: Boolean) {
        val sharedPreference = activity.getSharedPreferences(GPS_PERMISSIONS, MODE_PRIVATE)
        sharedPreference.edit().putBoolean(permission, firstTime).apply()
    }

    private fun isFirstTimeAskingPermission(permission: String) =
        activity.getSharedPreferences(GPS_PERMISSIONS, MODE_PRIVATE).getBoolean(permission, true)

    private class Callback(
        val onPermission: () -> Unit,
        val onFailed: () -> Unit,
    )

    private companion object {
        val allPermissions = listOfNotNull(
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION,
            if (SDK_INT >= VERSION_CODES.Q) ACCESS_BACKGROUND_LOCATION else null
        )
        val neededPermissions = listOfNotNull(
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION,
        )
        const val GPS_PERMISSIONS = "GPS_PERMISSIONS"
    }
}