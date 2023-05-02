package com.wangjiang.lint.checks.collector

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiType
import org.objectweb.asm.tree.*
import java.io.File
import java.io.FilenameFilter
import java.util.*
import java.util.logging.Logger

/**
 * 方法收集检查者
 */
class MethodCollectorDetector : Detector(), Detector.ClassScanner {


    private var methodCollectorHelper: MethodCollectorHelper? = null

    private val filePrefix by lazy(LazyThreadSafetyMode.NONE) {
        if (System.getProperties().getProperty("os.name").lowercase(Locale.ROOT)
                .contains("windows")
        ) "" else "file://"
    }

    private var checkMethodCollector = true

    private fun findTargetFile(fileName: String, dir: File): File? {
        return dir.listFiles { _, name -> fileName == name }?.takeIf { it.isNotEmpty() }?.get(0)
    }

    private fun createPrivacyHelper(project: Project): MethodCollectorHelper? {
        if (!project.isAndroidProject) {
            log("This is not an Android Project: ${project.name}")
            return null
        }
        if (methodCollectorHelper != null) return methodCollectorHelper

        var curDir: File? = project.dir
        while (curDir != null && findTargetFile(SETTINGS_GRADLE_NAME, curDir) == null) {
            curDir = curDir.parentFile
        }
        if (curDir == null) {
            log("Can't find Root Project for: ${project.name} ")
            return null
        }
        val collectorConfigFile = findTargetFile(COLLECTOR_CONFIG_NAME, curDir)
        if (collectorConfigFile == null) {
            log("Can't find Collector Config for: ${project.name} ")
            return null
        }
        methodCollectorHelper = MethodCollectorHelper(collectorConfigFile.path)
        return methodCollectorHelper
    }


    override fun getApplicableAsmNodeTypes(): IntArray {
        return intArrayOf(
            AbstractInsnNode.FIELD_INSN,
            AbstractInsnNode.METHOD_INSN
        )
    }

    private fun log(msg: String) {
        Logger.getLogger("MethodCollectorDetector").warning(
            msg
        )
    }

    override fun checkInstruction(
        context: ClassContext,
        classNode: ClassNode,
        method: MethodNode,
        instruction: AbstractInsnNode
    ) {
        super.checkInstruction(context, classNode, method, instruction)
        if (checkMethodCollector) {
            if (instruction is MethodInsnNode) {
                methodCollectorHelper?.matchClassMethod(
                    instruction.owner, instruction.name, classNode.name, method.name
                ).takeIf { msg -> !msg.isNullOrEmpty() }?.let { msg ->
                    logMethodWithFormat(context, classNode, method, instruction)
                    MethodReport.instance.addReporterModel(MethodReporterModel().apply {
                        this.ownerClassName = instruction.owner
                        this.ownerClassMethodName =
                            getOwnerClassMethodParameter(instruction.name, instruction.desc)
                        this.callerClassName = classNode.name
                        this.callerClassMethodName =
                            method.name + getCallerClassMethodParameter(context, classNode, method)
                        this.callerClassMethodLine = ClassContext.findLineNumber(method)
                    })
                    context.report(
                        ISSUE, context.getLocation(instruction), msg
                    )
                }

            } else if (instruction is FieldInsnNode) {
                methodCollectorHelper?.matchClassField(
                    instruction.owner, instruction.name, classNode.name, method.name
                ).takeIf { msg -> !msg.isNullOrEmpty() }?.let { msg ->
                    logFieldWithFormat(context, classNode, method, instruction)
                    MethodReport.instance.addReporterModel(MethodReporterModel().apply {
                        this.ownerClassName = instruction.owner
                        this.ownerClassFieldName =
                            getOwnerClassFieldParameter(instruction.name, instruction.desc)
                        this.callerClassName = classNode.name
                        this.callerClassMethodName =
                            method.name + getCallerClassMethodParameter(context, classNode, method)
                        this.callerClassMethodLine = ClassContext.findLineNumber(method)
                    })
                    context.report(
                        ISSUE, context.getLocation(instruction), msg
                    )
                }
            }

        }
    }

    /**
     *根据方法描述，解析方法返回类型和方法参数类型
     */
    private
    fun getTypeDes(typeDes: String): String {
        return if (typeDes.length == 1) {
            when (typeDes[0]) {
                'B' -> "byte"
                'S' -> "short"
                'Z' -> "boolean"
                'C' -> "char"
                'I' -> "int"
                'J' -> "long"
                'F' -> "float"
                'D' -> "double"
                'V' -> "void"
                else -> typeDes
            }
        } else {
            if (typeDes.startsWith('[')) {
                getTypeDes(typeDes.substring(1)) + "[]"
            } else {
                var endIndex = typeDes.indexOf(';')
                if (endIndex == -1) {
                    endIndex = typeDes.length
                }
                typeDes.substring(typeDes.indexOf('L') + 1, endIndex).replace('/', '.')
            }
        }
    }

    /**
     * 根据Owner Class Field，生成一个完整的方法字符串
     * @param name 方法名字
     * @param desc 方法描述（JNI方法描述）
     */
    private fun getOwnerClassFieldParameter(name: String, desc: String): String {
        val sb = StringBuilder()
        val fieldDes = getTypeDes(desc)
        sb.append(fieldDes)
        sb.append(" ")
        sb.append(name)
        return sb.toString()
    }

    /**
     * 根据Owner Class Method，生成一个完整的方法字符串
     * @param name 方法名字
     * @param desc 方法描述（JNI方法描述）
     */
    private fun getOwnerClassMethodParameter(name: String, desc: String): String {
        val sb = StringBuilder()
        val returnStr = desc.substring(desc.indexOf(')') + 1, desc.length)
        val returnDesc = getTypeDes(returnStr)
        sb.append(returnDesc)
        sb.append(" ")
        sb.append(name)
        val paramsStr = desc.substring(1, desc.length - returnStr.length - 1)
        //添加前括号
        sb.append(desc[0])
        paramsStr.split(';').forEach {
            if (it.startsWith('L')) {
                sb.append(getTypeDes(it))
                sb.append(',')
            } else {
                var index = 0
                while (index < it.length) {
                    var pre = ""
                    if (it[index] == '[') {
                        pre = it[index].toString()
                        ++index
                    }
                    val c = it[index]
                    if (c == 'L') {
                        sb.append(getTypeDes(pre + it.substring(index, it.length)))
                        sb.append(',')
                        sb.append(" ")
                        break
                    } else {
                        sb.append(getTypeDes(pre + c.toString()))
                        sb.append(',')
                        sb.append(" ")
                    }
                    index++
                }
            }
        }
        val startIndex = sb.lastIndexOf(',')
        if (startIndex != -1) sb.delete(
            startIndex, sb.length
        )
        //添加后括号
        sb.append(desc[desc.length - returnStr.length - 1])
        return sb.toString()
    }

    /**
     * 根据Caller Class Method，生成一个完整的方法字符串
     * @param context 类上下文
     * @param classNode 类节点
     * @param method 方法节点
     */
    private fun getCallerClassMethodParameter(
        context: ClassContext, classNode: ClassNode, method: MethodNode
    ): String {
        val sb = StringBuilder()
        sb.append('(')
        context.findPsiClass(classNode)?.findMethodsByName(method.name)?.iterator()?.forEach {
            it.parameters.forEach { jvmParameter ->
                val type = jvmParameter.type
                if (type is PsiType) {
                    sb.append(type.presentableText)
                }
                sb.append(" ")
                sb.append(jvmParameter.name)
                sb.append(",")
                sb.append(" ")
            }
        }
        if (sb.length > 2) sb.delete(
            sb.length - 2, sb.length
        )
        sb.append(')')
        return sb.toString()
    }

    /**
     * 打印日志
     */
    private fun logMethodWithFormat(
        context: ClassContext,
        classNode: ClassNode,
        method: MethodNode,
        instruction: MethodInsnNode
    ) {
        log(
            "MethodCollector found method owner: ${instruction.owner}#${instruction.name}\tcaller: ${classNode.name}#${method.name}\t${
                ClassContext.findLineNumber(
                    method
                )
            }\t$filePrefix${context.file.absolutePath}"
        )
    }

    private fun logFieldWithFormat(
        context: ClassContext,
        classNode: ClassNode,
        method: MethodNode,
        instruction: FieldInsnNode
    ) {
        log(
            "MethodCollectorfound field owner: ${instruction.owner}#${instruction.name}\tcaller: ${classNode.name}#${method.name}\t${
                ClassContext.findLineNumber(
                    method
                )
            }\t$filePrefix${context.file.absolutePath}"
        )
    }

    override fun beforeCheckRootProject(context: Context) {
        super.beforeCheckRootProject(context)
        checkMethodCollector = context.isEnabled(ISSUE)
        log("project ${context.mainProject.name} ${if (checkMethodCollector) "enable" else "disable"} method collector")
        if (!checkMethodCollector) {
            return
        }
        MethodReport.instance.clearReporterModels()
        createPrivacyHelper(context.mainProject)?.let {
            getBuildReportsDir(context).listFiles(object : FilenameFilter {
                override fun accept(dir: File?, name: String?): Boolean {
                    dir ?: return false
                    name ?: return false
                    return name.startsWith("lint-results")
                }
            })?.forEach {
                if (it.exists() && it.isFile) {
                    val path = it.path
                    val ret = it.delete()
                    log("Delete Lint report $path ${if (ret) "Success" else "Failed"}")
                }
            }
            getMethodCollectorReportDir(context).listFiles()?.takeIf { it.isNotEmpty() }
                ?.forEach {
                    if (it.exists() && it.isFile && it.name != MethodReport.LAST_JSON_RESULT_FILE_NAME) {
                        val path = it.path
                        val ret = it.delete()
                        log("Delete Method Collector report $path ${if (ret) "Success" else "Failed"}")
                    }
                }
        }
    }

    private fun getBuildReportsDir(context: Context): File {
        val buildFile = File(context.mainProject.dir, BUILD_NAME)
        if (!buildFile.exists()) {
            buildFile.mkdirs()
        }
        val buildReportsFile = File(buildFile, BUILD_REPORTS_NAME)
        if (!buildReportsFile.exists()) {
            buildReportsFile.mkdirs()
        }
        return buildReportsFile
    }

    private fun getMethodCollectorReportDir(context: Context): File {
        val variantName = context.mainProject.buildVariant.name
        val buildReportsFile = getBuildReportsDir(context)
        val collectorReportDir = File(buildReportsFile, "$METHOD_COLLECTOR_NAME-$variantName")
        if (!collectorReportDir.exists()) {
            collectorReportDir.mkdirs()
        }
        return collectorReportDir
    }

    override fun afterCheckRootProject(context: Context) {
        super.afterCheckRootProject(context)
        if (!checkMethodCollector) {
            return
        }
        methodCollectorHelper?.let {
            val collectorReportDir = getMethodCollectorReportDir(context)
            if (it.outputSingle()) {
                MethodReport.instance.reportSingle(
                    context.mainProject.name, collectorReportDir
                )
            }
            if (it.outputMulti()) {
                MethodReport.instance.reportMulti(
                    context.mainProject.name, collectorReportDir
                )
            }
            log("Wrote Method Collector report to dir ${filePrefix}${collectorReportDir.path}")
        }
    }

    companion object {

        private const val BUILD_NAME = "build"
        private const val BUILD_REPORTS_NAME = "reports"
        private const val METHOD_COLLECTOR_NAME = "method-collector"
        private const val SETTINGS_GRADLE_NAME = "settings.gradle"
        private const val COLLECTOR_CONFIG_NAME = "collector_config.json"

        val ISSUE = Issue.create(
            "MethodCollector",  //唯一 ID
            "在目录$BUILD_NAME/$BUILD_REPORTS_NAME/${METHOD_COLLECTOR_NAME}下查看相关方法调用者信息",  //简单描述
            "根据配置文件中类方法，找到调用它的地方，包括项目和三方库",  //详细描述
            Category.CORRECTNESS,  //问题种类（正确性、安全性等）
            6, Severity.WARNING,  //问题严重程度（忽略、警告、错误）
            Implementation( //实现，包括处理实例和作用域
                MethodCollectorDetector::class.java, Scope.ALL_CLASSES_AND_LIBRARIES
            )
        )
    }
}
