package com.wangjiang.lint.checks.collector

import java.io.File

/**
 * 生成报告结果
 */
interface ResultReporter {

    /**
     * 生成单独文件报告
     * @param projectName 项目名称
     * @param data 报告数据
     * @param dir 报告存放文件目录
     */
    fun writeToSingleFile(
        projectName: String,
        data: Map<String, HashSet<MethodReporterModel>>,
        dir: File
    )

    /**
     * 生成多个文件报告
     * @param projectName 项目名称
     * @param data 报告数据
     * @param dir 报告存放文件目录
     */
    fun writeToMultiFile(
        projectName: String,
        data: Map<String, HashSet<MethodReporterModel>>,
        dir: File
    )
}