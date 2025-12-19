package com.universal.mediadownloader.core.privacy

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

object IncognitoManager {
    
    fun clearData(context: Context) {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        
        // Clear WebView cache
        WebView(context).clearCache(true)
        WebView(context).clearHistory()
    }
}
