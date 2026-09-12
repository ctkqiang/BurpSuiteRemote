package xin.ctkqiang.burpsuite.remote.mobileapp.data.settings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 用 Android 密钥库加解密一字符串。
 *
 * 设备身份是能控制 Burp 的凭据，明文写进偏好文件等于把它放在应用私有目录里赌设备不被 root；
 * 密钥留在密钥库里、密文才落盘，代价只是多一次本地加解密（plan §55）。
 */
class AndroidKeystoreSecretCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) {
    /** 加密；返回「IV + 密文」的 Base64。 */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + cipherText, Base64.NO_WRAP)
    }

    /** 解密；密钥被系统作废或密文损坏时返回 null，由调用方按「未配对」处理。 */
    fun decrypt(encodedCipherText: String): String? =
        try {
            val combined = Base64.decode(encodedCipherText, Base64.NO_WRAP)
            if (combined.size <= INITIALISATION_VECTOR_LENGTH) {
                null
            } else {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey(),
                    GCMParameterSpec(
                        AUTHENTICATION_TAG_LENGTH_BITS,
                        combined,
                        0,
                        INITIALISATION_VECTOR_LENGTH,
                    ),
                )
                String(
                    cipher.doFinal(
                        combined,
                        INITIALISATION_VECTOR_LENGTH,
                        combined.size - INITIALISATION_VECTOR_LENGTH,
                    ),
                    Charsets.UTF_8,
                )
            }
        } catch (unreadableCredential: Exception) {
            // 密钥被系统作废（例如用户改了锁屏）属于预期内的结局：读不出来就当没配对，重扫一次即可。
            null
        }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { entry -> return entry.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        keyGenerator.init(
            KeyGenParameterSpec
                .Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"

        const val DEFAULT_KEY_ALIAS = "burp_remote_device_identity"

        const val TRANSFORMATION = "AES/GCM/NoPadding"

        const val INITIALISATION_VECTOR_LENGTH = 12

        const val AUTHENTICATION_TAG_LENGTH_BITS = 128
    }
}
