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
}
