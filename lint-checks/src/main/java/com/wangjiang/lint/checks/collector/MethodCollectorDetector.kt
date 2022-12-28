package com.wangjiang.lint.checks.collector

import com.android.tools.lint.detector.api.*
import com.wangjiang.lint.checks.collector.MethodReport
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
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
        if (System.getProperties().getProperty("os.name").toLowerCase(Locale.ROOT)
                .contains("windows")
        ) "" else "file://"
    }

    private var checkMethodCollector = true

    private fun findTargetFile(fileName: String, dir: File): File? {
        return dir.listFiles { _, name -> fileName == name }
            ?.takeIf { it.isNotEmpty() }?.get(0)
    }

    private fun createPrivacyHelper(project: Project): MethodCollectorHelper? {
        if (!project.isAndroidProject) {
            log("This is not a Android Project: ${project.name}")
            return null
        }
        if (methodCollectorHelper != null) return methodCollectorHelper

        var curDir: File? = project.dir
        val settingGradleFileName = "settings.gradle"
        while (curDir != null && findTargetFile(settingGradleFileName, curDir) == null) {
            curDir = curDir.parentFile
        }
        if (curDir == null) {
            log("Can't find Root Project for: ${project.name} ")
            return null
        }
        val collectorConfigFileName = "collector_config.json"
        val collectorConfigFile = findTargetFile(collectorConfigFileName, curDir)
        if (collectorConfigFile == null) {
            log("Can't find Collector Config for: ${project.name} ")
            return null
        }
        methodCollectorHelper = MethodCollectorHelper(collectorConfigFile.path)
        return methodCollectorHelper
    }


    override fun getApplicableAsmNodeTypes(): IntArray {
        return intArrayOf(
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
        if (checkMethodCollector && instruction is MethodInsnNode) {
            methodCollectorHelper?.match(
                instruction.owner,
                instruction.name,
                classNode.name,
                method.name
            )
                .takeIf { msg -> !msg.isNullOrEmpty() }
                ?.let { msg ->
                    logWithFormat(context, classNode, method, instruction)
                    MethodReport.instance.addReporterModel(MethodReporterModel().apply {
                        this.ownerClassName = instruction.owner
                        this.ownerClassMethodName = instruction.name
                        this.callerClassName = classNode.name
                        this.callerClassMethodName = method.name
                        this.callerClassMethodLine = ClassContext.findLineNumber(method)
                    })
                    context.report(
                        ISSUE, context.getLocation(instruction),
                        msg
                    )
                }

        }
    }

    /**
     * 打印日志
     */
    private fun logWithFormat(
        context: ClassContext,
        classNode: ClassNode,
        method: MethodNode,
        instruction: MethodInsnNode
    ) {
        log(
            "method collector found owner: ${instruction.owner}#${instruction.name}\tcaller: ${classNode.name}#${method.name}\t${
                ClassContext.findLineNumber(
                    method
                )
            }\t$filePrefix${context.file.path}\t${context.jarFile?.name ?: ""}"
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
            getMethodCollectorReportDir(context).listFiles()?.takeIf { it.isNotEmpty() }?.forEach {
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
                    context.mainProject.name,
                    collectorReportDir
                )
            }
            if (it.outputMulti()) {
                MethodReport.instance.reportMulti(
                    context.mainProject.name,
                    collectorReportDir
                )
            }
            log("Wrote Method Collector report to dir ${filePrefix}${collectorReportDir.path}")
        }
    }

    companion object {

        private const val BUILD_NAME = "build"
        private const val BUILD_REPORTS_NAME = "reports"
        private const val METHOD_COLLECTOR_NAME = "method-collector"

        val ISSUE = Issue.create(
            "MethodCollector",  //唯一 ID
            "在目录${BUILD_NAME}/${BUILD_REPORTS_NAME}/${METHOD_COLLECTOR_NAME}下查看相关方法调用者信息",  //简单描述
            "根据配置文件中类方法，找到调用它的地方，包括项目和三方库",  //详细描述
            Category.CORRECTNESS,  //问题种类（正确性、安全性等）
            6, Severity.WARNING,  //问题严重程度（忽略、警告、错误）
            Implementation( //实现，包括处理实例和作用域
                MethodCollectorDetector::class.java,
                Scope.ALL_CLASSES_AND_LIBRARIES
            )
        )
    }
}