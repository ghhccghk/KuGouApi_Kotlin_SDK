package top.ghhccghk.multiplatform.kugouapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    content = { paddingValues ->
                        App(paddingValues)
                    }
                )

            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}