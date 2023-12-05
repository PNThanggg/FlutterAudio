package com.pnt.flutter_audio

import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.window.SplashScreenView
import androidx.core.view.WindowCompat
import com.pnt.flutter_audio.constant.Constant
import com.pnt.flutter_audio.controller.MethodController
import com.pnt.flutter_audio.controller.PermissionController
import io.flutter.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    init {
        // Set default logging level
        Log.setLogLevel(Log.WARN)
    }

    companion object {
        // Get the current class name.
        private const val TAG: String = "MainActivity"

        // Method channel name.
        private const val CHANNEL_NAME = "pnt/audio_query"
    }

    private lateinit var permissionController: PermissionController
    private lateinit var methodController: MethodController

    private lateinit var channel: MethodChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aligns the Flutter view vertically with the window.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen
                .setOnExitAnimationListener { splashScreenView: SplashScreenView -> splashScreenView.remove() }
        }

        super.onCreate(savedInstanceState)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        PluginProvider.set(this@MainActivity)

        // Setup the method channel communication.
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler { call, result ->
            Log.d(TAG, "Started method call (${call.method})")

            // Init the plugin provider with current 'call' and 'result'.
            PluginProvider.setCurrentMethod(call, result)

            permissionController = PermissionController()
            methodController = MethodController()

            // If user deny permission request a pop up will immediately show up
            // If [retryRequest] is null, the message will only show when call method again
            val retryRequest = call.argument<Boolean>("retryRequest") ?: false
            permissionController.retryRequest = retryRequest

            Log.i(TAG, "Method call: ${call.method}")

            when (call.method) {
                // Permissions
                Constant.PERMISSION_STATUS -> {
                    val hasPermission = permissionController.permissionStatus()
                    result.success(hasPermission)
                }

                Constant.PERMISSION_REQUEST -> {
                    permissionController.requestPermission()
                }

                // Device information
                Constant.QUERY_DEVICE_INFO -> {
                    result.success(
                        hashMapOf<String, Any>(
                            "device_model" to Build.MODEL,
                            "device_sys_version" to Build.VERSION.SDK_INT,
                            "device_sys_type" to "Android"
                        )
                    )
                }

                // This method will scan the given path to update the 'state'.
                // When deleting a file using 'dart:io', call this method to update the file 'state'.
                Constant.SCAN -> {
                    val sPath: String? = call.argument<String>("path")
                    val context = PluginProvider.context()

                    // Check if the given file is null or empty.
                    if (sPath.isNullOrEmpty()) {
                        Log.w(TAG, "Method 'scan' was called with null or empty 'path'")
                        result.success(false)
                    }

                    // Scan and return
                    MediaScannerConnection.scanFile(context, arrayOf(sPath), null) { _, _ ->
                        Log.d(TAG, "Scanned file: $sPath")
                        result.success(true)
                    }
                }

                // Logging
                Constant.SET_LOG_CONFIG -> {
                    // Log level
                    Log.setLogLevel(call.argument<Int>("level")!!)

                    // Define if 'warn' level will show more detailed logging.
                    PluginProvider.showDetailedLog = call.argument<Boolean>("showDetailedLog")!!

                    result.success(true)
                }

                else -> {
                    Log.d(TAG, "Checking permissions...")

                    val hasPermission = permissionController.permissionStatus()
                    Log.d(TAG, "Application has permissions: $hasPermission")

                    if (!hasPermission) {
                        Log.w(TAG, "The application doesn't have access to the library")
                        result.error(
                            "MissingPermissions",
                            "Application doesn't have access to the library",
                            "Call the [permissionsRequest] method or install a external plugin to handle the app permission."
                        )
                    }

                    methodController.find()
                }
            }

            Log.d(TAG, "Ended method call (${call.method})")
        }
    }
}
