package com.mho.android.hselfiecamera.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

inline fun <reified T : Activity> Context.intentFor(body: Intent.() -> Unit): Intent =
    Intent(this, T::class.java).apply(body)

inline fun <reified T : Activity> Context.startActivity(body: Intent.() -> Unit) {
    startActivity(intentFor<T>(body))
}

fun Context.showLongToast(@StringRes resourceId: Int){
    Toast.makeText(this, resourceId, Toast.LENGTH_LONG).show()
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewModel> FragmentActivity.getViewModel(crossinline factory: () -> T): T {

    val vmFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <U : ViewModel> create(modelClass: Class<U>): U = factory() as U
    }

    return ViewModelProvider(this.viewModelStore, vmFactory)[T::class.java]
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T : ViewModel> Fragment.getViewModel(crossinline factory: () -> T): T {

    val vmFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <U : ViewModel> create(modelClass: Class<U>): U = factory() as U
    }

    return ViewModelProvider(this.viewModelStore, vmFactory)[T::class.java]
}
