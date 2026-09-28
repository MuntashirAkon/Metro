package code.name.monkey.retromusic.lrclib

import android.util.Log
import code.name.monkey.retromusic.model.Song
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object LrclibClient {
    private const val TAG = "LRCLIB"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Cloudflare/LRCLIB can behave differently with HTTP/2 from some Android networks.
            // Force HTTP/1.1 to make the Android request behave like the working curl request.
            .protocols(listOf(Protocol.HTTP_1_1))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Android; Metro LRCLIB)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val api: LrclibApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/api/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrclibApi::class.java)
    }

    sealed class Result {
        data class Success(val lyrics: String) : Result()
        data class NotFound(val message: String = "Track not found") : Result()
        data class HttpError(val code: Int, val message: String) : Result()
        data class NetworkError(val message: String, val cause: Throwable? = null) : Result()
        data class InvalidResponse(val message: String) : Result()
    }

    /**
     * Looks up synchronized lyrics using LRCLIB's metadata endpoint.
     * Errors are returned explicitly instead of being converted to null.
     */
    suspend fun getSyncedLyrics(song: Song): Result {
        val durationSeconds = (song.duration / 1000L).coerceIn(1L, 3600L)

        Log.d(TAG, "Searching: title='${song.title}', artist='${song.artistName}', album='${song.albumName}', duration=${durationSeconds}s")

        return try {
            var response = request(song, durationSeconds)

            if (response.code() == 429) {
                val retryAfterSeconds = response.headers()["Retry-After"]?.toLongOrNull()
                Log.w(TAG, "HTTP 429 Too Many Requests. Retry-After=$retryAfterSeconds")

                if (retryAfterSeconds != null && retryAfterSeconds > 0) {
                    delay((retryAfterSeconds * 1000L).coerceAtMost(30_000L))
                    response = request(song, durationSeconds)
                }
            }

            handleResponse(response)
        } catch (e: IOException) {
            Log.e(TAG, "Network error while contacting LRCLIB", e)
            Result.NetworkError(
                message = e.message ?: e.javaClass.simpleName,
                cause = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected LRCLIB error", e)
            Result.NetworkError(
                message = e.message ?: e.javaClass.simpleName,
                cause = e
            )
        }
    }

    private suspend fun request(song: Song, durationSeconds: Long): Response<LrclibResponse> {
        return api.getLyrics(
            trackName = song.title,
            artistName = song.artistName,
            albumName = song.albumName.takeIf { it.isNotBlank() },
            duration = durationSeconds
        )
    }

    private fun handleResponse(response: Response<LrclibResponse>): Result {
        val code = response.code()
        val raw = response.raw()
        val requestUrl = raw.request().url().toString()
        Log.d(TAG, "LRCLIB request URL: $requestUrl")
        Log.d(TAG, "LRCLIB protocol: ${raw.protocol()}")
        Log.d(TAG, "LRCLIB HTTP response: $code")

        if (code == 404) {
            Log.d(TAG, "Track not found on LRCLIB")
            return Result.NotFound()
        }

        if (!response.isSuccessful) {
            val errorBody = try {
                response.errorBody()?.string()?.take(500)
            } catch (_: Exception) {
                null
            }

            val message = errorBody?.takeIf { it.isNotBlank() }
                ?: response.message().takeIf { it.isNotBlank() }
                ?: "HTTP $code"

            Log.e(TAG, "LRCLIB HTTP error: $code - $message")
            return Result.HttpError(code, message)
        }

        val body = response.body()
            ?: return Result.InvalidResponse("LRCLIB returned an empty response body")

        if (body.instrumental == true) {
            Log.d(TAG, "Track is marked as instrumental")
            return Result.NotFound("Track is instrumental")
        }

        val syncedLyrics = body.syncedLyrics?.takeIf { it.isNotBlank() }
        if (syncedLyrics == null) {
            if (!body.plainLyrics.isNullOrBlank()) {
                Log.d(TAG, "LRCLIB returned plain lyrics but no synchronized lyrics")
                return Result.NotFound("Lyrics found, but no synchronized lyrics are available")
            }

            Log.d(TAG, "LRCLIB returned no lyrics")
            return Result.NotFound("No lyrics available")
        }

        Log.d(TAG, "Synchronized lyrics found (${syncedLyrics.length} characters)")
        return Result.Success(syncedLyrics)
    }
}
