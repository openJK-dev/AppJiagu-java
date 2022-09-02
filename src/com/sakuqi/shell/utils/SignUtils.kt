package com.sakuqi.shell.utils

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.Charset


object SignUtils {
    @Throws(InterruptedException::class, IOException::class)
    fun signature(unsignedApk: File, signedApk: File, keyStore: String) {
        val cmd = arrayOf(
            "jarsigner",
            "-sigalg",
            "SHA1withRSA",
            "-digestalg",
            "SHA1",
            "-keystore",
            keyStore,
            "-storepass",
            "a66388487",
            "-keypass",
            "a66388487",
            "-signedjar",
            signedApk.absolutePath,
            unsignedApk.absolutePath,
            "wuse"
        )
        val process = Runtime.getRuntime().exec(cmd)
        println("start sign")
        try {
            val waitResult = process.waitFor()
            println("waitResult: $waitResult")
        } catch (e: InterruptedException) {
            e.printStackTrace()
            throw e
        }

        println("process.exitValue() " + process.exitValue())
        if (process.exitValue() != 0) {
            val inputStream = process.errorStream
            var len: Int
            val buffer = ByteArray(2048)
            val bos = ByteArrayOutputStream()
            len = inputStream.read(buffer)
            while (len != -1) {
                bos.write(buffer, 0, len)
                len = inputStream.read(buffer)
            }
            println(String(bos.toByteArray(), Charset.forName("gbk")))
            throw RuntimeException("签名执行失败")
        }
        println("finish signed")
        process.destroy()
    }
}