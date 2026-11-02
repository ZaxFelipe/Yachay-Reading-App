package com.yachay.app.ui.reader.cbz

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import com.yachay.app.data.importer.ReadiumServices
import com.yachay.app.domain.ComicPages
import com.yachay.app.ui.reader.PageSource
import com.yachay.app.ui.reader.pdf.PdfZoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.getOrElse

/**
 * Quadrinho CBZ: um ZIP com uma imagem por página. O ZIP é lido pelo container do Readium
 * (acesso aleatório direto na URI do SAF, sem copiar o arquivo); cada página é decodificada
 * com `inSampleSize` para a largura pedida. Páginas renderizadas ficam num LRU, como no PDF.
 */
class CbzDocument private constructor(
    private val asset: ContainerAsset,
    private val pages: List<Url>,
    override val pageAspectRatios: List<Float>,
) : PageSource {
    private val mutex = Mutex()
    private var closed = false

    private val cache = object : LruCache<Int, Bitmap>(cacheSizeBytes()) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount
    }

    override fun maxRenderWidth(index: Int): Int = minOf(
        PdfZoom.MAX_RENDER_WIDTH_PX,
        PdfZoom.memoryCapWidth(Runtime.getRuntime().maxMemory() / 10, pageAspectRatios[index]),
    )

    override suspend fun render(index: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (closed) return@withLock null
            cache.get(index)?.takeIf { it.width == widthPx }?.let { return@withLock it }
            val bytes = read(asset, pages[index]) ?: return@withLock null
            decode(bytes, widthPx)?.also { cache.put(index, it) }
        }
    }

    override suspend fun close() = mutex.withLock {
        if (closed) return@withLock
        closed = true
        cache.evictAll()
        asset.close()
    }

    companion object {
        /** Cabeçalho lido para medir a página sem baixar a imagem inteira. */
        private const val HEADER_BYTES = 64 * 1024L

        /** null se não for um ZIP legível ou não tiver nenhuma imagem. */
        suspend fun open(readium: ReadiumServices, uri: Uri): CbzDocument? = withContext(Dispatchers.IO) {
            val asset = readium.openArchive(uri) ?: return@withContext null
            runCatching {
                val byPath = asset.container.entries.associateBy { it.path ?: it.toString() }
                val pages = ComicPages.pages(byPath.keys).map { byPath.getValue(it) }
                val ratios = pages.map { aspectRatio(asset, it) }
                if (pages.isEmpty() || ratios.any { it == null }) null else CbzDocument(asset, pages, ratios.map { it!! })
            }.getOrNull() ?: run {
                asset.close()
                null
            }
        }

        /** Primeira página em [widthPx], para a capa na importação. */
        suspend fun cover(document: CbzDocument, widthPx: Int): Bitmap? = document.render(0, widthPx)?.let {
            // A capa vai para o disco e é reciclada: copia para não reciclar o bitmap do cache.
            it.copy(it.config ?: Bitmap.Config.ARGB_8888, false)
        }

        private suspend fun read(asset: ContainerAsset, url: Url, range: LongRange? = null): ByteArray? {
            val resource = asset.container[url] ?: return null
            return try {
                resource.read(range).getOrElse { null }
            } finally {
                resource.close()
            }
        }

        private suspend fun aspectRatio(asset: ContainerAsset, url: Url): Float? {
            fun bounds(bytes: ByteArray): Float? {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                return if (options.outWidth > 0 && options.outHeight > 0) options.outWidth.toFloat() / options.outHeight else null
            }
            // Quase sempre o tamanho está no começo do arquivo; se não, lê a imagem toda.
            read(asset, url, 0 until HEADER_BYTES)?.let(::bounds)?.let { return it }
            return read(asset, url)?.let(::bounds)
        }

        private fun decode(bytes: ByteArray, widthPx: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= widthPx) sample *= 2
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return null
            if (decoded.width == widthPx) return decoded
            val height = (widthPx.toLong() * decoded.height / decoded.width).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(decoded, widthPx, height, true).also { if (it !== decoded) decoded.recycle() }
        }

        private fun cacheSizeBytes(): Int =
            (Runtime.getRuntime().maxMemory() / 6).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
