package com.sakuqi.shell.utils

import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import javax.xml.parsers.SAXParserFactory

object XmlParseUtils {
    fun sax2xml(ips: InputStream):String{
        val spf = SAXParserFactory.newInstance()
        //初始化Sax解析器
        val sp = spf.newSAXParser()
        val handler = MyHandler()
        sp.parse(ips,handler)
        return handler.originName?:""
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


    class MyHandler: DefaultHandler() {
        var originName:String?=null
        var isApplication = false
        override fun startDocument() {
            super.startDocument()
        }

        override fun endDocument() {
            super.endDocument()
        }

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?
        ) {
            super.startElement(uri, localName, qName, attributes)
            isApplication = "application" == qName
            if(isApplication) {
                originName = attributes?.getValue("android:name")
            }
        }



        override fun endElement(uri: String?, localName: String?, qName: String?) {
            super.endElement(uri, localName, qName)
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            super.characters(ch, start, length)
        }
    }
}