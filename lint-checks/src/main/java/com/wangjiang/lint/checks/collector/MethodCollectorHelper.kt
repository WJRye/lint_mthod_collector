package com.wangjiang.lint.checks.collector

import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream

class MethodCollectorHelper(configFilePath: String) {


    private var methodCollectorModel: MethodCollectorModel

    init {
        //这里读取文件和json转化不考虑主线程耗时
        this.methodCollectorModel =
            getMethodCollectorModel(configFilePath)
    }


    private fun getMethodCollectorModel(filePath: String): MethodCollectorModel {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            return MethodCollectorModel()
        }
        val inputStream = FileInputStream(file)
        val bytes = inputStream.readBytes()
        inputStream.close()
        return Gson().fromJson(String(bytes, Charsets.UTF_8), MethodCollectorModel::class.java)
    }


    /**
     * 与配置文件中要查找的类方法进行匹配
     * @param ownerClassName 目标所属类
     * @param ownerClassMethodName 目标所属类方法
     * @param callerClassName 调用所属类
     * @param callerClassMethodName 调用所属类方法
     */
    fun matchClassMethod(
        ownerClassName: String,
        ownerClassMethodName: String,
        callerClassName: String,
        callerClassMethodName: String
    ): String? {
        return match(
            methodCollectorModel.methods,
            ownerClassName,
            ownerClassMethodName,
            callerClassName,
            callerClassMethodName
        )

    }

    private fun match(
        models: List<MethodCollectorModel.TargetModel>, ownerClassName: String,
        ownerTargetName: String,
        callerClassName: String,
        callerClassTargetName: String
    ): String? {
        return models.find {
            if (it.isMatchClass()) {
                (it.owner == ownerClassName || ownerClassName.startsWith(it.owner)) && (it.name.isEmpty() || it.name == ownerTargetName) && it.excludes.find { exclude ->
                    (exclude.caller == callerClassName || callerClassName.startsWith(
                        exclude.caller
                    )) && (exclude.name.isEmpty() || exclude.name == callerClassTargetName)
                } == null
            } else if (it.isMatchPackage()) {
                ownerClassName.startsWith(it.owner) && it.excludes.find { exclude ->
                    callerClassName.startsWith(
                        exclude.caller
                    ) && (exclude.name.isEmpty() || exclude.name == callerClassTargetName)
                } == null
            } else {
                false
            }
        }?.message
    }

    /**
     * 与配置文件中要查找的类变量进行匹配
     * @param ownerClassName 目标所属类
     * @param ownerClassFieldName 目标所属类变量
     * @param callerClassName 调用所属类
     * @param callerClassFieldName 调用所属类变量
     */
    fun matchClassField(
        ownerClassName: String,
        ownerClassFieldName: String,
        callerClassName: String,
        callerClassFieldName: String
    ): String? {
        return match(
            methodCollectorModel.fields,
            ownerClassName,
            ownerClassFieldName,
            callerClassName,
            callerClassFieldName
        )
    }


    fun outputSingle() = methodCollectorModel.output.isSingle()
    fun outputMulti() = methodCollectorModel.output.isMulti()

    override fun toString(): String {
        return "MethodCollectorHelper(privacyModel=$methodCollectorModel)"
    }

}