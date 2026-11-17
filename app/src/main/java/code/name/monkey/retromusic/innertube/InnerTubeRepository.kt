package code.name.monkey.retromusic.innertube

import code.name.monkey.retromusic.model.Song

interface InnerTubeRepository {
    suspend fun search(query: String): List<Song>
}

class RealInnerTubeRepository(
    private val client: InnerTubeClient,
) : InnerTubeRepository {
    override suspend fun search(query: String): List<Song> = client.search(query)
}
