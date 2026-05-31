package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 曲谱/乐谱相关 API
 *
 * 对齐 Node.js KuGouMusicApi 中 module/sheet_*.js 系列接口。
 */
class SheetApi(private val executor: RequestExecutor) {

    /**
     * 曲谱标签列表
     * 对齐 module/sheet_tags.js
     */
    suspend fun getTags(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = "https://wwwapi.kugou.com",
                url = "/opern/v1/home/get_tags",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 曲谱推荐/探索
     *
     * 对齐 module/sheet_explore.js
     *
     * @param instruments 乐器类型：1=吉他, 2=尤克里里, 3=钢琴, 4=简谱
     * @param level 难度等级（取决于乐器类型）
     *   - 钢琴: 0=基础, 1=进阶
     *   - 吉他: 0=中级, 1=进阶, 2=基础
     *   - 尤克里里: 0=基础, 1=进阶
     *   - 简谱: 0=基础
     * @param tagid 标签 ID
     * @param page 页码
     * @param pageSize 每页条数
     */
    suspend fun getExplore(
        instruments: Int = 1,
        level: Int = 0,
        tagid: Int = 0,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/opern/v1/home/get_rec_opern",
                method = HttpMethod.POST,
                params = mapOf(
                    "pagesize" to pageSize,
                    "page" to page,
                    "opern_level" to level,
                    "instruments" to instruments,
                    "tagid" to tagid
                ),
                data = buildJsonObject {
                    put("exposure_mixids", "")
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 曲谱排行榜
     *
     * 对齐 module/sheet_rank.js
     *
     * @param instruments 乐器类型：1=吉他, 2=尤克里里, 3=钢琴, 4=简谱
     * @param level 难度等级
     * @param tagid 标签 ID
     * @param page 页码
     * @param pageSize 每页条数
     */
    suspend fun getRank(
        instruments: Int = 1,
        level: Int = 0,
        tagid: Int = 0,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/opern/v1/home/get_rank_opern",
                method = HttpMethod.POST,
                params = mapOf(
                    "pagesize" to pageSize,
                    "page" to page,
                    "opern_level" to level,
                    "instruments" to instruments,
                    "tagid" to tagid
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 曲谱合集/专区列表
     *
     * 对齐 module/sheet_collection.js
     *
     * @param position 位置，默认 2
     */
    suspend fun getCollections(
        position: Int = 2
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/miniyueku/v1/opern_square/get_home_module_config",
                method = HttpMethod.GET,
                params = mapOf(
                    "srcappid" to executor.config.srcAppid,
                    "position" to position
                ),
                encryptType = EncryptType.WEB
            )
        )
    }

    /**
     * 曲谱详情
     *
     * 对齐 module/sheet_detail.js
     *
     * @param opernId 曲谱 ID
     */
    suspend fun getDetail(
        opernId: String
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/opern/v1/detail/info",
                method = HttpMethod.GET,
                params = mapOf("opern_id" to opernId),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 曲谱歌曲信息
     *
     * 对齐 module/sheet_song.js
     *
     * @param albumAudioId 歌曲的 AlbumAudioId (mixsongid)
     * @param instruments 乐器类型：1=吉他, 2=尤克里里, 3=钢琴, 4=简谱
     * @param level 难度等级
     */
    suspend fun getSong(
        albumAudioId: String,
        instruments: Int = 1,
        level: Int = 0
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/opern/v1/detail/song_info",
                method = HttpMethod.GET,
                params = mapOf(
                    "mixsongid" to albumAudioId,
                    "instruments" to instruments,
                    "opern_level" to level
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
