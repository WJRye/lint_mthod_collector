package com.wangjiang.lint.checks.collector

import org.w3c.dom.DOMConfiguration
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import org.w3c.dom.ls.LSOutput
import org.w3c.dom.ls.LSSerializer
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 将结果写入Html文件报告
 */
class HtmlResultReporter : ResultReporter {
    companion object {
        @JvmStatic
        fun newInstance() = HtmlResultReporter()
    }

    override fun writeToSingleFile(
        projectName: String,
        data: Map<String, HashSet<MethodReporterModel>>,
        dir: File
    ) {
        val htmlDoc = makeBasicHTMLDoc("Lint Method Collector Results")
        createCss(htmlDoc)
        val divElement: Element = htmlDoc.createElement("div")
        val bodyElement = htmlDoc.getElementsByTagName("body").item(0) as Element
        bodyElement.appendChild(divElement)
        appendChild(htmlDoc, divElement, "h1", "项目 $projectName 方法收集结果\t${getSum(data)}")
        for ((key, value) in data) {
            appendTableElement(htmlDoc, divElement, key, value)
        }
        write(htmlDoc, dir.path, "all")
    }

    /**
     * 获取扫描结果总数
     */
    private fun getSum(data: Map<String, HashSet<MethodReporterModel>>): Int {
        var count = 0
        data.values.map { it.size }.forEach {
            count += it
        }
        return count
    }

    /**
     * 表格标题列表
     */
    private fun getTabTileList(target: String) =
        arrayListOf<String>("目标所属类", "目标所属类$target", "调用所属类", "调用所属类方法", "调用所属类方法行数")


    /**
     * 添加表格
     */
    private fun appendTableElement(
        htmlDoc: Document,
        divElement: Element,
        key: String,
        value: Set<MethodReporterModel>
    ) {
        appendChild(htmlDoc, divElement, "h2", "$key\t${value.size}")
        val tableElement = htmlDoc.createElement("table")

        fun addTableElement(
            sequence: Sequence<MethodReporterModel>,
            target: String,
            predicate: (MethodReporterModel) -> Boolean,
            keySelector: (MethodReporterModel) -> String
        ) {
            sequence.filter(predicate).groupBy(keySelector).flatMap { it.value }.toSet()
                .takeIf { it.isNotEmpty() }?.let { value ->
                    getTabTileList(target).forEach {
                        appendChild(htmlDoc, tableElement, "th", it)
                    }
                    value.forEach { model ->
                        val trElement = htmlDoc.createElement("tr")
                        val tabContent = arrayListOf<String>(
                            model.ownerClassName,
                            keySelector.invoke(model),
                            model.callerClassName,
                            model.callerClassMethodName,
                            model.callerClassMethodLine.toString()
                        )
                        tabContent.forEach {
                            appendChild(htmlDoc, trElement, "td", it)
                        }
                        tableElement.appendChild(trElement)
                    }

                }
        }

        addTableElement(
            value.asSequence(),
            "属性",
            { it.ownerClassFieldName.isNotEmpty() },
            { it.ownerClassFieldName })
        addTableElement(
            value.asSequence(),
            "方法",
            { it.ownerClassMethodName.isNotEmpty() },
            { it.ownerClassMethodName })

        divElement.appendChild(tableElement)
    }

    override fun writeToMultiFile(
        projectName: String,
        data: Map<String, HashSet<MethodReporterModel>>,
        dir: File
    ) {
        for ((key, value) in data) {
            val htmlDoc = makeBasicHTMLDoc("Lint Method Collector Results")
            createCss(htmlDoc)
            val divElement: Element = htmlDoc.createElement("div")
            val bodyElement = htmlDoc.getElementsByTagName("body").item(0) as Element
            bodyElement.appendChild(divElement)
            appendChild(htmlDoc, divElement, "h1", "项目 $projectName 方法收集结果\t${getSum(data)}")

            appendTableElement(htmlDoc, divElement, key, value)

            write(htmlDoc, dir.path, key.replace('/', '.'))
        }
    }

    /**
     * 写入 HTML 文档
     *
     * @param document 文档对象
     * @param path     文档路径
     * @param filename 文件名字
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws FileNotFoundException
     */
    @Throws(
        InstantiationException::class,
        IllegalAccessException::class,
        ClassNotFoundException::class,
        FileNotFoundException::class
    )
    private fun write(document: Document, path: String, filename: String) {
        val registry: DOMImplementationRegistry = DOMImplementationRegistry.newInstance()
        val domImplLS: DOMImplementationLS =
            registry.getDOMImplementation("LS") as DOMImplementationLS
        val lsSerializer: LSSerializer = domImplLS.createLSSerializer()
        val domConfig: DOMConfiguration = lsSerializer.domConfig
        domConfig.setParameter("format-pretty-print", true) //if you want it pretty and indented
        val lsOutput: LSOutput = domImplLS.createLSOutput()
        lsOutput.encoding = "UTF-8"
        val htmlFile = File(path, "$filename.html")
        val os: OutputStream = FileOutputStream(htmlFile)
        lsOutput.byteStream = os
        lsSerializer.write(document, lsOutput)
        os.close()
    }

    /**
     * 添加子元素
     *
     * @param htmlDoc       文档对象
     * @param parentElement 父元素
     * @param name          子元素名字
     * @param textContent   子元素文本值
     * @return 子元素
     */
    private fun appendChild(
        htmlDoc: Document,
        parentElement: Element,
        name: String,
        textContent: String?
    ) {
        val element = htmlDoc.createElement(name)
        if (textContent != null) element.textContent = textContent
        parentElement.appendChild(element)
    }

    /**
     * 创建 CSS
     *
     * @param htmlDoc 文档对象
     */
    private fun createCss(htmlDoc: Document) {
        val cssElement = htmlDoc.createElement("style")
        cssElement.setAttribute("type", "text/css")
        val sb = StringBuilder()
        sb.append("div{max-width:100%;width:50%;margin:0 auto}").append("\n")
            .append("table{width:100%;color:#333333;border-width:1px;border-color:#666666;border-collapse:collapse;}")
            .append("\n")
            .append("table th{border-width:1px;padding:10px;border-style:solid;border-color:#666666;background-color:#dedede;}")
            .append("\n")
            .append("table td{border-width:1px;padding:10px;border-style:solid;border-color:#666666;background-color:#ffffff;}")
            .append("\n")
        cssElement.textContent = sb.toString()
        val headElement = htmlDoc.getElementsByTagName("head").item(0) as Element
        headElement.appendChild(cssElement)
    }

    /**
     * 创建基本HTML文档
     *
     * @param title 文档标题
     * @return 文档对象
     */
    private fun makeBasicHTMLDoc(title: String?): Document {
        val documentBuilder: DocumentBuilder =
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val htmlDoc: Document = documentBuilder.newDocument()
        val htmlElement: Element = htmlDoc.createElementNS("http://www.w3.org/1999/xhtml", "html")
        htmlDoc.appendChild(htmlElement)
        val headElement: Element = htmlDoc.createElement("head")
        htmlElement.appendChild(headElement)
        val metaElement: Element = htmlDoc.createElement("meta")
        metaElement.setAttribute("content", "txt/html; charset=utf-8")
        metaElement.setAttribute("http-equiv", "content-type")
        headElement.appendChild(metaElement)
        val titleElement: Element = htmlDoc.createElement("title")
        if (title != null) titleElement.textContent = title
        headElement.appendChild(titleElement)
        val bodyElement: Element = htmlDoc.createElement("body")
        htmlElement.appendChild(bodyElement)
        return htmlDoc
    }
}