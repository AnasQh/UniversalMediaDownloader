package com.universal.mediadownloader.core

data class SniffedMedia(
    val url: String,
    val mimeType: String,
    val headers: Map<String, String>,
    val contentLength: Long = 0,
    val name: String = "video"
)
