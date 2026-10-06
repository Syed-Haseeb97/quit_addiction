package com.example.dmsonly.web

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class InstagramWebChromeClient(
    private val requestFileSelection: (
        acceptTypes: Array<String>,
        allowMultiple: Boolean,
        callback: ValueCallback<Array<Uri>>?
    ) -> Unit
) : WebChromeClient() {

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams
    ): Boolean {
        requestFileSelection(
            fileChooserParams.acceptTypes,
            fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE,
            filePathCallback
        )
        return true
    }
}
