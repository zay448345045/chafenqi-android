package com.nltv.chafenqi.view

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nltv.chafenqi.ChafenqiApplication

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            LoginPageViewModel(chafenqiApplication().container.maiListRepository)
        }
    }
}

fun CreationExtras.chafenqiApplication(): ChafenqiApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ChafenqiApplication)