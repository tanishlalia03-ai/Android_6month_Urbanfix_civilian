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
    private val client = Client(context)
        .setEndpoint("https://fra.cloud.appwrite.io/v1")
        .setProject("6996dc3e00250d7ae563")

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

    // Existing upload function
    suspend fun uploadImage(bucketId: String, file: File): io.appwrite.models.File {
        return storage.createFile(
            bucketId = bucketId,
            fileId = ID.unique(),
            file = InputFile.fromFile(file)
        )
    }

    // NEW HELPER: Converts Uri to File without touching other code
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}