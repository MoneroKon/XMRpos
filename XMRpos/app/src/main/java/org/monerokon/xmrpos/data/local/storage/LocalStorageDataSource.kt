package org.monerokon.xmrpos.data.local.storage


import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class LocalStorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun saveImage(uri: Uri, fileName: String): File? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            file // Return the File object
        } catch (e: Exception) {
            e.printStackTrace()
            null // Return null if an exception occurs
        }
    }

    fun readImage(fileName: String): File? {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file else null
    }

    fun deleteImage(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.delete() else false
    }
}