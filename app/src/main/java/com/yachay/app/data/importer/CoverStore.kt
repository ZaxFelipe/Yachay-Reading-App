package com.yachay.app.data.importer

import android.content.Context
import android.graphics.Bitmap
import java.io.File

/** Capas extraídas na importação, salvas em `filesDir/covers/{bookId}.jpg`. */
class CoverStore(context: Context) {

    private val dir = File(context.filesDir, "covers").apply { mkdirs() }

    /** Redimensiona para no máximo [MAX_WIDTH] px de largura e grava como JPEG. */
    fun save(bookId: Long, bitmap: Bitmap): String {
        val scaled = if (bitmap.width > MAX_WIDTH) {
            val height = bitmap.height * MAX_WIDTH / bitmap.width
            Bitmap.createScaledBitmap(bitmap, MAX_WIDTH, height, true)
        } else {
            bitmap
        }
        val file = File(dir, "$bookId.jpg")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (scaled !== bitmap) scaled.recycle()
        return file.absolutePath
    }

    fun delete(bookId: Long) {
        File(dir, "$bookId.jpg").delete()
    }

    private companion object {
        const val MAX_WIDTH = 480
    }
}
