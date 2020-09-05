package com.mho.android.hselfiecamera.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.mho.android.hselfiecamera.R
import com.mho.android.hselfiecamera.auth.AuthActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLogout.setOnClickListener {
            logoutHuaweiId()
        }
    }

    override fun onBackPressed() {

    }

    private fun logoutHuaweiId(){
        val authParams = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
            .createParams()
        val authManager = HuaweiIdAuthManager.getService(this, authParams)
        val logoutTask = authManager.signOut()
        logoutTask.addOnSuccessListener {
            val intent = Intent(this@MainActivity, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }
        logoutTask.addOnFailureListener {
            Toast.makeText(this@MainActivity, "Log out fallido!", Toast.LENGTH_LONG).show()
        }
    }
}
