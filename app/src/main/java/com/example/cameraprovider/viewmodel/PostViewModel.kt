package com.example.cameraprovider.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.example.cameraprovider.repository.UserRepository
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PostViewModel() : ViewModel(), Observable {

    private val postRepository = PostRepository()
    private val authRepository = UserRepository()

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

    fun iscurrentId(): String {
        return postRepository.iscurrentId()
    }

    val posts: Flow<PagingData<Post>> = postRepository.getPosts().cachedIn(viewModelScope)
    fun likePost(postId: String, icon: String) {
        _likeEvent.value = postId
        viewModelScope.launch(Dispatchers.IO) {
            postRepository.updateLikePost(postId, icon)
        }
    }


    private val _likesDataMap =
        mutableMapOf<String, MutableLiveData<List<Pair<String, List<String>>>>>()

    fun getLikes(postId: String): LiveData<List<Pair<String, List<String>>>> {
        if (!_likesDataMap.contains(postId)) {
            _likesDataMap[postId] = MutableLiveData()
            loadLikes(postId)
        }
        return _likesDataMap[postId]!!
        Log.d("PostViewModel", "Like $postId: ${_likesDataMap[postId]}")
    }

    private fun loadLikes(postId: String) {
        postRepository.getlikepost(postId) { likeList ->
            _likesDataMap[postId]?.value = likeList
        }
    }

    fun getcurrentId(): String {
        return authRepository.getCurrentUid()
    }

    private val _deletePost = MutableLiveData<Boolean>()
    val deletePost: LiveData<Boolean> = _deletePost
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val success = postRepository.deletePost(postId)
            if (success) {
                _deletePost.postValue(true)
            } else {
                _deletePost.postValue(false)
            }
        }
    }


    private val _contentgena = MutableLiveData<String>()
    val contentgena: LiveData<String> = _contentgena
    fun generateContent(imageBitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _contentgena.postValue(postRepository.generateContentFromImage(imageBitmap))
            }catch (e:Exception){
                _contentgena.postValue("Vui lòng kiểm tra kết nối mạng.")
                Log.d("PostViewModel", "generateContent: ${e.message}")
            }
        }
    }

    private val _newPostCount = MutableLiveData<Int>(0)
    val newPostCount: LiveData<Int> = _newPostCount

    init {
        postRepository.listenForNewPosts { newCount ->
            _newPostCount.postValue(newCount)
        }
    }

    fun onPostViewed(postId: String) {
        postRepository.updateViewedBy(postId) { success ->
            if (success) {
                _newPostCount.value = 0
                Log.e("PostViewModel", "Updated viewed by")

            } else {
                Log.e("PostViewModel", "Failed to update viewed by")
            }
        }
    }
    fun clearNewpostsize(){
        _newPostCount.value = 0
    }
    fun clearContentgena(){
        _contentgena.value = ""
    }


    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }
}