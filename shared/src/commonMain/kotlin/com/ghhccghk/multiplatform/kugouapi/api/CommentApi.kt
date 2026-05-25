package com.ghhccghk.multiplatform.kugouapi.api

import com.ghhccghk.multiplatform.kugouapi.core.*
import com.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 评论相关 API
 */
class CommentApi(private val executor: RequestExecutor) {

    /**
     * 获取歌曲评论
     */
    suspend fun getMusicComments(
        mixSongId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/mcomment/v1/cmtlist",
                method = HttpMethod.POST,
                params = mapOf(
                    "mixsongid" to mixSongId,
                    "need_show_image" to 1,
                    "p" to page,
                    "pagesize" to pageSize,
                    "show_classify" to 1,
                    "show_hotword_list" to 1,
                    "extdata" to "0",
                    "code" to "fc4be23b4e972707f36b8a828a93ba8a"
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取专辑评论
     */
    suspend fun getAlbumComments(
        id: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/m.comment.service/v1/cmtlist",
                method = HttpMethod.POST,
                params = mapOf(
                    "childrenid" to id,
                    "need_show_image" to 1,
                    "p" to page,
                    "pagesize" to pageSize,
                    "show_classify" to 1,
                    "show_hotword_list" to 1,
                    "code" to "94f1792ced1df89aa68a7939eaf2efca"
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取歌单评论
     */
    suspend fun getPlaylistComments(
        id: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/m.comment.service/v1/cmtlist",
                method = HttpMethod.POST,
                params = mapOf(
                    "childrenid" to id,
                    "need_show_image" to 1,
                    "p" to page,
                    "pagesize" to pageSize,
                    "show_classify" to 1,
                    "show_hotword_list" to 1,
                    "code" to "ca53b96fe5a1d9c22d71c8f522ef7c4f",
                    "content_type" to 0,
                    "tag" to 5
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取评论数
     */
    suspend fun getCommentCount(hash: String = "", specialId: String = ""): KuGouResponse {
        val params = mutableMapOf(
            "r" to "comments/getcommentsnum",
            "code" to "fc4be23b4e972707f36b8a828a93ba8a"
        )
        if (hash.isNotEmpty()) params["hash"] = hash
        if (specialId.isNotEmpty()) params["childrenid"] = specialId

        return executor.execute(
            KuGouRequest(
                url = "/index.php",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.WEB,
                headers = mapOf("x-router" to "sum.comment.service.kugou.com")
            )
        )
    }

    /**
     * 获取楼层评论 (回复列表)
     *
     * @param resourceType: "song", "playlist", "album"
     */
    suspend fun getFloorComments(
        tid: String,
        specialId: String = "",
        mixSongId: String = "",
        resourceType: String = "song",
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        val songCode = "fc4be23b4e972707f36b8a828a93ba8a"
        val playlistCode = "ca53b96fe5a1d9c22d71c8f522ef7c4f"
        val albumCode = "94f1792ced1df89aa68a7939eaf2efca"

        val code = when (resourceType.lowercase()) {
            "playlist" -> playlistCode
            "album" -> albumCode
            else -> songCode
        }

        val useServiceEndpoint = resourceType == "playlist" || resourceType == "album"

        val params = mutableMapOf<String, Any?>(
            "childrenid" to specialId,
            "need_show_image" to 1,
            "p" to page,
            "pagesize" to pageSize,
            "show_classify" to 1,
            "show_hotword_list" to 1,
            "code" to code,
            "tid" to tid
        )
        if (mixSongId.isNotEmpty()) params["mixsongid"] = mixSongId

        return executor.execute(
            KuGouRequest(
                url = if (useServiceEndpoint) "/m.comment.service/v1/hot_replylist" else "/mcomment/v1/hot_replylist",
                method = HttpMethod.POST,
                params = params,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 根据分类获取评论
     */
    suspend fun getMusicCommentsByClassify(
        mixSongId: String,
        typeId: String,
        sort: Int = 1,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/mcomment/v1/cmt_classify_list",
                method = HttpMethod.POST,
                params = mapOf(
                    "mixsongid" to mixSongId,
                    "need_show_image" to 1,
                    "page" to page,
                    "pagesize" to pageSize,
                    "type_id" to typeId,
                    "extdata" to "0",
                    "code" to "fc4be23b4e972707f36b8a828a93ba8a",
                    "sort_method" to if (sort == 2) 2 else 1
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 根据热词获取评论
     */
    suspend fun getMusicCommentsByHotWord(
        mixSongId: String,
        hotWord: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/mcomment/v1/get_hot_word",
                method = HttpMethod.POST,
                params = mapOf(
                    "mixsongid" to mixSongId,
                    "need_show_image" to 1,
                    "p" to page,
                    "pagesize" to pageSize,
                    "hot_word" to hotWord,
                    "extdata" to "0",
                    "code" to "fc4be23b4e972707f36b8a828a93ba8a"
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
