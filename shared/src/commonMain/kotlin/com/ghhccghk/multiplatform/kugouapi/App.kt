package com.ghhccghk.multiplatform.kugouapi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ghhccghk.multiplatform.kugouapi.shared.KuGouClient
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource

import kogouapikotlinmultiplatformsdk.shared.generated.resources.Res
import kogouapikotlinmultiplatformsdk.shared.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("点击我！")
            }
            AnimatedVisibility(showContent) {
                var searchResult by remember { mutableStateOf("初始化中...") }
                LaunchedEffect(Unit) {
                    try {
                        val client = KuGouClient()
                        // 1. 先注册设备获取 dfid
                        val regResponse = client.auth.registerDev()
                        val data = regResponse.body["data"]?.jsonObject
                        print("注册设备响应: ${regResponse.body}")
                        val dfid = data?.get("dfid")?.jsonPrimitive?.content ?: "-"
                        client.cookieJar["dfid"] = dfid
                        
                        // 2. 再进行搜索
                        val response = client.search.search("周杰伦")
                        searchResult = response.body.toString()
                        print("搜索结果: ${response.body}")
                    } catch (e: Exception) {
                        searchResult = "错误: ${e.message}"
                        print("搜索结果: ${e.message}")
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("搜索结果: $searchResult")
                }
            }
        }
    }
}