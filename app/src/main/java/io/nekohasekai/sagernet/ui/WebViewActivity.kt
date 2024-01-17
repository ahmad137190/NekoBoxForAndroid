package io.nekohasekai.sagernet.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.ui.ThemedActivity.GlobalStuff.base_url
import moe.matsuri.nb4a.utils.WebViewUtil


class WebViewActivity : ThemedActivity() {


    lateinit var mWebView: WebView


    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_webview)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.app_name)
            //  setDisplayHomeAsUpEnabled(true)
            //setHomeAsUpIndicator(R.drawable.ic_navigation_close)
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        // layout

//            toolbar.setTitle(R.string.menu_dashboard)
//
//
//        toolbar.inflateMenu(R.menu.yacd_menu)
//        toolbar.setOnMenuItemClickListener(this)

        //val binding = LayoutWebviewBinding.bind(view)

        // webview
        mWebView = findViewById<WebView>(R.id.webview)

        // mWebView = binding.webview
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.databaseEnabled = true
        mWebView.webChromeClient = WebChromeClient()


        mWebView.webViewClient = object : WebViewClient() {
          //  @Deprecated("Deprecated in Java")
//            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
//                view?.loadUrl("https://app.satia.co")
//                return true
//            }
//          @Deprecated("")
//          override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
//              return true
//          }
            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                WebViewUtil.onReceivedError(view, request, error)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }
            //val token = (this@WebViewActivity).GetAutUser("token")
        val url = (this@WebViewActivity).GetAutUser("url")
        val url1 = (this@WebViewActivity).GetAutUser("login_url")
        val url2 = (this@WebViewActivity).GetAutUser("login_url2")

       // mWebView.loadUrl("$base_url/login/466ad396f592342dc7bb356cb5452044d617989ee1c459ccb7e0bb3d50e3e263")
        if (url2.equals("")) {
            if (url1.equals("")) {
                url?.let { mWebView.loadUrl(it) }
            }
            else{
                url1?.let { mWebView.loadUrl(it) }
            }
        }
        else{

            var token = GetAutUser("token")

            token=  token?.replace("Bearer ","")
       //     println("WebViewActivity###   $base_url/login/$token")
            //     url2?.let { mWebView.loadUrl(base_url+it) }
            url2?.let { mWebView.loadUrl("$base_url/login/$token") }
        }
     //   mWebView.loadUrl("https://shop.holoo.pro?token=$token")

    }

}