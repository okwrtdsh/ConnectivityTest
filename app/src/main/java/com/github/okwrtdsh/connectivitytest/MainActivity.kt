package com.github.okwrtdsh.connectivitytest

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var cm: ConnectivityManager
    private var isRegister = false

    companion object {
        private const val TAG: String = "connectivitytest"
        private const val channelId = "com.github.okwrtdsh.connectivitytest"
        private const val channelDescription = "Description"
        private const val channelName = "connectivity_test"
        private const val REQUEST_CODE_PERMISSIONS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        buttonStart.setOnClickListener {
            registerCallback()
        }

        buttonStop.setOnClickListener {
            unregisterCallback()
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_NETWORK_STATE),
            REQUEST_CODE_PERMISSIONS
        )

    }

    private fun registerCallback() {
        if (!isRegister) {
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
                .run {
                    cm.registerNetworkCallback(this, mNetworkCallback)
                }
            isRegister = true
            debug("Start")
        }
    }

    private fun unregisterCallback() {
        if (isRegister) {
            cm.unregisterNetworkCallback(mNetworkCallback)
            isRegister = false
            debug("Stop")

        }
    }

    private val mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) = checkConnection()
        override fun onLost(network: Network?) = checkConnection()
    }

    private fun checkConnection() {
        val activeNetworks = cm.allNetworks.mapNotNull {
            cm.getNetworkCapabilities(it)
        }.filter {
            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        val isConnected = activeNetworks.isNotEmpty()
        val isWiFi = activeNetworks.any { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) }

        notify(TAG, "isConnected: ${isConnected}, isWiFi: ${isWiFi}")
    }

    private fun notify(title: String, text: String) {
        debug("notify(title: $title, text: $text)")
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(channelId) == null)
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = channelDescription
                    lightColor = Color.BLUE
                    setSound(null, null)
                    enableLights(false)
                    enableVibration(false)
                }
                .run {
                    manager.createNotificationChannel(this)
                }

        NotificationCompat
            .Builder(this, channelId)
            .apply {
                setContentTitle(title)
                setContentText(text)
                setSmallIcon(R.drawable.ic_launcher_background)
                setAutoCancel(true)
                setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        0,
                        Intent(applicationContext, MainActivity::class.java)
                            .apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_NEW_TASK
                            },
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                )
            }
            .build()
            .run {
                manager.notify(1, this)
            }
    }

    private fun debug(message: String) {
        Log.d(TAG, message)
    }
}
