package code.name.monkey.retromusic.network

import android.content.Context
import code.name.monkey.retromusic.util.MusicUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * بيجيب نبذة (Biography) عن الفنان من Last.fm API، بنفس أسلوب
 * ArtistImageFetcher.kt بالظبط (اتصال HTTP مباشر + JSONObject من غير
 * Retrofit) عشان يفضل متسق مع باقي الكود.
 *
 * النص بيتحفظ في SharedPreferences بعد أول تحميل، فبيبقى متاح Offline
 * بعد كده بنفس فكرة الكاش بتاع الصور (Glide disk cache).
 *
 * Last.fm - على عكس Deezer - محتاج API key. تقدر تجيب واحد ببلاش من:
 * https://www.last.fm/api/account/create
 */
object LastFmBiographyFetcher {

    // ملحوظة: الـ API key ده مكشوف دلوقتي في الكود مباشرة بناءً على طلبك -
    // لما تكون جاهز تخبيه (BuildConfig / local.properties) قولي وأظبطها.
    private const val API_KEY = "3d5376f1ce3b00fa7507d3616c479fa1"
    private const val PREFS_NAME = "lastfm_biography_cache"

    suspend fun fetchBiography(context: Context, artistName: String): String? =
        withContext(Dispatchers.IO) {
            if (MusicUtil.isArtistNameUnknown(artistName) || API_KEY.isBlank()) {
                return@withContext null
            }

            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cacheKey = artistName.trim().lowercase()

            // بنجرب الكاش الأول عشان النبذة تشتغل Offline بعد أول تحميل
            prefs.getString(cacheKey, null)?.let { cached ->
                return@withContext cached
            }

            try {
                val query = URLEncoder.encode(artistName, "UTF-8")
                val url = URL(
                    "https://ws.audioscrobbler.com/2.0/?method=artist.getinfo" +
                            "&artist=$query&api_key=$API_KEY&format=json&autocorrect=1"
                )

                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }

                val json = try {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }

                val artistObject = JSONObject(json).optJSONObject("artist")
                    ?: return@withContext null
                val bioObject = artistObject.optJSONObject("bio") ?: return@withContext null
                val summary = bioObject.optString("summary")

                val bio = cleanBiography(summary)
                if (bio.isNotEmpty()) {
                    prefs.edit().putString(cacheKey, bio).apply()
                    bio
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Last.fm بيرجع النص بـ HTML tags، وبيضيف في الآخر لينك
     * "<a href=...>Read more on Last.fm</a>" - بنشيلهم عشان يبان
     * كنص عادي نضيف من غير HTML أو لينكات.
     */
    private fun cleanBiography(rawSummary: String): String {
        return rawSummary
            .replace(Regex("<a[^>]*>.*?</a>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]+>"), "")
            .trim()
    }
}
