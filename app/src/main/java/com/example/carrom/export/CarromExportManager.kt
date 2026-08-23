package com.example.carrom.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.carrom.data.local.entity.MatchEntity
import com.example.carrom.data.local.entity.PlayerEntity
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object CarromExportManager {

    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Creates a temporary CSV file in the cache directory and returns its contentUri for sharing.
     */
    fun createTempCsvFile(context: Context, csvContent: String, prefix: String): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "${prefix}_${fileDateFormat.format(Date())}.csv"
        val file = File(exportDir, fileName)
        file.writeText(csvContent, Charsets.UTF_8)
        return file
    }

    /**
     * Creates a temporary JSON file in the cache directory and returns the file.
     */
    fun createTempJsonFile(context: Context, jsonContent: String, prefix: String = "Carrom_Backup"): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "${prefix}_${fileDateFormat.format(Date())}.json"
        val file = File(exportDir, fileName)
        file.writeText(jsonContent, Charsets.UTF_8)
        return file
    }

    /**
     * Gets a shareable content Uri via FileProvider.
     */
    fun getContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Triggers the Android system share sheet for a file.
     */
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String = "Share Carrom Data") {
        val uri = getContentUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Writes raw string content to a content Uri (e.g. from CreateDocument launcher).
     */
    fun writeStringToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Copies a source file (such as a generated PDF) to a target Uri (from CreateDocument).
     */
    fun copyFileToUri(context: Context, sourceFile: File, destinationUri: Uri): Boolean {
        return try {
            FileInputStream(sourceFile).use { input ->
                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads text content from a selected content Uri (from GetContent / OpenDocument).
     */
    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
