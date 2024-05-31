package com.example.cameraprovider.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cameraprovider.repository.UserRepository

class AuthViewModelFactory(private val repository: UserRepository, private val context:Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(AuthViewModel::class.java)){
            return AuthViewModel(repository,context) as T
        }
        throw IllegalArgumentException("Unknow view model")
    }
}