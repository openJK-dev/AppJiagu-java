package com.sakuqi.shell.utils

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.Charset

object DexUtils {
    @Throws(IOException::class,InterruptedException::class)
    fun dxCommand(jar:File,dex:File){
        var runtime = Runtime.getRuntime()
        var process = runtime.exec("dx --dex --output "+dex.absolutePath+" "+jar.absolutePath)
        try {
            process.waitFor()
        }catch (e:InterruptedException){
            e.printStackTrace()
            throw e
        }
        if(process.exitValue() != 0){
            val inputStream = process.errorStream
            var buffer = ByteArray(1024)
            val bos = ByteArrayOutputStream()
            var len = inputStream.read(buffer)
            while (len != -1){
                bos.write(buffer,0,len)
                len = inputStream.read(buffer)
            }
            System.out.println(String(bos.toByteArray(), Charset.forName("GBK")))
            throw RuntimeException("dx run failed")
        }else{
            System.out.println("执行成功:"+process.exitValue())
        }
        process.destroy()
    }

    /**
     * 读取文件
     * @param file
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getBytes(file: File?): ByteArray {
        val r = RandomAccessFile(file, "r")
        val buffer = ByteArray(r.length().toInt())
        r.readFully(buffer)
        r.close()
        return buffer
    }
}