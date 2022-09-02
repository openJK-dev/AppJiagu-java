package com.sakuqi.shell.utils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.util.zip.*

object ZipUtils{
    fun unZip(zip: File,dir:File){
        try{
            dir.delete()
            var zipFile = ZipFile(zip)
            var entries = zipFile.entries()
            while (entries.hasMoreElements()){
                var zipEntry = entries.nextElement()
                var name = zipEntry.name
                if(name.equals("META-INF/CERT.RSA") || name.equals("META-INF/CERT.SF")
                    || name.equals("META-INF/MANIFEST.MF")){
                    continue
                }
                if(!zipEntry.isDirectory){
                    var file = File(dir,name)
                    if(!file.parentFile.exists()){
                        file.parentFile.mkdirs()
                    }
                    var fos = FileOutputStream(file)
                    var ips = zipFile.getInputStream(zipEntry)
                    var buffer = ByteArray(1024)
                    var len = ips.read(buffer)
                    while (len != -1){
                        fos.write(buffer,0,len)
                        len = ips.read(buffer)
                    }
                    ips.close()
                    fos.close()
                }
            }
            zipFile.close()
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun zip(dir: File,zip: File){
        zip.delete()
        var cos = CheckedOutputStream(FileOutputStream(zip),CRC32())
        var zos = ZipOutputStream(cos)
        compress(dir,zos,"")
        zos.flush()
        zos.close()
    }


    private fun compress(dir: File, zos: ZipOutputStream, s: String) {
        if(dir.isDirectory){
            compressDir(dir,zos,s)
        }else{
            compressFile(dir,zos,s)
        }
    }

    private fun compressFile(file: File, zos: ZipOutputStream, dir: String) {
        var dirName = dir + file.name
        var dirNameNew = dirName.split("/")
        var buffer = StringBuffer()
        if(dirNameNew.size > 1){
            for (i in 1 until dirNameNew.size){
                buffer.append("/")
                buffer.append(dirNameNew[i])
            }
        }else{
            buffer.append("/")
        }

        var entry = ZipEntry(buffer.substring(1))
        zos.putNextEntry(entry)
        var bis = BufferedInputStream(FileInputStream(file))
        var data = ByteArray(1024)
        var len = bis.read(data,0,1024)
        while (len != -1){
            zos.write(data,0,len)
            len = bis.read(data,0,1024)
        }
        bis.close()
        zos.closeEntry()
    }


    private fun compressDir(dir: File, zos: ZipOutputStream, basePath: String) {
        var files = dir.listFiles()
        for (file in files){
            compress(file,zos,basePath+dir.name+"/")
        }
    }
}