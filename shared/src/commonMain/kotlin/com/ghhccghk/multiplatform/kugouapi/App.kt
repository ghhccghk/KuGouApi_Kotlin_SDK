@file:OptIn(ExperimentalLayoutApi::class)

package com.ghhccghk.multiplatform.kugouapi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
        val scope = rememberCoroutineScope()
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
                Text("酷狗 SDK 测试界面", style = MaterialTheme.typography.headlineMedium)
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
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Auth Group
                        TestButton("注册设备 (dfid)") {
                            val resp = client.auth.registerDev()
                            addLog("Auth:registerDev", resp.body)
                        }

                        // Search Group
                        TestButton("搜索 (Search)") {
                            val resp = client.search.search(keyword)
                            addLog("Search:search", resp.body)
                        }
                        TestButton("热搜 (Hot)") {
                            val resp = client.search.searchHot()
                            addLog("Search:searchHot", resp.body)
                        }

                        // Song Group
                        TestButton("歌曲信息 (Song)") {
                            val target = inputId.ifEmpty { "E6B6478980F16C6A97491C781A327E7A" }
                            val resp = client.song.getAudioInfo(target)
                            addLog("Song:getAudioInfo", resp.body)
                        }

                        // Album Group
                        TestButton("专辑详情 (Album)") {
                            val target = inputId.ifEmpty { "1064292" }
                            val resp = client.album.getAlbumDetail(target)
                            addLog("Album:getAlbumDetail", resp.body)
                        }

                        // Artist Group
                        TestButton("歌手详情 (Artist)") {
                            val target = inputId.ifEmpty { "3066" }
                            val resp = client.artist.getDetail(target)
                            addLog("Artist:getDetail", resp.body)
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
                    // Log error internally if needed
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
