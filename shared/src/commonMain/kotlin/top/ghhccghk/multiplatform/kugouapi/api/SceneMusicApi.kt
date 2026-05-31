package top.ghhccghk.multiplatform.kugouapi.api

import top.ghhccghk.multiplatform.kugouapi.core.*
import top.ghhccghk.multiplatform.kugouapi.model.*
import kotlinx.serialization.json.*

/**
 * 场景音乐相关 API
 */
class SceneMusicApi(private val executor: RequestExecutor) {

    /**
     * 获取场景列表
     */
    suspend fun getLists(): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/scene/v1/scene/list",
                method = HttpMethod.GET,
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取场景列表 (V2)
     */
    suspend fun getListsV2(
        id: String,
        page: Int = 1,
        pageSize: Int = 30,
        sort: SceneSort = SceneSort.REC
    ): KuGouResponse {
        val sortMap = mapOf(SceneSort.REC to 1, SceneSort.HOT to 2, SceneSort.NEW to 3)
        
        return executor.execute(
            KuGouRequest(
                url = "/scene/v1/scene/list_v2",
                method = HttpMethod.POST,
                params = mapOf(
                    "scene_id" to id,
                    "page" to page,
                    "pagesize" to pageSize,
                    "sort_type" to (sortMap[sort] ?: 1),
                    "kugouid" to executor.cookieJar.getUserid()
                ),
                data = buildJsonObject {
                    put("exposure", buildJsonArray { })
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取场景模块
     */
    suspend fun getModule(id: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/scene/v1/scene/module",
                method = HttpMethod.POST,
                params = mapOf("scene_id" to id),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取场景模块详情
     */
    suspend fun getModuleInfo(id: String, moduleId: String): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/scene/v1/scene/module_info",
                method = HttpMethod.GET,
                params = mapOf(
                    "scene_id" to id,
                    "module_id" to moduleId
                ),
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取场景音乐推荐
     */
    suspend fun getMusic(id: String, page: Int = 1, pageSize: Int = 30): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/genesisapi/v1/scene_music/rec_music",
                method = HttpMethod.POST,
                params = mapOf(
                    "scene_id" to id,
                    "page" to page,
                    "pagesize" to pageSize
                ),
                data = buildJsonObject {
                    put("exposure", buildJsonArray { })
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取场景音频列表
     */
    suspend fun getAudioList(
        id: String,
        moduleId: String,
        tag: String = "",
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/scene/v1/scene/audio_list",
                method = HttpMethod.POST,
                params = mapOf(
                    "scene_id" to id,
                    "module_id" to moduleId,
                    "tag" to tag,
                    "page" to page,
                    "page_size" to pageSize
                ),
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clientver", executor.config.activeClientVersion)
                    put("token", executor.cookieJar.getToken())
                    put("userid", executor.cookieJar.getUserid())
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取场景视频列表
     */
    suspend fun getVideoList(
        tagId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/scene/v1/distribution/video_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clientver", executor.config.activeClientVersion)
                    put("token", executor.cookieJar.getToken())
                    put("userid", executor.cookieJar.getUserid())
                    put("tag_id", tagId)
                    put("page", page)
                    put("page_size", pageSize)
                    put("exposed_data", buildJsonArray { })
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }

    /**
     * 获取场景歌单列表
     */
    suspend fun getCollectionList(
        tagId: String,
        page: Int = 1,
        pageSize: Int = 30
    ): KuGouResponse {
        return executor.execute(
            KuGouRequest(
                url = "/scene/v1/distribution/collection_list",
                method = HttpMethod.POST,
                data = buildJsonObject {
                    put("appid", executor.config.activeAppId)
                    put("clientver", executor.config.activeClientVersion)
                    put("token", executor.cookieJar.getToken())
                    put("userid", executor.cookieJar.getUserid())
                    put("tag_id", tagId)
                    put("page", page)
                    put("page_size", pageSize)
                    put("exposed_data", buildJsonArray { })
                },
                encryptType = EncryptType.ANDROID
            )
        )
    }
}
