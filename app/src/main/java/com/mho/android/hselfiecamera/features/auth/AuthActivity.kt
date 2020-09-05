package com.mho.android.hselfiecamera.features.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.Observer
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.mho.android.hselfiecamera.R
import com.mho.android.hselfiecamera.features.auth.AuthViewModel.AuthNavigation
import com.mho.android.hselfiecamera.features.auth.AuthViewModel.AuthNavigation.StartAuthentication
import com.mho.android.hselfiecamera.features.main.MainActivity
import com.mho.android.hselfiecamera.utils.*
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

    private val authViewModel: AuthViewModel by lazy {
        getViewModel { AuthViewModel() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        btnLogin.setOnClickListener {
            authViewModel.onLogInHuaweiIdAuth(HuaweiIdAuthManager.getService(this, authParams))
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
            is StartAuthentication -> navigation.run {
                startActivityForResult(authService.signInIntent, requestCode)
            }
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
