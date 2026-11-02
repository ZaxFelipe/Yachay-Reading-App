package com.yachay.app.data.importer

import android.content.Context
import android.net.Uri
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.asset.ContainerAsset
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** Ponto único de acesso ao Readium para abrir EPUBs (e ZIPs de CBZ) a partir de URIs do SAF. */
class ReadiumServices(context: Context) {

    private val httpClient = DefaultHttpClient()

    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)

    // PDF é tratado pelo PdfRenderer nativo, então não registramos um adaptador de PDF aqui.
    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context = context,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = null,
        ),
    )

    /**
     * Abre um arquivo ZIP (CBZ) como container de entradas com acesso aleatório; null se não
     * for um arquivo. O chamador fecha o asset.
     */
    suspend fun openArchive(uri: Uri): ContainerAsset? {
        val url = uri.toUrl() as? AbsoluteUrl ?: return null
        val asset = assetRetriever.retrieve(url).getOrElse { return null }
        return asset as? ContainerAsset ?: run {
            asset.close()
            null
        }
    }

    /** Abre a publicação; o chamador é responsável por `publication.close()`. */
    suspend fun open(uri: Uri): Publication? {
        val url = uri.toUrl() as? AbsoluteUrl ?: return null
        val asset = assetRetriever.retrieve(url).getOrElse { return null }
        return publicationOpener.open(asset, allowUserInteraction = false).getOrElse {
            asset.close()
            return null
        }
    }
}
