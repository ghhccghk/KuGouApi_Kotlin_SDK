# KuGou Music API Kotlin Multiplatform SDK

基于 Kotlin Multiplatform (KMP) 重新实现的酷狗音乐 API SDK。对齐 [MakcRe/KuGouMusicApi](https://github.com/MakcRe/KuGouMusicApi) 的 Node.js 实现，支持 Android、iOS、JVM、Web (Wasm/JS) 等多平台。

---

## 🚀 核心特性

- **多平台支持**: 统一的 API 调用接口，底层适配不同平台的 HTTP 与加密实现。
- **协程优先**: 所有网络请求均采用 `suspend` 挂起函数，完美契合 Kotlin 协程流。
- **全量加密对齐**: 严格对齐酷狗 Android/iOS 端的 MD5 签名、AES-CBC 与 RSA-PKCS1 加密算法。
- **模块化设计**: 16 个 API 分类模块，按需调用。

---

## 📦 已实现 API 模块状态

| 模块 | 功能描述 | 状态 |
| :--- | :--- | :---: |
| **Auth** | 设备注册 (dfid)、密码/手机登录、二维码登录、Token 刷新 | ✅ |
| **Search** | 综合搜索、混合搜索、搜索建议、热搜 | ✅ |
| **Song** | 歌曲详情、伴奏匹配、KTV总数、权限查询、歌词下载及 KRC 解码 | ✅ |
| **Playlist** | 歌单增删改查、歌单内歌曲管理、音效歌单、相似歌单 | ✅ |
| **Album** | 专辑元数据、专辑详情、专辑歌曲、唱片店 | ✅ |
| **Artist** | 歌手详情、作品列表、视频 MV、关注/取消关注、荣誉详情 | ✅ |
| **Rank** | 排行榜列表、详情、往期版本、推荐榜单 | ✅ |
| **Recommend** | 每日推荐、风格推荐、私人 FM、AI 智能推荐、好友推荐 | ✅ |
| **Comment** | 歌曲/专辑/歌单评论、楼层回复、热词评论、评论数 | ✅ |
| **Radio** | 分类 FM、推荐 FM、PC 端电台 | ✅ |
| **Scene** | 场景音乐列表、模块详情、推荐音频/视频/歌单 | ✅ |
| **User** | 最近播放获取与上报、收藏计数 | ✅ |
| **Image** | 歌手/专辑/音频相关图片获取 | ✅ |
| **Video** | 歌曲关联 MV 信息 | ✅ |
| **LongAudio** | 长音频专辑、有声书推荐（每日/每周/VIP） | ✅ |
| **Misc** | 刷刷 Feed 流、IP 专区、系统工具 | ✅ |

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

## ⚖️ 免责声明

本应用仅用于学习及测试，请在下载 24 小时内删除。由于非官方实现，因使用本 SDK 产生的任何法律后果由使用者自行承担。请尊重版权，严禁商业用途。
