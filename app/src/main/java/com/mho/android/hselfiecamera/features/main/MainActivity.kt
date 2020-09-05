package com.mho.android.hselfiecamera.features.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService
import com.mho.android.hselfiecamera.R
import com.mho.android.hselfiecamera.features.auth.AuthActivity
import com.mho.android.hselfiecamera.features.main.MainViewModel.MainNavigation
import com.mho.android.hselfiecamera.usecases.LogOutHMSUseCase
import com.mho.android.hselfiecamera.utils.getViewModel
import com.mho.android.hselfiecamera.utils.showLongToast
import com.mho.android.hselfiecamera.utils.startActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val authParams: HuaweiIdAuthParams by lazy {
        HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .createParams()
    }

    private val authService: HuaweiIdAuthService by lazy {
        HuaweiIdAuthManager.getService(this, authParams)
    }

    private val logOutHMSUseCase: LogOutHMSUseCase by lazy {
        LogOutHMSUseCase(authService)
    }

    private val mainViewModel: MainViewModel by lazy {
        getViewModel { MainViewModel(logOutHMSUseCase) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLogout.setOnClickListener { mainViewModel.onLogoutHuaweiId() }

        mainViewModel.events.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { validateNavigation(it) }
        })
    }

    override fun onBackPressed() { }

    private fun validateNavigation(navigation: MainNavigation) {
        when(navigation){
            MainNavigation.NavigateToAuth -> {
                startActivity<AuthActivity>{  }
                finish()
            }
            MainNavigation.ShowLogOutFailed -> {
                showLongToast(R.string.error_log_out_failed)
            }
        }
    }
}
