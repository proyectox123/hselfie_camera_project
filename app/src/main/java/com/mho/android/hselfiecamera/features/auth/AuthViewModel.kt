package com.mho.android.hselfiecamera.features.auth

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService
import com.mho.android.hselfiecamera.features.auth.AuthViewModel.AuthNavigation.*
import com.mho.android.hselfiecamera.utils.Event

class AuthViewModel(
    private val authService: HuaweiIdAuthService
): ViewModel() {

    private val _events = MutableLiveData<Event<AuthNavigation>>()
    val events: LiveData<Event<AuthNavigation>> get() = _events

    fun onLogInHuaweiIdAuth(){
        _events.value = Event(StartAuthentication(authService, REQUEST_CODE_LOGIN_SERVICE_ID))
    }

    fun onValidateActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_CODE_LOGIN_SERVICE_ID){
            if(resultCode == Activity.RESULT_CANCELED){
                _events.value = Event(ShowCancelledLogInMessage)
            } else if(resultCode == Activity.RESULT_OK){
                val authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data)
                if (authHuaweiIdTask.isSuccessful){
                    _events.value = Event(NavigateToMain)
                } else{
                    _events.value = Event(ShowLogInFailed)
                }
            }
        }
    }

    sealed class AuthNavigation {
        data class StartAuthentication(
            val authService: HuaweiIdAuthService,
            val requestCode: Int
        ) : AuthNavigation()
        object NavigateToMain: AuthNavigation()
        object ShowCancelledLogInMessage: AuthNavigation()
        object ShowLogInFailed: AuthNavigation()
    }

    companion object {

        private const val REQUEST_CODE_LOGIN_SERVICE_ID = 1000
    }
}
