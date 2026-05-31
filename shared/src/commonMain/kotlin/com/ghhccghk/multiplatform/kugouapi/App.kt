@file:OptIn(ExperimentalLayoutApi::class)

package com.ghhccghk.multiplatform.kugouapi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
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
            } catch (_: Exception) {
                content.toString()
            }
            logText = "[$tag]\n$formatted\n\n------------------\n$logText"
        }

        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(
                    "酷狗 SDK 测试",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                val inputs = remember { mutableStateMapOf<String, String>() }
                fun get(key: String, default: String = "") = inputs[key] ?: default

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ============ 全局凭证 ============
                    SectionHeader("全局凭证")
                    ActionRow {
                        Input("userid", get("userid"), { inputs["userid"] = it; client.cookieJar.setUserid(it.toLongOrNull() ?: 0L) }, "用户ID")
                        Input("token", get("token"), { inputs["token"] = it; client.cookieJar.setToken(it) }, "Token")
                        Btn("注册设备") { addLog("Auth:registerDev", client.auth.registerDev().body) }
                    }

                    Divider()

                    // ============ 身份认证 ============
                    SectionHeader("身份认证")
                    ActionRow {
                        Btn("登录设备列表") { addLog("Auth:getLoginDevices", client.auth.getLoginDevices().body) }
                        Btn("创建微信登录") { addLog("Auth:createWxLogin", client.auth.createWxLogin().body) }
                    }
                    ActionRow {
                        Input("sendCaptcha_phone", get("sendCaptcha_phone"), { inputs["sendCaptcha_phone"] = it }, "手机号")
                        Btn("发送验证码") { addLog("Auth:sendCaptcha", client.auth.sendCaptcha(get("sendCaptcha_phone")).body) }
                    }
                    ActionRow {
                        Input("login_pwd_user", get("login_pwd_user"), { inputs["login_pwd_user"] = it }, "用户名")
                        Input("login_pwd_pass", get("login_pwd_pass"), { inputs["login_pwd_pass"] = it }, "密码")
                        Btn("密码登录") { addLog("Auth:loginByPassword", client.auth.loginByPassword(get("login_pwd_user"), get("login_pwd_pass")).body) }
                    }
                    ActionRow {
                        Input("login_code_phone", get("login_code_phone"), { inputs["login_code_phone"] = it }, "手机号")
                        Input("login_code_code", get("login_code_code"), { inputs["login_code_code"] = it }, "验证码")
                        Btn("验证码登录") { addLog("Auth:loginByPhoneCode", client.auth.loginByPhoneCode(get("login_code_phone"), get("login_code_code")).body) }
                    }
                    ActionRow {
                        Input("login_token_token", get("login_token_token"), { inputs["login_token_token"] = it }, "Token")
                        Input("login_token_userid", get("login_token_userid"), { inputs["login_token_userid"] = it }, "Userid")
                        Btn("Token登录") { addLog("Auth:loginByToken", client.auth.loginByToken(get("login_token_token"), get("login_token_userid")).body) }
                    }
                    ActionRow {
                        Input("qrkey_keyword", get("qrkey_keyword"), { inputs["qrkey_keyword"] = it }, "关键词", "qrcode")
                        Btn("创建扫码Key") { addLog("Auth:createQrKey", client.auth.createQrKey(get("qrkey_keyword", "qrcode")).body) }
                    }
                    ActionRow {
                        Input("qrcheck_key", get("qrcheck_key"), { inputs["qrcheck_key"] = it }, "Key")
                        Btn("检查扫码") { addLog("Auth:checkQrCode", client.auth.checkQrCode(get("qrcheck_key")).body) }
                    }
                    ActionRow {
                        Input("wxcheck_uuid", get("wxcheck_uuid"), { inputs["wxcheck_uuid"] = it }, "UUID")
                        Btn("检查微信登录") { addLog("Auth:checkWxLogin", client.auth.checkWxLogin(get("wxcheck_uuid")).body) }
                    }
                    ActionRow {
                        Input("kick_mid", get("kick_mid"), { inputs["kick_mid"] = it }, "目标MID")
                        Btn("踢出设备") { addLog("Auth:kickDevice", client.auth.kickDevice(get("kick_mid")).body) }
                    }
                    ActionRow {
                        Input("openplat_code", get("openplat_code"), { inputs["openplat_code"] = it }, "微信Code")
                        Btn("开放平台登录") { addLog("Auth:loginByOpenPlat", client.auth.loginByOpenPlat(get("openplat_code")).body) }
                    }

                    Divider()

                    // ============ 搜索 ============
                    SectionHeader("搜索")
                    ActionRow {
                        Btn("热搜榜") { addLog("Search:searchHot", client.search.searchHot().body) }
                        Btn("搜索默认词") { addLog("Search:searchDefault", client.search.searchDefault().body) }
                    }
                    ActionRow {
                        Input("search_keyword", get("search_keyword", "周杰伦"), { inputs["search_keyword"] = it }, "关键词", "周杰伦")
                        Btn("搜索") { addLog("Search:search", client.search.search(get("search_keyword", "周杰伦")).body) }
                        Btn("混合") { addLog("Search:searchMixed", client.search.searchMixed(get("search_keyword", "周杰伦")).body) }
                        Btn("联想") { addLog("Search:searchSuggest", client.search.searchSuggest(get("search_keyword", "周杰伦")).body) }
                        Btn("复杂") { addLog("Search:searchComplex", client.search.searchComplex(get("search_keyword", "周杰伦")).body) }
                        Btn("歌词") { addLog("Search:searchLyric", client.search.searchLyric(get("search_keyword", "周杰伦")).body) }
                    }

                    Divider()

                    // ============ 歌曲 ============
                    SectionHeader("歌曲")
                    ActionRow {
                        Input("song_hash", get("song_hash"), { inputs["song_hash"] = it }, "Hash")
                        Btn("歌曲信息") { addLog("Song:getAudioInfo", client.song.getAudioInfo(get("song_hash", "E6B6478980F16C6A97491C781A327E7A")).body) }
                    }
                    ActionRow {
                        Input("song_lyric_id", get("song_lyric_id"), { inputs["song_lyric_id"] = it }, "歌词ID")
                        Input("song_lyric_key", get("song_lyric_key"), { inputs["song_lyric_key"] = it }, "AccessKey")
                        Btn("获取歌词") { addLog("Song:getLyric", client.song.getLyric(get("song_lyric_id", "191543369"), get("song_lyric_key", "1AE02A6C2E7596187C717727708422CB"), decode = true).body) }
                    }
                    ActionRow {
                        Input("song_related_id", get("song_related_id"), { inputs["song_related_id"] = it }, "AlbumAudioId")
                        Btn("相关歌曲") { addLog("Song:getRelatedAudio", client.song.getRelatedAudio(get("song_related_id").toLongOrNull() ?: 0L).body) }
                    }
                    ActionRow {
                        Input("song_match_hash", get("song_match_hash"), { inputs["song_match_hash"] = it }, "Hash")
                        Input("song_match_file", get("song_match_file"), { inputs["song_match_file"] = it }, "文件名")
                        Btn("匹配伴奏") { addLog("Song:matchAccompany", client.song.matchAccompany(get("song_match_hash"), get("song_match_file")).body) }
                    }
                    ActionRow {
                        Input("song_ktv_id", get("song_ktv_id"), { inputs["song_ktv_id"] = it }, "SongId")
                        Input("song_ktv_singer", get("song_ktv_singer"), { inputs["song_ktv_singer"] = it }, "歌手")
                        Input("song_ktv_hash", get("song_ktv_hash"), { inputs["song_ktv_hash"] = it }, "Hash")
                        Btn("KTV总数") { addLog("Song:getKtvTotal", client.song.getKtvTotal(get("song_ktv_id").toLongOrNull() ?: 0L, get("song_ktv_singer"), get("song_ktv_hash")).body) }
                    }
                    ActionRow {
                        Input("song_priv_hash", get("song_priv_hash"), { inputs["song_priv_hash"] = it }, "Hash")
                        Input("song_priv_album", get("song_priv_album"), { inputs["song_priv_album"] = it }, "AlbumId")
                        Btn("权限(Lite)") { addLog("Song:getPrivilegeLite", client.song.getPrivilegeLite(get("song_priv_hash"), get("song_priv_album")).body) }
                    }
                    ActionRow {
                        Input("song_krm_id", get("song_krm_id"), { inputs["song_krm_id"] = it }, "AlbumAudioId")
                        Btn("KRM元数据") { addLog("Song:getKrmAudio", client.song.getKrmAudio(get("song_krm_id")).body) }
                    }
                    ActionRow {
                        Input("song_url_hash", get("song_url_hash"), { inputs["song_url_hash"] = it }, "Hash")
                        Input("song_url_album", get("song_url_album"), { inputs["song_url_album"] = it }, "AlbumId")
                        Input("song_url_audioid", get("song_url_audioid"), { inputs["song_url_audioid"] = it }, "AudioId")
                        Btn("v5") { addLog("Song:getSongUrl", client.song.getSongUrl(get("song_url_hash"), get("song_url_album").toLongOrNull() ?: 0L, get("song_url_audioid").toLongOrNull() ?: 0L).body) }
                        Btn("v6") { addLog("Song:getSongUrlNew", client.song.getSongUrlNew(get("song_url_hash"), get("song_url_audioid").toLongOrNull() ?: 0L).body) }
                    }
                    ActionRow {
                        Input("song_climax_hash", get("song_climax_hash"), { inputs["song_climax_hash"] = it }, "Hash")
                        Btn("高潮部分") { addLog("Song:getSongClimax", client.song.getSongClimax(get("song_climax_hash")).body) }
                    }
                    ActionRow {
                        Input("song_rankinfo_id", get("song_rankinfo_id"), { inputs["song_rankinfo_id"] = it }, "AudioId")
                        Btn("成绩单") { addLog("Song:getSongRanking", client.song.getSongRanking(get("song_rankinfo_id").toLongOrNull() ?: 0L).body) }
                        Btn("榜单过滤") { addLog("Song:getSongRankingFilter", client.song.getSongRankingFilter(get("song_rankinfo_id").toLongOrNull() ?: 0L).body) }
                    }

                    Divider()

                    // ============ 专辑 ============
                    SectionHeader("专辑")
                    ActionRow {
                        Input("album_detail_id", get("album_detail_id"), { inputs["album_detail_id"] = it }, "专辑ID", "1064292")
                        Btn("详情") { addLog("Album:getAlbumDetail", client.album.getAlbumDetail(get("album_detail_id", "1064292")).body) }
                        Btn("商店") { addLog("Album:getAlbumShop", client.album.getAlbumShop().body) }
                    }
                    ActionRow {
                        Input("album_meta_id", get("album_meta_id"), { inputs["album_meta_id"] = it }, "专辑ID")
                        Input("album_meta_lang", get("album_meta_lang"), { inputs["album_meta_lang"] = it }, "语言", "zh-CN")
                        Btn("元数据") { addLog("Album:getAlbumMetadata", client.album.getAlbumMetadata(get("album_meta_id"), get("album_meta_lang", "zh-CN")).body) }
                    }
                    ActionRow {
                        Input("album_songs_id", get("album_songs_id"), { inputs["album_songs_id"] = it }, "专辑ID")
                        Btn("歌曲列表") { addLog("Album:getAlbumSongs", client.album.getAlbumSongs(get("album_songs_id")).body) }
                    }

                    Divider()

                    // ============ 歌手 ============
                    SectionHeader("歌手")
                    ActionRow {
                        Input("artist_detail_id", get("artist_detail_id"), { inputs["artist_detail_id"] = it }, "歌手ID", "3066")
                        Btn("详情") { addLog("Artist:getDetail", client.artist.getDetail(get("artist_detail_id", "3066")).body) }
                        Btn("列表") { addLog("Artist:getLists", client.artist.getLists().body) }
                    }
                    ActionRow {
                        Input("artist_albums_id", get("artist_albums_id"), { inputs["artist_albums_id"] = it }, "歌手ID", "3066")
                        Btn("专辑") { addLog("Artist:getAlbums", client.artist.getAlbums(get("artist_albums_id", "3066")).body) }
                    }
                    ActionRow {
                        Input("artist_audios_id", get("artist_audios_id"), { inputs["artist_audios_id"] = it }, "歌手ID", "3066")
                        Btn("歌曲") { addLog("Artist:getAudios", client.artist.getAudios(get("artist_audios_id", "3066")).body) }
                    }
                    ActionRow {
                        Input("artist_videos_id", get("artist_videos_id"), { inputs["artist_videos_id"] = it }, "歌手ID", "3066")
                        Btn("视频") { addLog("Artist:getVideos", client.artist.getVideos(get("artist_videos_id", "3066")).body) }
                    }
                    ActionRow {
                        Input("artist_follow_id", get("artist_follow_id"), { inputs["artist_follow_id"] = it }, "歌手ID")
                        Btn("关注") { addLog("Artist:follow", client.artist.follow(get("artist_follow_id")).body) }
                        Btn("取消关注") { addLog("Artist:unfollow", client.artist.unfollow(get("artist_follow_id")).body) }
                        Btn("新歌") { addLog("Artist:getFollowNewSongs", client.artist.getFollowNewSongs(0L).body) }
                    }
                    ActionRow {
                        Input("artist_honour_id", get("artist_honour_id"), { inputs["artist_honour_id"] = it }, "歌手ID", "3066")
                        Btn("荣誉") { addLog("Artist:getHonour", client.artist.getHonour(get("artist_honour_id", "3066")).body) }
                    }

                    Divider()

                    // ============ 歌单 ============
                    SectionHeader("歌单")
                    ActionRow {
                        Input("playlist_detail_id", get("playlist_detail_id"), { inputs["playlist_detail_id"] = it }, "歌单ID", "552406")
                        Btn("详情") { addLog("Playlist:getDetail", client.playlist.getPlaylistDetail(get("playlist_detail_id", "552406")).body) }
                        Btn("标签") { addLog("Playlist:getTags", client.playlist.getPlaylistTags().body) }
                        Btn("精选") { addLog("Playlist:getEffect", client.playlist.getEffectPlaylists().body) }
                    }
                    ActionRow {
                        Input("playlist_similar_id", get("playlist_similar_id"), { inputs["playlist_similar_id"] = it }, "歌单ID")
                        Btn("相似") { addLog("Playlist:getSimilar", client.playlist.getSimilarPlaylists(get("playlist_similar_id", "552406")).body) }
                    }
                    ActionRow {
                        Input("playlist_tracks_id", get("playlist_tracks_id"), { inputs["playlist_tracks_id"] = it }, "歌单ID")
                        Btn("歌曲") { addLog("Playlist:getTracks", client.playlist.getPlaylistTracks(get("playlist_tracks_id", "552406")).body) }
                        Btn("歌曲(New)") { addLog("Playlist:getTracksNew", client.playlist.getPlaylistTracksNew(get("playlist_tracks_id", "552406")).body) }
                    }
                    ActionRow {
                        Input("playlist_add_name", get("playlist_add_name"), { inputs["playlist_add_name"] = it }, "名称", "测试歌单")
                        Input("playlist_add_uid", get("playlist_add_uid"), { inputs["playlist_add_uid"] = it }, "Userid")
                        Input("playlist_add_listid", get("playlist_add_listid"), { inputs["playlist_add_listid"] = it }, "ListId")
                        Btn("创建") { addLog("Playlist:addPlaylist", client.playlist.addPlaylist(get("playlist_add_name", "测试歌单"), get("playlist_add_uid"), get("playlist_add_listid")).body) }
                    }
                    ActionRow {
                        Input("playlist_del_id", get("playlist_del_id"), { inputs["playlist_del_id"] = it }, "歌单ID")
                        Btn("删除") { addLog("Playlist:deletePlaylist", client.playlist.deletePlaylist(get("playlist_del_id").toLongOrNull() ?: 0L).body) }
                    }
                    ActionRow {
                        Input("playlist_addtracks_id", get("playlist_addtracks_id"), { inputs["playlist_addtracks_id"] = it }, "歌单ID")
                        Input("playlist_addtracks_hash", get("playlist_addtracks_hash"), { inputs["playlist_addtracks_hash"] = it }, "Hash")
                        Btn("添加歌曲") { addLog("Playlist:addTracks", client.playlist.addTracks(get("playlist_addtracks_id"), get("playlist_addtracks_hash")).body) }
                    }
                    ActionRow {
                        Input("playlist_remtracks_id", get("playlist_remtracks_id"), { inputs["playlist_remtracks_id"] = it }, "歌单ID")
                        Input("playlist_remtracks_hash", get("playlist_remtracks_hash"), { inputs["playlist_remtracks_hash"] = it }, "Hash")
                        Btn("移除歌曲") { addLog("Playlist:removeTracks", client.playlist.removeTracks(get("playlist_remtracks_id"), get("playlist_remtracks_hash")).body) }
                    }

                    Divider()

                    // ============ 评论 ============
                    SectionHeader("评论")
                    ActionRow {
                        Input("comment_music_id", get("comment_music_id"), { inputs["comment_music_id"] = it }, "MixSongID", "34796338")
                        Btn("歌曲评论") { addLog("Comment:getMusic", client.comment.getMusicComments(get("comment_music_id", "34796338")).body) }
                    }
                    ActionRow {
                        Input("comment_album_id", get("comment_album_id"), { inputs["comment_album_id"] = it }, "专辑ID")
                        Btn("专辑评论") { addLog("Comment:getAlbum", client.comment.getAlbumComments(get("comment_album_id")).body) }
                    }
                    ActionRow {
                        Input("comment_playlist_id", get("comment_playlist_id"), { inputs["comment_playlist_id"] = it }, "歌单ID")
                        Btn("歌单评论") { addLog("Comment:getPlaylist", client.comment.getPlaylistComments(get("comment_playlist_id")).body) }
                    }
                    ActionRow {
                        Input("comment_count_id", get("comment_count_id"), { inputs["comment_count_id"] = it }, "资源ID")
                        Input("comment_count_type", get("comment_count_type"), { inputs["comment_count_type"] = it }, "类型", "music")
                        Btn("数量") { addLog("Comment:getCount", client.comment.getCommentCount(get("comment_count_id"), get("comment_count_type", "music")).body) }
                    }
                    ActionRow {
                        Input("comment_floor_rid", get("comment_floor_rid"), { inputs["comment_floor_rid"] = it }, "评论ID")
                        Input("comment_floor_eid", get("comment_floor_eid"), { inputs["comment_floor_eid"] = it }, "EId")
                        Input("comment_floor_sid", get("comment_floor_sid"), { inputs["comment_floor_sid"] = it }, "MixSongID")
                        Input("comment_floor_type", get("comment_floor_type"), { inputs["comment_floor_type"] = it }, "类型", "music")
                        Btn("楼层") { addLog("Comment:getFloor", client.comment.getFloorComments(get("comment_floor_rid"), get("comment_floor_eid"), get("comment_floor_sid"), get("comment_floor_type", "music")).body) }
                    }

                    Divider()

                    // ============ 排行榜 ============
                    SectionHeader("排行榜")
                    ActionRow {
                        Btn("排行列表") { addLog("Rank:getList", client.rank.getList().body) }
                        Btn("排行榜推荐") { addLog("Rank:getTop", client.rank.getTop().body) }
                    }
                    ActionRow {
                        Input("rank_info_id", get("rank_info_id"), { inputs["rank_info_id"] = it }, "排行榜ID")
                        Btn("详情") { addLog("Rank:getInfo", client.rank.getInfo(get("rank_info_id")).body) }
                    }
                    ActionRow {
                        Input("rank_audio_id", get("rank_audio_id"), { inputs["rank_audio_id"] = it }, "排行榜ID")
                        Btn("音乐") { addLog("Rank:getAudio", client.rank.getAudio(get("rank_audio_id")).body) }
                    }
                    ActionRow {
                        Input("rank_vol_id", get("rank_vol_id"), { inputs["rank_vol_id"] = it }, "排行榜ID")
                        Btn("往期") { addLog("Rank:getVol", client.rank.getVol(get("rank_vol_id")).body) }
                    }

                    Divider()

                    // ============ 推荐 ============
                    SectionHeader("推荐")
                    ActionRow {
                        Btn("每日推荐") { addLog("Recommend:getDaily", client.recommend.getDailyRecommend().body) }
                        Btn("私人FM") { addLog("Recommend:getPersonalFm", client.recommend.getPersonalFm().body) }
                        Btn("好友推荐") { addLog("Recommend:getFriend", client.recommend.getFriendRecommend().body) }
                    }
                    ActionRow {
                        Input("rec_ai_id", get("rec_ai_id"), { inputs["rec_ai_id"] = it }, "专辑ID", "1064292")
                        Btn("AI推荐") { addLog("Recommend:getAi", client.recommend.getAiRecommend(get("rec_ai_id", "1064292")).body) }
                    }
                    ActionRow {
                        Input("rec_style_scene", get("rec_style_scene"), { inputs["rec_style_scene"] = it }, "场景ID", "0")
                        Btn("风格推荐") { addLog("Recommend:getStyle", client.recommend.getStyleRecommend(get("rec_style_scene", "0")).body) }
                    }
                    ActionRow {
                        Input("rec_songs_id", get("rec_songs_id"), { inputs["rec_songs_id"] = it }, "Hash")
                        Btn("推荐歌曲") { addLog("Recommend:getSongs", client.recommend.getRecommendSongs(get("rec_songs_id")).body) }
                    }

                    Divider()

                    // ============ 用户 ============
                    SectionHeader("用户")
                    ActionRow {
                        Btn("最近收听") { addLog("User:getLatestSongs", client.user.getLatestSongsListen().body) }
                    }
                    ActionRow {
                        Input("user_fav_id", get("user_fav_id"), { inputs["user_fav_id"] = it }, "用户ID")
                        Btn("收藏数量") { addLog("User:getFavCount", client.user.getFavoriteCount(get("user_fav_id")).body) }
                    }
                    ActionRow {
                        Input("user_play_hash", get("user_play_hash"), { inputs["user_play_hash"] = it }, "Hash")
                        Input("user_play_songid", get("user_play_songid"), { inputs["user_play_songid"] = it }, "歌曲ID")
                        Btn("上传播放记录") { addLog("User:uploadPlay", client.user.uploadPlayHistory(get("user_play_hash").toLongOrNull() ?: 0L, get("user_play_songid").toLongOrNull() ?: 0L).body) }
                    }

                    Divider()

                    // ============ 电台 ============
                    SectionHeader("电台")
                    ActionRow {
                        Btn("分类") { addLog("Radio:getFmClass", client.radio.getFmClass().body) }
                        Btn("推荐") { addLog("Radio:getFmRecommend", client.radio.getFmRecommend().body) }
                        Btn("PC电台") { addLog("Radio:getPcDiantai", client.radio.getPcDiantai().body) }
                    }
                    ActionRow {
                        Input("radio_songs_id", get("radio_songs_id"), { inputs["radio_songs_id"] = it }, "电台ID")
                        Btn("电台歌曲") { addLog("Radio:getFmSongs", client.radio.getFmSongs(get("radio_songs_id")).body) }
                    }
                    ActionRow {
                        Input("radio_img_id", get("radio_img_id"), { inputs["radio_img_id"] = it }, "电台ID")
                        Btn("电台图片") { addLog("Radio:getFmImage", client.radio.getFmImage(get("radio_img_id")).body) }
                    }

                    Divider()

                    // ============ 场景音乐 ============
                    SectionHeader("场景音乐")
                    ActionRow {
                        Btn("场景列表") { addLog("Scene:getLists", client.sceneMusic.getLists().body) }
                    }
                    ActionRow {
                        Input("scene_v2_id", get("scene_v2_id"), { inputs["scene_v2_id"] = it }, "场景ID")
                        Btn("V2") { addLog("Scene:getListsV2", client.sceneMusic.getListsV2(get("scene_v2_id")).body) }
                    }
                    ActionRow {
                        Input("scene_module_id", get("scene_module_id"), { inputs["scene_module_id"] = it }, "模块ID")
                        Btn("模块") { addLog("Scene:getModule", client.sceneMusic.getModule(get("scene_module_id")).body) }
                    }
                    ActionRow {
                        Input("scene_music_id", get("scene_music_id"), { inputs["scene_music_id"] = it }, "场景ID")
                        Btn("音乐") { addLog("Scene:getMusic", client.sceneMusic.getMusic(get("scene_music_id")).body) }
                    }
                    ActionRow {
                        Input("scene_audio_id", get("scene_audio_id"), { inputs["scene_audio_id"] = it }, "场景ID")
                        Input("scene_audio_module", get("scene_audio_module"), { inputs["scene_audio_module"] = it }, "模块ID")
                        Input("scene_audio_tag", get("scene_audio_tag"), { inputs["scene_audio_tag"] = it }, "标签")
                        Btn("音频列表") { addLog("Scene:getAudioList", client.sceneMusic.getAudioList(get("scene_audio_id"), get("scene_audio_module"), get("scene_audio_tag")).body) }
                    }
                    ActionRow {
                        Input("scene_video_id", get("scene_video_id"), { inputs["scene_video_id"] = it }, "场景ID")
                        Btn("视频") { addLog("Scene:getVideoList", client.sceneMusic.getVideoList(get("scene_video_id")).body) }
                    }

                    Divider()

                    // ============ 图片 ============
                    SectionHeader("图片")
                    ActionRow {
                        Input("img_hash", get("img_hash"), { inputs["img_hash"] = it }, "Hash", "E6B6478980F16C6A97491C781A327E7A")
                        Btn("获取图片") { addLog("Image:getImages", client.image.getImages(get("img_hash", "E6B6478980F16C6A97491C781A327E7A")).body) }
                    }
                    ActionRow {
                        Input("img_audio_hash", get("img_audio_hash"), { inputs["img_audio_hash"] = it }, "Hash")
                        Btn("音频图片") { addLog("Image:getAudioImages", client.image.getAudioImages(get("img_audio_hash")).body) }
                    }

                    Divider()

                    // ============ 视频 ============
                    SectionHeader("视频")
                    ActionRow {
                        Input("video_album_audio_id", get("video_album_audio_id"), { inputs["video_album_audio_id"] = it }, "Hash")
                        Btn("KRM音频MV") { addLog("Video:getKmrAudioMv", client.video.getKmrAudioMv(get("video_album_audio_id")).body) }
                    }

                    Divider()

                    // ============ 有声书 ============
                    SectionHeader("有声书")
                    ActionRow {
                        Btn("推荐排名") { addLog("LongAudio:getRankRecommend", client.longAudio.getRankRecommend().body) }
                        Btn("VIP推荐") { addLog("LongAudio:getVipRecommend", client.longAudio.getVipRecommend().body) }
                        Btn("每周推荐") { addLog("LongAudio:getWeekRecommend", client.longAudio.getWeekRecommend().body) }
                        Btn("每日推荐") { addLog("LongAudio:getDailyRecommend", client.longAudio.getDailyRecommend().body) }
                    }
                    ActionRow {
                        Input("longaudio_album_id", get("longaudio_album_id"), { inputs["longaudio_album_id"] = it }, "专辑ID")
                        Btn("专辑") { addLog("LongAudio:getAlbumAudios", client.longAudio.getAlbumAudios(get("longaudio_album_id")).body) }
                        Btn("详情") { addLog("LongAudio:getAlbumDetail", client.longAudio.getAlbumDetail(get("longaudio_album_id")).body) }
                    }

                    Divider()

                    // ============ 杂项 ============
                    SectionHeader("杂项")
                    ActionRow {
                        Btn("刷刷Feed") { addLog("Misc:brush", client.misc.brush().body) }
                        Btn("IP区域") { addLog("Misc:getIpZone", client.misc.getIpZone().body) }
                    }
                    ActionRow {
                        Input("misc_ip_data", get("misc_ip_data"), { inputs["misc_ip_data"] = it }, "IP地址")
                        Btn("IP数据") { addLog("Misc:getIpData", client.misc.getIpData(get("misc_ip_data")).body) }
                        Btn("IP详情") { addLog("Misc:getIpDetail", client.misc.getIpDetail(get("misc_ip_data")).body) }
                    }
                    ActionRow {
                        Input("misc_ip_playlist", get("misc_ip_playlist"), { inputs["misc_ip_playlist"] = it }, "IP标识")
                        Btn("IP歌单") { addLog("Misc:getIpPlaylist", client.misc.getIpPlaylist(get("misc_ip_playlist")).body) }
                        Btn("IP首页") { addLog("Misc:getIpZoneHome", client.misc.getIpZoneHome(get("misc_ip_playlist")).body) }
                    }
                    ActionRow {
                        Btn("服务器时间") { addLog("Misc:getServerTime", client.misc.getServerTime().body) }
                    }

                    Divider()

                    // ============ 曲谱 ============
                    SectionHeader("曲谱")
                    ActionRow {
                        Btn("标签列表") { addLog("Sheet:getTags", client.sheet.getTags().body) }
                        Btn("合集列表") { addLog("Sheet:getCollections", client.sheet.getCollections().body) }
                    }
                    ActionRow {
                        Input("sheet_instruments", get("sheet_instruments"), { inputs["sheet_instruments"] = it }, "乐器", "1")
                        Input("sheet_level", get("sheet_level"), { inputs["sheet_level"] = it }, "难度", "0")
                        Btn("推荐曲谱") { addLog("Sheet:getExplore", client.sheet.getExplore(get("sheet_instruments", "1").toIntOrNull() ?: 1, get("sheet_level", "0").toIntOrNull() ?: 0).body) }
                        Btn("排行曲谱") { addLog("Sheet:getRank", client.sheet.getRank(get("sheet_instruments", "1").toIntOrNull() ?: 1, get("sheet_level", "0").toIntOrNull() ?: 0).body) }
                    }
                    ActionRow {
                        Input("sheet_detail_id", get("sheet_detail_id"), { inputs["sheet_detail_id"] = it }, "曲谱ID")
                        Btn("曲谱详情") { addLog("Sheet:getDetail", client.sheet.getDetail(get("sheet_detail_id")).body) }
                    }
                    ActionRow {
                        Input("sheet_song_aid", get("sheet_song_aid"), { inputs["sheet_song_aid"] = it }, "AudioId")
                        Input("sheet_song_inst", get("sheet_song_inst"), { inputs["sheet_song_inst"] = it }, "乐器", "1")
                        Btn("曲谱歌曲") { addLog("Sheet:getSong", client.sheet.getSong(get("sheet_song_aid"), get("sheet_song_inst", "1").toIntOrNull() ?: 1).body) }
                    }

                    Divider()

                    // ============ 清空日志 ============
                    ActionRow {
                        Btn("清空日志", color = MaterialTheme.colorScheme.error) { logText = "日志已清空" }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("运行日志:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = logText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ============================================================
//  紧凑组件：输入框 + 按钮同行
// ============================================================

/** 分区小标题 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

/** 分隔线 */
@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * 一行式操作区域：输入框和按钮混排在同一个 FlowRow 中。
 *
 * 输入框使用紧凑的 BasicTextField，不占用整行宽度。
 */
@Composable
private fun ActionRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

/**
 * 紧凑输入框：带浅色边框和 placeholder 的内联文本框。
 *
 * - 高度 ~34dp，宽度根据内容自适应
 * - 显示 placeholder 提示
 * - 不使用 Material TextField 的大 label 浪费空间
 */
@Composable
private fun FlowRowScope.Input(
    @Suppress("UNUSED_PARAMETER") key: String,
    value: String,
    onValueChange: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") hint: String,
    placeholder: String = ""
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 80.dp)
            .height(34.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 0.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(
                text = placeholder,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 紧凑按钮：小巧的 FilledButton，适合与输入框混排。
 */
@Composable
private fun FlowRowScope.Btn(
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
                try { onClick() }
                catch (_: Exception) {}
                finally { loading = false }
            }
        },
        enabled = !loading,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        modifier = Modifier.height(34.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontSize = 11.sp, maxLines = 1)
        }
    }
}
