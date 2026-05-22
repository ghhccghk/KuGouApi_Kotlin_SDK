package com.ghhccghk.multiplatform.kugouapi.shared

import com.ghhccghk.multiplatform.kugouapi.shared.api.SearchApi
import com.ghhccghk.multiplatform.kugouapi.shared.core.CookieJar
import com.ghhccghk.multiplatform.kugouapi.shared.core.RequestExecutor

/**
 * KuGou Music API Kotlin Multiplatform SDK
 *
 * Usage:
 * ```
 * val client = KuGouClient()
 * val result = client.search.search("周杰伦")
 * println(result.body)
 * ```
 */
class KuGouClient(val config: KuGouConfig = KuGouConfig()) : AutoCloseable {

    val cookieJar: CookieJar = CookieJar(config)
    private val executor = RequestExecutor(config, cookieJar)

//    val auth = AuthApi(executor)
    val search = SearchApi(executor)
//    val song = SongApi(executor)
//    val playlist = PlaylistApi(executor)
//    val album = AlbumApi(executor)
//    val artist = ArtistApi(executor)
//    val rank = RankApi(executor)
//    val recommend = RecommendApi(executor)
//    val comment = CommentApi(executor)
//    val radio = RadioApi(executor)
//    val sceneMusic = SceneMusicApi(executor)
//    val user = UserApi(executor)
//    val image = ImageApi(executor)
//    val video = VideoApi(executor)
//    val longAudio = LongAudioApi(executor)
//    val misc = MiscApi(executor)

    override fun close() {
        executor.close()
    }
}
