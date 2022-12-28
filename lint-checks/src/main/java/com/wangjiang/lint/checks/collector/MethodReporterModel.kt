package com.wangjiang.lint.checks.collector

class MethodReporterModel {
    /**目标所属类名称*/
    var ownerClassName: String = ""

    /**目标所属类方法名称*/
    var ownerClassMethodName: String = ""

    /**调用所属类名称*/
    var callerClassName: String = ""

    /**调用所属类方法名称*/
    var callerClassMethodName: String = ""

    /**调用所属类方法行数*/
    var callerClassMethodLine: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodReporterModel

        if (ownerClassName != other.ownerClassName) return false
        if (ownerClassMethodName != other.ownerClassMethodName) return false
        if (callerClassName != other.callerClassName) return false
        if (callerClassMethodName != other.callerClassMethodName) return false
        if (callerClassMethodLine != other.callerClassMethodLine) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ownerClassName.hashCode()
        result = 31 * result + ownerClassMethodName.hashCode()
        result = 31 * result + callerClassName.hashCode()
        result = 31 * result + callerClassMethodName.hashCode()
        result = 31 * result + callerClassMethodLine
        return result
    }


}