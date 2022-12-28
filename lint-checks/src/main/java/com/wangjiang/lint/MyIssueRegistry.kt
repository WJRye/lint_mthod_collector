package com.wangjiang.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.wangjiang.lint.checks.collector.MethodCollectorDetector

@Suppress("UnstableApiUsage")
class MyIssueRegistry : IssueRegistry() {
    override val issues = listOf(
        MethodCollectorDetector.ISSUE,
    )

    override val api: Int
        get() = CURRENT_API

    override val minApi: Int
        get() = 8 // works with Studio 4.1 or later; see com.android.tools.lint.detector.api.Api / ApiKt

    // Requires lint API 30.0+; if you're still building for something
    // older, just remove this property.
    override val vendor: Vendor = Vendor(
        vendorName = "Wj Custom Lint",
        feedbackUrl = "https://github.com/WJRye/lint_mthod_collector.git",
        contact = "1126594968@qq.com"
    )
}