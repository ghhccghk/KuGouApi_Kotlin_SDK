package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.EncryptType
import kotlinx.serialization.json.*

/**
 * 一起听相关 API
 * 提供房间创建、加入、状态查询、聊天、自习室等功能。
 * 对齐 module/listen_together_*.js
 */
class ListenTogetherApi(private val executor: RequestExecutor) {

    companion object {
        private const val YOUTH_BASE = "https://youth.kugou.com"
        private const val GATEWAY_BASE = "https://gateway.kugou.com"
        private const val SELF_STUDY_BIZ = "1000"
        private const val MUSIC_ROOM_BIZ = "1009"
    }

    // ... 之前的方法保持不变 ...

    /**
     * 获取用户创建的自习室列表
     */
    suspend fun getStudyCreatedRooms(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/study/user_create_room_list",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 删除自习室
     *
     * @param globalCollectionId 频道全局收藏ID
     * @param roomId 房间ID
     */
    suspend fun deleteStudyCreatedRoom(
        globalCollectionId: String,
        roomId: String
    ): KuGouResponse {
        val params = mapOf(
            "global_collection_id" to globalCollectionId,
            "roomid" to roomId
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/room/delete_room",
                method = HttpMethod.DELETE,
                params = params,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取自习室列表
     *
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 20
     * @param sort 排序方式，默认 0
     * @param pageId 页面ID，默认 191708212
     * @param parentPageId 父页面ID，默认 356753938
     * @param tagId 标签ID
     */
    suspend fun getStudyRoomList(
        page: Int = 1,
        pageSize: Int = 20,
        sort: Int = 0,
        pageId: Int = 191708212,
        parentPageId: String = "356753938",
        tagId: String = ""
    ): KuGouResponse {
        val params = mapOf(
            "page" to page,
            "pagesize" to pageSize,
            "sort" to sort,
            "page_id" to pageId,
            "ppage_id" to parentPageId,
            "tag_id" to tagId
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/room/get_room_list_by_tag",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取自习室详情
     *
     * @param roomId 房间ID
     */
    suspend fun getStudyRoomDetail(
        roomId: String
    ): KuGouResponse {
        val params = mapOf("room_id" to roomId)

        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/room/get_room_detail",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取自习室成员列表
     *
     * @param roomId 房间ID
     * @param page 页码，默认 1
     * @param pageSize 每页条数，默认 20
     * @param memberType 成员类型，默认 1
     */
    suspend fun getStudyRoomMembers(
        roomId: String,
        page: Int = 1,
        pageSize: Int = 20,
        memberType: Int = 1
    ): KuGouResponse {
        val params = mapOf(
            "room_id" to roomId,
            "page" to page,
            "pagesize" to pageSize,
            "member_type" to memberType
        )

        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/room/get_member_list",
                method = HttpMethod.GET,
                params = params,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 配置自习室
     *
     * @param roomId 房间ID
     * @param roomName 房间名称
     * @param globalCollectionId 频道全局收藏ID
     * @param roomNotice 房间公告
     * @param allowChat 是否允许聊天，默认 1
     * @param roomTag 房间标签，默认 "2"
     * @param musicType 音乐类型，默认 1
     * @param musicStyle 音乐风格
     * @param audios 音频列表
     * @param whiteNoiseType 白噪音类型，默认 1
     * @param pageId 页面ID
     * @param parentPageId 父页面ID
     * @param type 类型，默认 1
     */
    suspend fun configureStudyRoom(
        roomId: String,
        roomName: String = "",
        globalCollectionId: String = "",
        roomNotice: String = "",
        allowChat: Int = 1,
        roomTag: String = "2",
        musicType: Int = 1,
        musicStyle: String? = null,
        audios: List<String>? = null,
        whiteNoiseType: Int = 1,
        pageId: Int? = null,
        parentPageId: String = "356753938",
        type: Int = 1
    ): KuGouResponse {
        val pageIds = mapOf(1 to 711586122, 2 to 971343961, 3 to 711357575)
        val actualPageId = pageId ?: pageIds[musicType] ?: 711586122

        val params = mapOf(
            "page_id" to actualPageId,
            "ppage_id" to parentPageId,
            "type" to type
        )

        val data = buildJsonObject {
            put("room_id", roomId)
            put("room_name", roomName)
            put("global_collection_id", globalCollectionId)
            put("room_notice", roomNotice)
            put("allow_chat", allowChat)
            put("room_tag", roomTag)
            put("music_type", musicType)
            if (musicType == 1 || musicType == 2) {
                musicStyle?.let { put("music_style", it) }
                audios?.let { audioList ->
                    put("audios", buildJsonArray {
                        audioList.forEach { add(it) }
                    })
                }
            } else if (musicType == 3) {
                put("white_noise_type", whiteNoiseType)
            }
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/user/make_room",
                method = HttpMethod.POST,
                params = params,
                data = data,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 同步播放器状态
     *
     * @param roomId 房间ID
     * @param frm 来源，默认 2
     * @param pageId 页面ID
     * @param parentPageId 父页面ID
     */
    suspend fun syncStudyPlayer(
        roomId: String,
        frm: Int = 2,
        pageId: Int = 711586122,
        parentPageId: String = "356753938"
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()

        val params = mapOf(
            "page_id" to pageId,
            "ppage_id" to parentPageId
        )

        val data = buildJsonObject {
            put("roomid", roomId)
            put("frm", frm)
            put("userid", userid)
            put("token", token)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/music/sync_player",
                method = HttpMethod.POST,
                params = params,
                data = data,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取自习室播放列表
     *
     * @param roomId 房间ID
     * @param frm 来源，默认 2
     * @param pageSize 每页条数，默认 50
     * @param pageId 页面ID
     * @param parentPageId 父页面ID
     */
    suspend fun getStudyPlaylist(
        roomId: String,
        frm: Int = 2,
        pageSize: Int = 50,
        pageId: Int = 711586122,
        parentPageId: String = "356753938"
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()

        val params = mapOf(
            "page_id" to pageId,
            "ppage_id" to parentPageId
        )

        val data = buildJsonObject {
            put("roomid", roomId)
            put("frm", frm)
            put("pagesize", pageSize)
            put("userid", userid)
            put("token", token)
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/music/fetch_list",
                method = HttpMethod.POST,
                params = params,
                data = data,
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
