package com.wangjiang.lint.checks.collector

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream

/**
 * 单例
 * 收集报告数据
 */
class MethodReport {


    private val collectedData = hashMapOf<String, HashSet<MethodReporterModel>>()

    companion object {
        val instance by lazy { MethodReport() }

        /**上一个目标方法检查结果json文件名称*/
        const val LAST_JSON_RESULT_FILE_NAME = "lint-method-collector.json"
    }

    fun clearReporterModels() {
        collectedData.clear()
    }

    /**
     * 添加报告数据
     */
    fun addReporterModel(model: MethodReporterModel) {
        var models = collectedData[model.ownerClassName]
        if (models == null) {
            models = hashSetOf()
        }
        models.add(model)
        collectedData[model.ownerClassName] = models
    }

    /**
     * 生成单独报告文件
     * @param projectName 项目名称
     * @param collectorDir 报告文件存放目录
     */
    fun reportSingle(
        projectName: String,
        collectorDir: File
    ) {
        filterCollectedData(getLastJsonResult(collectorDir)).let { filterCollectedData ->
            JsonResultReporter.newInstance()
                .writeToSingleFile(projectName, filterCollectedData, collectorDir)
            HtmlResultReporter.newInstance()
                .writeToSingleFile(projectName, filterCollectedData, collectorDir)
        }

    }

    /**
     * 生成多个报告文件
     * @param projectName 项目名称
     * @param collectorDir 报告文件存放目录
     */
    fun reportMulti(projectName: String, collectorDir: File) {
        filterCollectedData(getLastJsonResult(collectorDir)).let { filterCollectedData ->
            JsonResultReporter.newInstance()
                .writeToMultiFile(projectName, filterCollectedData, collectorDir)
            HtmlResultReporter.newInstance()
                .writeToMultiFile(projectName, filterCollectedData, collectorDir)
        }
    }

    /**
     * 获取上一个目标方法检查结果数据
     * @param collectorDir 报告文件存放目录
     */
    private fun getLastJsonResult(collectorDir: File): List<List<MethodReporterModel>> {
        val lastJsonResultFile = File(collectorDir, LAST_JSON_RESULT_FILE_NAME)
        if (!lastJsonResultFile.exists() || !lastJsonResultFile.isFile) {
            return arrayListOf()
        }
        val inputStream = FileInputStream(lastJsonResultFile)
        val bytes = inputStream.readBytes()
        inputStream.close()
        return Gson().fromJson(
            String(bytes, Charsets.UTF_8),
            object : TypeToken<List<List<MethodReporterModel>>>() {}.type
        )
    }

    /**
     * 上一个检查结果数据与当前检查结果数据进行diff
     */
    private fun filterCollectedData(lastReporterModels: List<List<MethodReporterModel>>): Map<String, HashSet<MethodReporterModel>> {
        lastReporterModels.forEach { models ->
            models.forEach { model ->
                collectedData[model.ownerClassName]?.let {
                    if (it.contains(model)) {
                        it.remove(model)
                    }
                }
            }
        }
        return collectedData
    }
}