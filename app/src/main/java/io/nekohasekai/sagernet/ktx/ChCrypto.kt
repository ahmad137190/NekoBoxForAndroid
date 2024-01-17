package io.nekohasekai.sagernet.ktx

import android.os.Build
import androidx.annotation.RequiresApi
import okio.ByteString.Companion.decodeBase64
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object ChCrypto{
    @JvmStatic fun aesEncrypt(v:String, secretKey:String) =
        AES256.encrypt(v, secretKey)


    @JvmStatic fun aesDecrypt(v:String, secretKey:String) =
        AES256.decrypt(v, secretKey)

}

private object AES256{
    @RequiresApi(Build.VERSION_CODES.O)
    private val encorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Base64.getEncoder()
    } else {
        null
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private val decorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Base64.getDecoder()
    } else {
        null
    }
    private fun cipher(opmode:Int, secretKey:String):Cipher{
        if(secretKey.length != 32) throw RuntimeException("SecretKey length is not 32 chars")
        val c = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val sk = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "AES")
        val iv = IvParameterSpec(secretKey.substring(0, 16).toByteArray(Charsets.UTF_8))
        c.init(opmode, sk, iv)
        return c
    }

    fun encrypt(str:String, secretKey:String):String{
        val encrypted = cipher(Cipher.ENCRYPT_MODE, secretKey).doFinal(str.toByteArray(Charsets.UTF_8))



        if (Build.VERSION.SDK_INT >= 26) {
            return encorder?.encode(encrypted).toString();
        } else {
            return android.util.Base64.encodeToString(encrypted, 0)
        }
        //return String(encorder.encode(encrypted))
    }

    fun decrypt(str:String, secretKey:String):String{
     //   var byteStr: byte[] ? = null
        var byteStr: ByteArray? = null
      //  val byteStr = decorder.decode(str.toByteArray(Charsets.UTF_8))

        if (Build.VERSION.SDK_INT >= 26) {
            byteStr= decorder?.decode(str.toByteArray(Charsets.UTF_8)) as Nothing?
        } else {
            byteStr=  android.util.Base64
                .decode(
                    str.toByteArray(Charsets.UTF_8), 0)
        }
        return String(cipher(Cipher.DECRYPT_MODE, secretKey).doFinal(byteStr))
    }
}