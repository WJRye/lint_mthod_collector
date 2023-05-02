package com.wangjiang.lint.checks.collector

class MethodCollectorModel {

    companion object {
        private const val TYPE_SINGLE = "single"
        private const val TYPE_MULTI = "multi"
        private const val MATCH_PACKAGE = "package"
        private const val MATCH_CLASS = "class"
    }

    var methods = arrayListOf<TargetModel>()
    var fields = arrayListOf<TargetModel>()


    var output: OutputModel = OutputModel()

    class TargetModel {
        var owner: String = ""
        var name: String = ""
        var message: String = ""
        var match: String = MATCH_CLASS
        var excludes = arrayListOf<ExcludeModel>()

        fun isMatchPackage() = match == MATCH_PACKAGE
        fun isMatchClass() = match.isEmpty() || match == MATCH_CLASS

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