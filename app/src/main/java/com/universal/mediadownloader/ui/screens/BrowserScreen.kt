package com.universal.mediadownloader.ui.screens

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.universal.mediadownloader.core.VideoSniffer
import com.universal.mediadownloader.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: MainViewModel,
    initialUrl: String = "https://www.google.com"
) {
    val detectedMedia by viewModel.detectedMedia.collectAsState()
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    val sheetState = rememberModalBottomSheetState()

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                request?.let {
                                    if (com.universal.mediadownloader.core.privacy.AdBlocker.shouldBlock(it)) {
                                        return com.universal.mediadownloader.core.privacy.AdBlocker.createEmptyResponse()
                                    }
                                    
                                    val media = VideoSniffer.sniff(it)
                                    if (media != null) {
                                        viewModel.onMediaDetected(media)
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }
                        }
                        
                        loadUrl(initialUrl)
                        webView = this
                    }
                },
                update = {
                    // Update logic if needed
                }
            )
            
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (detectedMedia != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissSheet() },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(androidx.compose.ui.unit.dp)) {
                    Text(text = "Download Detected!")
                    Text(text = detectedMedia?.name ?: "Unknown")
                    Text(text = detectedMedia?.mimeType ?: "")
                    Button(
                        onClick = { viewModel.startDownload(detectedMedia!!) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download")
                    }
                }
            }
        }
    }
}
