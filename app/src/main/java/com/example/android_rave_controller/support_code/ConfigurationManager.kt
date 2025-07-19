package com.example.android_rave_controller.support_code

import android.content.Context
import android.util.Log
import com.example.android_rave_controller.models.RaveConfiguration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException

object ConfigurationManager {

    private val gson = Gson()

    private fun getFile(context: Context, filename: String): File {
        val properFilename = if (filename.endsWith(".json")) filename else "$filename.json"
        return File(context.filesDir, properFilename)
    }

    fun getSavedConfigurations(context: Context): Array<String> {
        return context.filesDir.listFiles { _, name -> name.endsWith(".json") }
            ?.map { it.name.removeSuffix(".json") }
            ?.sorted() // Sort them alphabetically
            ?.toTypedArray() ?: emptyArray()
    }

    fun saveConfiguration(context: Context, configuration: RaveConfiguration, filename: String) {
        if (filename.isBlank()) {
            Log.e("ConfigManager", "Filename cannot be blank.")
            return
        }
        try {
            val jsonString = gson.toJson(configuration)
            val file = getFile(context, filename)
            file.writeText(jsonString)
            Log.d("ConfigManager", "Configuration saved to ${file.name}")
        } catch (e: IOException) {
            Log.e("ConfigManager", "Error saving configuration", e)
        }
    }

    fun loadConfiguration(context: Context, filename: String): RaveConfiguration? {
        return try {
            val file = getFile(context, filename)
            if (!file.exists()) {
                Log.d("ConfigManager", "Configuration file not found: $filename")
                return null
            }
            val jsonString = file.readText()
            val type = object : TypeToken<RaveConfiguration>() {}.type
            gson.fromJson(jsonString, type)
        } catch (e: Exception) { // Catch broader exceptions for GSON parsing etc.
            Log.e("ConfigManager", "Error loading configuration", e)
            null
        }
    }

    // New function to delete a configuration file
    fun deleteConfiguration(context: Context, filename: String): Boolean {
        return try {
            val file = getFile(context, filename)
            if (file.exists()) {
                file.delete()
            } else {
                Log.w("ConfigManager", "File not found for deletion: $filename")
                false
            }
        } catch (e: Exception) {
            Log.e("ConfigManager", "Error deleting configuration: $filename", e)
            false
        }
    }

    // New function to rename a configuration file
    fun renameConfiguration(context: Context, oldFilename: String, newFilename: String): Boolean {
        if (newFilename.isBlank()) return false
        val oldFile = getFile(context, oldFilename)
        val newFile = getFile(context, newFilename)
        if (!oldFile.exists() || newFile.exists()) {
            Log.w("ConfigManager", "Rename failed. Old file not found or new file already exists.")
            return false
        }
        return try {
            oldFile.renameTo(newFile)
        } catch (e: Exception) {
            Log.e("ConfigManager", "Error renaming file", e)
            false
        }
    }
}