package com.example.urbanfix.Appwrite

import android.content.Context
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.services.Storage
import io.appwrite.models.InputFile
import java.io.File

class AppwriteManager private constructor(context: Context) {

    // 1. Initialize the Appwrite Client
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

    suspend fun uploadImage(bucketId: String, file: File): io.appwrite.models.File {
        return storage.createFile(
            bucketId = bucketId,
            fileId = ID.unique(),
            file = InputFile.fromFile(file)
        )
    }
}