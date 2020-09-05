package com.mho.android.hselfiecamera

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import kotlinx.android.synthetic.main.activity_auth.*

class AuthActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        btnLogin.setOnClickListener {
            logInHuaweiIdAuth()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE_LOGIN_SERVICE_ID){
            if(resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(this, "Inicio de sesión cancelado", Toast.LENGTH_LONG).show()
            } else if(resultCode == Activity.RESULT_OK){
                val authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data)

                if (authHuaweiIdTask.isSuccessful){
                    Toast.makeText(this, "Autenticación exitosa!!", Toast.LENGTH_LONG).show()
                } else{
                    Toast.makeText(this, "Autenticación Fallida ...", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun logInHuaweiIdAuth(){
        val authParams = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .setIdToken()
            .setEmail()
            .setAccessToken()
            .setProfile()
            .setId()
            .createParams()

        val authManager = HuaweiIdAuthManager.getService(this, authParams)

        startActivityForResult(authManager.signInIntent, REQUEST_CODE_LOGIN_SERVICE_ID)
    }

    companion object {

        private const val REQUEST_CODE_LOGIN_SERVICE_ID = 1000
    }
}
