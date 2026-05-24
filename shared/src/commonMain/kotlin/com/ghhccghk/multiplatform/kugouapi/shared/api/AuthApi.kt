package com.ghhccghk.multiplatform.kugouapi.shared.api

import com.ghhccghk.multiplatform.kugouapi.shared.core.*
import com.ghhccghk.multiplatform.kugouapi.shared.model.EncryptType
import kotlinx.serialization.json.*

class AuthApi(private val executor: RequestExecutor) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 注册设备获取 dfid
     * 完整对齐 https://github.com/MakcRe/KuGouMusicApi/blob/main/module/register_dev.js
     */
    suspend fun registerDev(): KuGouResponse {
        val guid = executor.cookieJar.getGuid()
        val useridStr = executor.cookieJar.getUserid()
        val token = executor.cookieJar.getToken()

        // 1. 准备设备指纹数据
        val dataMap = buildJsonObject {
            put("availableRamSize", 4983533568L)
            put("availableRomSize", 48114719L)
            put("availableSDSize", 48114717L)
            put("basebandVer", "")
            put("batteryLevel", 100)
            put("batteryStatus", 3)
            put("brand", "Redmi")
            put("buildSerial", "unknown")
            put("device", "marble")
            put("imei", guid)
            put("imsi", "")
            put("manufacturer", "Xiaomi")
            put("uuid", guid)
            // 传感器信息
            put("accelerometer", false)
            put("accelerometerValue", "")
            put("gravity", false)
            put("gravityValue", "")
            put("gyroscope", false)
            put("gyroscopeValue", "")
            put("light", false)
            put("lightValue", "")
            put("magnetic", false)
            put("magneticValue", "")
            put("orientation", false)
            put("orientationValue", "")
            put("pressure", false)
            put("pressureValue", "")
            put("step_counter", false)
            put("step_counterValue", "")
            put("temperature", false)
            put("temperatureValue", "")
        }

        // 2. AES 加密 (playlistAesEncrypt)
        val aesKeyBase = PlatformIdentity.generateRandomString(6).lowercase()
        val md5Key = Crypto.md5(aesKeyBase)
        val encryptKey = md5Key.substring(0, 16)
        val iv = md5Key.substring(16, 32)
        
        val dataStr = dataMap.toString()
        val encryptedData = Crypto.aesEncryptBase64(dataStr, encryptKey, iv)

        // 3. RSA 加密 (rsaEncrypt2)
        val rsaData = buildJsonObject {
            put("aes", aesKeyBase)
            // 对齐 Node.js: 如果是数字 0 则传数字
            val uid = useridStr.toLongOrNull() ?: 0L
            put("uid", uid)
            put("token", token)
        }.toString().encodeToByteArray()
        
        val p = Crypto.rsaEncryptPkcs1(rsaData, Crypto.publicRasKey)

        // 4. 发起请求
        val response = executor.execute(
            KuGouRequest(
                baseUrl = "https://userservice.kugou.com",
                url = "/risk/v2/r_register_dev",
                method = HttpMethod.POST,
                data = encryptedData,
                params = mapOf(
                    "part" to 1,
                    "platid" to 1,
                    "p" to p
                ),
                encryptType = EncryptType.ANDROID,
                responseType = ResponseType.BYTES
            )
        )

        // 5. 解密响应
        if (response.status == 200) {
            val bytesStr = response.body["bytes"]?.jsonPrimitive?.content ?: ""
            if (bytesStr.isNotEmpty()) {
                try {
                    val bytes = bytesStr.split(",").map { it.toByte() }.toByteArray()
                    val responseBase64 = Crypto.encodeBase64(bytes)
                    val decryptedJson = Crypto.aesDecryptBase64(responseBase64, encryptKey, iv)
                    
                    val body = json.parseToJsonElement(decryptedJson) as JsonObject
                    val data = body["data"]?.jsonObject ?: buildJsonObject {}
                    val dfid = data["dfid"]?.jsonPrimitive?.content
                    
                    if (dfid != null) {
                        executor.cookieJar["dfid"] = dfid
                    }

                    // 合并本地身份信息到返回结果中，对齐用户期待的格式
                    val mergedData = buildJsonObject {
                        data.forEach { (k, v) -> put(k, v) }
                        put("mid", executor.cookieJar.getMid())
                        put("guid", executor.cookieJar.getGuid())
                        put("serverDev", executor.cookieJar.getDev())
                        put("mac", executor.cookieJar["KUGOU_API_MAC"] ?: "02:00:00:00:00:00")
                    }

                    val mergedBody = buildJsonObject {
                        // 确保 status 和 error_code 存在
                        put("status", body["status"] ?: JsonPrimitive(1))
                        put("error_code", body["error_code"] ?: JsonPrimitive(0))
                        body.forEach { (k, v) -> 
                            if (k != "data" && k != "status" && k != "error_code") put(k, v) 
                        }
                        put("data", mergedData)
                    }

                    return response.copy(body = mergedBody)
                } catch (e: Exception) {
                    return response.copy(body = buildJsonObject { 
                        put("status", 0)
                        put("error_code", -1)
                        put("msg", "Decrypt failed: ${e.message}")
                    })
                }
            }
        }

        return response
    }
}
