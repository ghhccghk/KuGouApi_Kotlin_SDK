@file:OptIn(ExperimentalLayoutApi::class)

package com.ghhccghk.multiplatform.kugouapi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Composable
fun App(
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    MaterialTheme {
        val client = remember { KuGouClient() }
        var logText by remember { mutableStateOf("等待测试...") }

        val jsonFormatter = remember { Json { prettyPrint = true } }

        fun addLog(tag: String, content: Any?) {
            val formatted = try {
                if (content is JsonObject) jsonFormatter.encodeToString(content)
                else content.toString()
            } catch (e: Exception) {
                content.toString()
            }
            logText = "[$tag]\n$formatted\n\n------------------\n$logText"
        }

        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("酷狗 SDK 综合测试", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // 全局输入区域
                val inputs = remember { mutableStateMapOf<String, String>() }
                fun get(key: String, default: String = "") = inputs[key] ?: default

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ============ 全局凭证输入 ============
                    TestSection("全局凭证") {
                        TestButton("注册设备 (dfid)") {
                            val resp = client.auth.registerDev()
                            addLog("Auth:registerDev", resp.body)
                        }
                    }
                    InputRow("userid", get("userid"),
                        {
                        inputs["userid"] = it
                        client.cookieJar.setUserid(it.toLong())
                                                      }, "用户ID (选填)")
                    InputRow("token", get("token"), {
                        inputs["token"] = it
                        client.cookieJar.setToken(it)
                                                    }, "Token (选填)")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 身份认证 ============
                    TestSection("身份认证 - Auth") {
                        TestButton("注册设备 (dfid)") {
                            val resp = client.auth.registerDev()
                            addLog("Auth:registerDev", resp.body)
                        }
                        TestButton("登录设备列表") {
                            val resp = client.auth.getLoginDevices()
                            addLog("Auth:getLoginDevices", resp.body)
                        }
                        TestButton("创建微信登录") {
                            val resp = client.auth.createWxLogin()
                            addLog("Auth:createWxLogin", resp.body)
                        }
                    }
                    InputRow("sendCaptcha_phone", get("sendCaptcha_phone"), { inputs["sendCaptcha_phone"] = it }, "手机号")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("发送验证码") {
                            val resp = client.auth.sendCaptcha(get("sendCaptcha_phone"))
                            addLog("Auth:sendCaptcha", resp.body)
                        }
                    }

                    InputRow("login_pwd_user", get("login_pwd_user"), { inputs["login_pwd_user"] = it }, "用户名")
                    InputRow("login_pwd_pass", get("login_pwd_pass"), { inputs["login_pwd_pass"] = it }, "密码")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("密码登录") {
                            val resp = client.auth.loginByPassword(get("login_pwd_user"), get("login_pwd_pass"))
                            addLog("Auth:loginByPassword", resp.body)
                        }
                    }

                    InputRow("login_code_phone", get("login_code_phone"), { inputs["login_code_phone"] = it }, "手机号")
                    InputRow("login_code_code", get("login_code_code"), { inputs["login_code_code"] = it }, "验证码")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("手机验证码登录") {
                            val resp = client.auth.loginByPhoneCode(get("login_code_phone"), get("login_code_code"))
                            addLog("Auth:loginByPhoneCode", resp.body)
                        }
                    }

                    InputRow("login_token_token", get("login_token_token"), { inputs["login_token_token"] = it }, "Token")
                    InputRow("login_token_userid", get("login_token_userid"), { inputs["login_token_userid"] = it }, "Userid")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("Token登录") {
                            val resp = client.auth.loginByToken(get("login_token_token"), get("login_token_userid"))
                            addLog("Auth:loginByToken", resp.body)
                        }
                    }

                    InputRow("qrkey_keyword", get("qrkey_keyword"), { inputs["qrkey_keyword"] = it }, "关键词 (默认: qrcode)")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("创建扫码Key") {
                            val resp = client.auth.createQrKey(get("qrkey_keyword", "qrcode"))
                            addLog("Auth:createQrKey", resp.body)
                        }
                    }

                    InputRow("qrcheck_key", get("qrcheck_key"), { inputs["qrcheck_key"] = it }, "Key")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("检查扫码状态") {
                            val resp = client.auth.checkQrCode(get("qrcheck_key"))
                            addLog("Auth:checkQrCode", resp.body)
                        }
                    }

                    InputRow("wxcheck_uuid", get("wxcheck_uuid"), { inputs["wxcheck_uuid"] = it }, "UUID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("检查微信登录") {
                            val resp = client.auth.checkWxLogin(get("wxcheck_uuid"))
                            addLog("Auth:checkWxLogin", resp.body)
                        }
                    }

                    InputRow("kick_mid", get("kick_mid"), { inputs["kick_mid"] = it }, "目标设备MID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("踢出设备") {
                            val resp = client.auth.kickDevice(get("kick_mid"))
                            addLog("Auth:kickDevice", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 搜索 ============
                    TestSection("搜索 - Search") {
                        TestButton("热搜榜") {
                            val resp = client.search.searchHot()
                            addLog("Search:searchHot", resp.body)
                        }
                        TestButton("搜索默认词") {
                            val resp = client.search.searchDefault()
                            addLog("Search:searchDefault", resp.body)
                        }
                    }
                    InputRow("search_keyword", get("search_keyword", "周杰伦"), { inputs["search_keyword"] = it }, "搜索关键词")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("搜索") {
                            val resp = client.search.search(get("search_keyword", "周杰伦"))
                            addLog("Search:search", resp.body)
                        }
                        TestButton("混合搜索") {
                            val resp = client.search.searchMixed(get("search_keyword", "周杰伦"))
                            addLog("Search:searchMixed", resp.body)
                        }
                        TestButton("搜索联想") {
                            val resp = client.search.searchSuggest(get("search_keyword", "周杰伦"))
                            addLog("Search:searchSuggest", resp.body)
                        }
                        TestButton("复杂搜索") {
                            val resp = client.search.searchComplex(get("search_keyword", "周杰伦"))
                            addLog("Search:searchComplex", resp.body)
                        }
                        TestButton("歌词搜索") {
                            val resp = client.search.searchLyric(get("search_keyword", "周杰伦"))
                            addLog("Search:searchLyric", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 歌曲 ============
                    TestSection("歌曲 - Song") {
                        TestButton("默认歌曲信息") {
                            val resp = client.song.getAudioInfo("E6B6478980F16C6A97491C781A327E7A")
                            addLog("Song:getAudioInfo", resp.body)
                        }
                    }
                    InputRow("song_hash", get("song_hash"), { inputs["song_hash"] = it }, "歌曲Hash (多个逗号分隔)")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌曲信息") {
                            val resp = client.song.getAudioInfo(get("song_hash", "E6B6478980F16C6A97491C781A327E7A"))
                            addLog("Song:getAudioInfo", resp.body)
                        }
                    }

                    InputRow("song_lyric_id", get("song_lyric_id"), { inputs["song_lyric_id"] = it }, "歌词ID")
                    InputRow("song_lyric_key", get("song_lyric_key"), { inputs["song_lyric_key"] = it }, "AccessKey")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("获取歌词") {
                            val resp = client.song.getLyric(get("song_lyric_id", "191543369"), get("song_lyric_key", "1AE02A6C2E7596187C717727708422CB"), decode = true)
                            addLog("Song:getLyric", resp.body)
                        }
                    }

                    InputRow("song_related_id", get("song_related_id"), { inputs["song_related_id"] = it }, "AlbumAudioId")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("相关歌曲") {
                            val resp = client.song.getRelatedAudio(get("song_related_id").toLongOrNull() ?: 0L)
                            addLog("Song:getRelatedAudio", resp.body)
                        }
                    }

                    InputRow("song_match_hash", get("song_match_hash"), { inputs["song_match_hash"] = it }, "Hash")
                    InputRow("song_match_file", get("song_match_file"), { inputs["song_match_file"] = it }, "文件名")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("匹配伴奏") {
                            val resp = client.song.matchAccompany(get("song_match_hash"), get("song_match_file"))
                            addLog("Song:matchAccompany", resp.body)
                        }
                    }

                    InputRow("song_ktv_id", get("song_ktv_id"), { inputs["song_ktv_id"] = it }, "SongId")
                    InputRow("song_ktv_singer", get("song_ktv_singer"), { inputs["song_ktv_singer"] = it }, "歌手名")
                    InputRow("song_ktv_hash", get("song_ktv_hash"), { inputs["song_ktv_hash"] = it }, "Hash")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("KTV作品总数") {
                            val resp = client.song.getKtvTotal(get("song_ktv_id").toLongOrNull() ?: 0L, get("song_ktv_singer"), get("song_ktv_hash"))
                            addLog("Song:getKtvTotal", resp.body)
                        }
                    }

                    InputRow("song_priv_hash", get("song_priv_hash"), { inputs["song_priv_hash"] = it }, "Hash")
                    InputRow("song_priv_album", get("song_priv_album"), { inputs["song_priv_album"] = it }, "AlbumId")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌曲权限(Lite)") {
                            val resp = client.song.getPrivilegeLite(get("song_priv_hash"), get("song_priv_album"))
                            addLog("Song:getPrivilegeLite", resp.body)
                        }
                    }

                    InputRow("song_krm_id", get("song_krm_id"), { inputs["song_krm_id"] = it }, "AlbumAudioId")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("KRM音频元数据") {
                            val resp = client.song.getKrmAudio(get("song_krm_id"))
                            addLog("Song:getKrmAudio", resp.body)
                        }
                    }

                    InputRow("song_url_hash", get("song_url_hash"), { inputs["song_url_hash"] = it }, "歌曲Hash")
                    InputRow("song_url_album", get("song_url_album"), { inputs["song_url_album"] = it }, "AlbumId")
                    InputRow("song_url_audioid", get("song_url_audioid"), { inputs["song_url_audioid"] = it }, "AlbumAudioId")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌曲播放链接 (v5)") {
                            val resp = client.song.getSongUrl(
                                get("song_url_hash"),
                                get("song_url_album").toLongOrNull() ?: 0L,
                                get("song_url_audioid").toLongOrNull() ?: 0L
                            )
                            addLog("Song:getSongUrl", resp.body)
                        }
                        TestButton("歌曲播放链接 (v6)") {
                            val resp = client.song.getSongUrlNew(
                                get("song_url_hash"),
                                get("song_url_audioid").toLongOrNull() ?: 0L
                            )
                            addLog("Song:getSongUrlNew", resp.body)
                        }
                    }

                    InputRow("song_climax_hash", get("song_climax_hash"), { inputs["song_climax_hash"] = it }, "Hash (多个逗号分隔)")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌曲高潮部分") {
                            val resp = client.song.getSongClimax(get("song_climax_hash"))
                            addLog("Song:getSongClimax", resp.body)
                        }
                    }

                    InputRow("song_rankinfo_id", get("song_rankinfo_id"), { inputs["song_rankinfo_id"] = it }, "AlbumAudioId")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌曲成绩单") {
                            val resp = client.song.getSongRanking(get("song_rankinfo_id").toLongOrNull() ?: 0L)
                            addLog("Song:getSongRanking", resp.body)
                        }
                        TestButton("歌曲榜单过滤") {
                            val resp = client.song.getSongRankingFilter(get("song_rankinfo_id").toLongOrNull() ?: 0L)
                            addLog("Song:getSongRankingFilter", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 专辑 ============
                    TestSection("专辑 - Album") {
                        TestButton("默认专辑详情") {
                            val resp = client.album.getAlbumDetail("1064292")
                            addLog("Album:getAlbumDetail", resp.body)
                        }
                        TestButton("专辑商店") {
                            val resp = client.album.getAlbumShop()
                            addLog("Album:getAlbumShop", resp.body)
                        }
                    }
                    InputRow("album_detail_id", get("album_detail_id"), { inputs["album_detail_id"] = it }, "专辑ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("专辑详情") {
                            val resp = client.album.getAlbumDetail(get("album_detail_id", "1064292"))
                            addLog("Album:getAlbumDetail", resp.body)
                        }
                    }

                    InputRow("album_meta_id", get("album_meta_id"), { inputs["album_meta_id"] = it }, "专辑ID")
                    InputRow("album_meta_lang", get("album_meta_lang"), { inputs["album_meta_lang"] = it }, "语言 (默认zh-CN)")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("专辑元数据") {
                            val resp = client.album.getAlbumMetadata(get("album_meta_id"), get("album_meta_lang", "zh-CN"))
                            addLog("Album:getAlbumMetadata", resp.body)
                        }
                    }

                    InputRow("album_songs_id", get("album_songs_id"), { inputs["album_songs_id"] = it }, "专辑ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("专辑歌曲列表") {
                            val resp = client.album.getAlbumSongs(get("album_songs_id"))
                            addLog("Album:getAlbumSongs", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 歌手 ============
                    TestSection("歌手 - Artist") {
                        TestButton("默认歌手详情") {
                            val resp = client.artist.getDetail("3066")
                            addLog("Artist:getDetail", resp.body)
                        }
                    }
                    InputRow("artist_detail_id", get("artist_detail_id"), { inputs["artist_detail_id"] = it }, "歌手ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌手详情") {
                            val resp = client.artist.getDetail(get("artist_detail_id", "3066"))
                            addLog("Artist:getDetail", resp.body)
                        }
                    }

                    InputRow("artist_albums_id", get("artist_albums_id"), { inputs["artist_albums_id"] = it }, "歌手ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌手专辑") {
                            val resp = client.artist.getAlbums(get("artist_albums_id", "3066"))
                            addLog("Artist:getAlbums", resp.body)
                        }
                    }

                    InputRow("artist_audios_id", get("artist_audios_id"), { inputs["artist_audios_id"] = it }, "歌手ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌手歌曲") {
                            val resp = client.artist.getAudios(get("artist_audios_id", "3066"))
                            addLog("Artist:getAudios", resp.body)
                        }
                    }

                    InputRow("artist_videos_id", get("artist_videos_id"), { inputs["artist_videos_id"] = it }, "歌手ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌手视频") {
                            val resp = client.artist.getVideos(get("artist_videos_id", "3066"))
                            addLog("Artist:getVideos", resp.body)
                        }
                    }

                    TestButton("歌手列表") {
                        val resp = client.artist.getLists()
                        addLog("Artist:getLists", resp.body)
                    }

                    InputRow("artist_follow_id", get("artist_follow_id"), { inputs["artist_follow_id"] = it }, "歌手ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("关注歌手") {
                            val resp = client.artist.follow(get("artist_follow_id"))
                            addLog("Artist:follow", resp.body)
                        }
                        TestButton("取消关注") {
                            val resp = client.artist.unfollow(get("artist_follow_id"))
                            addLog("Artist:unfollow", resp.body)
                        }
                    }

                    TestButton("关注歌手新歌") {
                        val resp = client.artist.getFollowNewSongs(0L)
                        addLog("Artist:getFollowNewSongs", resp.body)
                    }

                    InputRow("artist_honour_id", get("artist_honour_id"), { inputs["artist_honour_id"] = it }, "歌手ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌手荣誉") {
                            val resp = client.artist.getHonour(get("artist_honour_id", "3066"))
                            addLog("Artist:getHonour", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 歌单 ============
                    TestSection("歌单 - Playlist") {
                        TestButton("默认歌单详情") {
                            val resp = client.playlist.getPlaylistDetail("552406")
                            addLog("Playlist:getDetail", resp.body)
                        }
                        TestButton("歌单标签") {
                            val resp = client.playlist.getPlaylistTags()
                            addLog("Playlist:getTags", resp.body)
                        }
                        TestButton("精选歌单") {
                            val resp = client.playlist.getEffectPlaylists()
                            addLog("Playlist:getEffect", resp.body)
                        }
                    }
                    InputRow("playlist_detail_id", get("playlist_detail_id"), { inputs["playlist_detail_id"] = it }, "歌单ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌单详情") {
                            val resp = client.playlist.getPlaylistDetail(get("playlist_detail_id", "552406"))
                            addLog("Playlist:getDetail", resp.body)
                        }
                    }

                    InputRow("playlist_similar_id", get("playlist_similar_id"), { inputs["playlist_similar_id"] = it }, "歌单ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("相似歌单") {
                            val resp = client.playlist.getSimilarPlaylists(get("playlist_similar_id", "552406"))
                            addLog("Playlist:getSimilar", resp.body)
                        }
                    }

                    InputRow("playlist_tracks_id", get("playlist_tracks_id"), { inputs["playlist_tracks_id"] = it }, "歌单ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌单歌曲") {
                            val resp = client.playlist.getPlaylistTracks(get("playlist_tracks_id", "552406"))
                            addLog("Playlist:getTracks", resp.body)
                        }
                        TestButton("歌单歌曲(New)") {
                            val resp = client.playlist.getPlaylistTracksNew(get("playlist_tracks_id", "552406"))
                            addLog("Playlist:getTracksNew", resp.body)
                        }
                    }

                    InputRow("playlist_add_name", get("playlist_add_name"), { inputs["playlist_add_name"] = it }, "歌单名称")
                    InputRow("playlist_add_uid", get("playlist_add_uid"), { inputs["playlist_add_uid"] = it }, "创建者Userid")
                    InputRow("playlist_add_listid", get("playlist_add_listid"), { inputs["playlist_add_listid"] = it }, "歌单ListId")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("创建歌单") {
                            val resp = client.playlist.addPlaylist(
                                get("playlist_add_name", "测试歌单"),
                                get("playlist_add_uid"),
                                get("playlist_add_listid")
                            )
                            addLog("Playlist:addPlaylist", resp.body)
                        }
                    }

                    InputRow("playlist_del_id", get("playlist_del_id"), { inputs["playlist_del_id"] = it }, "歌单ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("删除歌单") {
                            val resp = client.playlist.deletePlaylist(get("playlist_del_id").toLongOrNull() ?: 0L)
                            addLog("Playlist:deletePlaylist", resp.body)
                        }
                    }

                    InputRow("playlist_addtracks_id", get("playlist_addtracks_id"), { inputs["playlist_addtracks_id"] = it }, "歌单ID")
                    InputRow("playlist_addtracks_hash", get("playlist_addtracks_hash"), { inputs["playlist_addtracks_hash"] = it }, "歌曲Hash")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("添加歌曲到歌单") {
                            val resp = client.playlist.addTracks(get("playlist_addtracks_id"), get("playlist_addtracks_hash"))
                            addLog("Playlist:addTracks", resp.body)
                        }
                    }

                    InputRow("playlist_remtracks_id", get("playlist_remtracks_id"), { inputs["playlist_remtracks_id"] = it }, "歌单ID")
                    InputRow("playlist_remtracks_hash", get("playlist_remtracks_hash"), { inputs["playlist_remtracks_hash"] = it }, "歌曲Hash")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("从歌单移除歌曲") {
                            val resp = client.playlist.removeTracks(get("playlist_remtracks_id"), get("playlist_remtracks_hash"))
                            addLog("Playlist:removeTracks", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 评论 ============
                    TestSection("评论 - Comment") {
                        TestButton("默认歌曲评论") {
                            val resp = client.comment.getMusicComments("34796338")
                            addLog("Comment:getMusic", resp.body)
                        }
                    }
                    InputRow("comment_music_id", get("comment_music_id"), { inputs["comment_music_id"] = it }, "歌曲MixSongID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌曲评论") {
                            val resp = client.comment.getMusicComments(get("comment_music_id", "34796338"))
                            addLog("Comment:getMusic", resp.body)
                        }
                    }

                    InputRow("comment_album_id", get("comment_album_id"), { inputs["comment_album_id"] = it }, "专辑ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("专辑评论") {
                            val resp = client.comment.getAlbumComments(get("comment_album_id"))
                            addLog("Comment:getAlbum", resp.body)
                        }
                    }

                    InputRow("comment_playlist_id", get("comment_playlist_id"), { inputs["comment_playlist_id"] = it }, "歌单ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("歌单评论") {
                            val resp = client.comment.getPlaylistComments(get("comment_playlist_id"))
                            addLog("Comment:getPlaylist", resp.body)
                        }
                    }

                    InputRow("comment_count_id", get("comment_count_id"), { inputs["comment_count_id"] = it }, "资源ID")
                    InputRow("comment_count_type", get("comment_count_type"), { inputs["comment_count_type"] = it }, "类型 (music/album/playlist)")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("评论数量") {
                            val resp = client.comment.getCommentCount(get("comment_count_id"), get("comment_count_type", "music"))
                            addLog("Comment:getCount", resp.body)
                        }
                    }

                    InputRow("comment_floor_rid", get("comment_floor_rid"), { inputs["comment_floor_rid"] = it }, "评论ID")
                    InputRow("comment_floor_eid", get("comment_floor_eid"), { inputs["comment_floor_eid"] = it }, "EId")
                    InputRow("comment_floor_sid", get("comment_floor_sid"), { inputs["comment_floor_sid"] = it }, "MixSongID")
                    InputRow("comment_floor_type", get("comment_floor_type"), { inputs["comment_floor_type"] = it }, "类型")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("楼层评论") {
                            val resp = client.comment.getFloorComments(
                                get("comment_floor_rid"), get("comment_floor_eid"), get("comment_floor_sid"),
                                get("comment_floor_type", "music")
                            )
                            addLog("Comment:getFloor", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 排行榜 ============
                    TestSection("排行榜 - Rank") {
                        TestButton("排行列表") {
                            val resp = client.rank.getList()
                            addLog("Rank:getList", resp.body)
                        }
                        TestButton("排行榜推荐") {
                            val resp = client.rank.getTop()
                            addLog("Rank:getTop", resp.body)
                        }
                    }
                    InputRow("rank_info_id", get("rank_info_id"), { inputs["rank_info_id"] = it }, "排行榜ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("排行榜详情") {
                            val resp = client.rank.getInfo(get("rank_info_id"))
                            addLog("Rank:getInfo", resp.body)
                        }
                    }

                    InputRow("rank_audio_id", get("rank_audio_id"), { inputs["rank_audio_id"] = it }, "排行榜ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("排行榜音乐") {
                            val resp = client.rank.getAudio(get("rank_audio_id"))
                            addLog("Rank:getAudio", resp.body)
                        }
                    }

                    InputRow("rank_vol_id", get("rank_vol_id"), { inputs["rank_vol_id"] = it }, "排行榜ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("排行榜往期") {
                            val resp = client.rank.getVol(get("rank_vol_id"))
                            addLog("Rank:getVol", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 推荐 ============
                    TestSection("推荐 - Recommend") {
                        TestButton("每日推荐") {
                            val resp = client.recommend.getDailyRecommend()
                            addLog("Recommend:getDaily", resp.body)
                        }
                        TestButton("私人FM") {
                            val resp = client.recommend.getPersonalFm()
                            addLog("Recommend:getPersonalFm", resp.body)
                        }
                        TestButton("好友推荐") {
                            val resp = client.recommend.getFriendRecommend()
                            addLog("Recommend:getFriend", resp.body)
                        }
                    }

                    InputRow("rec_ai_id", get("rec_ai_id"), { inputs["rec_ai_id"] = it }, "专辑ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("AI推荐") {
                            val resp = client.recommend.getAiRecommend(get("rec_ai_id", "1064292"))
                            addLog("Recommend:getAi", resp.body)
                        }
                    }

                    InputRow("rec_style_scene", get("rec_style_scene"), { inputs["rec_style_scene"] = it }, "场景ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("风格推荐") {
                            val resp = client.recommend.getStyleRecommend(get("rec_style_scene", "0"))
                            addLog("Recommend:getStyle", resp.body)
                        }
                    }

                    InputRow("rec_songs_id", get("rec_songs_id"), { inputs["rec_songs_id"] = it }, "歌曲Hash")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("推荐歌曲") {
                            val resp = client.recommend.getRecommendSongs(get("rec_songs_id"))
                            addLog("Recommend:getSongs", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 用户 ============
                    TestSection("用户 - User") {
                        TestButton("最近收听") {
                            val resp = client.user.getLatestSongsListen()
                            addLog("User:getLatestSongs", resp.body)
                        }
                    }
                    InputRow("user_fav_id", get("user_fav_id"), { inputs["user_fav_id"] = it }, "用户ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("收藏数量") {
                            val resp = client.user.getFavoriteCount(get("user_fav_id"))
                            addLog("User:getFavCount", resp.body)
                        }
                    }

                    InputRow("user_play_hash", get("user_play_hash"), { inputs["user_play_hash"] = it }, "歌曲Hash")
                    InputRow("user_play_songid", get("user_play_songid"), { inputs["user_play_songid"] = it }, "歌曲ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("上传播放记录") {
                            val resp = client.user.uploadPlayHistory(
                                get("user_play_hash").toLongOrNull() ?: 0L,
                                get("user_play_songid").toLongOrNull() ?: 0L
                            )
                            addLog("User:uploadPlay", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 电台 ============
                    TestSection("电台 - Radio") {
                        TestButton("电台分类") {
                            val resp = client.radio.getFmClass()
                            addLog("Radio:getFmClass", resp.body)
                        }
                        TestButton("电台推荐") {
                            val resp = client.radio.getFmRecommend()
                            addLog("Radio:getFmRecommend", resp.body)
                        }
                        TestButton("PC电台") {
                            val resp = client.radio.getPcDiantai()
                            addLog("Radio:getPcDiantai", resp.body)
                        }
                    }
                    InputRow("radio_songs_id", get("radio_songs_id"), { inputs["radio_songs_id"] = it }, "电台ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("电台歌曲") {
                            val resp = client.radio.getFmSongs(get("radio_songs_id"))
                            addLog("Radio:getFmSongs", resp.body)
                        }
                    }

                    InputRow("radio_img_id", get("radio_img_id"), { inputs["radio_img_id"] = it }, "电台ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("电台图片") {
                            val resp = client.radio.getFmImage(get("radio_img_id"))
                            addLog("Radio:getFmImage", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 场景音乐 ============
                    TestSection("场景音乐 - SceneMusic") {
                        TestButton("场景列表") {
                            val resp = client.sceneMusic.getLists()
                            addLog("Scene:getLists", resp.body)
                        }
                    }
                    InputRow("scene_v2_id", get("scene_v2_id"), { inputs["scene_v2_id"] = it }, "场景ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("场景列表V2") {
                            val resp = client.sceneMusic.getListsV2(get("scene_v2_id"))
                            addLog("Scene:getListsV2", resp.body)
                        }
                    }

                    InputRow("scene_module_id", get("scene_module_id"), { inputs["scene_module_id"] = it }, "模块ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("场景模块") {
                            val resp = client.sceneMusic.getModule(get("scene_module_id"))
                            addLog("Scene:getModule", resp.body)
                        }
                    }

                    InputRow("scene_music_id", get("scene_music_id"), { inputs["scene_music_id"] = it }, "场景ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("场景音乐") {
                            val resp = client.sceneMusic.getMusic(get("scene_music_id"))
                            addLog("Scene:getMusic", resp.body)
                        }
                    }

                    InputRow("scene_audio_id", get("scene_audio_id"), { inputs["scene_audio_id"] = it }, "场景ID")
                    InputRow("scene_audio_module", get("scene_audio_module"), { inputs["scene_audio_module"] = it }, "模块ID")
                    InputRow("scene_audio_tag", get("scene_audio_tag"), { inputs["scene_audio_tag"] = it }, "标签")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("场景音频列表") {
                            val resp = client.sceneMusic.getAudioList(get("scene_audio_id"), get("scene_audio_module"), get("scene_audio_tag"))
                            addLog("Scene:getAudioList", resp.body)
                        }
                    }

                    InputRow("scene_video_id", get("scene_video_id"), { inputs["scene_video_id"] = it }, "场景ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("场景视频") {
                            val resp = client.sceneMusic.getVideoList(get("scene_video_id"))
                            addLog("Scene:getVideoList", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 图片 ============
                    TestSection("图片 - Image") {
                        TestButton("默认图片") {
                            val resp = client.image.getImages("E6B6478980F16C6A97491C781A327E7A")
                            addLog("Image:getImages", resp.body)
                        }
                    }
                    InputRow("img_hash", get("img_hash"), { inputs["img_hash"] = it }, "Hash")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("获取图片") {
                            val resp = client.image.getImages(get("img_hash"))
                            addLog("Image:getImages", resp.body)
                        }
                    }

                    InputRow("img_audio_hash", get("img_audio_hash"), { inputs["img_audio_hash"] = it }, "Hash")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("音频图片") {
                            val resp = client.image.getAudioImages(get("img_audio_hash"))
                            addLog("Image:getAudioImages", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 视频 ============
                    TestSection("视频 - Video") {
                        // No default button, needs params
                    }
                    InputRow("video_album_audio_id", get("video_album_audio_id"), { inputs["video_album_audio_id"] = it }, "Hash")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("KRM音频MV") {
                            val resp = client.video.getKmrAudioMv(get("video_album_audio_id"))
                            addLog("Video:getKmrAudioMv", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 有声书 ============
                    TestSection("有声书 - LongAudio") {
                        TestButton("推荐排名") {
                            val resp = client.longAudio.getRankRecommend()
                            addLog("LongAudio:getRankRecommend", resp.body)
                        }
                        TestButton("VIP推荐") {
                            val resp = client.longAudio.getVipRecommend()
                            addLog("LongAudio:getVipRecommend", resp.body)
                        }
                        TestButton("每周推荐") {
                            val resp = client.longAudio.getWeekRecommend()
                            addLog("LongAudio:getWeekRecommend", resp.body)
                        }
                    }
                    InputRow("longaudio_album_id", get("longaudio_album_id"), { inputs["longaudio_album_id"] = it }, "有声书专辑ID")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("有声书专辑") {
                            val resp = client.longAudio.getAlbumAudios(get("longaudio_album_id"))
                            addLog("LongAudio:getAlbumAudios", resp.body)
                        }
                        TestButton("有声书详情") {
                            val resp = client.longAudio.getAlbumDetail(get("longaudio_album_id"))
                            addLog("LongAudio:getAlbumDetail", resp.body)
                        }
                    }

                    TestButton("有声书每日推荐") {
                        val resp = client.longAudio.getDailyRecommend()
                        addLog("LongAudio:getDailyRecommend", resp.body)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 杂项 ============
                    TestSection("杂项 - Misc") {
                        TestButton("刷刷Feed") {
                            val resp = client.misc.brush()
                            addLog("Misc:brush", resp.body)
                        }
                        TestButton("IP区域") {
                            val resp = client.misc.getIpZone()
                            addLog("Misc:getIpZone", resp.body)
                        }
                    }

                    InputRow("misc_ip_data", get("misc_ip_data"), { inputs["misc_ip_data"] = it }, "IP地址")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("IP数据") {
                            val resp = client.misc.getIpData(get("misc_ip_data"))
                            addLog("Misc:getIpData", resp.body)
                        }
                        TestButton("IP详情") {
                            val resp = client.misc.getIpDetail(get("misc_ip_data"))
                            addLog("Misc:getIpDetail", resp.body)
                        }
                    }

                    InputRow("misc_ip_playlist", get("misc_ip_playlist"), { inputs["misc_ip_playlist"] = it }, "IP标识")
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("IP歌单") {
                            val resp = client.misc.getIpPlaylist(get("misc_ip_playlist"))
                            addLog("Misc:getIpPlaylist", resp.body)
                        }
                        TestButton("IP首页") {
                            val resp = client.misc.getIpZoneHome(get("misc_ip_playlist"))
                            addLog("Misc:getIpZoneHome", resp.body)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // ============ 清空日志 ============
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TestButton("清空日志", color = MaterialTheme.colorScheme.error) {
                            logText = "日志已清空"
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("运行日志:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    // 日志显示区
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .background(Color.Black.copy(alpha = 0.05f))
                            .padding(8.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = logText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
//  通用组件
// ============================================================

@Composable
fun TestSection(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun InputRow(
    @Suppress("UNUSED_PARAMETER") key: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        singleLine = true
    )
}

@Composable
fun TestButton(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            scope.launch {
                loading = true
                try {
                    onClick()
                } catch (e: Exception) {
                    // 内部处理异常
                } finally {
                    loading = false
                }
            }
        },
        enabled = !loading,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontSize = 12.sp)
        }
    }
}
