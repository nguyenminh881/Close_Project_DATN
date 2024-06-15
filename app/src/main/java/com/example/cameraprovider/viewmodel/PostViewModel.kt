package com.example.cameraprovider.viewmodel

import android.net.Uri
import android.util.Log
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.cameraprovider.model.Post
import com.example.cameraprovider.repository.PostRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostViewModel : ViewModel(), Observable {

    private val postRepository = PostRepository()

    @Bindable
    var content = MutableLiveData<String?>()

    private val _likeEvent = MutableLiveData<String>()
    val likeEvent: LiveData<String> get() = _likeEvent


    private val _postResultLiveData = MutableLiveData<PostRepository.PostResult>()
    val postResultLiveData: LiveData<PostRepository.PostResult> get() = _postResultLiveData

    fun addPost(contentUri: Uri, content: String, isImage: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = postRepository.addPost(contentUri, content, isImage)
            _postResultLiveData.postValue(result)
        }
    }

    val posts: Flow<PagingData<Post>> = postRepository.getPosts().cachedIn(viewModelScope)
    fun likePost(postId: String,icon:String) {
        _likeEvent.value=postId
        viewModelScope.launch(Dispatchers.IO) {
            postRepository.updateLikePost(postId,icon)
        }
    }



















    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }
}