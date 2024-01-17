package io.nekohasekai.sagernet.ui

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.view.KeyEvent
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.preference.PreferenceDataStore
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import io.nekohasekai.sagernet.*
import io.nekohasekai.sagernet.aidl.ISagerNetService
import io.nekohasekai.sagernet.aidl.SpeedDisplayData
import io.nekohasekai.sagernet.aidl.TrafficData
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.bg.SagerConnection
import io.nekohasekai.sagernet.database.*
import io.nekohasekai.sagernet.database.preference.OnPreferenceDataStoreChangeListener
import io.nekohasekai.sagernet.databinding.LayoutMainBinding
import io.nekohasekai.sagernet.fmt.AbstractBean
import io.nekohasekai.sagernet.fmt.KryoConverters
import io.nekohasekai.sagernet.fmt.PluginEntry
import io.nekohasekai.sagernet.fmt.ssh.SSHBean
import io.nekohasekai.sagernet.group.GroupInterfaceAdapter
import io.nekohasekai.sagernet.group.GroupUpdater
import io.nekohasekai.sagernet.ktx.*
import io.nekohasekai.sagernet.widget.ListHolderListener
import moe.matsuri.nb4a.utils.Util
import org.json.JSONObject
import java.util.*

class MainActivity : ThemedActivity(),
    SagerConnection.Callback,
    OnPreferenceDataStoreChangeListener,
    NavigationView.OnNavigationItemSelectedListener {
    lateinit var locale: Locale
    var selectIndexNavigation: Int = 0
    var context: Context? = null
    var resources1: Resources? = null
    var dialogLoading: AlertDialog? = null

    lateinit var binding: LayoutMainBinding
    lateinit var navigation: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lang =  DataStore.languageMode
        if (lang == "") {
        } else
            MyContextWrapper.wrap(this, lang)

        var url = GetAutUser("base_url")
        if (url != null) {
            if (url.isNotEmpty()) {
                println("notif**** base_url 22222  " + GetAutUser("base_url"))
                GlobalStuff.base_url = url
            }

        }
        window?.apply {
            statusBarColor = Color.TRANSPARENT
        }

        binding = LayoutMainBinding.inflate(layoutInflater)
        binding.fab.initProgress(binding.fabProgress)
        if (themeResId !in intArrayOf(
                R.style.Theme_SagerNet_Black
            )
        ) {
            navigation = binding.navView
            binding.drawerLayout.removeView(binding.navViewBlack)
        } else {
            navigation = binding.navViewBlack
            binding.drawerLayout.removeView(binding.navView)
        }
        navigation.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            displayFragmentWithId(R.id.nav_configuration)
        }
        onBackPressedDispatcher.addCallback {
            if (supportFragmentManager.findFragmentById(R.id.fragment_holder) is ConfigurationFragment) {
                moveTaskToBack(true)
            } else {
                displayFragmentWithId(R.id.nav_configuration)
            }
        }

        binding.fab.setOnClickListener {
            if (DataStore.serviceState.canStop) SagerNet.stopService() else connect.launch(
                null
            )
            Handler().postDelayed({
                if (DataStore.serviceState.connected) binding.stats.testConnection()
            }, 3000)
        }
        binding.stats.setOnClickListener { if (DataStore.serviceState.connected) binding.stats.testConnection() }

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinator, ListHolderListener)
        changeState(BaseService.State.Idle)
        connection.connect(this, this)
        DataStore.configurationStore.registerChangeListener(this)
        GroupManager.userInterface = GroupInterfaceAdapter(this)

        if (intent?.action == Intent.ACTION_VIEW) {
            onNewIntent(intent)
        }

        refreshNavMenu(DataStore.enableClashAPI)
        dialogLoading =
            AlertDialog.Builder(this).setView(R.layout.progress).setCancelable(false).create()

        // sdk 33 notification
        if (Build.VERSION.SDK_INT >= 33) {
            val checkPermission =
                ContextCompat.checkSelfPermission(this@MainActivity, POST_NOTIFICATIONS)
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                //动态申请
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(POST_NOTIFICATIONS), 0
                )
            }
            else {
                getTokenFirebase()
            }
        }
        else {
            getTokenFirebase()
        }
        val token = GetAutUser("token")
        println("onResponse token1   $token  " + token.equals(null))

//        if (token.equals("")) {
//            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
//        }

        // println("onResponse isInternetAvailable  " + isInternetAvailable(this))
    }

    fun refreshNavMenu(clashApi: Boolean) {
        if (::navigation.isInitialized) {
//            navigation.menu.findItem(R.id.nav_traffic)?.isVisible = clashApi
//            navigation.menu.findItem(R.id.nav_tuiguang)?.isVisible = !isPlay
        }
    }
    fun setLocale(lang: String?) {

//        context = LocaleHelper.setLocale(this, "lang")
//        resources1 = context!!.getResources()

        val myLocale = Locale(lang)
        val res = resources
        val dm = res.displayMetrics
        val conf: Configuration = res.configuration
        conf.locale = myLocale
        res.updateConfiguration(conf, dm)
        //  val refresh = Intent(this, LoginActivity::class.java)
        // finish()
        // startActivity(refresh)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val uri = intent.data ?: return

        runOnDefaultDispatcher {
            if ((uri.scheme == "sn"||uri.scheme == "hologate" ) && uri.host == "subscription" || uri.scheme == "clash") {
                importSubscription(uri)
            } else {
                importProfile(uri)
            }
        }
    }

    fun urlTest(): Int {
        if (!DataStore.serviceState.connected || connection.service == null) {
            error("not started")
        }
        return connection.service!!.urlTest()
    }

    suspend fun importSubscription(uri: Uri) {
        val group: ProxyGroup

        val url = uri.getQueryParameter("url")
        if (!url.isNullOrBlank()) {
            group = ProxyGroup(type = GroupType.SUBSCRIPTION)
            val subscription = SubscriptionBean()
            group.subscription = subscription

            // cleartext format
            subscription.link = url
            group.name = uri.getQueryParameter("name")
        } else {
            val data = uri.encodedQuery.takeIf { !it.isNullOrBlank() } ?: return
            try {
                group = KryoConverters.deserialize(
                    ProxyGroup().apply { export = true }, Util.zlibDecompress(Util.b64Decode(data))
                ).apply {
                    export = false
                }
            } catch (e: Exception) {
                onMainDispatcher {
                    alert(e.readableMessage).show()
                }
                return
            }
        }

        val name = group.name.takeIf { !it.isNullOrBlank() } ?: group.subscription?.link
        ?: group.subscription?.token
        if (name.isNullOrBlank()) return

        group.name = group.name.takeIf { !it.isNullOrBlank() }
            ?: ("Subscription #" + System.currentTimeMillis())

        onMainDispatcher {

            displayFragmentWithId(R.id.nav_group)

            MaterialAlertDialogBuilder(this@MainActivity).setTitle(R.string.subscription_import)
                .setMessage(getString(R.string.subscription_import_message, name))
                .setPositiveButton(R.string.yes) { _, _ ->
                    runOnDefaultDispatcher {
                        finishImportSubscription(group)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()

        }

    }

    private suspend fun finishImportSubscription(subscription: ProxyGroup) {
        GroupManager.createGroup(subscription)
        GroupUpdater.startUpdate(subscription, true)
    }

    suspend fun importProfile(uri: Uri) {

        ////*******file************/////
        val scheme = uri.scheme
        if (scheme == "content") {
            try {

                /*  val fIn =  contentResolver.openInputStream(uri)
                  val myReader = BufferedReader(InputStreamReader(fIn))
                  var aDataRow : String?
                  var aBuffer = ""
                  while (withContext(Dispatchers.IO) {
                          myReader.readLine()
                      }.also { aDataRow = it } != null) {
                      aBuffer+= aDataRow + "\n";
                  }
                  println("selectedFile Exception **  $aBuffer")

                  //edittext2.setText(aBuffer)
                  myReader.close()*/
                val proxies = mutableListOf<AbstractBean>()
                val entities = ArrayList<AbstractBean>()

                val entitie = SSHBean()
                val fileText = contentResolver.openInputStream(uri)!!.use {
                    it.bufferedReader().readText()
                }
                var obj = JSONObject(fileText)
                val resOutbounds = obj.getJSONArray("outbounds")

                val object22: JSONObject = resOutbounds.getJSONObject(0)

                entitie.authType = 1
                entitie.privateKey = ""
                entitie.publicKey = ""
                entitie.privateKeyPassphrase = ""
                entitie.customConfigJson = ""
                entitie.customOutboundJson = ""
                entitie.password = ChCrypto.aesDecrypt(object22.getString("password"), Key.KEY_HASH)
                entitie.username = ChCrypto.aesDecrypt(object22.getString("user"), Key.KEY_HASH)
                if (object22.has("name"))
                    entitie.name = object22.getString("name")

                entitie.serverAddress = ChCrypto.aesDecrypt(object22.getString("server"),
                    Key.KEY_HASH
                )
                entitie.serverPort = object22.getInt("server_port")
                if (object22.has("expiration_date"))
                    entitie.expireDate = object22.getString("expiration_date")

                //  import(proxies)
                entities.add(entitie)
                if (entities.isEmpty()) onMainDispatcher {
                    snackbar(getString(R.string.no_proxies_found_in_file)).show()
                } else {
//                    runOnUiThread {
//
//                    }
                    val fragment = ConfigurationFragment()
                    // fragment.yourPublicMethod()
                    fragment.import(entities, this)
                }


//                RawUpdater.parseRaw(fileText)?.let { pl -> proxies.addAll(pl) }
////                RawUpdater.parseRaw(aBuffer)?.let { pl -> proxies.addAll(pl) }
//
//                if (proxies.isEmpty()) onMainDispatcher {
//                    snackbar(getString(R.string.no_proxies_found_in_file)).show()
//                } else {
//                    val fragment =ConfigurationFragment()
//                   // fragment.yourPublicMethod()
//                    fragment.import(proxies)}
//                // var obj = JSONObject(aBuffer)
//                runOnUiThread {
////                val clipboard =
////                    baseContext!!.getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
////                val clip = ClipData.newPlainText("Copied Text", aBuffer)
////                clipboard.setPrimaryClip(clip)
////                Toast.makeText(
////                    baseContext,
////                    "Done reading file from sdcard $aBuffer",
////                    Toast.LENGTH_SHORT
////                ).show()
//                }
            } catch (e: java.lang.Exception) {
                println("selectedFile catch Exception  $e")
                runOnUiThread {
                    Toast.makeText(baseContext, e.message, Toast.LENGTH_SHORT).show()
                }
            }
            ////*******file************/////
        }
        else {
        val profile = try {
            parseProxies(uri.toString()).getOrNull(0) ?: error(getString(R.string.no_proxies_found))
        } catch (e: Exception) {
            onMainDispatcher {
                alert(e.readableMessage).show()
            }
            return
        }

        onMainDispatcher {
            MaterialAlertDialogBuilder(this@MainActivity).setTitle(R.string.profile_import)
                .setMessage(getString(R.string.profile_import_message, profile.displayName()))
                .setPositiveButton(R.string.yes) { _, _ ->
                    runOnDefaultDispatcher {
                        finishImportProfile(profile)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        }
    }

    private suspend fun finishImportProfile(profile: AbstractBean) {
        val targetId = DataStore.selectedGroupForImport()

        ProfileManager.createProfile(targetId, profile)

        onMainDispatcher {
            displayFragmentWithId(R.id.nav_configuration)

            snackbar(resources.getQuantityString(R.plurals.added, 1, 1)).show()
        }
    }

    override fun missingPlugin(profileName: String, pluginName: String) {
        val pluginEntity = PluginEntry.find(pluginName)

        // unknown exe or neko plugin
        if (pluginEntity == null) {
            snackbar(getString(R.string.plugin_unknown, pluginName)).show()
            return
        }

        // official exe

        MaterialAlertDialogBuilder(this).setTitle(R.string.missing_plugin)
            .setMessage(
                getString(
                    R.string.profile_requiring_plugin, profileName, pluginEntity.displayName
                )
            )
            .setPositiveButton(R.string.action_download) { _, _ ->
                showDownloadDialog(pluginEntity)
            }
            .setNeutralButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.action_learn_more) { _, _ ->
                launchCustomTab("https://matsuridayo.github.io/m-plugin/")
            }
            .show()
    }

    private fun showDownloadDialog(pluginEntry: PluginEntry) {
        var index = 0
        var playIndex = -1
        var fdroidIndex = -1

        val items = mutableListOf<String>()
        if (pluginEntry.downloadSource.playStore) {
            items.add(getString(R.string.install_from_play_store))
            playIndex = index++
        }
        if (pluginEntry.downloadSource.fdroid) {
            items.add(getString(R.string.install_from_fdroid))
            fdroidIndex = index++
        }

        items.add(getString(R.string.download))
        val downloadIndex = index

        MaterialAlertDialogBuilder(this).setTitle(pluginEntry.name)
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    playIndex -> launchCustomTab("https://play.google.com/store/apps/details?id=${pluginEntry.packageName}")
                    fdroidIndex -> launchCustomTab("https://f-droid.org/packages/${pluginEntry.packageName}/")
                    downloadIndex -> launchCustomTab(pluginEntry.downloadSource.downloadLink)
                }
            }
            .show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.isChecked) binding.drawerLayout.closeDrawers() else {
            return displayFragmentWithId(item.itemId)
        }
        return true
    }


    @SuppressLint("CommitTransaction")
    fun displayFragment(fragment: ToolbarFragment) {
        if (fragment is ConfigurationFragment) {
            binding.stats.allowShow = true
            binding.fab.show()
        } else if (!DataStore.showBottomBar) {
            binding.stats.allowShow = false
            binding.stats.performHide()
            binding.fab.hide()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_holder, fragment)
            .commitAllowingStateLoss()
        binding.drawerLayout.closeDrawers()
    }

    fun displayFragmentWithId(@IdRes id: Int): Boolean {
        when (id) {
            R.id.nav_configuration -> {
                displayFragment(ConfigurationFragment())
            }

            R.id.nav_group -> displayFragment(GroupFragment())
            R.id.nav_route -> displayFragment(RouteFragment())
            R.id.nav_settings -> displayFragment(SettingsFragment())
//            R.id.nav_traffic -> displayFragment(WebviewFragment())
            R.id.nav_tools -> displayFragment(ToolsFragment())
            R.id.nav_logcat -> displayFragment(LogcatFragment())
            R.id.nav_login -> {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))

            }
            R.id.nav_telegram -> {
                launchCustomTab(
                    "https://t.me/hologate6"
                )
                return false
            }
            R.id.nav_exit -> {

                RemoveData("token")?.commit()
                navigation.menu.findItem(id).isChecked = false
                if (selectIndexNavigation == 0) {
                    navigation.menu.findItem(R.id.nav_configuration).isChecked = true
                    navigation.setCheckedItem(R.id.nav_configuration)
                } else {
                    navigation.menu.findItem(selectIndexNavigation).isChecked = true
                    navigation.setCheckedItem(selectIndexNavigation)
                }

                navigation.menu.findItem(id).isVisible = false
                Toast.makeText(
                    this, R.string.description_logout, Toast.LENGTH_SHORT
                ).show()
                binding.drawerLayout.closeDrawers()
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))

                // navigation.menu.findItem(id).isEnabled = false
                // RemoveData("token")?.commit()
                //navigation.menu.findItem(id).isVisible = false
                //   navigation.menu.findItem(id).isChecked = false
            }
//            R.id.nav_faq -> {
//                launchCustomTab("https://matsuridayo.github.io/")
//                return false
//            }

//            R.id.nav_about -> displayFragment(AboutFragment())
//            R.id.nav_tuiguang -> {
//                launchCustomTab("https://matsuricom.pages.dev/")
//                return false
//            }

            else -> return false
        }
        //navigation.menu.findItem(id).isChecked = true
        selectIndexNavigation = id;
        if (id != R.id.nav_exit)
            navigation.menu.findItem(id).isChecked = true
        return true
    }

    private fun changeState(
        state: BaseService.State,
        msg: String? = null,
        animate: Boolean = false,
    ) {
        DataStore.serviceState = state

        binding.fab.changeState(state, DataStore.serviceState, animate)
        binding.stats.changeState(state)
        if (msg != null) snackbar(getString(R.string.vpn_error, msg)).show()

        when (state) {
            BaseService.State.Stopped -> {
                runOnDefaultDispatcher {
                    // refresh view
                    ProfileManager.postUpdate(DataStore.currentProfile)

                    //refresh recycle
                    println("ahmad@ refresh recycle 2")
                    val result = SagerDatabase.proxyDao.getByGroup(DataStore.currentGroupId())
                    ProfileManager.updateProfile(result)
                    //refresh recycle
                }
            }

            else -> {}
        }
    }

    override fun snackbarInternal(text: CharSequence): Snackbar {
        return Snackbar.make(binding.coordinator, text, Snackbar.LENGTH_LONG).apply {
            if (binding.fab.isShown) {
                anchorView = binding.fab
            }
            // TODO
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state, msg, true)
    }

    val connection = SagerConnection(SagerConnection.CONNECTION_ID_MAIN_ACTIVITY_FOREGROUND, true)
    override fun onServiceConnected(service: ISagerNetService) = changeState(
        try {
            BaseService.State.values()[service.state]
        } catch (_: RemoteException) {
            BaseService.State.Idle
        }
    )

    override fun onServiceDisconnected() = changeState(BaseService.State.Idle)
    override fun onBinderDied() {
        connection.disconnect(this)
        connection.connect(this, this)
    }

    private val connect = registerForActivityResult(VpnRequestActivity.StartService()) {
        if (it) snackbar(R.string.vpn_permission_denied).show()
    }

    // may NOT called when app is in background
    // ONLY do UI update here, write DB in bg process
    override fun cbSpeedUpdate(stats: SpeedDisplayData) {
        binding.stats.updateSpeed(stats.txRateProxy, stats.rxRateProxy)
    }

    override fun cbTrafficUpdate(data: TrafficData) {
        runOnDefaultDispatcher {
            ProfileManager.postUpdate(data)
        }
    }

    override fun cbSelectorUpdate(id: Long) {
        val old = DataStore.selectedProxy
        DataStore.selectedProxy = id
        DataStore.currentProfile = id
        runOnDefaultDispatcher {
            ProfileManager.postUpdate(old, true)
            ProfileManager.postUpdate(id, true)
        }
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.SERVICE_MODE -> onBinderDied()
            Key.PROXY_APPS, Key.BYPASS_MODE, Key.INDIVIDUAL -> {
                if (DataStore.serviceState.canStop) {
                    snackbar(getString(R.string.need_reload)).setAction(R.string.apply) {
                        SagerNet.reloadService()
                    }.show()
                }
            }
        }
    }

    override fun onStart() {
        connection.updateConnectionId(SagerConnection.CONNECTION_ID_MAIN_ACTIVITY_FOREGROUND)
        super.onStart()
    }

    override fun onStop() {
        connection.updateConnectionId(SagerConnection.CONNECTION_ID_MAIN_ACTIVITY_BACKGROUND)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        GroupManager.userInterface = null
        DataStore.configurationStore.unregisterChangeListener(this)
        connection.disconnect(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (super.onKeyDown(keyCode, event)) return true
                binding.drawerLayout.open()
                navigation.requestFocus()
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (binding.drawerLayout.isOpen) {
                    binding.drawerLayout.close()
                    return true
                }
            }
        }

        if (super.onKeyDown(keyCode, event)) return true
        if (binding.drawerLayout.isOpen) return false

        val fragment =
            supportFragmentManager.findFragmentById(R.id.fragment_holder) as? ToolbarFragment
        return fragment != null && fragment.onKeyDown(keyCode, event)
    }
    @SuppressLint("SimpleDateFormat")
    override fun onResume() {
        super.onResume()
        //println("onResponse Exception  $e")

        // navigation.menu.findItem(R.id.nav_exit).isVisible =

        val token = (this@MainActivity).GetAutUser("token").equals("")
        val username = (this@MainActivity).GetAutUser("username")
        val check_username = username.equals("")
        navigation.menu.findItem(R.id.nav_exit).isVisible = !token
//        navigation.menu.findItem(R.id.nav_profile).isVisible = !token
        navigation.menu.findItem(R.id.nav_login).isVisible = token
        navigation.menu.findItem(R.id.nav_profile1).isVisible =! check_username
        if (!check_username)
            navigation.menu.findItem(R.id.nav_profile1).title = username

//        val sdf = SimpleDateFormat("yyyy-MM-dd")
//        val now = System.currentTimeMillis()
//        val expire = Libcore.getExpireTime() * 1000
//        val dateExpire = Date(expire)
//        val build = Libcore.getBuildTime() * 1000
//        val dateBuild = Date(build)
//
//        var text: String? = null
//        if (now > expire) {
//            text = getString(
//                R.string.please_update_force, sdf.format(dateBuild), sdf.format(dateExpire)
//            )
//        } else if (now > (expire - 2592000000)) {
//            // 30 days remind :D
//            text = getString(
//                R.string.please_update, sdf.format(dateBuild), sdf.format(dateExpire)
//            )
//        }
//
//
//        if (text != null) {
//            MaterialAlertDialogBuilder(this@MainActivity).setTitle(R.string.insecure)
//                .setMessage(text)
//                .setPositiveButton(R.string.action_download) { _, _ ->
//                    launchCustomTab(
//                        "https://github.com/MatsuriDayo/NekoBoxForAndroid/releases"
//                    )
//                }
//                .setNegativeButton(android.R.string.cancel, null)
//                .setCancelable(false)
//                .show()
//        }
    }

    /*    private fun setLocale(localeName: String) {
            //if (localeName != currentLanguage) {
                locale = Locale(localeName)
                val res = resources
                val dm = res.displayMetrics
                val conf = res.configuration
                conf.locale = locale
                res.updateConfiguration(conf, dm)
                val refresh = Intent(
                    this,
                    MainActivity::class.java
                )
                refresh.putExtra("fa", localeName)
                startActivity(refresh)
    //        } else {
    //            Toast.makeText(
    //                this@MainActivity, "Language, , already, , selected)!", Toast.LENGTH_SHORT).show();
    //        }
        }*/

//    private fun updateResources(context: Context, language: String): Boolean {
//        val locale = Locale(language)
//        Locale.setDefault(locale)
//        val resources: Resources = context.getResources()
//        val configuration: Configuration = resources.getConfiguration()
//        configuration.locale = locale
//        resources.updateConfiguration(configuration, resources.getDisplayMetrics())
//        return true
//    }

//    override fun attachBaseContext(newBase: Context?) {
//        super.attachBaseContext(MyContextWrapper.wrap(newBase, lang_HoloGate))
//    }


    override fun onBackPressed() {

        val myFragment: ConfigurationFragment? =
            supportFragmentManager.findFragmentByTag("ConfigurationFragment") as ConfigurationFragment?

        // ConfigurationFragment()
        if (myFragment != null && myFragment.isVisible()) {
            finish()
            // add your code here
        } else {
            navigation.menu.findItem(R.id.nav_configuration).isChecked = true
            navigation.setCheckedItem(R.id.nav_configuration)
            displayFragment(ConfigurationFragment())
        }

        /*   supportFragmentManager.
           if (supportFragmentManager.backStackEntryCount > 0) {
              // displayFragment(ConfigurationFragment())
               supportFragmentManager.popBackStackImmediate();
           }
           else{
               finish()
           }*/
    }

    fun getTokenFirebase() {
//        val i = intent
//        val extras = i.extras
//        if (extras != null) {
//            for (key in extras.keySet()) {
//                val value = extras[key]
//               // Log.d(Application.APPTAG, "Extras received at onCreate:  Key: $key Value: $value")
//            }
//            val title = extras.getString("title")
//            val message = extras.getString("body")
//            if (message != null && message.length > 0) {
//                intent.removeExtra("body")
//              //  showNotificationInADialog(title, message)
//            }
//        }


        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                println("getTokenFirebase  " + task.exception)

                //Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            GlobalStuff.token_fb = token + ""
            // id_fb= FirebaseInstallations.getInstance().id.toString()
            //    println("TOKEN_FB  " + token)
            //  FirebaseAnalytics.getInstance(this).logEvent("main_activity_ready",null)

//            FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    println("TOKEN_FB  id***" + task.result)
//
//                } else {
//                    println("Installations "+ "Unable to get Installation ID")
//                }
//            }

            //  println("TOKEN_FB  id" + id_fb)
            // println("TOKEN_FB  token" + FirebaseInstallations.getInstance().getToken(true).toString())
            SetAutUser("TOKEN_FB", token)?.apply()
            // Log and toast
            //val msg = getString(R.string.msg_token_fmt, token)
            //  Log.d(TAG, msg)
            //Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        })
    }

    fun exportShare(link: String) {
        val success = SagerNet.trySetPrimaryClip(link)
        snackbar(if (success) R.string.action_export_msg else R.string.action_export_err)
            .show()

        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, link)
        intent.type = "text/plain"
        startActivity(Intent.createChooser(intent, "HoloGate link Share To:"))
    }


    fun isInternetAvailable(context: Context): String {
        var result = "false"
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return "null"
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return "null"
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "false"
            }
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> "WIFI"
                        ConnectivityManager.TYPE_MOBILE -> "CELLULAR"
                        ConnectivityManager.TYPE_ETHERNET -> "ETHERNET"
                        else -> "null"
                    }

                }
            }
        }

        return result
    }
}
