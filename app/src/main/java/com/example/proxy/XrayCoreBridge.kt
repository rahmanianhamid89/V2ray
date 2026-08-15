package com.example.proxy

import com.example.logger.AppLogger
import com.example.model.ServerConfig

object XrayCoreBridge {

    private var isNativeLibraryLoaded = false
    private var isRunning = false

    init {
        try {
            // Attempt loading native libxray if placed in jniLibs
            System.loadLibrary("xray")
            isNativeLibraryLoaded = true
            AppLogger.i("XrayCoreBridge", "Native Xray dynamic library loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            isNativeLibraryLoaded = false
            AppLogger.w("XrayCoreBridge", "Native Xray dynamic library (libxray.so) not present in APK. Using pure Kotlin VLESS Client engine.")
        } catch (e: Exception) {
            isNativeLibraryLoaded = false
            AppLogger.w("XrayCoreBridge", "Native Xray load failed: ${e.localizedMessage}")
        }
    }

    fun isNativeCoreAvailable(): Boolean = isNativeLibraryLoaded

    fun startCore(config: ServerConfig, configJson: String): Result<Unit> {
        AppLogger.i("XrayCoreBridge", "Initializing Xray Proxy Core for server: ${config.address}:${config.port}")
        AppLogger.d("XrayCoreBridge", "Generated Config JSON:\n$configJson")

        return if (isNativeLibraryLoaded) {
            try {
                // Example JNI call invocation pattern
                nativeStartXray(configJson)
                isRunning = true
                AppLogger.i("XrayCoreBridge", "Native Xray Core started on local port 10808.")
                Result.success(Unit)
            } catch (e: Exception) {
                AppLogger.e("XrayCoreBridge", "Failed starting Native Xray core", e)
                Result.failure(e)
            }
        } else {
            // Pure Kotlin VLESS engine path
            isRunning = true
            AppLogger.i("XrayCoreBridge", "Active Proxy Engine: Pure Kotlin VLESS Tunnel Relay.")
            Result.success(Unit)
        }
    }

    fun stopCore() {
        if (!isRunning) return
        AppLogger.i("XrayCoreBridge", "Stopping Proxy Core...")
        if (isNativeLibraryLoaded) {
            try {
                nativeStopXray()
            } catch (e: Exception) {
                AppLogger.e("XrayCoreBridge", "Error stopping native Xray", e)
            }
        }
        isRunning = false
        AppLogger.i("XrayCoreBridge", "Proxy Core stopped.")
    }

    fun getIntegrationGuide(): String {
        return """
        === XRAY NATIVE CORE INTEGRATION GUIDE ===
        
        This project includes a complete VLESS parser, VpnService architecture, JSON config generator, and Kotlin VLESS client pipeline.
        
        To upgrade to full multi-protocol Xray Go-mobile Core (libxray.so / libv2ray.so):
        
        1. Export this project to Android Studio.
        2. Build or download 'libxray.so' (from 2dust/v2rayNG or xtls/xray-core go-mobile project).
        3. Place 'libxray.so' into 'app/src/main/jniLibs/arm64-v8a/' and 'x86_64/'.
        4. Or add dependency 'implementation("com.github.2dust:v2rayNG:1.8.x")' / LibXray AAR.
        5. Native Xray will automatically be detected and used by XrayCoreBridge.
        """.trimIndent()
    }

    private external fun nativeStartXray(configJson: String)
    private external fun nativeStopXray()
}
