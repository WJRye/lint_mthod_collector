package com.wangjiang.lint.checks.collector

class MethodCollectorModel {

    companion object {
        private const val TYPE_SINGLE = "single"
        private const val TYPE_MULTI = "multi"
    }

    var methods = arrayListOf<MethodModel>()


    var output: OutputModel = OutputModel()

    class MethodModel {
        var owner: String = ""
        var name: String = ""
        var message: String = ""
        var excludes = arrayListOf<ExcludeModel>()
        override fun toString(): String {
            return "MethodModel(owner='$owner', name='$name', message='$message', excludes=$excludes)"
        }

    }

    class ExcludeModel {
        var caller: String = ""
        var name: String = ""
        override fun toString(): String {
            return "ExcludeModel(caller='$caller', name='$name')"
        }
    }

    class OutputModel {
        var type: String = TYPE_SINGLE
        fun isSingle() = type == TYPE_SINGLE
        fun isMulti() = type == TYPE_MULTI
    }

    override fun toString(): String {
        return "MethodCollectorModel(methods=$methods, output=$output)"
    }


}