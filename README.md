# KuGou Music API Kotlin Multiplatform SDK


### 基于 Kotlin Multiplatform (KMP) 重新实现的酷狗音乐 API SDK。对齐 [MakcRe/KuGouMusicApi](https://github.com/MakcRe/KuGouMusicApi) 的 Node.js 实现，支持 Android、iOS、JVM、Web (Wasm/JS) 等多平台。
---
### 使用了ChatGPT，Gemini，Claude，XiaoMi Mimo 等 AI 模型辅助加速开发，可能会有错误
---

## 🚀 核心特性

- **多平台支持**: 统一的 API 调用接口，底层适配不同平台的 HTTP 与加密实现。
- **协程优先**: 所有网络请求均采用 `suspend` 挂起函数，完美契合 Kotlin 协程流。
- **全量加密对齐**: 严格对齐酷狗 Android/iOS 端的 MD5 签名、AES-CBC 与 RSA-PKCS1 加密算法。
- **模块化设计**: 17 个 API 分类模块，112 个接口，按需调用。

---

## 📦 已实现 API 模块状态（共 17 个模块 · 112 个 API）

### Auth — 认证与身份（13 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `registerDev()` | `register_dev.js` | 设备注册，获取 dfid |
| `sendCaptcha(mobile)` | `captcha_sent.js` | 发送手机验证码 |
| `loginByPassword(username, password)` | `login.js` | 账号密码登录 |
| `loginByPhoneCode(mobile, code)` | `login_cellphone.js` | 手机验证码登录 |
| `loginByToken(token, userid)` | `login_token.js` | Token 刷新登录 |
| `createQrKey(type)` | `login_qr_key.js` | 创建二维码登录 Key |
| `createQrCodeUrl(key)` | `login_qr_create.js` | 生成二维码 URL（本地） |
| `checkQrCode(key)` | `login_qr_check.js` | 轮询二维码扫码状态 |
| `getLoginDevices()` | `login_device.js` | 获取已登录设备列表 |
| `kickDevice(targetMid)` | `login_device_kick.js` | 踢出指定设备 |
| `createWxLogin()` | `login_wx_create.js` | 微信登录 - 获取二维码 |
| `checkWxLogin(uuid)` | `login_wx_check.js` | 微信登录 - 检查扫码状态 |
| `loginByOpenPlat(code)` | `login_openplat.js` | 微信 OAuth 开放平台登录 |

### Search — 搜索（7 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `search(keywords, page, pageSize, type)` | `search.js` | 综合搜索 |
| `searchDefault()` | `search_default.js` | 获取搜索默认关键词 |
| `searchHot()` | `search_hot.js` | 热搜列表 |
| `searchSuggest(keywords)` | `search_suggest.js` | 搜索联想建议 |
| `searchComplex(keywords, page, pageSize)` | `search_complex.js` | 复杂搜索 |
| `searchLyric(keywords, page, pageSize)` | `search_lyric.js` | 歌词搜索 |
| `searchMixed(keywords)` | `search_mixed.js` | 混合搜索 |

### Song — 歌曲（12 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getAudioInfo(hash)` | `audio.js` | 获取歌曲/音频信息 |
| `getRelatedAudio(audioId, ...)` | `audio_related.js` | 获取相关歌曲 |
| `matchAccompany(hash, fileName, ...)` | `audio_accompany_matching.js` | 匹配伴奏 |
| `getKtvTotal(songId, singer, hash)` | `audio_ktv_total.js` | 获取 K 歌作品总数 |
| `getPrivilegeLite(hash, albumId)` | `privilege_lite.js` | 歌曲权限查询 (Lite) |
| `getKrmAudio(albumAudioId, ...)` | `krm_audio.js` | 获取 KRM 音频元数据 |
| `getLyric(id, accessKey, ...)` | `lyric.js` | 下载歌词及 KRC 解码 |
| `getSongUrl(hash, albumId, audioId, ...)` | — | 获取歌曲播放链接 (v5) |
| `getSongUrlNew(hash, audioId, ...)` | — | 获取歌曲播放链接 (v6) |
| `getSongClimax(hash)` | — | 获取歌曲高潮部分 |
| `getSongRanking(audioId)` | — | 歌曲成绩单 |
| `getSongRankingFilter(audioId, ...)` | — | 歌曲榜单过滤 |

### Album — 专辑（4 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getAlbumMetadata(albumId, fields)` | `album.js` | 批量获取专辑元数据 |
| `getAlbumDetail(albumId, ...)` | `album_detail.js` | 获取专辑详情 |
| `getAlbumShop()` | `album_shop.js` | 唱片店分类数据 |
| `getAlbumSongs(albumId, page, pageSize, ...)` | `album_songs.js` | 获取专辑歌曲列表 |

### Artist — 歌手（9 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getDetail(id)` | `artist_detail.js` | 获取歌手详情 |
| `getAlbums(id, page, pageSize, sort)` | `artist_albums.js` | 获取歌手专辑 |
| `getAudios(id, page, pageSize, sort)` | `artist_audios.js` | 获取歌手单曲 |
| `getVideos(id, page, pageSize, tag)` | `artist_videos.js` | 获取歌手 MV |
| `getLists(musician, sexType, regionType, hotSize)` | `singer_list.js` | 获取歌手列表 |
| `follow(id)` | `artist_follow.js` | 关注歌手 |
| `unfollow(id)` | `artist_unfollow.js` | 取消关注歌手 |
| `getFollowNewSongs(userId, page, sort)` | `artist_follow_newsongs.js` | 关注歌手新歌 |
| `getHonour(id, page, pageSize)` | `artist_honour.js` | 获取歌手荣誉详情 |

### Playlist — 歌单（10 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `addPlaylist(name, uid, listId, ...)` | `playlist_add.js` | 新建歌单 |
| `deletePlaylist(listId)` | `playlist_del.js` | 删除歌单 |
| `getPlaylistDetail(id)` | `playlist_detail.js` | 获取歌单详情 |
| `getEffectPlaylists(page, pageSize)` | `playlist_effect.js` | 音效/精选歌单 |
| `getSimilarPlaylists(id)` | `playlist_similar.js` | 获取相似歌单 |
| `getPlaylistTags()` | `playlist_tags.js` | 获取歌单标签 |
| `getPlaylistTracks(id, page, pageSize)` | `playlist_track_all.js` | 获取歌单全部歌曲 |
| `getPlaylistTracksNew(id, page, pageSize)` | `playlist_track_all_new.js` | 获取歌单全部歌曲（新版） |
| `addTracks(listId, hash)` | `playlist_tracks_add.js` | 向歌单添加歌曲 |
| `removeTracks(listId, hash)` | `playlist_tracks_del.js` | 从歌单移除歌曲 |

### Comment — 评论（7 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getMusicComments(mixSongId, page, pageSize)` | `comment_music.js` | 获取歌曲评论 |
| `getAlbumComments(albumId, page, pageSize)` | `comment_album.js` | 获取专辑评论 |
| `getPlaylistComments(listId, page, pageSize)` | `comment_playlist.js` | 获取歌单评论 |
| `getCommentCount(id, type)` | `comment_count.js` | 获取评论数量 |
| `getFloorComments(id, eId, sId, type, ...)` | `comment_floor.js` | 获取楼层评论 |
| `getMusicCommentsByClassify(id, classify, ...)` | `comment_music_classify.js` | 按分类获取歌曲评论 |
| `getMusicCommentsByHotWord(id, hotWord, ...)` | `comment_music_hotword.js` | 按热词获取歌曲评论 |

### Rank — 排行榜（5 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getList(version)` | `rank_list.js` | 获取排行榜列表 |
| `getInfo(id, page, pageSize, ...)` | `rank_info.js` | 获取排行榜信息 |
| `getAudio(id, page, pageSize, ...)` | `rank_audio.js` | 获取排行榜歌曲 |
| `getTop()` | `rank_top.js` | 获取推荐榜单 |
| `getVol(id, version)` | `rank_vol.js` | 获取排行榜往期 |

### Recommend — 推荐（7 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getDailyRecommend(...)` | `everyday_recommend.js` | 每日推荐 |
| `getStyleRecommend(sceneId, ...)` | `everyday_style_recommend.js` | 风格推荐 |
| `getHistory(mode, platform, ...)` | `everyday_history.js` | 历史推荐 |
| `getFriendRecommend()` | `everyday_friend.js` | 好友推荐 |
| `getRecommendSongs(hash)` | `recommend_songs.js` | 推荐相似歌曲 |
| `getPersonalFm(action, mode, ...)` | `personal_fm.js` | 私人 FM |
| `getAiRecommend(albumId)` | `ai_recommend.js` | AI 智能推荐 |

### Radio — 电台/FM（5 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getFmClass()` | `fm_class.js` | 获取分类 FM |
| `getFmRecommend()` | `fm_recommend.js` | 获取推荐 FM |
| `getFmSongs(fmId, type, ...)` | `fm_songs.js` | 获取 FM 歌曲列表 |
| `getFmImage(fmId)` | `fm_image.js` | 获取 FM 图片信息 |
| `getPcDiantai()` | `pc_diantai.js` | 获取 PC 端电台 Banner |

### SceneMusic — 场景音乐（8 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getLists()` | `scene_lists.js` | 获取场景音乐列表 |
| `getListsV2(id, page, pageSize, sort)` | `scene_lists_v2.js` | 获取场景列表 (V2) |
| `getModule(id)` | `scene_module.js` | 获取场景模块 |
| `getModuleInfo(id, moduleId)` | `scene_module_info.js` | 获取场景模块详情 |
| `getMusic(id, page, pageSize)` | `scene_music.js` | 获取场景音乐推荐 |
| `getAudioList(id, moduleId, tag, ...)` | `scene_audio_list.js` | 获取场景音频列表 |
| `getVideoList(tagId, page, pageSize)` | `scene_video_list.js` | 获取场景视频列表 |
| `getCollectionList(tagId, page, pageSize)` | `scene_collection_list.js` | 获取场景歌单列表 |

### User — 用户（3 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getLatestSongsListen(page)` | `lastest_songs_listen.js` | 获取最近收听 |
| `getFavoriteCount(userId)` | `favorite_count.js` | 获取收藏数量 |
| `uploadPlayHistory(songId, fileId, ...)` | `playhistory_upload.js` | 上传播放记录 |

### Image — 图片（2 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getImages(hash, ...)` | `images.js` | 获取歌手/专辑图片 |
| `getAudioImages(hash, ...)` | `images_audio.js` | 获取音频相关图片 |

### Video — 视频（1 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getKmrAudioMv(albumAudioId, ...)` | `kmr_audio_mv.js` | 获取 KRM 音频 MV |

### LongAudio — 有声书/长音频（6 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getAlbumAudios(albumId, page, pageSize)` | `longaudio_album_audios.js` | 获取有声书专辑音频 |
| `getAlbumDetail(albumId)` | `longaudio_album_detail.js` | 获取有声书专辑详情 |
| `getDailyRecommend(page, pageSize)` | `longaudio_daily_recommend.js` | 有声书每日推荐 |
| `getRankRecommend()` | `longaudio_rank_recommend.js` | 有声书排行榜推荐 |
| `getVipRecommend()` | `longaudio_vip_recommend.js` | 有声书 VIP 推荐 |
| `getWeekRecommend()` | `longaudio_week_recommend.js` | 有声书每周推荐 |

### Misc — 杂项（7 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `brush(mode, count)` | `brush.js` | 刷刷 Feed 流 |
| `getIpData(ip, type, page, pageSize)` | `ip.js` | 获取 IP 区域数据 |
| `getIpDetail(ip)` | `ip_dateil.js` | 获取 IP 详情 |
| `getIpPlaylist(ip, page, pageSize)` | `ip_playlist.js` | 获取 IP 歌单 |
| `getIpZone()` | `ip_zone.js` | 获取当前 IP 区域 |
| `getIpZoneHome(id)` | `ip_zone_home.js` | IP 专区主页 |
| `getServerTime()` | `server_now.js` | 获取服务器时间 |

### Sheet — 曲谱/乐谱（6 个）

| 方法 | 对齐 Node.js 模块 | 说明 |
| :--- | :--- | :--- |
| `getTags()` | `sheet_tags.js` | 获取曲谱标签 |
| `getExplore(instruments, level, ...)` | `sheet_explore.js` | 推荐/探索曲谱 |
| `getRank(instruments, level, ...)` | `sheet_rank.js` | 曲谱排行榜 |
| `getCollections(position)` | `sheet_collection.js` | 曲谱合集/专区 |
| `getDetail(opernId)` | `sheet_detail.js` | 曲谱详情 |
| `getSong(albumAudioId, instruments, level)` | `sheet_song.js` | 曲谱歌曲信息 |

---

## 🛠 编译与运行

项目使用 Gradle 进行管理，支持以下指令：

### 1. 编译 SDK 模块
```bash
# 编译所有平台的库文件
./gradlew :shared:build

# 仅编译 JVM 版本（用于快速验证）
./gradlew :shared:compileKotlinJvm
```

### 2. 运行示例应用
- **Android**: 在 Android Studio 中运行 `androidApp` 或执行 `./gradlew :androidApp:assembleDebug`。
- **Desktop (JVM)**: `./gradlew :desktopApp:run`。
- **Web (Wasm)**: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`。
- **iOS**: 使用 Xcode 打开 `iosApp` 目录并运行。

---

## 💡 使用示例

```kotlin
// 1. 初始化客户端
val client = KuGouClient()

// 2. 注册设备（获取 dfid 是很多 API 的前提）
val regResp = client.auth.registerDev()
println("Dfid: ${client.cookieJar.getDfid()}")

// 3. 搜索音乐
val searchResp = client.search.search("周杰伦")

// 4. 下载并解码歌词
val lyricResp = client.song.getLyric(id = "...", accessKey = "...", decode = true)
val krcText = lyricResp.body["decodeContent"]
```

---

## 🛠 技术栈

- **HTTP Client**: [Ktor](https://ktor.io/)
- **Serialization**: [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- **Byte/IO**: [Okio](https://square.github.io/okio/)
- **UI (Example)**: Compose Multiplatform

---
## ⚠ 注意事项

使用web开发请注意 CORS 限制，需要在服务器端设置代理或使用浏览器插件绕过。iOS 端需要配置 ATS 以允许非 HTTPS 请求。

---

## ⚖️ 免责声明

本应用仅用于学习及测试，请在下载 24 小时内删除。由于非官方实现，因使用本 SDK 产生的任何法律后果由使用者自行承担。请尊重版权，严禁商业用途。
