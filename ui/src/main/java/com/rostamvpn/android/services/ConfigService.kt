package com.rostamvpn.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.rostamvpn.android.activity.MainActivity
import com.rostamvpn.android.model.ConfigData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ConfigService : Service() {
    private val binder = LocalBinder()

    companion object {
        private const val CHANNEL_ID = "ConfigServiceChannel"
        private const val NOTIFICATION_ID = 1

        private const val FILENAME = "data.json"
        private const val TAG = "ConfigService"
    }

    private lateinit var configData: MutableLiveData<ConfigData?>

    private val oneHourInMillis = 60 * 60 * 1000

    override fun onCreate() {
        super.onCreate()

        configData = MutableLiveData()
        configData.postValue(loadConfigData())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "User Data Check Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for User Data Check Service"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()

        saveConfigData()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): ConfigService = this@ConfigService
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("User Data Check Service")
            .setContentText("Service is running")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun getFirstRun(): Boolean {
        try {
            configData.value?.let {
                return it.firstRun
            }
        } catch (err: Exception) {
            err.message?.let {
                Log.e(TAG, it)
            }
        }

        return true
    }

    fun hasValidConfig(): Boolean {
        try {
            configData.value?.let {
                return it.tunnelConfig != null
                        && it.tunnelConfigTime != null
                        && (System.currentTimeMillis() - it.tunnelConfigTime!! < oneHourInMillis)
            }
        } catch (err: Exception) {
            err.message?.let {
                Log.e(TAG, it)
            }
        }

        return false
    }

    fun hasNonNullConfig(): Boolean {
        try {
            configData.value?.tunnelConfig != null
        } catch (err: Exception) {
            err.message?.let {
                Log.e(TAG, it)
            }
        }

        return false
    }

    fun setNewConfig(configString: String) {
        try {
            configData.value.let {
                if (it == null) {
                    configData.postValue(ConfigData(firstRun = false, tunnelConfig = configString, tunnelConfigTime = System.currentTimeMillis()))
                } else {
                    it.tunnelConfig = configString
                    it.tunnelConfigTime = System.currentTimeMillis()
                }
            }

            saveConfigData()
        } catch (err: Exception) {
            err.message?.let {
                Log.e(TAG, it)
            }
        }
    }

    fun getConfigString(): String? {
        return configData.value?.tunnelConfig
    }

    private fun loadConfigData(): ConfigData? {
        try {
            val file = File(this.filesDir, FILENAME)
            if (!file.exists()) {
                return null
            }

            val jsonString = readJsonFromFile(this)
            Log.d(TAG, "JSON Data: $jsonString")
            return Json.decodeFromString(jsonString)
        } catch (err: Exception) {
            err.message?.let {
                Log.e(TAG, it)
            }
        }

        val defaultConfig = ConfigData(false, null, null)
        configData.postValue(defaultConfig)
        return defaultConfig
    }

    private fun saveConfigData() {
        configData.let {
            val jsonString = Json.encodeToString(it)
            Log.d(TAG, "JSON Data: $jsonString")
            saveJsonToFile(this, jsonString)
        }
    }

    private fun readJsonFromFile(context: Context): String {
        val sb = StringBuilder()
        var fis: FileInputStream? = null
        var isr: InputStreamReader? = null
        var br: BufferedReader? = null

        try {
            fis = context.openFileInput(FILENAME)
            isr = InputStreamReader(fis)
            br = BufferedReader(isr)
            var line: String?

            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                br?.close()
                isr?.close()
                fis?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return sb.toString()
    }

    private fun saveJsonToFile(context: Context, jsonData: String) {
        var fos: FileOutputStream? = null
        var osw: OutputStreamWriter? = null

        try {
            fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE)
            osw = OutputStreamWriter(fos)
            osw.write(jsonData)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                osw?.close()
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}