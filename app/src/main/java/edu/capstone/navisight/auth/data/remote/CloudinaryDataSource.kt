package edu.capstone.navisight.auth.data.remote

import android.content.Context
import android.net.Uri
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Properties

object CloudinaryDataSource {

    private lateinit var cloudinary: Cloudinary
    private lateinit var appContext: Context

    fun init(context: Context) {
        val props = Properties()
        context.assets.open("cloudinary.properties").use { props.load(it) }

        cloudinary = Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", props.getProperty("CLOUD_NAME"),
                "api_key", props.getProperty("API_KEY"),
                "api_secret", props.getProperty("API_SECRET")
            )
        )

        appContext = context.applicationContext
    }

    suspend fun uploadImage(fileUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            if (!::cloudinary.isInitialized) {
                throw IllegalStateException("CloudinaryDataSource not initialized. Call init(context) first.")
            }

            val tempFile = createTempFileFromUri(appContext, fileUri)
            val result = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap())
            tempFile.delete() // Clean up after upload

            result["secure_url"] as? String
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createTempFileFromUri(context: Context, uri: Uri): File {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }
}
