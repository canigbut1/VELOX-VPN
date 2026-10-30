package com.velox.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import libXray.DialerController
import libXray.LibXray
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

class VeloxVpnService : VpnService() {
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var tun: ParcelFileDescriptor? = null
    @Volatile private var running = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> worker.execute { stopVpn() }
            ACTION_CONNECT -> {
                val raw = intent.getStringExtra(EXTRA_CONFIG)
                if (raw.isNullOrBlank()) {
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, notification("در حال اتصال…"))
                worker.execute { startVpn(raw, startId) }
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn(raw: String, startId: Int) {
        stopCoreOnly()
        try {
            val converted = invoke("convertShareLinksToXrayJson", JSONObject().put("text", raw))
            val data = converted.optJSONObject("data")
                ?: error(converted.optString("error", "تبدیل کانفیگ ناموفق بود"))
            val outbounds = data.optJSONArray("outbounds")
                ?: error("کانفیگ فاقد سرور خروجی معتبر است")
            require(outbounds.length() > 0) { "هیچ سرور معتبری در کانفیگ پیدا نشد" }

            val vpn = Builder()
                .setSession("VELOX VPN")
                .setMtu(1500)
                .addAddress("10.77.0.1", 30)
                .addRoute("0.0.0.0", 0)
                .addAddress("fd00:77::1", 126)
                .addRoute("::", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("2606:4700:4700::1111")
                .setBlocking(true)
                .establish() ?: error("ساخت تونل VPN توسط اندروید ناموفق بود")
            tun = vpn

            val controller = object : DialerController {
                override fun protectFd(fd: Long): Boolean = protect(fd.toInt())
            }
            LibXray.registerDialerController(controller)
            LibXray.registerListenerController(controller)
            LibXray.setDNS(controller, "1.1.1.1:53")

            val config = JSONObject()
                .put("log", JSONObject().put("loglevel", "warning"))
                .put("dns", JSONObject().put("servers", JSONArray().put("1.1.1.1").put("8.8.8.8")))
                .put("inbounds", JSONArray().put(
                    JSONObject().put("tag", "velox-tun").put("protocol", "tun")
                        .put("settings", JSONObject().put("name", "velox0").put("mtu", 1500))
                ))
                .put("outbounds", outbounds)
                .put("env", JSONObject().put("xray.tun.fd", vpn.fd.toString()))

            val started = invoke("runXray", JSONObject().put("xrayJson", config.toString()))
            if (!started.optBoolean("success")) {
                error(started.optString("error", "موتور Xray شروع نشد"))
            }
            running = true
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, notification("متصل به Xray"))
        } catch (t: Throwable) {
            Log.e(TAG, "VPN startup failed", t)
            stopCoreOnly()
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, notification("خطا: ${t.message ?: "اتصال ناموفق"}"))
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf(startId)
        }
    }

    private fun invoke(method: String, payload: JSONObject): JSONObject {
        val request = JSONObject().put("apiVersion", 3).put("method", method).put("payload", payload)
        return JSONObject(LibXray.invoke(request.toString()))
    }

    private fun stopCoreOnly() {
        runCatching {
            invoke("stopXray", JSONObject())
        }.onFailure { Log.w(TAG, "Xray stop returned an error", it) }
        runCatching { LibXray.resetDNS() }
        running = false
        runCatching { tun?.close() }
        tun = null
    }

    private fun stopVpn() {
        stopCoreOnly()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        worker.execute { stopVpn() }
        super.onRevoke()
    }

    override fun onDestroy() {
        stopCoreOnly()
        worker.shutdown()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "VELOX VPN", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(message: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disconnect = PendingIntent.getService(
            this, 1, Intent(this, VeloxVpnService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
            else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("VELOX VPN").setContentText(message)
            .setContentIntent(open).setOngoing(running || message.contains("اتصال"))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "قطع اتصال", disconnect)
            .build()
    }

    companion object {
        const val ACTION_CONNECT = "com.velox.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.velox.vpn.DISCONNECT"
        const val EXTRA_CONFIG = "config"
        private const val TAG = "VeloxVpnService"
        private const val CHANNEL_ID = "velox_vpn"
        private const val NOTIFICATION_ID = 9001
    }
}
