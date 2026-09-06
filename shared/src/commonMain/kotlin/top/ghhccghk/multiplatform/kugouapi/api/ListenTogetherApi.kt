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
        private const val DEFAULT_MUSIC_ROOM_BG = "https://youthimgbssdl.kugou.com/6e9cdcef8d163d06225d8cbeaa2f1ece.JPEG"
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

    /**
     * 发送聊天消息
     * 对齐 module/listen_together_chat.js (send)
     *
     * @param roomId 房间ID
     * @param message 消息内容
     * @param nickname 昵称
     * @param biz 业务类型
     */
    suspend fun sendChatMessage(
        roomId: String,
        message: String,
        nickname: String = "",
        biz: String = SELF_STUDY_BIZ
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/rmservice/v1/group/chat",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("userid", userid)
                    put("token", token)
                    put("biz", biz)
                    put("groupid", roomId)
                    putJsonObject("message") {
                        put("msgtype", 801)
                        put("nickname", nickname)
                        put("img", "")
                        put("alert", message)
                    }
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取聊天历史
     * 对齐 module/listen_together_chat.js (history)
     *
     * @param roomId 房间ID
     * @param maxId 最大消息ID
     * @param pageSize 每页数量
     * @param biz 业务类型
     */
    suspend fun getChatHistory(
        roomId: String,
        maxId: String = "0",
        pageSize: Int = 20,
        biz: String = SELF_STUDY_BIZ
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/rmservice/v1/group/msg_history",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("userid", userid)
                    put("token", token)
                    put("biz", biz)
                    put("groupid", roomId)
                    put("maxid", maxId)
                    put("pagesize", pageSize.toString())
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 搜索频道
     * 对齐 module/listen_together_discovery.js (channel_search)
     *
     * @param keyword 搜索关键词
     * @param page 页码
     * @param position 位置
     */
    suspend fun searchChannel(
        keyword: String,
        page: Int = 1,
        position: Int = 1
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/search/channel",
                method = HttpMethod.GET,
                params = mapOf(
                    "keyword" to keyword,
                    "page" to page,
                    "position" to position
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取一起听广场列表
     * 对齐 module/listen_together_discovery.js (kugroup_square)
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param orderType 排序类型
     */
    suspend fun getKugroupSquare(
        page: Int = 1,
        pageSize: Int = 20,
        orderType: Int = 1
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/kugroup/square",
                method = HttpMethod.GET,
                params = mapOf(
                    "page" to page,
                    "pagesize" to pageSize,
                    "order_type" to orderType
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取众乐房主播列表
     * 对齐 module/listen_together_discovery.js (kugroup_streamers)
     *
     * @param longitude 经度
     * @param latitude 纬度
     */
    suspend fun getKugroupStreamers(
        longitude: Double = 0.0,
        latitude: Double = 0.0
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v1/kugroup/get_streamer_list",
                method = HttpMethod.GET,
                params = mapOf(
                    "longitude" to longitude,
                    "latitude" to latitude
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取最近访问的房间
     * 对齐 module/listen_together_discovery.js (recent_rooms)
     */
    suspend fun getRecentRooms(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = YOUTH_BASE,
                url = "/v3/user/recent_room_dynamic",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 创建音乐房间
     * 对齐 module/listen_together_room.js (create)
     *
     * @param roomName 房间名称
     * @param biz 业务类型
     * @param backgroundUrl 背景图片URL
     * @param globalCollectionId 频道全局收藏ID
     * @param capacity 容量
     * @param roomPrivacy 房间隐私：1=公开，3=私密
     */
    suspend fun createMusicRoom(
        roomName: String = "",
        biz: String = MUSIC_ROOM_BIZ,
        backgroundUrl: String = DEFAULT_MUSIC_ROOM_BG,
        globalCollectionId: String = "",
        capacity: Int = 5,
        roomPrivacy: Int = 1
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/rmservice/v1/group/create",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("userid", userid)
                    put("token", token)
                    put("biz", biz)
                    put("introduction", roomName)
                    put("capacity", capacity)
                    putJsonObject("pass_through_data") {
                        put("room_privacy", roomPrivacy)
                        put("cp_notice", 1)
                        put("room_bg_content", buildJsonObject {
                            put("bg_img", backgroundUrl)
                            put("room_bg_type", "2")
                        }.toString())
                        if (roomPrivacy == 1 && globalCollectionId.isNotEmpty()) {
                            put("global_collection_id", globalCollectionId)
                        }
                    }
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 加入音乐房间
     * 对齐 module/listen_together_room.js (join)
     *
     * @param roomId 房间ID
     * @param biz 业务类型
     */
    suspend fun joinMusicRoom(
        roomId: String,
        biz: String = MUSIC_ROOM_BIZ
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/rmservice/v1/group/join",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("userid", userid)
                    put("token", token)
                    put("biz", biz)
                    put("groupid", roomId)
                    putJsonObject("pass_through_data") {
                        put("cp_notice", 1)
                    }
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取音乐房间状态
     * 对齐 module/listen_together_room.js (state)
     *
     * @param roomId 房间ID
     * @param biz 业务类型
     */
    suspend fun getMusicRoomState(
        roomId: String,
        biz: String = MUSIC_ROOM_BIZ
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/rmservice/v1/group/info",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("groupid", roomId)
                },
                params = mapOf("biz" to biz),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 发送心跳
     * 对齐 module/listen_together_room.js (heartbeat)
     *
     * @param roomId 房间ID
     * @param biz 业务类型
     */
    suspend fun sendHeartbeat(
        roomId: String,
        biz: String = SELF_STUDY_BIZ
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/rmservice/v1/group/heartbeat",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("userid", userid)
                    put("token", token)
                    put("biz", biz)
                    put("groupid", roomId)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 离开音乐房间
     * 对齐 module/listen_together_room.js (leave)
     *
     * @param roomId 房间ID
     * @param biz 业务类型
     */
    suspend fun leaveMusicRoom(
        roomId: String,
        biz: String = SELF_STUDY_BIZ
    ): KuGouResponse {
        val userid = executor.cookieJar.getUserid().toLongOrNull() ?: 0L
        val token = executor.cookieJar.getToken()

        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/rmservice/v1/group/leave",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("userid", userid)
                    put("token", token)
                    put("biz", biz)
                    put("groupid", roomId)
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取音乐房间播放列表
     * 对齐 module/listen_together_music.js (playlist)
     *
     * @param roomId 房间ID
     * @param pageSize 每页数量
     */
    suspend fun getMusicRoomPlaylist(
        roomId: String,
        pageSize: Int = 50
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/genting/music_fetch_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("pagesize", pageSize)
                },
                params = mapOf("roomid" to roomId),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取音乐房间详情
     * 对齐 module/listen_together_music.js (detail)
     *
     * @param roomId 房间ID
     */
    suspend fun getMusicRoomDetail(roomId: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/genting/get_musicroom_info",
                method = HttpMethod.GET,
                params = mapOf(
                    "roomid" to roomId,
                    "biz" to MUSIC_ROOM_BIZ
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取音乐房间成员
     * 对齐 module/listen_together_music.js (members)
     *
     * @param roomId 房间ID
     * @param page 页码
     * @param pageSize 每页数量
     */
    suspend fun getMusicRoomMembers(
        roomId: String,
        page: Int = 1,
        pageSize: Int = 100
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/genting/get_musicroom_member",
                method = HttpMethod.GET,
                params = mapOf(
                    "roomid" to roomId,
                    "page" to page,
                    "pagesize" to pageSize,
                    "apiver" to "3"
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 初始化音乐房间
     * 对齐 module/listen_together_music.js (initialize)
     *
     * @param roomId 房间ID
     */
    suspend fun initializeMusicRoom(roomId: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/genting/init_musicroom",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("sendall", 1)
                    put("audios", buildJsonArray { })
                },
                params = mapOf("roomid" to roomId),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 同步音乐房间播放器
     * 对齐 module/listen_together_music.js (sync_player)
     *
     * @param roomId 房间ID
     * @param frm 来源
     */
    suspend fun syncMusicPlayer(
        roomId: String,
        frm: Int = 2
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/genting/music_sync_player",
                method = HttpMethod.POST,
                data = buildJsonObject { },
                params = mapOf(
                    "roomid" to roomId,
                    "frm" to frm
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 切换歌曲
     * 对齐 module/listen_together_music.js (switch_song)
     *
     * @param roomId 房间ID
     * @param hash 歌曲哈希
     * @param mixsongid 歌曲MixID
     * @param actType 操作类型
     */
    suspend fun switchSong(
        roomId: String,
        hash: String = "",
        mixsongid: String = "",
        actType: Int = 1
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/genting/music_sw",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("act_type", actType)
                    putJsonObject("audio") {
                        put("hash", hash)
                        put("mixsongid", mixsongid)
                    }
                },
                params = mapOf("roomid" to roomId),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 播放器操作
     * 对齐 module/listen_together_music.js (player_operation)
     *
     * @param roomId 房间ID
     * @param action 操作类型：1=播放模式，2=进度，3=暂停/播放
     * @param playMode 播放模式（action=1时）
     * @param progress 进度（action=2时）
     * @param pause 是否暂停（action=3时）
     */
    suspend fun playerOperation(
        roomId: String,
        action: Int = 3,
        playMode: Int = 1,
        progress: Int = 0,
        pause: Boolean = true
    ): KuGouResponse {
        val data = buildJsonObject {
            put("action", action)
            when (action) {
                1 -> put("play_mode", playMode)
                2 -> put("progress", progress.coerceAtLeast(0))
                3 -> put("pause", if (pause) "1" else "2")
            }
        }

        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/genting/music_player_opr",
                method = HttpMethod.POST,
                data = data,
                params = mapOf("roomid" to roomId),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 点歌
     * 对齐 module/listen_together_music.js (order_song)
     *
     * @param roomId 房间ID
     * @param hash 歌曲哈希
     * @param mixsongid 歌曲MixID
     */
    suspend fun orderSong(
        roomId: String,
        hash: String = "",
        mixsongid: String = ""
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/genting/order_song",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("mixsongid", mixsongid)
                    put("hash", hash)
                },
                params = mapOf("roomid" to roomId),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取点歌列表
     * 对齐 module/listen_together_music.js (song_order_list)
     *
     * @param roomId 房间ID
     */
    suspend fun getSongOrderList(roomId: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                baseUrl = GATEWAY_BASE,
                url = "/youth/v1/genting/song_order_list",
                method = HttpMethod.GET,
                params = mapOf("roomid" to roomId),
                encryptType = EncryptType.ANDROID
            )
        )
    }
}