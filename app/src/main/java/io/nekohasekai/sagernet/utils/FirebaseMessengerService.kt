package io.nekohasekai.sagernet.utils


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ui.MainActivity
import io.nekohasekai.sagernet.ui.ThemedActivity.GlobalStuff.base_url
import org.json.JSONObject


class FirebaseMessengerService : FirebaseMessagingService() {

    @SuppressLint("CommitPrefEdits")
    override fun handleIntent(intent: Intent?) {
        super.handleIntent(intent)

        if (intent != null) {

            val i = intent
            val extras = i.extras
            if (extras != null) {
//                for (key in extras.keySet()) {
//                    val value = extras[key]
//                    println("notif**** handleIntent  value $key  " + value)
//                    // Log.d(Application.APPTAG, "Extras received at onCreate:  Key: $key Value: $value")
//                }
//                val title = extras.getString("gcm.notification.title")
//                println("notif**** handleIntent  title   " + title)
//                val message = extras.getString("gcm.notification.body")
//                println("notif**** handleIntent  message   " + message)

                //val link_url = extras.getString("link_url")
                val link_url = extras.getString("link_url")
//                println("notif**** handleIntent  link_url  " + link_url)
                if (link_url != null) {
                    // SetAutUser("TOKEN_FB",token)
                    val editor = getSharedPreferences("Wedding", MODE_PRIVATE).edit()
                    editor.putString("base_url", link_url)
                    editor.apply()
                    base_url=link_url
//                    println("notif**** handleIntent  link_url2" + link_url)
                }

                val link_dns = extras.getString("link_dns")
//                println("notif**** handleIntent  link_url  " + link_url)
                if (link_dns != null) {
                    DataStore.remoteDns = link_dns
                }

//                if (message != null && message.isNotEmpty()) {
//                    intent.removeExtra("body")
//                    //  showNotificationInADialog(title, message)
//                }
            }
            // println("notif**** handleIntent  " + intent.data)
        }
        // you can get ur data here
        //intent.getExtras().get("your_data_key")
    }

    override fun onNewToken(token: String) {

        super.onNewToken(token)
       // println("notif**** onReceive  $token  ")
        //   Log.i("William TOKEN_FB", token)
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onMessageReceived(message: RemoteMessage) {
//        println("notif**** onMessageReceived111:   ")
//        if (message.data.isNotEmpty()) {
//
//            val jsonData: String = message.data.toString()
//            val Jobject = JSONObject(jsonData)
//
//            if (Jobject.has("url_link"))
//                println("notif**** Message data payload: ${message.data}" + Jobject.getString("url_link"))
//        }

        if (message.notification != null) {


            val rm: RemoteMessage.Notification = message.notification!!
          //  println("notif**** onMessageReceived:   ")
            pushNotification(rm.title, rm.body)
        }
    }

    private fun pushNotification(title: String?, body: String?) {
       // println("notif**** pushNotification: $title $body ")

        val nm: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelID = "push_notification"

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelID)
        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Dream Wedding") //     .setPriority(Notification.PRIORITY_MAX)
            .setContentTitle(title)
            .setContentText(body)
            .setContentInfo("Info")
        nm.notify( /*notification id*/1, notificationBuilder.build())
    }

}