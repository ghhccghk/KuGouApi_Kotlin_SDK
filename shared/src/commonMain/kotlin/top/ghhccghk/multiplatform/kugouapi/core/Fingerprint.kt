package top.ghhccghk.multiplatform.kugouapi.core

import kotlin.math.*
import kotlin.random.Random

/**
 * 浏览器端行为指纹生成工具 (Kotlin 重写版)
 *
 * 对齐: https://github.com/MakcRe/KuGouMusicApi/blob/main/public/fingerprint.js
 */
object Fingerprint {

    // ============================================================
    //  常量
    // ============================================================

    /** RSA 公钥 (SPKI DER 格式的 hex 字符串)，用于 RSA-OAEP SHA-256 加密 AES 密钥 */
    private const val RSA_SPKI_HEX = "30820122300d06092a864886f70d01010105000382010f003082010a0282010100a16dbe625a3c00b78f4904cfd31045945984387bc10fdb52facec30657ca12edd1cf3bd94da5f526d61b5f8f80554aa3e80473f0833e08a072a8616f6c737f5bae17c4d23eabbcf7e9a8c22f75532765b91bd302262b5cea819b8ab7b83507e1684ab49c2fa1c41590bc26c815f940d88b6b2d46d253bcf56c703f6be8e5426e0e5af63e20a9d3af23894cfb93d7234e5636c9f3004b2b2d83810afda4fa963e6110b46a51e4833d57c29aa3a3da49d29839619b5f78b6f91cc82a1bd9531c6d2707556ea3e50cf956f61e3fc4805ce7a2e0bebe1a225f2716dc1b8f85095544c5b86aecd2d63d1ffb57bd9db675408ab86c56fe05bb645fa05f3eaf1ed61aad0203010001"

    /** AES 初始化向量 (固定值): "kugousecurity123" */
    private val AES_IV = "kugousecurity123".encodeToByteArray()

    // ============================================================
    //  随机数与数学工具
    // ============================================================

    private fun ri(min: Int, max: Int): Int = Random.nextInt(min, max + 1)

    private data class Point(val x: Double, val y: Double)

    // ============================================================
    //  事件记录格式化
    // ============================================================

    private fun f3(t: Int, i: Int, x: Int, y: Int): String = "3,$t,$i,$x,$y"
    private fun f5(t: Int, i: Int): String = "5,$t,$i"
    private fun f6(t: Int, i: Int, x: Int, y: Int): String = "6,$t,$i,$x,$y"

    private fun fs3(sentinel: Long, i: Int, x: Int, y: Int): String = "3,$sentinel,$i,$x,$y"
    private fun fs5(sentinel: Long, i: Int): String = "5,$sentinel,$i"
    private fun fs6(sentinel: Long, i: Int, x: Int, y: Int): String = "6,$sentinel,$i,$x,$y"

    // ============================================================
    //  贝塞尔曲线鼠标路径生成
    // ============================================================

    /**
     * 用三阶贝塞尔曲线生成模拟真人的鼠标移动路径
     */
    private fun bezierPath(sx: Int, sy: Int, ex: Int, ey: Int, n: Int): List<Point> {
        val c1x = sx + (ex - sx) * 0.3 + ri(-80, 80)
        val c1y = sy + (ey - sy) * 0.2 + ri(-60, 60)
        val c2x = sx + (ex - sx) * 0.7 + ri(-60, 60)
        val c2y = sy + (ey - sy) * 0.8 + ri(-40, 40)

        return List(n + 1) { i ->
            val t = i.toDouble() / n
            val u = 1 - t
            val x = u * u * u * sx + 3 * u * u * t * c1x + 3 * u * t * t * c2x + t * t * t * ex
            val y = u * u * u * sy + 3 * u * u * t * c1y + 3 * u * t * t * c2y + t * t * t * ey
            val jitter = max(0.5, 3 - t * 2.5)
            Point(
                x + (Random.nextDouble() - 0.5) * jitter,
                y + (Random.nextDouble() - 0.5) * jitter
            )
        }
    }

    // ============================================================
    //  WebGL 指纹生成
    // ============================================================

    /**
     * 生成 WebGL 指纹哈希值
     *
     * 移动端/桌面端默认生成随机 uint64 模拟值。
     * 只有 JS/WasmJS 环境可能会调用真实的 Canvas 逻辑 (通过 expect/actual 扩展，此处先提供通用版)。
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun generateWebGLHash(): String {
        val hi = Random.nextLong(0, 0xFFFFFFFFL).toULong()
        val lo = Random.nextLong(0, 0xFFFFFFFFL).toULong()
        return (hi * 4294967296UL + lo).toString()
    }

    // ============================================================
    //  行为数据生成
    // ============================================================

    /**
     * 生成 sid 中的 data 字段（用户行为指纹数据）
     */
    fun generateEDTData(
        startX: Int = ri(50, 200),
        startY: Int = ri(50, 200),
        endX: Int = ri(300, 600),
        endY: Int = ri(300, 600),
        mousePoints: Int = ri(30, 60)
    ): String {
        val sentinel = 0xFFFFFFFFL - Random.nextInt(20)
        val entries = mutableListOf<String>()
        var ts = 0
        var ei = 0

        entries.add(f5(0, 0))
        entries.add(fs5(sentinel, 0))
        entries.add(f5(0, 0))
        entries.add(fs5(sentinel, 0))

        ts += ri(5, 20)
        entries.add(f6(ts, ei, 750, 500))
        entries.add(fs6(sentinel, ei, 750, 500))
        ei++

        repeat(3) {
            ts += ri(80, 600)
            entries.add(f5(ts, ei))
            entries.add(fs5(sentinel, ei))
            ei++
        }

        val path = bezierPath(startX, startY, endX, endY, mousePoints)
        var si = 0
        path.forEachIndexed { j, p ->
            ts += ri(8, 50)
            val px = p.x.roundToInt()
            val py = p.y.roundToInt()
            entries.add(f3(ts, si, px, py))
            entries.add(fs3(sentinel, si, px, py))

            if (j > 0 && j % 12 == 0) {
                ts += ri(20, 60)
                entries.add(f5(ts, ei))
                entries.add(fs5(sentinel, ei))
                ei++
            }
            si = (si + 1) % 2
        }

        ts += ri(5, 30)
        entries.add(f3(ts, 1, (endX + ri(-5, 5)), (endY + ri(-5, 5))))
        entries.add(fs3(sentinel, 1, endX, endY))

        return entries.joinToString(":")
    }

    // ============================================================
    //  加密流程
    // ============================================================

    data class EncryptionResult(
        val plaintext: String,
        val aesKeyHex: String,
        val aesIvHex: String,
        val aesCiphertextHex: String,
        val rsaCiphertextHex: String,
    )

    /**
     * 完整的 sid 加密流程
     */
    suspend fun encryptSid(plaintext: String): EncryptionResult {
        // 1. 生成随机 AES-128 密钥
        val aesKeyRaw = Random.nextBytes(16)
        val aesKeyHex = aesKeyRaw.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

        // 2. 用 AES-128-CBC 加密行为数据
        val ptBytes = plaintext.encodeToByteArray()
        val aesCt = Crypto.aesEncryptRaw(ptBytes, aesKeyRaw, AES_IV)
        val aesCiphertextHex = aesCt.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

        // 3. 用 RSA-OAEP SHA-256 加密 AES 密钥
        val rsaCt = Crypto.rsaEncryptOaep(aesKeyRaw, RSA_SPKI_HEX)
        val rsaCiphertextHex = rsaCt.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

        return EncryptionResult(
            plaintext = plaintext,
            aesKeyHex = aesKeyHex,
            aesIvHex = AES_IV.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') },
            aesCiphertextHex = aesCiphertextHex,
            rsaCiphertextHex = rsaCiphertextHex
        )
    }
}
