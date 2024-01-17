package io.nekohasekai.sagernet.ui

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import io.nekohasekai.sagernet.utils.Theme

abstract class ThemedActivity : AppCompatActivity {
    constructor() : super()
    constructor(contentLayoutId: Int) : super(contentLayoutId)
    object GlobalStuff {
        var checkFreeAccount = false
        var checkGetListAccount = false
        var lang_HoloGate = "en"
        var check_language = false
        var check_add_profile_hologate = false
        //        var base_url = "http://136.243.86.130"
        var base_url = "https://shop.hologate.pro"
//        var base_url = "https://shop.holoo2.info"
//        var base_url = "http://deploy.hologate.info:2013"

        //        var base_url = "https://5.9.191.253"
        var token_fb: String? = null
        var id_fb: String? = null
    }
    var UrlHologate = ""
    //  var checkFreeAccount = false
//  var checkGetListAccount = false
    var themeResId = 0
    var uiMode = 0
    open val isDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isDialog) {
            Theme.apply(this)
        } else {
            Theme.applyDialog(this)
        }
        Theme.applyNightTheme()

        super.onCreate(savedInstanceState)

        uiMode = resources.configuration.uiMode
    }

    override fun setTheme(resId: Int) {
        super.setTheme(resId)

        themeResId = resId
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (newConfig.uiMode != uiMode) {
            uiMode = newConfig.uiMode
            ActivityCompat.recreate(this)
        }
    }

    fun snackbar(@StringRes resId: Int): Snackbar = snackbar("").setText(resId)
    fun snackbar(text: CharSequence): Snackbar = snackbarInternal(text).apply {
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).apply {
            maxLines = 10
        }
    }
    internal open fun snackbarInternal(text: CharSequence): Snackbar = throw NotImplementedError()

    fun GetAutUser(key: String?): String? {
        val prefs = getSharedPreferences("Wedding", MODE_PRIVATE)
        val restoredText = prefs.getString(key, "")
        return if (restoredText != "") {
            prefs.getString(key, "")
        } else ""
    }
    fun SetAutUser(key: String?, data: String?): SharedPreferences.Editor? {
        val editor = getSharedPreferences("Wedding", MODE_PRIVATE).edit()
        editor.putString(key, data)
        return editor
    }

    fun RemoveData(key: String?): SharedPreferences.Editor? {
        val editor = getSharedPreferences("Wedding", MODE_PRIVATE).edit()
        editor.remove(key)
        return editor
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}