package com.universal.mediadownloader.core

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.util.Locale

object VideoSniffer {

    private val VIDEO_EXTENSIONS = listOf(
        ".mp4", ".m3u8", ".ts", ".webm", ".mkv", ".flv", ".avi", ".mov", ".wmv"
    )

    private val IGNORED_EXTENSIONS = listOf(
        ".png", ".jpg", ".jpeg", ".gif", ".css", ".js", ".html", ".woff", ".ttf", ".svg"
    )

    fun shouldSniff(request: WebResourceRequest): Boolean {
        val url = request.url.toString().lowercase(Locale.ROOT)
        
        // Quick extension check
        if (IGNORED_EXTENSIONS.any { url.endsWith(it) }) return false
        
        // Check for video extensions
        if (VIDEO_EXTENSIONS.any { url.contains(it) }) return true
        
        // Check headers if available (often not available in request, but response interception is harder with WebViewClient)
        // We rely mostly on URL patterns and Content-Type sniffing if we were to do a full proxy, 
        // but for standard WebViewClient, we check the URL.
        
        return false
    }

    fun sniff(request: WebResourceRequest): SniffedMedia? {
        val url = request.url.toString()
        if (!shouldSniff(request)) return null

        val headers = request.requestHeaders ?: emptyMap()
        
        // Determine mime type based on extension (rough guess, refined later)
        val mimeType = when {
            url.contains(".m3u8") -> "application/x-mpegURL"
            url.contains(".mp4") -> "video/mp4"
            url.contains(".webm") -> "video/webm"
            else -> "video/unknown"
        }

        // Extract name from URL
        val name = url.substringAfterLast("/").substringBefore("?")

        return SniffedMedia(
            url = url,
            mimeType = mimeType,
            headers = headers,
            name = name
        )
    }
}
