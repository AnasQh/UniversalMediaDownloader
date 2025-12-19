package com.universal.mediadownloader.core.privacy

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {
    private val AD_HOSTS = setOf(
        "doubleclick.net", "googlesyndication.com", "facebook.com/tr", "ads.google.com"
        // In a real app, load this from a file
    )

    fun shouldBlock(request: WebResourceRequest): Boolean {
        val host = request.url.host ?: return false
        return AD_HOSTS.any { host.contains(it) }
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
    }
}
