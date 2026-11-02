package com.yachay.app.ui.reader.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import android.util.LruCache
import com.yachay.app.ui.reader.PageSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Wrapper sobre o [PdfRenderer] nativo. O renderer não é thread-safe e só permite uma página
 * aberta por vez, então todo acesso passa pelo [mutex]. Páginas renderizadas ficam num LRU.
 */
class PdfDocument private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    override val pageAspectRatios: List<Float>,
) : PageSource {
    private val mutex = Mutex()
    private var closed = false

    private val cache = object : LruCache<Int, Bitmap>(cacheSizeBytes()) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
    }

    /**
     * Maior largura de re-render (zoom) para a página: um bitmap nunca passa de 1/10 do heap,
     * nem de [PdfZoom.MAX_RENDER_WIDTH_PX]. Evita OutOfMemoryError ampliando 5×.
     */
    override fun maxRenderWidth(index: Int): Int = minOf(
        PdfZoom.MAX_RENDER_WIDTH_PX,
        PdfZoom.memoryCapWidth(Runtime.getRuntime().maxMemory() / 10, pageAspectRatios[index]),
    )

    override suspend fun render(index: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed) return@withLock null
            cache.get(index)?.takeIf { it.width == widthPx }?.let { return@withLock it }
            renderer.openPage(index).use { page ->
                val heightPx = (widthPx.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put(index, bitmap)
                }
            }
        }
    }

    /** Texto da página (só Android 15+, quando o PdfRenderer passou a expor o texto). */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun pageText(index: Int): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed) return@withLock ""
            renderer.openPage(index).use { page -> page.textContents.joinToString("\n") { it.text } }
        }
    }

    override suspend fun close() = mutex.withLock {
        if (closed) return@withLock
        closed = true
        cache.evictAll()
        renderer.close()
        descriptor.close()
    }

    companion object {
        /** Busca no PDF depende do texto do PdfRenderer, que só existe no Android 15+. */
        val supportsSearch: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM

        suspend fun open(context: Context, uri: Uri): PdfDocument? = withContext(Dispatchers.IO) {
            runCatching {
                val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@runCatching null
                val renderer = PdfRenderer(descriptor)
                val ratios = List(renderer.pageCount) { index ->
                    renderer.openPage(index).use { it.width.toFloat() / it.height.coerceAtLeast(1) }
                }
                PdfDocument(descriptor, renderer, ratios)
            }.getOrNull()
        }

        private fun cacheSizeBytes(): Int =
            (Runtime.getRuntime().maxMemory() / 6).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
