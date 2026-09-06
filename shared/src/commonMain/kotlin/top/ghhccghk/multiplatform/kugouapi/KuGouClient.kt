package top.ghhccghk.multiplatform.kugouapi

import top.ghhccghk.multiplatform.kugouapi.api.*
import top.ghhccghk.multiplatform.kugouapi.core.*

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

    // 原有API模块
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
    val sheet = SheetApi(executor)
    val audioMatch = AudioMatchApi(executor)
    val yueku = YuekuApi(executor)
    val youth = YouthApi(executor)
    val top = TopApi(executor)
    val effects = EffectsApi(executor)
    val listenTogether = ListenTogetherApi(executor)
    val userCloud = UserCloudApi(executor)
    val blacklist = BlacklistApi(executor)
    val songAuth = SongAuthApi(executor)
    val playlistEffect = PlaylistEffectApi(executor)
    val userPurchased = UserPurchasedApi(executor)
    val userUpdate = UserUpdateApi(executor)
}
