package com.sakuqi.shell

import com.sakuqi.shell.utils.*
import java.io.File
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.util.concurrent.TimeUnit


// 未签名 APK
lateinit var orginApk:File
lateinit var orginApkName:String
// 壳 AAR
lateinit var shellAAR:File
//签名文件
var signFile = File("keystore/wuse.jks")
//apk 反编译目录
val apkDecode = File("output/apktool/decode")
//apk 重新编译
lateinit var apkBuild :File

fun findOriginApk(){
    var inputDir = File("input/apk")
    var apkFile = inputDir.listFiles { _, name ->
        name.endsWith(".apk")
    }.first()
    if(apkFile.exists()){
        orginApk = apkFile
        orginApkName = orginApk.name
        apkBuild = File("output/apktool",orginApkName)
    }else{
        throw Exception("没有找到加固 apk")
    }
}

fun findOriginShell(){
    var inputDir = File("input/shell")
    var shellFile = inputDir.listFiles { _, name ->
        name.endsWith(".aar")
    }.first()
    if(shellFile.exists()){
        shellAAR = shellFile
    }else{
        throw Exception("没有找到壳 aar")
    }
}

/**
 * 反编译 APK 文件
 */
fun apkDecode(){
    println("开始反编译")
    val process = Runtime.getRuntime()
        .exec("java -jar outlibs/apktool_2.5.0.jar d "+ orginApk.absolutePath+" -o "+apkDecode.absolutePath)
    process.waitFor()
    if(process.exitValue() != 0) {
        FileUtils.printStream(process.errorStream)
    }else{
        FileUtils.printStream(process.inputStream)
    }
    process.destroy()
}

/**
 * 编译 APK 文件
 */
fun apkBuild(){
    println("开始重新编译")
    val process = Runtime.getRuntime()
        .exec("java -jar outlibs/apktool_2.5.0.jar b "+ apkDecode.absolutePath+" -o "+ apkBuild.absolutePath)
    process.waitFor()
    if(process.exitValue() != 0) {
        FileUtils.printStream(process.errorStream)
    }else{
        FileUtils.printStream(process.inputStream)
    }
    process.destroy()
}

/**
 * 解压 APK 文件并加密所有的dex文件
 */
fun unZipApkAndEncrypt(){
    println("解压 APK")
    val apkUnzipDir = File("output/unzip/apk")
    if(!apkUnzipDir.exists()){
        apkUnzipDir.mkdirs()
    }
    FileUtils.delete(apkUnzipDir)
    ZipUtils.unZip(apkBuild,apkUnzipDir)
    //删除 META-INF/CERT.RSA,META-INF/CERT.SF,META-INF/MANIFEST.MF
    val certRSA = File(apkUnzipDir,"META-INF/CERT.RSA")
    certRSA.delete()
    val certSF = File(apkUnzipDir,"META-INF/CERT.SF")
    certSF.delete()
    val manifestMF = File(apkUnzipDir,"META-INF/MANIFEST.MF")
    manifestMF.delete()
    //changeAndroidManifest(apkUnzipDir)
    //获取dex 文件
    val apkFiles = apkUnzipDir.listFiles(object :FilenameFilter{
        override fun accept(dir: File?, name: String?): Boolean {
            return name?.endsWith(".dex") == true
        }
    })
    for (dexFile in apkFiles){
        val name = dexFile.name
        println("dex:$name")
        val bytes = DexUtils.getBytes(dexFile)
        val encrypt: ByteArray? = EncryptUtils.encrypt(bytes, EncryptUtils.ivBytes)
        val fos: FileOutputStream = FileOutputStream(
            File(
                dexFile.parent,
                "secret-" + dexFile.getName()
            )
        )
        fos.write(encrypt)
        fos.flush()
        fos.close()
        dexFile.delete()
    }

}

/**
 * 修改 xml 文件
 */
fun changeAndroidManifest(){
    println("开始修改 AndroidManifest")
    var manifestFile = File("output/apktool/decode/AndroidManifest.xml")
    XmlParseUtils.changeXmlBySax(manifestFile,"com.sakuqi.steady.SteadyApplication")
}

/**
 * 修改 AndroidManifest
 */
fun changeAndroidManifest(apkUnzipDir:File){
    val aManifest = apkUnzipDir.listFiles { _, name ->
        name?.equals("AndroidManifest.xml") == true
    }
    val file = if (aManifest != null && aManifest.isNotEmpty()) {
        aManifest[0]
    }else{null}
    file?.let {
        //将模版插入 AndroidManifest 中
        val process2 = Runtime.getRuntime()
            .exec("java -jar outlibs/AXMLEditor.jar -tag -i tool/src/main/assets/ApplicationName.xml " +
                    file.absolutePath+" "+file.absolutePath)
        process2.waitFor()
        if(process2.exitValue() != 0) {
            println("2")
            FileUtils.printStream(process2.errorStream)
        }
        process2.destroy()

        //解析出原来的 Application 类名
        var process0 = Runtime.getRuntime()
            .exec("java -jar tool/libs/AXMLPrinter2.jar "+file.absolutePath)
        process0.waitFor()
        val applicationPath = XmlParseUtils.sax2xml(process0.inputStream)
        if(process0.exitValue() != 0){
            println("0")
            FileUtils.printStream(process0.errorStream)
        }
        process0.destroy()

        //参考 https://github.com/fourbrother/AXMLEditor
        //修改 Application 下 插入标签的值
        val process1 = Runtime.getRuntime()
            .exec("java -jar tool/libs/AXMLEditor.jar -attr -i meta-data package value "+applicationPath
                    + " " + file.absolutePath+" "+file.absolutePath)
        process1.waitFor()
        if(process1.exitValue() != 0){
            println("1")
            FileUtils.printStream(process1.errorStream)
        }
        process1.destroy()

        //参考 https://github.com/fourbrother/AXMLEditor
        //修改 Application 下 name 标签
        val process3 = Runtime.getRuntime()
            .exec("java -jar tool/libs/AXMLEditor.jar -attr -m application package name com.sakuqi.shell.NewApplication"
                    + " " + file.absolutePath+" "+file.absolutePath)
        process3.waitFor()
        if(process3.exitValue() != 0){
            println("3")
            FileUtils.printStream(process3.errorStream)
        }
        process3.destroy()

        //解析出原来的 Application 类名
        var process4 = Runtime.getRuntime()
            .exec("java -jar tool/libs/AXMLPrinter2.jar "+file.absolutePath)
        process4.waitFor()
        FileUtils.printStream(process4.inputStream)
        process4.destroy()

    }
}

/**
 * 解压壳aar 并转化jar 为dex
 */
fun makeDecodeDex(){
    println("解压壳 AAR")
    var shellUnzipDir = File("output/unzip/shell")
    if(!shellUnzipDir.exists()){
        shellUnzipDir.mkdirs()
    }
    FileUtils.delete(shellUnzipDir)
    //解压 AAR
    ZipUtils.unZip(shellAAR,shellUnzipDir)
    //将 jar 转成 dex
    println("将 jar 转成 dex")
    var shellJar = File(shellUnzipDir,"classes.jar")
    var shellDex = File("output/unzip/apk","classes.dex")
    DexUtils.dxCommand(shellJar,shellDex)
    moveLibSoToApk()
    //打包
    println("打包 APK")
    var unsignedApk = File("output/unsigned_$orginApkName")
    ZipUtils.zip(File("output/unzip/apk"),unsignedApk)
}

/**
 * 将壳中的lib文件移到apk 中
 */
fun moveLibSoToApk(){
    var shellUnzipLibDir = File("output/unzip/shell/jni")
    var apkUnzipLibDir = File("output/unzip/apk/lib")
    if(!apkUnzipLibDir.exists()){
        apkUnzipLibDir.mkdirs()
    }

    FileUtils.copy(shellUnzipLibDir,apkUnzipLibDir)
}

/**
 * 对齐
 */
fun zipalign(){
    println("将打包的 apk 对齐")
    var unsignedApk = File("output/unsigned_$orginApkName")
    val alignedApk = File("output/unsigned-aligned_$orginApkName")
    val process = Runtime.getRuntime().exec(
        "zipalign -p -f -v 4 " + unsignedApk.absolutePath + " " + alignedApk.absolutePath)
    process.waitFor(5,TimeUnit.SECONDS)
    try {
        if (process.exitValue() != 0) {
            println("zipalign 出错")
            FileUtils.printStream(process.errorStream)
        } else {
            FileUtils.printStream(process.inputStream)
        }
        println("完成 apk 的对齐")
        process.destroy()
    }catch (e:Exception){
        println("对齐超时...")
    }
}

/**
 * 对 APK 签名
 */
fun jksToApk(){
    println("签名 APK")
    var signedApk = File("output/signed_$orginApkName")
    val alignedApk = File("output/unsigned-aligned_$orginApkName")
    SignUtils.signature(alignedApk,signedApk,signFile.absolutePath)
}


fun main() {
    //清空输出目录
    var output = File("output/")
    if (output.exists()) {
        FileUtils.delete(output)
    }
    println("清空工作空间")
    findOriginApk()
    findOriginShell()
    apkDecode()
    changeAndroidManifest()
    apkBuild()
    //解压需要加固的 apk
    unZipApkAndEncrypt()
    //解压壳aar
    makeDecodeDex()
    //对齐打包apk
    zipalign()
    //删除解压目录
    //FileUtils.delete("output/unzip")
    jksToApk()
    println("Finished!!!")
}