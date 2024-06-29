package com.example.cameraprovider.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cameraprovider.model.Message
import com.example.cameraprovider.model.User
import com.example.cameraprovider.repository.MessageRepository
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.protobuf.Internal.BooleanList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log

class MessageViewModel(): ViewModel(), Observable {

    val messageRepository = MessageRepository()

    private lateinit var generativeModel: GenerativeModel
    @Bindable
    var messagesend = MutableLiveData<String?>()

    private val _sendSuccess = MutableLiveData<Boolean>()
    val sendSuccess: LiveData<Boolean> get() = _sendSuccess
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading
    fun sendMessage(postId: String,receiverId: String,imgUrl:String,VoiceUrl:String,content:String,createAt:String,avtUserPost:String) {
        _loading.value =true
        var message = messagesend.value?:""
        viewModelScope.launch(Dispatchers.IO) {
           val result = messageRepository.sendMessage(message,postId,receiverId,imgUrl,VoiceUrl,content,createAt,avtUserPost)
            result.onSuccess { success ->
                if (success) {
                    withContext(Dispatchers.Main) {
                        _loading.value=true
                        _sendSuccess.value= true
                    }

                }
            }.onFailure { exception ->
                withContext(Dispatchers.Main) {
                    _loading.value=true
                    _sendSuccess.value= true
                }
            }
        }
    }


    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> get() = _messages

    fun getMessages(userId: String, friendId: String) {
        viewModelScope.launch {
            messageRepository.getMessages(userId, friendId).collect {
                _messages.value = it
            }
        }
    }



    private val _friendList = MutableLiveData<List<User>>()
    val friendList: LiveData<List<User>> get() = _friendList

    private val _lastMessages = MutableLiveData<Map<String, Message>>()
    val lastMessages: LiveData<Map<String, Message>> get() = _lastMessages

    fun getlistchats() {
        viewModelScope.launch {
            try {
                val (friends, messages) = messageRepository.getFriendsWithLastMessages()
                _friendList.value = friends
                _lastMessages.value = messages
            } catch (e: Exception) {
                Log.e("MessageViewModel", "Error getting friends and last messages", e)
            }
        }
    }
    private val remoteConfig = Firebase.remoteConfig

    init{
        getlistchats()

    }

    fun sendMessageToGemini() {
        var message = messagesend.value?:""
        viewModelScope.launch {
            val result = messageRepository.sendMessageToGemini(message)
            result.onSuccess { message ->
                withContext(Dispatchers.Main) {
                    _loading.value=true
                    _sendSuccess.value= true
                }
            }.onFailure { exception ->
                withContext(Dispatchers.Main) {
                    _loading.value=true
                    _sendSuccess.value= true
                }
                Log.d("MessageViewModel", "Error sending message to Gemini: ${exception.message}")
            }
        }
    }
    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }
}