package com.wangjiang.lint.checks.collector

import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

/**
 * 将结果写入Json文件报告
 */
class JsonResultReporter : ResultReporter {
    companion object {
        @JvmStatic
        fun newInstance() = JsonResultReporter()
    }

    override fun writeToSingleFile(
        projectName: String,
        data: Map<String, HashSet<MethodReporterModel>>,
        dir: File
    ) {
        val list = data.values.map {
            it.groupBy { it.ownerClassMethodName }
        }.flatMap { it.values }

        val outputStream = FileOutputStream(File(dir, "all.json"))
        outputStream.write(Gson().toJson(list).toByteArray(Charsets.UTF_8))
        outputStream.flush()
        outputStream.close()
    }

    override fun writeToMultiFile(
        projectName: String,
        data: Map<String, HashSet<MethodReporterModel>>,
        dir: File
    ) {
        for ((key, value) in data) {
            val outputStream = FileOutputStream(File(dir, "${key.replace('/', '.')}.json"))
            outputStream.write(Gson().toJson(value.groupBy { it.ownerClassMethodName }
                .flatMap { it.value }).toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()
        }
    }
}