package com.sakuqi.shell.utils

import java.io.*
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.SecretKeySpec


object EncryptUtils {
    /**
     * 加密算法
     */
    private val ENCRY_ALGORITHM = "AES"


    /**
     * 加密算法/加密模式/填充类型
     * 本例采用AES加密，ECB加密模式，PKCS5Padding填充
     */
    private val CIPHER_MODE = "AES/ECB/PKCS5Padding"

    val ivBytes = "huangdh'l,.AMWK;".toByteArray()

    /**
     * 设置iv偏移量
     * 本例采用ECB加密模式，不需要设置iv偏移量
     */
    private val IV_: String? = null

    /**
     * 设置加密字符集
     * 本例采用 UTF-8 字符集
     */
    private val CHARACTER = Charsets.UTF_8

    /**
     * 设置加密密码处理长度。
     * 不足此长度补0；
     */
    private val PWD_SIZE = 16

    /**
     * 密码处理方法
     * 如果加解密出问题，
     * 请先查看本方法，排除密码长度不足补"0",导致密码不一致
     * @param password 待处理的密码
     * @return
     * @throws UnsupportedEncodingException
     */
    @Throws(UnsupportedEncodingException::class)
    private fun pwdHandler(password: String?): ByteArray {
        var password = password
        var data: ByteArray? = null
        if (password == null) {
            password = ""
        }
        val sb = StringBuffer(PWD_SIZE)
        sb.append(password)
        while (sb.length < PWD_SIZE) {
            sb.append("0")
        }
        if (sb.length > PWD_SIZE) {
            sb.setLength(PWD_SIZE)
        }

        data = sb.toString().toByteArray(charset("UTF-8"))

        return data
    }

    //======================>原始加密<======================

    /**
     * 原始加密
     * @param clearTextBytes 明文字节数组，待加密的字节数组
     * @param pwdBytes 加密密码字节数组
     * @return 返回加密后的密文字节数组，加密错误返回null
     */
    fun encrypt(clearTextBytes: ByteArray, pwdBytes: ByteArray): ByteArray? {
        try {
            // 1 获取加密密钥
            val keySpec = SecretKeySpec(pwdBytes, ENCRY_ALGORITHM)

            // 2 获取Cipher实例
            val cipher = Cipher.getInstance(CIPHER_MODE)

            // 查看数据块位数 默认为16（byte） * 8 =128 bit
            //            System.out.println("数据块位数(byte)：" + cipher.getBlockSize());

            // 3 初始化Cipher实例。设置执行模式以及加密密钥
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)

            // 4 执行

            // 5 返回密文字符集
            return cipher.doFinal(clearTextBytes)

        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 原始解密
     * @param cipherTextBytes 密文字节数组，待解密的字节数组
     * @param pwdBytes 解密密码字节数组
     * @return 返回解密后的明文字节数组，解密错误返回null
     */
    fun decrypt(cipherTextBytes: ByteArray, pwdBytes: ByteArray): ByteArray? {

        try {
            // 1 获取解密密钥
            val keySpec = SecretKeySpec(pwdBytes, ENCRY_ALGORITHM)

            // 2 获取Cipher实例
            val cipher = Cipher.getInstance(CIPHER_MODE)

            // 查看数据块位数 默认为16（byte） * 8 =128 bit
            //            System.out.println("数据块位数(byte)：" + cipher.getBlockSize());

            // 3 初始化Cipher实例。设置执行模式以及加密密钥
            cipher.init(Cipher.DECRYPT_MODE, keySpec)

            // 4 执行

            // 5 返回明文字符集
            return cipher.doFinal(cipherTextBytes)

        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 解密错误 返回null
        return null
    }


    //======================>HEX<======================

    /**
     * HEX加密
     * @param clearText 明文，待加密的内容
     * @param password 密码，加密的密码
     * @return 返回密文，加密后得到的内容。加密错误返回null
     */
    fun encryptHex(clearText: String, password: String): String? {
        try {
            // 1 获取加密密文字节数组
            val cipherTextBytes = encrypt(clearText.toByteArray(CHARACTER), pwdHandler(password))

            // 2 对密文字节数组进行 转换为 HEX输出密文

            // 3 返回 HEX输出密文
            return byte2hex(cipherTextBytes!!)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 加密错误返回null
        return null
    }

    /**
     * HEX解密
     * @param cipherText 密文，带解密的内容
     * @param password 密码，解密的密码
     * @return 返回明文，解密后得到的内容。解密错误返回null
     */
    fun decryptHex(cipherText: String?, password: String): String? {
        try {
            // 1 将HEX输出密文 转为密文字节数组
            val cipherTextBytes = hex2byte(cipherText)

            // 2 将密文字节数组进行解密 得到明文字节数组
            val clearTextBytes = decrypt(cipherTextBytes, pwdHandler(password))

            // 3 根据 CHARACTER 转码，返回明文字符串
            return String(clearTextBytes!!, CHARACTER)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 解密错误返回null
        return null
    }

    /*字节数组转成16进制字符串  */
    fun byte2hex(bytes: ByteArray): String { // 一个字节的数，
        val sb = StringBuffer(bytes.size * 2)
        var tmp = ""
        for (n in bytes.indices) {
            // 整数转成十六进制表示
            tmp = Integer.toHexString(bytes[n].toInt() and 0XFF)
            if (tmp.length == 1) {
                sb.append("0")
            }
            sb.append(tmp)
        }
        return sb.toString().toUpperCase() // 转成大写
    }

    /*将hex字符串转换成字节数组 */
    private fun hex2byte(str: String?): ByteArray {
        var str = str
        if (str == null || str.length < 2) {
            return ByteArray(0)
        }
        str = str.toLowerCase()
        val l = str.length / 2
        val result = ByteArray(l)
        for (i in 0 until l) {
            val tmp = str.substring(2 * i, 2 * i + 2)
            result[i] = (Integer.parseInt(tmp, 16) and 0xFF).toByte()
        }
        return result
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val data = encrypt("123456".toByteArray(), EncryptUtils.ivBytes)
        data?.forEach {
            print("$it ")
        }

    }
}