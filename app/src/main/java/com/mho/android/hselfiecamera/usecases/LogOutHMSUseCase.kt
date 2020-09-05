package com.mho.android.hselfiecamera.usecases

import com.huawei.hms.support.hwid.service.HuaweiIdAuthService

class LogOutHMSUseCase(
    private val authService: HuaweiIdAuthService
) {

    fun invoke(successListener: () -> Unit, failedListener: () -> Unit){
        authService.signOut().run {
            addOnSuccessListener { successListener() }
            addOnFailureListener { failedListener() }
        }
    }
}
