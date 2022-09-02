# ApkJiagu

#### 介绍
该项目讲诉如何将 apk 反编译、编译、修改 AndroidManifest文件、加密 dex 文件、重新打包、签名、加固

[Android APK 加固技术探究（一）](https://juejin.cn/post/6985038911680544775/)

[Android APK 加固技术探究（二）](https://juejin.cn/post/6985040096042942495/)

[Android APK 加固技术探究（三）](https://juejin.cn/post/7025600894829854733/)

> 为了保证 Android 应用的源码安全性，我们一般会对上线的应用进行代码混淆，然而仅仅做代码混淆还不够，我们还要对我们的应用加固，防止别人通过反编译获取到我们的源码。目前 apk 加固技术比较成熟完善，市面上比较流行的有“360加固”。本文就 apk 加固技术做一个技术探究，希望读者看过后能明白加固的其中原理，并也能自己实现加固方案。

在 [Android apk 加固技术探究（二）](https://juejin.cn/post/6985040096042942495/)中，我们已经通过创建 Steady 模块生成了一个 shell.arr 文件，用来对加密后的 dex 文件进行解密和类加载操作。这篇文章主要讲解如何对原 apk 的 dex 加密和把 shell.arr 打入到原 apk 中并最终生成一个新的 apk

### 一、反编译 APK 文件
[Android APK 加固技术探究（一）](https://juejin.cn/post/6985038911680544775/)中讲解了如何反编译 apk 文件，这里使用 apktool 这个工具来反编译 apk。通过执行命令 java -jar outlibs/apktool_2.5.0.jar d '待解压apk路径' -o '解压后存放的路径'

```kotlin
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
```
### 二、修改 AndroidManifest.xml 文件
步骤一中获得了解压后的文件目录，找到目录中的 AndroidManifest 文件。这里修改 AndroidManifest.xml 文件尝试过有2种方式，一种是通过 “AXMLEditor.jar” 和 “AXMLPrinter2.jar” 工具修改 AndroidManifest.xml 文件，另一种是通过 SAX 的方式解析 xml 文件，然后在相应的节点位置插入需要的数据，最后发现方法一虽然修改了xml 文件但是最终打包的新 APK 中 AndroidManifest.xml 文件没有生效，后来使用方法二生效了。下面把2种方式的代码都贴出来，如果哪个大佬发现了方法一中的问题，还请不吝赐教。

方法一：关于 “AXMLEditor.jar” 和 “AXMLPrinter2.jar” 两个工具如何使用可以自行百度，这里不做展开
```kotlin
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
```

方法二：SAXReader 的使用方式自行查看相关 API 文档
```kotlin
/**
 * 修改 xml 文件
 */
fun changeAndroidManifest(){
    println("开始修改 AndroidManifest")
    var manifestFile = File("output/apktool/decode/AndroidManifest.xml")
   changeXmlBySax(manifestFile,"com.sakuqi.steady.SteadyApplication")
   //com.sakuqi.steady.SteadyApplication名称为 Shell.arr 中的Application 类
}

/**
 * 修改xml文件
 */
fun changeXmlBySax(fileXml:File,newApplicationName:String){
    var sax = SAXReader()
    var document = sax.read(fileXml)
    var root = document.rootElement
    var application = root.element("application")
    //原有的 application 名称
    var applicationName = application.attributeValue("name")
    var applicationAttr = application.attribute("name")
    //将壳中的 application  替换原来的 application
    applicationAttr.text = newApplicationName

    var element = application.addElement("meta-data")
    element.addAttribute("android:name","app_name")
    element.addAttribute("android:value",applicationName)
    saveDocument(document,fileXml)

}
fun saveDocument(document:Document,file:File){
    var osWrite = OutputStreamWriter(FileOutputStream(file))
    var format = OutputFormat.createPrettyPrint()// 获取输出的指定格式
    format.encoding = "UTF-8"
    var writer = XMLWriter(osWrite,format)
    writer.write(document)
    writer.flush()
    writer.close()
}
```
### 三、编译修改 AndroidManifest.xml 后的反编译目录

```
/**
 * 编译 APK 文件
 */
fun apkBuild(){
    println("开始重新编译")
    val process = Runtime.getRuntime()
        .exec("java -jar outlibs/apktool_2.5.0.jar b "+ "反编译后的目录"+" -o "+ “编译后的目录”)
    process.waitFor()
    if(process.exitValue() != 0) {
        FileUtils.printStream(process.errorStream)
    }else{
        FileUtils.printStream(process.inputStream)
    }
    process.destroy()
}
```

### 四、解压 APK 文件并加密所以 Dex 文件

解压使用的是 java.util.zip.ZipFile 类，这里封装了工具类最后会放到源码里，这里就不展开了。解压后需要将原 apk 中的签名文件删除，以便后续重新签名。过滤出解压目录下的所有 dex 后缀文件，然后对其进行加密，需要注意的是加密方式需要和 shell.arr 中的解密方式保持一致，这里使用的是 AES 的加密方式，源代码会在后续的开源项目中展示。加密后需要将原来的 dex 文件删除。大致代码如下：
```kotlin
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
```

### 五、解压壳 aar 得到 class.jar ,然后把 class.jar 在转换成 class.dex，再将class.dex 移到原 apk 的解压目录，最后压缩成新的 apk 文件
这里解压依然使用的是 unzip 的工具类，转换 class.dex 使用的是 Android SDK 中自带的命令 dx

```kotlin
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
```
```kotlin
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
```

### 五、将压缩后的新的 apk 文件进行 zip 对齐操作
```kotlin
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
```

### 六、将对齐后的 apk 文件进行签名
```kotlin
/**
 * 对 APK 签名
 */
fun jksToApk(){
    println("签名 APK")
    var signedApk = File("output/signed_$orginApkName")
    val alignedApk = File("output/unsigned-aligned_$orginApkName")
    SignUtils.signature(alignedApk,signedApk,signFile.absolutePath)
}
```
```kotlin
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
            "密码",
            "-keypass",
            "密码",
            "-signedjar",
            signedApk.absolutePath,
            unsignedApk.absolutePath,
            "alinas"
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
```

至此 apk 的加固流程全部讲完

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
