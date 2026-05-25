package com.ghhccghk.multiplatform.kugouapi

import com.ghhccghk.multiplatform.kugouapi.api.*
import com.ghhccghk.multiplatform.kugouapi.core.*

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
class KuGouClient(
    val config: KuGouConfig = KuGouConfig(),
    val cookieJar : CookieJar = CookieJar(config)
) {

    private val executor =
        RequestExecutor(config, cookieJar)

    val auth = AuthApi(executor)
    val search = SearchApi(executor)
    val album = AlbumApi(executor)
    val artist = ArtistApi(executor)
    val playlist = PlaylistApi(executor)
    val song = SongApi(executor)
    val comment = CommentApi(executor)
    val image = ImageApi(executor)
    val longAudio = LongAudioApi(executor)
    val rank = RankApi(executor)
    val sceneMusic = SceneMusicApi(executor)
    val misc = MiscApi(executor)
    val user = UserApi(executor)
    val video = VideoApi(executor)
    val radio = RadioApi(executor)
    val recommend = RecommendApi(executor)
}
