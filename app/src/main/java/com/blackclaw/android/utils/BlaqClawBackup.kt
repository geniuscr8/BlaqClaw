package com.blackclaw.android.utils

import android.util.Base64
import com.blackclaw.android.automation.AutomationProfileStore
import com.blackclaw.android.automation.AutomationRuleStore
import com.blackclaw.android.memory.UserMemoryStore
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable, password-protected backup for user configuration and local data. */
object BlaqClawBackup {
    private const val VERSION = 1
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12

    fun create(passphrase: CharArray): ByteArray {
        require(passphrase.size >= 8) { "Use at least 8 characters." }
        val payload = JSONObject().apply {
            put("format", "blaqclaw-backup")
            put("version", VERSION)
            put("createdAt", System.currentTimeMillis())
            put("themeId", KVUtils.getString("THEME_ID", "blaqclaw_dark"))
            put("globalPrompt", KVUtils.getGlobalPrompt())
            put("fastPath", KVUtils.getBoolean("cfg_fast_path", true))
            put("externalAutomation", KVUtils.isExternalAutomationEnabled())
            put("memory", UserMemoryStore.exportJson())
            put("automationProfiles", AutomationProfileStore.exportJson())
            put("automationRules", AutomationRuleStore.exportJson())
        }.toString().toByteArray(StandardCharsets.UTF_8)
        return encrypt(payload, passphrase)
    }

    fun restore(bytes: ByteArray, passphrase: CharArray): RestoreSummary {
        val root = JSONObject(String(decrypt(bytes, passphrase), StandardCharsets.UTF_8))
        require(root.optString("format") == "blaqclaw-backup") { "Not a BlaqClaw backup." }
        require(root.optInt("version") in 1..VERSION) { "Unsupported backup version." }
        KVUtils.putString("THEME_ID", root.optString("themeId", "blaqclaw_dark"))
        KVUtils.setGlobalPrompt(root.optString("globalPrompt", ""))
        KVUtils.putBoolean("cfg_fast_path", root.optBoolean("fastPath", true))
        KVUtils.setExternalAutomationEnabled(root.optBoolean("externalAutomation", false))
        val memory = UserMemoryStore.importJson(root.optJSONArray("memory") ?: org.json.JSONArray())
        val profiles = AutomationProfileStore.importJson(root.optJSONArray("automationProfiles") ?: org.json.JSONArray())
        val rules = AutomationRuleStore.importJson(root.optJSONArray("automationRules") ?: org.json.JSONArray())
        KVUtils.sync()
        return RestoreSummary(memory, profiles, rules)
    }

    data class RestoreSummary(val memories: Int, val profiles: Int, val rules: Int)

    private fun encrypt(plain: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(passphrase, salt), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plain)
        return JSONObject().apply {
            put("format", "blaqclaw-encrypted")
            put("version", VERSION)
            put("iterations", ITERATIONS)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun decrypt(bytes: ByteArray, passphrase: CharArray): ByteArray {
        val envelope = JSONObject(String(bytes, StandardCharsets.UTF_8))
        require(envelope.optString("format") == "blaqclaw-encrypted") { "Invalid backup file." }
        val salt = Base64.decode(envelope.getString("salt"), Base64.DEFAULT)
        val iv = Base64.decode(envelope.getString("iv"), Base64.DEFAULT)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(passphrase, salt), GCMParameterSpec(128, iv))
        return cipher.doFinal(Base64.decode(envelope.getString("data"), Base64.DEFAULT))
    }

    private fun key(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
