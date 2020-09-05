package com.mho.android.hselfiecamera.features.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService
import com.mho.android.hselfiecamera.features.main.MainViewModel.MainNavigation.NavigateToAuth
import com.mho.android.hselfiecamera.features.main.MainViewModel.MainNavigation.ShowLogOutFailed
import com.mho.android.hselfiecamera.utils.Event

class MainViewModel(
    private val authService: HuaweiIdAuthService
): ViewModel() {

    private val _events = MutableLiveData<Event<MainNavigation>>()
    val events: LiveData<Event<MainNavigation>> get() = _events

    fun onLogoutHuaweiId(){
        authService.signOut().run {
            addOnSuccessListener {
                _events.value = Event(NavigateToAuth)
            }.addOnFailureListener {
                _events.value = Event(ShowLogOutFailed)
            }
        }
    }

    sealed class MainNavigation {
        object NavigateToAuth: MainNavigation()
        object ShowLogOutFailed: MainNavigation()
    }
}
