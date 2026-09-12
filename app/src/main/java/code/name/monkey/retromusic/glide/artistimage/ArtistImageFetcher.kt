package code.name.monkey.retromusic.glide.artistimage

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import code.name.monkey.retromusic.util.MusicUtil
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ArtistImageFetcher(
    private val context: Context,
    val model: ArtistImage,
) : DataFetcher<InputStream> {

    private var connection: HttpURLConnection? = null

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.REMOTE
    }

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val artistName = model.artist.name
        if (MusicUtil.isArtistNameUnknown(artistName)) {
            callback.onLoadFailed(FileNotFoundException("Unknown artist, skipping image fetch"))
            return
        }
        try {
            val stream = fetchDeezerArtistImage(artistName)
            if (stream != null) {
                callback.onDataReady(stream)
            } else {
                callback.onLoadFailed(FileNotFoundException("No Deezer image found for $artistName"))
            }
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }

    /**
     * Looks up [artistName] via Deezer's public search API and downloads
     * that artist's picture. Returns null if Deezer has no match.
     */
    private fun fetchDeezerArtistImage(artistName: String): InputStream? {
        val query = URLEncoder.encode(artistName, "UTF-8")
        val searchUrl = URL("https://api.deezer.com/search/artist?q=$query&limit=1")

        val searchConnection = (searchUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
        }

        val json = try {
            searchConnection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            searchConnection.disconnect()
        }

        val results = JSONObject(json).optJSONArray("data") ?: return null
        if (results.length() == 0) return null

        val artistObject = results.getJSONObject(0)
        val pictureUrl = artistObject.optString("picture_xl")
            .ifEmpty { artistObject.optString("picture_big") }
            .ifEmpty { artistObject.optString("picture_medium") }

        if (pictureUrl.isEmpty()) return null

        val imageConnection = (URL(pictureUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
        }
        connection = imageConnection
        return imageConnection.inputStream
    }

    override fun cleanup() {
        connection?.disconnect()
        connection = null
    }

    override fun cancel() {
        connection?.disconnect()
    }
}
