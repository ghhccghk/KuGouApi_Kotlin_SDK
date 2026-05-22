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
                var searchResult by remember { mutableStateOf("搜索中...") }
                LaunchedEffect(Unit) {
                    try {
                        val client = KuGouClient()
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