package com.example.repository

import android.content.Context
import com.example.logging.LogKeeper
import com.example.model.ElementSelectors
import com.example.model.ModuleConfig
import org.json.JSONObject
import java.io.File

object ModuleRepository {
    private const val MODULES_DIR = "modules"

    fun saveModule(context: Context, config: ModuleConfig): Boolean {
        return try {
            val dir = File(context.filesDir, MODULES_DIR)
            if (!dir.exists()) dir.mkdirs()

            val json = JSONObject().apply {
                put("id", config.id)
                put("name", config.name)
                put("url", config.url)
                put("thread_type", config.threadType)
                put("selectors", JSONObject().apply {
                    put("conversation_container", config.selectors.conversationContainer)
                    put("input_box", config.selectors.inputBox)
                })
                put("send_method", config.sendMethod)
                put("login_check", config.loginCheck)
                put("is_google_only_auth", config.isGoogleOnlyAuth)
            }

            val file = File(dir, "${config.id}.json")
            file.writeText(json.toString(2))
            LogKeeper.log(context, "MODULE_SAVED", "Saved module JSON: ${config.name} (${config.id})")
            true
        } catch (e: Exception) {
            LogKeeper.log(context, "MODULE_SAVE_ERROR", "Failed to save module: ${e.message}", e)
            false
        }
    }

    fun getSavedModules(context: Context): List<ModuleConfig> {
        val modules = mutableListOf<ModuleConfig>()
        try {
            val dir = File(context.filesDir, MODULES_DIR)
            if (dir.exists()) {
                dir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
                    val json = JSONObject(file.readText())
                    val selectorsObj = json.optJSONObject("selectors")
                    val selectors = ElementSelectors(
                        conversationContainer = selectorsObj?.optString("conversation_container") ?: "",
                        inputBox = selectorsObj?.optString("input_box") ?: ""
                    )
                    modules.add(
                        ModuleConfig(
                            id = json.getString("id"),
                            name = json.getString("name"),
                            url = json.getString("url"),
                            threadType = json.optString("thread_type", "chat"),
                            selectors = selectors,
                            sendMethod = json.optString("send_method", "enter_key"),
                            loginCheck = json.optString("login_check", ""),
                            isGoogleOnlyAuth = json.optBoolean("is_google_only_auth", false)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            LogKeeper.log(context, "MODULE_READ_ERROR", "Failed to load modules: ${e.message}", e)
        }
        return modules
    }
}
