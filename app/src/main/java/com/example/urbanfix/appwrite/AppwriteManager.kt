package com.example.urbanfix.appwrite

import android.content.Context
import android.net.Uri
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.services.Storage
import io.appwrite.models.InputFile
import java.io.File
import java.io.FileOutputStream

class AppwriteManager private constructor(context: Context) {

    private val endpoint = "https://fra.cloud.appwrite.io/v1"
    private val projectId = "6996dc3e00250d7ae563"

    private val client = Client(context)
        .setEndpoint(endpoint)
        .setProject(projectId)

    private val storage = Storage(client)

    companion object {
        @Volatile
        private var INSTANCE: AppwriteManager? = null

        fun getInstance(context: Context): AppwriteManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppwriteManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * THE ALL-IN-ONE FUNCTION
     * 1. Converts Uri to File
     * 2. Uploads to Appwrite
     * 3. Returns the final Viewable URL
     */
    suspend fun uploadAndGetUrl(context: Context, bucketId: String, uri: Uri): String? {
        return try {
            // Step 1: Conversion
            val file = getFileFromUri(context, uri) ?: return null

            // Step 2: Upload
            val uploadedFile = storage.createFile(
                bucketId = bucketId,
                fileId = ID.unique(),
                file = InputFile.fromFile(file)
            )

            // Step 3: URL Generation
            "$endpoint/storage/buckets/$bucketId/files/${uploadedFile.id}/view?project=$projectId"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Keep this private as it's now handled by the function above
    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                inputStream.use { input -> input.copyTo(output) }
            }
            tempFile
        } catch (e: Exception) { null }
    }
}