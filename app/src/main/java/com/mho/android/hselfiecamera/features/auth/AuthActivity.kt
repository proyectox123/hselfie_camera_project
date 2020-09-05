package com.mho.android.hselfiecamera.features.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService
import com.mho.android.hselfiecamera.R
import com.mho.android.hselfiecamera.features.auth.AuthViewModel.AuthNavigation
import com.mho.android.hselfiecamera.features.auth.AuthViewModel.Companion.REQUEST_CODE_LOGIN_SERVICE_ID
import com.mho.android.hselfiecamera.features.main.MainActivity
import com.mho.android.hselfiecamera.usecases.LogInHMSUseCase
import com.mho.android.hselfiecamera.utils.getViewModel
import com.mho.android.hselfiecamera.utils.showLongToast
import com.mho.android.hselfiecamera.utils.startActivity
import kotlinx.android.synthetic.main.activity_auth.*

class AuthActivity : AppCompatActivity() {

    private val authParams: HuaweiIdAuthParams by lazy {
        HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .setIdToken()
            .setEmail()
            .setAccessToken()
            .setProfile()
            .setId()
            .createParams()
    }

    private val authService: HuaweiIdAuthService by lazy {
        HuaweiIdAuthManager.getService(this, authParams)
    }

    private val logInHMSUseCase: LogInHMSUseCase by lazy {
        LogInHMSUseCase()
    }

    private val authViewModel: AuthViewModel by lazy {
        getViewModel { AuthViewModel(logInHMSUseCase) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        btnLogin.setOnClickListener {
            startActivityForResult(authService.signInIntent, REQUEST_CODE_LOGIN_SERVICE_ID)
        }

        authViewModel.events.observe(this, Observer { event ->
            event.getContentIfNotHandled()?.let { validateNavigation(it) }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        authViewModel.onValidateActivityResult(requestCode, resultCode, data)
    }

    private fun validateNavigation(navigation: AuthNavigation) {
        when(navigation){
            AuthNavigation.NavigateToMain -> {
                startActivity<MainActivity>{ }
                finish()
            }
            AuthNavigation.ShowCancelledLogInMessage -> {
                showLongToast(R.string.error_log_in_cancelled)
            }
            AuthNavigation.ShowLogInFailed -> {
                showLongToast(R.string.error_log_in_failed)
            }
        }
    }
}
