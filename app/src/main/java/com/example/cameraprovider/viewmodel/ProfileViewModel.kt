package com.example.cameraprovider.viewmodel

import androidx.databinding.Observable
import androidx.lifecycle.ViewModel
import com.example.cameraprovider.repository.UserRepository

class ProfileViewModel:ViewModel(),Observable {

    private val userRepository:UserRepository=UserRepository()

















    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        TODO("Not yet implemented")
    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        TODO("Not yet implemented")
    }
}