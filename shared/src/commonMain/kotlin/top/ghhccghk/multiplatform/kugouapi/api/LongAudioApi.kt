package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 长音频（有声书）相关 API。提供有声书、播客、长音频内容的搜索、分类、推荐等功能。对齐 module/longaudio_* 系列。
 */
class LongAudioApi(private val executor: RequestExecutor) {

    /**
     * 获取长音频专辑下的音乐列表
     */
    suspend fun getAlbumAudios(
        albumId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v2/album_audios",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("album_id", albumId)
                    put("area_code", 1)
                    put("tagid", 0)
                    put("page", page)
                    put("pagesize", pageSize)
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf(
                    "x-router" to "openapi.kugou.com",
                    "KG-TID" to "78"
                )
            )
        )
    }

    /**
     * 获取长音频专辑详情
     */
    suspend fun getAlbumDetail(ids: String): KuGouResponse {
        val data = buildJsonArray {
            ids.split(",").filter { it.isNotEmpty() }.forEach { id ->
                addJsonObject { put("album_id", id) }
            }
        }

        return executor.execute(
            KuGouRequest(
                url = "/openapi/v2/broadcast",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("data", data)
                    put("show_album_tag", 1)
                    put("fields", "album_name,album_id,category,authors,sizable_cover,intro,author_name,trans_param,album_tag,mix_intro,full_intro,is_publish")
                },
                encryptType = EncryptType.ANDROID,
                headers = mapOf("KG-TID" to "78")
            )
        )
    }

    /**
     * 获取长音频每日推荐
     */
    suspend fun getDailyRecommend(page: Int = 1, pageSize: Int = 30): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v1/home_new/daily_recommend",
                method = HttpMethod.POST,
                params = mapOf(
                    "module_id" to 1,
                    "size" to pageSize,
                    "page" to page
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取长音频排行榜推荐
     */
    suspend fun getRankRecommend(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v1/home_new/rank_card_recommend",
                method = HttpMethod.GET,
                params = mapOf("platform" to "ios"),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取长音频 VIP 推荐
     */
    suspend fun getVipRecommend(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v1/home_new/vip_select_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject { put("album_playlist", buildJsonArray { }) },
                params = mapOf("position" to "2", "clientver" to 12329),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取长音频每周新书推荐
     */
    suspend fun getWeekRecommend(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v1/home_new/week_new_albums_recommend",
                method = HttpMethod.POST,
                data = buildJsonObject { put("album_playlist", buildJsonArray { }) },
                params = mapOf("clientver" to 12329),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 听书免费书库/分类榜单专辑列表
     * 对齐 module/longaudio_album_list.js
     *
     * @param tagId 分类ID，默认 906（有声小说全部分类）
     * @param gender 性别：0=不限 / 1=男频 / 2=女频
     * @param status 状态：0=全部 / 1=连载 / 2=完结
     * @param sort 排序：0=默认 / 1=播放量 / 2=更新时间
     * @param page 页码
     * @param pageSize 每页条数
     */
    suspend fun getAlbumList(
        tagId: Int = 906,
        gender: Int = 0,
        status: Int = 0,
        sort: Int = 0,
        page: Int = 1,
        pageSize: Int = 20
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/longaudio/v1/album/list",
                method = HttpMethod.GET,
                params = mapOf(
                    "api_ver" to 2,
                    "gender" to gender.coerceAtLeast(0),
                    "sort" to sort.coerceAtLeast(0),
                    "tag_id" to tagId.coerceAtLeast(0),
                    "free" to 1,
                    "status" to status.coerceAtLeast(0),
                    "page" to page.coerceAtLeast(1),
                    "page_size" to pageSize.coerceAtLeast(1),
                    "clientver" to 20789
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 听书搜索
     * 对齐 module/longaudio_search.js
     *
     * @param keywords 搜索关键词
     * @param page 页码
     * @param pageSize 每页条数
     * @param userid 用户ID
     */
    suspend fun search(
        keywords: String,
        page: Int = 1,
        pageSize: Int = 30,
        userid: String = "0"
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/complexsearch/v4/search/song",
                method = HttpMethod.GET,
                params = mapOf(
                    "area_code" to 1,
                    "albumhide" to 1,
                    "com_user_type" to 0,
                    "privilegefilter" to 0,
                    "dopicfull" to 1,
                    "filter" to 12,
                    "platform" to "AndroidFilter",
                    "tag" to "em",
                    "recver" to 2,
                    "iscorrection" to 1,
                    "search_ability" to 223,
                    "sec_aggre" to 1,
                    "sec_aggre_bitmap" to 0,
                    "mode_ability" to 1,
                    "nocollect" to 1,
                    "user_type" to 0,
                    "userid" to (if (userid != "0") userid else executor.cookieJar.getUserid()),
                    "keyword" to keywords,
                    "page" to page.coerceAtLeast(1),
                    "pagesize" to pageSize.coerceAtLeast(1),
                    "clientver" to 20789
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 听书分类标签列表
     * 对齐 module/longaudio_tag_list.js
     */
    suspend fun getTagList(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/v3/list_audiobook_tags",
                method = HttpMethod.GET,
                params = mapOf(
                    "platform" to "android",
                    "clientver" to 20789
                ),
                encryptType = EncryptType.ANDROID,
                headers = mapOf("x-router" to "longaudio.kugou.com")
            )
        )
    }
}