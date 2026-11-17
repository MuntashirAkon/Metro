package code.name.monkey.retromusic.innertube

import code.name.monkey.retromusic.model.Song
import java.util.Locale

class InnerTubeClient(
    private val api: InnerTubeApi,
) {
    private val apiKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilwYE_0M0Dg"

    private val contextPayload = mapOf(
        "client" to mapOf(
            "clientName" to "WEB",
            "clientVersion" to "2.20240709.00.00",
            "hl" to "en",
            "gl" to "US"
        )
    )

    suspend fun search(query: String): List<Song> {
        if (query.isBlank()) return emptyList()

        val body = mapOf(
            "context" to contextPayload,
            "query" to query,
            "params" to "EgIQAQ%3D%3D"
        )

        val response = api.search(apiKey, body)
        if (response.error != null) return emptyList()

        val videos = response.contents
            ?.twoColumnSearchResultsRenderer
            ?.primaryContents
            ?.sectionListRenderer
            ?.contents
            ?.flatMap { section ->
                val itemList = mutableListOf<VideoRenderer>()
                section.richItemRenderer?.content?.videoRenderer?.let { itemList.add(it) }
                section.itemSectionRenderer?.contents?.mapNotNull { it.videoRenderer }?.let(itemList::addAll)
                itemList
            }
            .orEmpty()

        return videos.mapNotNull { renderer ->
            val videoId = renderer.videoId ?: return@mapNotNull null
            val title = renderer.title?.runs?.joinToString("") { it.text.orEmpty() }
                ?: renderer.title?.simpleText
                ?: "Unknown title"
            val artist = renderer.longBylineText?.runs?.joinToString("") { it.text.orEmpty() }
                ?: "YouTube"
            val durationSeconds = parseDuration(renderer.lengthText?.simpleText)
            val streamUrl = getStreamUrl(videoId) ?: return@mapNotNull null

            Song(
                id = videoId.hashCode().toLong(),
                title = title,
                trackNumber = 0,
                year = 0,
                duration = durationSeconds * 1000L,
                data = streamUrl,
                dateModified = System.currentTimeMillis(),
                albumId = -1L,
                albumName = "",
                artistId = -1L,
                artistName = artist,
                composer = null,
                albumArtist = artist
            )
        }
    }

    private suspend fun getStreamUrl(videoId: String): String? {
        val response = api.player(
            apiKey,
            mapOf(
                "context" to contextPayload,
                "videoId" to videoId
            )
        )

        val formats = response.streamingData?.adaptiveFormats.orEmpty()
        return formats.firstOrNull {
            it.url != null && it.mimeType?.contains("audio", ignoreCase = true) == true
        }?.url ?: response.streamingData?.formats
            ?.firstOrNull { it.url != null && it.mimeType?.contains("audio", ignoreCase = true) == true }
            ?.url
    }

    private fun parseDuration(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return runCatching {
            val normalized = raw.trim()
            when {
                normalized.contains(":") -> {
                    val parts = normalized.split(":")
                    var total = 0L
                    var multiplier = 1L
                    for (part in parts.reversed()) {
                        total += part.toLong() * multiplier
                        multiplier *= 60L
                    }
                    total
                }
                else -> normalized.toLong()
            }
        }.getOrDefault(0L)
    }
}
