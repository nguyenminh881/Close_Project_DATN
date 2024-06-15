package com.example.cameraprovider.viewmodel

import android.content.Intent
import androidx.databinding.Observable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cameraprovider.model.Friendship
import com.example.cameraprovider.repository.FriendRepository
import com.google.firebase.dynamiclinks.ShortDynamicLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendViewmodel:ViewModel(),Observable{
    private val friendRepository = FriendRepository()
    private val _dynamicLink = MutableLiveData<String>()
    val dynamicLink: LiveData<String> = _dynamicLink

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
//    private val _userId = MutableLiveData<String?>()
//    val userId: LiveData<String?> = _userId
//
//    private val _userName = MutableLiveData<String?>()
//    val userName: LiveData<String?> = _userName

   private val _friendshipResult =  MutableLiveData<Friendship?>()
    val friendshipResult: LiveData<Friendship?> = _friendshipResult

    fun createDynamicLink() {
        viewModelScope.launch{
            try {
                val shortDynamicLink = friendRepository.createdynamicLink()
                _dynamicLink.postValue(shortDynamicLink.toString())
            } catch (e: Exception) {
                _errorMessage.postValue(e.message)
            }
        }
    }
//    fun handleDynamicLink(intent: Intent) {
//        viewModelScope.launch {
//            val (userId, userName) = friendRepository.handleDynamicLink(intent)
//            _userId.value = userId
//            _userName.value = userName
//        }
//    }
fun handleFriendRequest(intent: Intent) {
    viewModelScope.launch {
        try {

            val result = friendRepository.handleFriendRequest(intent)
            _friendshipResult.postValue(result)
        } catch (e: Exception) {
            _errorMessage.postValue(e.message)
        }
    }
}



    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

}