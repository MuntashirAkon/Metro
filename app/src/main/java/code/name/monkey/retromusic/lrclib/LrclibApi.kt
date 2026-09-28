package code.name.monkey.retromusic.lrclib

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LrclibApi {
    @GET("get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String?,
        @Query("duration") duration: Long?
    ): Response<LrclibResponse>
}
