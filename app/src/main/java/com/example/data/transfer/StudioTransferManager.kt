package com.example.data.transfer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.data.local.ProjectEntity
import com.example.data.remote.SecurityManager
import com.example.engine.graphics.GameBackgroundType
import com.example.engine.graphics.GameCollectible
import com.example.engine.graphics.OneButtonGameEngine
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model for Level and Game Engine Presets
 */
data class GameLevelExportData(
    val title: String,
    val backgroundType: String,
    val controlMode: String,
    val gravity: Float,
    val speed: Float,
    val items: List<GameCollectible>,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
    val hardwareTarget: String = "Universal (itel A70 / Android 5+)"
)

/**
 * High-speed, robust Import / Export & Transfer Manager.
 * Supports:
 * - JSON Level & Code serialization/deserialization
 * - AES-256 encrypted Studio Packages (.studiopkg) with SHA-256 checksums
 * - Clipboard Copy/Paste
 * - Validation & Rollback security
 */
object StudioTransferManager {

    /**
     * Export Game Level to JSON String
     */
    fun exportLevelToJson(engine: OneButtonGameEngine, title: String = "Studio Custom Level"): String {
        val root = JSONObject()
        root.put("packageType", "STUDIO_GAME_LEVEL")
        root.put("version", "2.0")
        root.put("title", title)
        root.put("backgroundType", engine.currentBgType.name)
        root.put("controlMode", engine.controlMode)
        root.put("gravityDirection", engine.gravityDirection.toDouble())
        root.put("playerRadius", engine.playerRadius.toDouble())
        root.put("maxJumps", engine.maxJumps)
        root.put("createdAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        root.put("targetDevice", "itel A70 / Android 5+ Optimized")

        val itemsArray = JSONArray()
        for (item in engine.items) {
            val itemObj = JSONObject()
            itemObj.put("id", item.id)
            itemObj.put("x", item.x.toDouble())
            itemObj.put("y", item.y.toDouble())
            itemObj.put("type", item.type)
            itemObj.put("size", item.size.toDouble())
            itemsArray.put(itemObj)
        }
        root.put("items", itemsArray)

        val jsonStr = root.toString(2)
        val checksum = SecurityManager.computeSha256Checksum(jsonStr)
        root.put("checksum", checksum)

        return root.toString(2)
    }

    /**
     * Import JSON String into Game Engine with robust error recovery
     */
    fun importLevelFromJson(jsonString: String, engine: OneButtonGameEngine): Result<String> {
        return try {
            val root = JSONObject(jsonString)
            val title = root.optString("title", "Imported Level")
            val bgName = root.optString("backgroundType", "STATIC_BG")
            val ctrlMode = root.optString("controlMode", "JUMP")
            val gravityDir = root.optDouble("gravityDirection", 1.0).toFloat()
            val radius = root.optDouble("playerRadius", 18.0).toFloat()
            val maxJ = root.optInt("maxJumps", 2)

            // Update Engine State
            engine.currentBgType = try {
                GameBackgroundType.valueOf(bgName)
            } catch (e: Exception) {
                GameBackgroundType.STATIC_BG
            }
            engine.controlMode = ctrlMode
            engine.gravityDirection = gravityDir
            engine.playerRadius = radius
            engine.maxJumps = maxJ

            // Parse Items
            val itemsArray = root.optJSONArray("items")
            if (itemsArray != null) {
                engine.items.clear()
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    engine.items.add(
                        GameCollectible(
                            id = obj.optInt("id", i + 1),
                            x = obj.optDouble("x", 200.0 + i * 200.0).toFloat(),
                            y = obj.optDouble("y", 300.0).toFloat(),
                            type = obj.optString("type", "COIN"),
                            size = obj.optDouble("size", 22.0).toFloat()
                        )
                    )
                }
            }

            Result.success("✔ Successfully loaded '$title' with ${engine.items.size} collectibles & $bgName background.")
        } catch (e: Exception) {
            Result.failure(Exception("JSON Parsing Error: ${e.message}"))
        }
    }

    /**
     * Export Full Project / Code to Encrypted Studio Package String (.studiopkg)
     */
    fun exportEncryptedProject(
        title: String,
        category: String,
        language: String,
        code: String,
        passphrase: String = "StudioAIEngine2026!"
    ): String {
        val root = JSONObject()
        root.put("exportType", "STUDIO_ENCRYPTED_BUNDLE")
        root.put("title", title)
        root.put("category", category)
        root.put("language", language)
        root.put("code", code)
        root.put("timestamp", System.currentTimeMillis())

        val rawJson = root.toString()
        val checksum = SecurityManager.computeSha256Checksum(rawJson)
        val encryptedPayload = SecurityManager.encrypt(rawJson, passphrase)

        val packageWrapper = JSONObject()
        packageWrapper.put("format", "STUDIO_PKG_V2")
        packageWrapper.put("encryptedPayload", encryptedPayload)
        packageWrapper.put("checksum", checksum)
        packageWrapper.put("encryption", "AES-256-CBC")

        return packageWrapper.toString(2)
    }

    /**
     * Import Encrypted Studio Package String
     */
    fun importEncryptedProject(
        pkgString: String,
        passphrase: String = "StudioAIEngine2026!"
    ): Result<ProjectEntity> {
        return try {
            val wrapper = JSONObject(pkgString)
            val encPayload = wrapper.getString("encryptedPayload")
            val expectedChecksum = wrapper.optString("checksum", "")

            val decryptedJson = SecurityManager.decrypt(encPayload, passphrase)
            val calculatedChecksum = SecurityManager.computeSha256Checksum(decryptedJson)

            if (expectedChecksum.isNotBlank() && expectedChecksum != calculatedChecksum) {
                return Result.failure(Exception("Integrity Check Failed: Checksum mismatch."))
            }

            val data = JSONObject(decryptedJson)
            val project = ProjectEntity(
                title = data.optString("title", "Imported Project"),
                category = data.optString("category", "GAME"),
                language = data.optString("language", "KOTLIN"),
                codeContent = data.optString("code", "// Empty code"),
                description = "Imported from Studio Package V2 on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}",
                isSynced = false
            )
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(Exception("Decryption/Import Failed: ${e.message}"))
        }
    }

    /**
     * Copy text to Android Clipboard
     */
    fun copyToClipboard(context: Context, label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Read text from Android Clipboard
     */
    fun readFromClipboard(context: Context): String {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).text?.toString() ?: ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }
}
