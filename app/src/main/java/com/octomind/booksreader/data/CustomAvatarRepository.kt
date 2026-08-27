package com.octomind.booksreader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.octomind.booksreader.domain.CustomAvatarPolicy
import java.io.File

class CustomAvatarRepository(private val context: Context) {
    private val avatarDirectory = File(context.filesDir, "narrator_avatars")
    val avatarFile: File = File(avatarDirectory, "custom_avatar.png")

    fun import(uri: Uri) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
        val fileBytes = resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0 }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: error("No fue posible leer la imagen")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "La imagen no contiene píxeles válidos" }
        require(CustomAvatarPolicy.accepts(mimeType, fileBytes, bounds.outWidth, bounds.outHeight)) {
            "Usa una imagen PNG, JPG o WebP de hasta 10 MB y al menos 128 px por lado"
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: error("La imagen no contiene píxeles válidos")

        avatarDirectory.mkdirs()
        val temporary = File(avatarDirectory, "custom_avatar.tmp")
        try {
            temporary.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "No fue posible preparar la imagen"
                }
            }
            temporary.copyTo(avatarFile, overwrite = true)
        } finally {
            bitmap.recycle()
            temporary.delete()
        }
    }

    fun delete() {
        if (avatarFile.exists() && !avatarFile.delete()) {
            error("No fue posible eliminar la imagen personalizada")
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > 2_048 || height / sample > 2_048) sample *= 2
        return sample
    }
}
