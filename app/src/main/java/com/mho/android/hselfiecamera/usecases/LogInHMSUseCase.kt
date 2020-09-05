package com.mho.android.hselfiecamera.usecases

import android.app.Activity
import android.content.Intent
import com.huawei.hms.support.hwid.HuaweiIdAuthManager

class LogInHMSUseCase {

    fun invoke(resultCode: Int, data: Intent?, successListener: () -> Unit,
               failedListener: () -> Unit, cancelListener: () -> Unit){
        if(resultCode == Activity.RESULT_CANCELED){
            cancelListener()
        } else if(resultCode == Activity.RESULT_OK){
            val authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data)
            if (authHuaweiIdTask.isSuccessful){
                successListener()
            } else{
                failedListener()
            }
        }
    }
}
