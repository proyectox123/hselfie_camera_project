package com.mho.android.hselfiecamera.push

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import kotlin.concurrent.thread

class HuaweiPushService: HmsMessageService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received!")
        remoteMessage?.let {
            Log.d(TAG, " - Data ${it.data}")
        }
    }

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        Log.d(TAG, "Huawei push token: $token")
    }

    companion object {

        private const val TAG = "HuaweiPushService"
    }
}

class GetTokenAction(){

    private val handler: Handler = Handler(Looper.getMainLooper())

    fun getToken(context: Context, callback: (String) -> Unit){
        thread {
            try {
                val appID = AGConnectServicesConfig.fromContext(context).getString("client/app_id")
                val token = HmsInstanceId.getInstance(context).getToken(appID, "HCM")
                handler.post { callback(token) }
            }catch (e: Exception) {
                Log.e("Error: ", e.toString())
            }
        }
    }
}