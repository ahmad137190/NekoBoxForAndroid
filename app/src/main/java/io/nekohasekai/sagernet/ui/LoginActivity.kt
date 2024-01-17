package io.nekohasekai.sagernet.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.UnsafeOkHttpClient
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.launchCustomTab
import io.nekohasekai.sagernet.ktx.onMainDispatcher
import io.nekohasekai.sagernet.ktx.readableMessage
import io.nekohasekai.sagernet.ui.ThemedActivity.GlobalStuff.base_url
import io.nekohasekai.sagernet.ui.ThemedActivity.GlobalStuff.check_language
import io.nekohasekai.sagernet.utils.TLSSocketFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class LoginActivity : ThemedActivity() {


    var _emailText: EditText? = null
    var _passwordText: EditText? = null
    var _loginButton: Button? = null
    var btn_Register: Button? = null
    var dialog: AlertDialog? = null

    //var radioGroup22: RadioGroup?=null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_login)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.app_name)

            //  setDisplayHomeAsUpEnabled(true)
            //setHomeAsUpIndicator(R.drawable.ic_navigation_close)
        }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        _loginButton = findViewById<Button>(R.id.btn_login)
        btn_Register = findViewById<Button>(R.id.btn_Register)
        _passwordText = findViewById<EditText>(R.id.password)
        _emailText = findViewById<EditText>(R.id.username)
        var radioGroup22 = findViewById<RadioGroup>(R.id.radioGroup)
        /*  radioGroup.setOnCheckedChangeListener { group, checkedId ->
                 val text = "You selected: " + if (R.id.radioEnglish == checkedId) "english" else "persian"
                 Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
             }
     */

        dialog = AlertDialog.Builder(this).setView(R.layout.progress).setCancelable(false).create()

        /*
       val builder: AlertDialog.Builder = Builder(this)
         builder.setView(R.layout.progress)
         // This should be called once in your Fragment's onViewCreated() or in Activity onCreate() method to avoid dialog duplicates.
         // This should be called once in your Fragment's onViewCreated() or in Activity onCreate() method to avoid dialog duplicates.
         dialog = builder.create()
        * */


//            AlertDialog.Builder(this@LoginActivity)
//                .setTitle(R.string.error_title)
//                .setMessage("sdfsd")
//                .setPositiveButton(android.R.string.ok) { _, _ ->
//                    finish()
//                }
//                .setOnCancelListener {
//                    finish()
//                }


       // val lang = DataStore.configurationStore.getString(Key.LANGUAGE_MODE, "")
        val lang =  DataStore.languageMode
        if (lang == "" || lang == "en") {
            radioGroup22.check(R.id.radioEnglish)
        } else {
            radioGroup22.check(R.id.radioPersian)
        }

        radioGroup22.setOnCheckedChangeListener(
            RadioGroup.OnCheckedChangeListener { group, checkedId ->

                val radio_langange: RadioButton = findViewById(checkedId)

                if (radio_langange.text.equals("english")) {
                   // DataStore.configurationStore.putString(Key.LANGUAGE_MODE, "en")
                    DataStore.languageMode=Key.MODE_EN
                } else {
                  //  DataStore.configurationStore.putString(Key.LANGUAGE_MODE, "fa")
                    DataStore.languageMode=Key.MODE_FA

                }
                check_language = true
                finish()
                //   ActivityCompat.recreate(MainActivity::class.java)
//                val intent = Intent(this, MainActivity::class.java)
//                this.startActivity(intent)
//                finishAffinity()
//                  val refresh = Intent(this, MainActivity::class.java)
//                 finish()
//                 startActivity(refresh)
                //   Toast.makeText(applicationContext," On Checked change :${radio_langange.text}",Toast.LENGTH_SHORT).show()


            }
        )
        _loginButton!!.setOnClickListener {

            if (validate())
                login()

        }

        btn_Register!!.setOnClickListener {
            launchCustomTab(
                base_url + "/register?type=android"
            )
            //  startActivity(Intent(this@LoginActivity, WebViewActivity::class.java))
//            val intent = Intent(this@LoginActivity, WebViewActivity::class.java)
//            val b = Bundle()
//            b.putString("url", "https://shop.holoo.pro/register?type=android") //Your name
//
//            intent.putExtras(b) //Put your id to your next Intent
//
//            startActivity(intent)


        }
//
//        toolbar!!.setOnClickListener {
//            finish()
//
//        }


    }

    fun login() {

        val b = intent.extras
        var nameClass = "" // or other values

        if (b != null) nameClass = b.getString("name").toString()

        val email = _emailText!!.text.toString()
        val password = _passwordText!!.text.toString()

        //val payload = "username=majidlx@gmail.com&&password=123456789"

        //val okHttpClient = OkHttpClient()
        val okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient().build()
        // val requestBody = payload.toRequestBody()

        val jsonObject = JSONObject()
        try {
//            jsonObject.put("username", "majidlx@gmail.com")
//            jsonObject.put("password", "123456789")
            jsonObject.put("username", email)
            jsonObject.put("password", password)
            jsonObject.put("token_fb", GlobalStuff.token_fb)

        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val body: RequestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            jsonObject.toString()
        )

        //val mediaType = "application/json; charset=utf-8".toMediaType()
        //  val body = jsonObject.toString().toRequestBody(mediaType)
        // val tlsSocketFactory = TLSSocketFactory()


        dialog?.show()
        val request = Request.Builder()
            // .header("Authorization", "Bearer 5|HF0ERRIE1fgVXKes5AYC7WOUEK9y2ieDJeBIGwBD")
            .method("POST", body)
            // .method("POST", requestBody)
            // .post(body)

            .url(base_url + "/api/login")
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

                this@LoginActivity!!.runOnUiThread {
                    dialog?.dismiss()
                    Toast.makeText(
                        this@LoginActivity,
                        "The server encountered a problem",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Handle this
                println("onFailure : $e")

            }

            override fun onResponse(call: Call, response: Response) {
                dialog?.dismiss()
                // Handle this
                response.use {
                    println("onResponse000  $response")
                    dialog?.dismiss()

                    /*  for ((name, value) in response.headers) {
                          println("$name: $value")
                      }*/

                    try {
                        if (!response.isSuccessful) {
                            val jsonData: String = response.body!!.string()
                            val Jobject = JSONObject(jsonData)
                            this@LoginActivity!!.runOnUiThread {
                                Toast.makeText(
                                    this@LoginActivity,
                                    Jobject.getString("message"),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            throw IOException("Unexpected code $response")
                        }

                        val jsonData: String = response.body!!.string()
                        println("onResponse111   $jsonData")
                        //activity.SetAutUser("token", "").apply();
                        //  token = GetAutUser("token");
                        val Jobject = JSONObject(jsonData)
                        val token1: String? =
                            Jobject.getString("token_type") + " " + Jobject.getString("access_token")
                        (this@LoginActivity).SetAutUser(
                            "token",
                            token1
                        )?.apply()
                        (this@LoginActivity).SetAutUser(
                            "username",
                            _emailText!!.text.toString()
                        )?.apply()

                        (this@LoginActivity).SetAutUser(
                            "login_url",
                            Jobject.getString("login_url")
                        )?.apply()

                        if (Jobject.has("login_url2"))
                            (this@LoginActivity).SetAutUser(
                                "login_url2",
                                Jobject.getString("login_url2")
                            )?.apply()

                        println("onResponse token1   $token1")
                        if (nameClass == "action_profile") {
                            GlobalStuff.checkGetListAccount = true
                            startActivity(Intent(this@LoginActivity, WebViewActivity::class.java))
                        } else if (nameClass == "action_add_profile_hologate")
                            GlobalStuff.check_add_profile_hologate = true
                        else if (nameClass == "action_free_account")
                            GlobalStuff.checkFreeAccount = true
                        else {
                            GlobalStuff.checkGetListAccount = true
                        }
                        finish()
//                        val Jarray: JSONArray = Jobject.getJSONArray("accounts")
//                        for (i in 0 until Jarray.length()) {
//                            val object22: JSONObject = Jarray.getJSONObject(i)
//
//                            println("onResponse222   $object22")
//                            println("onResponse333  "+object22.getString("name"))
//                        }
                    } catch (e: Exception) {

                        println("onResponse Exception  $e")
                        //Toast.makeText(requireActivity(), "aaaaa", Toast.LENGTH_LONG).show()
                        this@LoginActivity!!.runOnUiThread {
                            dialog?.dismiss()
                            Toast.makeText(
                                this@LoginActivity,
                                "The server encountered a problem",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }


//                            for ((name, value) in response.headers) {
//                                println("$name: $value")
//                            }
//                            println("onResponse   "+response.body!!.string())

                }

            }
        })
    }

    fun validate(): Boolean {
        var valid = true

        val email = _emailText!!.text.toString()
        val password = _passwordText!!.text.toString()

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText!!.error = "enter a valid email address"
            valid = false
        } else {
            _emailText!!.error = null
        }

        if (password.isEmpty() || password.length < 4 || password.length > 30) {
            _passwordText!!.error = "between 4 and 10 alphanumeric characters"
            valid = false
        } else {
            _passwordText!!.error = null
        }

        return valid
    }

    companion object {
        private val TAG = "LoginActivity"
        private val REQUEST_SIGNUP = 0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_import_file) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}