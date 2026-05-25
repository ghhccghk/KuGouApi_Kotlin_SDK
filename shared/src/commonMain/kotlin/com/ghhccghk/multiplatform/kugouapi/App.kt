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
fun App() {
    MaterialTheme {
        val client = remember { KuGouClient() }
        var logText by remember { mutableStateOf("等待测试...") }
        var keyword by remember { mutableStateOf("周杰伦") }
        var inputId by remember { mutableStateOf("") }

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
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("酷狗 SDK 综合测试", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // 输入区域
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("关键词 (Search)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = inputId,
                        onValueChange = { inputId = it },
                        label = { Text("ID (Album/Artist/Song)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 操作按钮区域
                Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    
                    TestSection("身份与搜索") {
                        TestButton("注册设备 (dfid)") {
                            val resp = client.auth.registerDev()
                            addLog("Auth:registerDev", resp.body)
                        }
                        TestButton("发送验证码") {
                            val resp = client.auth.sendCaptcha(keyword)
                            addLog("Auth:sendCaptcha", resp.body)
                        }
                        TestButton("搜索 (Search)") {
                            val resp = client.search.search(keyword)
                            addLog("Search:search", resp.body)
                        }
                        TestButton("混合搜索 (Mixed)") {
                            val resp = client.search.searchMixed(keyword)
                            addLog("Search:searchMixed", resp.body)
                        }
                        TestButton("热搜 (Hot)") {
                            val resp = client.search.searchHot()
                            addLog("Search:searchHot", resp.body)
                        }
                    }

                    TestSection("歌曲与元数据") {
                        TestButton("歌曲信息 (Song)") {
                            val target = inputId.ifEmpty { "E6B6478980F16C6A97491C781A327E7A" }
                            val resp = client.song.getAudioInfo(target)
                            addLog("Song:getAudioInfo", resp.body)
                        }
                        TestButton("专辑详情 (Album)") {
                            val target = inputId.ifEmpty { "1064292" }
                            val resp = client.album.getAlbumDetail(target)
                            addLog("Album:getAlbumDetail", resp.body)
                        }
                        TestButton("歌手详情 (Artist)") {
                            val target = inputId.ifEmpty { "3066" }
                            val resp = client.artist.getDetail(target)
                            addLog("Artist:getDetail", resp.body)
                        }
                        TestButton("获取歌词 (Lyric)") {
                            // 默认使用一个周杰伦歌曲的示例
                            val resp = client.song.getLyric("191543369", "1AE02A6C2E7596187C717727708422CB", decode = true)
                            addLog("Song:getLyric", resp.body)
                        }
                    }

                    TestSection("排行榜与推荐") {
                        TestButton("排行列表") {
                            val resp = client.rank.getList()
                            addLog("Rank:getList", resp.body)
                        }
                        TestButton("每日推荐") {
                            val resp = client.recommend.getDailyRecommend()
                            addLog("Recommend:getDaily", resp.body)
                        }
                        TestButton("私人 FM") {
                            val resp = client.recommend.getPersonalFm()
                            addLog("Recommend:getPersonalFm", resp.body)
                        }
                        TestButton("AI 推荐") {
                            val target = inputId.ifEmpty { "1064292" }
                            val resp = client.recommend.getAiRecommend(target)
                            addLog("Recommend:getAi", resp.body)
                        }
                    }

                    TestSection("歌单与评论") {
                        TestButton("歌单详情") {
                            val target = inputId.ifEmpty { "552406" }
                            val resp = client.playlist.getPlaylistDetail(target)
                            addLog("Playlist:getDetail", resp.body)
                        }
                        TestButton("歌单分类") {
                            val resp = client.playlist.getPlaylistTags()
                            addLog("Playlist:getTags", resp.body)
                        }
                        TestButton("歌曲评论") {
                            val target = inputId.ifEmpty { "34796338" }
                            val resp = client.comment.getMusicComments(target)
                            addLog("Comment:getMusic", resp.body)
                        }
                    }

                    TestSection("其他") {
                        TestButton("电台列表") {
                            val resp = client.radio.getFmClass()
                            addLog("Radio:getFmClass", resp.body)
                        }
                        TestButton("场景列表") {
                            val resp = client.sceneMusic.getLists()
                            addLog("Scene:getLists", resp.body)
                        }
                        TestButton("刷刷 Feed") {
                            val resp = client.misc.brush()
                            addLog("Misc:brush", resp.body)
                        }
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
                            .height(400.dp)
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
