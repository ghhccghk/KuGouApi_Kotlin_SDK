package top.ghhccghk.multiplatform.kugouapi.core

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class FingerprintTest {

    @Test
    fun testGenerateWebGLHash() {
        val hash = Fingerprint.generateWebGLHash()
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())
        // 验证是否为数字字符串
        assertTrue(hash.all { it.isDigit() })
    }

    @Test
    fun testGenerateEDTData() {
        val data = Fingerprint.generateEDTData()
        assertNotNull(data)
        assertTrue(data.isNotEmpty())
        
        // 验证格式 (type,value,index[,x,y] 冒号分隔)
        val parts = data.split(":")
        assertTrue(parts.size > 10, "EDT data should have multiple events")
        
        parts.forEach { part ->
            val fields = part.split(",")
            assertTrue(fields.size >= 3, "Each event should have at least 3 fields")
            val type = fields[0]
            assertTrue(type in listOf("3", "5", "6"), "Invalid event type: $type")
        }
    }

    @Test
    fun testEncryptSid() = runTest {
        val plaintext = "test_behavior_data"
        val result = Fingerprint.encryptSid(plaintext)
        
        assertEquals(plaintext, result.plaintext)
        assertEquals(32, result.aesKeyHex.length)
        assertEquals(32, result.aesIvHex.length)
        assertTrue(result.aesCiphertextHex.isNotEmpty())
        assertTrue(result.rsaCiphertextHex.isNotEmpty())
    }

    @Test
    fun testGenerateSimulate() = runTest {
        val result = Fingerprint.generateSimulate(
            mid = "06fhssdccf",
            userid = "114514",
            dfid = "7e77fh482008295a6mfw24fsw222djf35f5werf6g",
            webglHash = "7840188744182008295"
        )

        // 验证 edt 和 sid 非空
        assertTrue(result.edt.isNotEmpty(), "edt should not be empty")
        assertTrue(result.sid.isNotEmpty(), "sid should not be empty")

        // 验证 Base64 可解码
        val edtBytes = try {
            Crypto.decodeBase64(result.edt)
        } catch (e: Exception) {
            fail("edt should be valid Base64: ${e.message}")
        }
        assertTrue(edtBytes.isNotEmpty(), "decoded edt should not be empty")

        val sidBytes = try {
            Crypto.decodeBase64(result.sid)
        } catch (e: Exception) {
            fail("sid should be valid Base64: ${e.message}")
        }
        assertTrue(sidBytes.isNotEmpty(), "decoded sid should not be empty")

        // RSA-OAEP SHA-256 加密 16 字节密钥，输出大小取决于 RSA 密钥长度
        assertTrue(sidBytes.size >= 64, "sid should be at least 64 bytes, was ${sidBytes.size}")
    }

    @Test
    fun testGenerateSimulateDefaultWebGL() = runTest {
        // 测试不传 webglHash 时自动生成
        val result = Fingerprint.generateSimulate(
            mid = "mid",
            userid = "0",
            dfid = "-"
        )

        assertTrue(result.edt.isNotEmpty(), "edt should not be empty even without webglHash")
        assertTrue(result.sid.isNotEmpty(), "sid should not be empty even without webglHash")
    }
}
