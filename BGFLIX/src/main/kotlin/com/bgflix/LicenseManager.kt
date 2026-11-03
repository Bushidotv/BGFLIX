package com.bgflix

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.widget.Toast
import com.lagradost.cloudstream3.app
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Manages license validation against the BGFLIX License API.
 *
 * - Stores the license key in SharedPreferences
 * - Generates a unique device HWID from Android ID (SHA-256 hashed)
 * - Validates license before any content is served
 * - Caches validation result for 5 minutes to avoid excessive API calls
 */
object LicenseManager {

    private const val PREFS_NAME = "bgflix_license_prefs"
    private const val KEY_LICENSE = "license_key"
    private const val KEY_CACHED_VALID = "cached_valid"
    private const val KEY_CACHED_TIME = "cached_time"
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes

    private const val VERIFY_URL = "https://lisans-api-six.vercel.app/validate_license"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getPrefs(): SharedPreferences? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ---- License Key ----

    fun getLicenseKey(): String {
        return getPrefs()?.getString(KEY_LICENSE, "") ?: ""
    }

    fun saveLicenseKey(key: String) {
        getPrefs()?.edit()?.putString(KEY_LICENSE, key.trim())?.apply()
        // Clear cache when key changes so next check hits the API
        clearCache()
    }

    // ---- Hardware ID ----

    @SuppressLint("HardwareIds")
    fun getHwid(): String {
        val ctx = appContext ?: return "unknown_device"
        val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            ?: return "unknown_device"
        // SHA-256 hash for privacy
        return sha256(androidId)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ---- Cache ----

    private fun clearCache() {
        getPrefs()?.edit()
            ?.remove(KEY_CACHED_VALID)
            ?.remove(KEY_CACHED_TIME)
            ?.apply()
    }

    private fun getCachedResult(): Boolean? {
        val prefs = getPrefs() ?: return null
        val cachedTime = prefs.getLong(KEY_CACHED_TIME, 0L)
        if (System.currentTimeMillis() - cachedTime > CACHE_DURATION_MS) return null
        return prefs.getBoolean(KEY_CACHED_VALID, false)
    }

    private fun setCachedResult(valid: Boolean) {
        getPrefs()?.edit()
            ?.putBoolean(KEY_CACHED_VALID, valid)
            ?.putLong(KEY_CACHED_TIME, System.currentTimeMillis())
            ?.apply()
    }

    // ---- Validation ----

    /**
     * Validates the license against the API.
     * Returns true if license is valid, false otherwise.
     * Uses a 5-minute cache to avoid spamming the API.
     *
     * @throws RuntimeException with user-friendly message if license is invalid
     */
    suspend fun validateOrThrow() {
        val licenseKey = getLicenseKey()
        if (licenseKey.isBlank()) {
            throw RuntimeException("BGFLIX: License key not set. Please enter your license key in plugin settings.")
        }

        // Check cache first
        val cached = getCachedResult()
        if (cached == true) return
        if (cached == false) {
            throw RuntimeException("BGFLIX: License invalid or expired. Please check your license key in plugin settings.")
        }

        // Hit the API
        val hwid = getHwid()
        try {
            val response = app.get(
                "$VERIFY_URL?license_key=$licenseKey&hwid=$hwid",
                timeout = 10
            ).text

            val json = JSONObject(response)
            val valid = json.optBoolean("valid", false)
            val reason = json.optString("reason", "unknown")

            setCachedResult(valid)

            if (!valid) {
                val message = when (reason) {
                    "license_not_found" -> "BGFLIX: License key not found."
                    "license_expired" -> "BGFLIX: License has expired."
                    "device_limit_reached" -> "BGFLIX: Device limit reached for this license."
                    else -> "BGFLIX: License invalid ($reason)."
                }
                throw RuntimeException(message)
            }
        } catch (e: RuntimeException) {
            // Re-throw our own RuntimeExceptions (license errors)
            throw e
        } catch (e: Exception) {
            // Network errors: fail-closed (block access)
            throw RuntimeException("BGFLIX: Could not verify license. Check your internet connection.")
        }
    }

    /**
     * Shows a toast on the main thread.
     */
    fun showToast(context: Context?, message: String) {
        val ctx = context ?: appContext ?: return
        android.os.Handler(ctx.mainLooper).post {
            Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
        }
    }
}