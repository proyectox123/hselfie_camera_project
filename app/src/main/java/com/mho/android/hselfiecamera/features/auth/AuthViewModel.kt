package com.mho.android.hselfiecamera.features.auth

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mho.android.hselfiecamera.features.auth.AuthViewModel.AuthNavigation.*
import com.mho.android.hselfiecamera.usecases.LogInHMSUseCase
import com.mho.android.hselfiecamera.utils.Event

class AuthViewModel(
    private val logInHMSUseCase: LogInHMSUseCase
): ViewModel() {

    private val _events = MutableLiveData<Event<AuthNavigation>>()
    val events: LiveData<Event<AuthNavigation>> get() = _events

    fun onValidateActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_CODE_LOGIN_SERVICE_ID){
            logInHMSUseCase.invoke(
                resultCode,
                data,
                successListener = { _events.value = Event(NavigateToMain) },
                failedListener = { _events.value = Event(ShowLogInFailed) },
                cancelListener = { _events.value = Event(ShowCancelledLogInMessage) }
            )
        }
    }

    sealed class AuthNavigation {
        object NavigateToMain: AuthNavigation()
        object ShowCancelledLogInMessage: AuthNavigation()
        object ShowLogInFailed: AuthNavigation()
    }

    companion object {

        internal const val REQUEST_CODE_LOGIN_SERVICE_ID = 1000
    }
}
