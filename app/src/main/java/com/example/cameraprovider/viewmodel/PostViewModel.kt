package com.example.cameraprovider.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.Bindable
import androidx.databinding.InverseMethod
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
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun invalidatePagingSource() {
        postRepository.invalidatePagingSource()
    }

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


    @Bindable
    val contentgena = MutableLiveData<String>().apply {
        value = "Nội dung được hiển thị ở đây."
    }

    fun generateContent(imageBitmap: Bitmap) {
        contentgena.value = "Bạn chờ xíu nhé!"
        viewModelScope.launch {
            try {
                contentgena.postValue(postRepository.generateContentFromImage(imageBitmap))
            } catch (e: Exception) {
                contentgena.postValue("Không thể phân tích được nội dung, vui lòng thử lại!.")
                Log.d("PostViewModel", "generateContent: ${e.message}")
            }
        }
    }

    @Bindable
    val contentvoice = MutableLiveData<String>()
    fun generateContentVoice(prompt: String) {
        contentvoice.value = "Bạn chờ xíu nhé!"
        viewModelScope.launch {
            try {
                contentvoice.postValue(postRepository.generateContentFromText(prompt))
            } catch (e: Exception) {
                contentvoice.postValue("Không thể phân tích được nội dung, vui lòng thử lại!.")
                Log.d("PostViewModel", "generateContent: ${e.message}")
            }
        }
    }

    private val _newPostCount = MutableLiveData<Int>(0)
    val newPostCount: LiveData<Int> = _newPostCount

    private val _recognizedText = MutableLiveData<String>()
    val recognizedText: LiveData<String> = _recognizedText

    fun genarateTemp(prompt: String) {
        viewModelScope.launch {
            try {
                _recognizedText.postValue(postRepository.addPunctuation(prompt))
            } catch (e: Exception) {
                _recognizedText.postValue("Không thể phân tích được nội dung, vui lòng thử lại!.")
                Log.d("PostViewModel", "generateContent: ${e.message}")
            }
        }
    }


//    fun onPostViewed(postId: String) {
//        postRepository.updateViewedBy(postId) { success ->
//            if (success) {
//                Log.e("PostViewModel", "Updated viewed by")
//            } else {
//                Log.e("PostViewModel", "Failed to update viewed by")
//            }
//        }
//    }
fun onPostViewed(postId: String) {
    viewModelScope.launch {
        val success = postRepository.updateViewedBy(postId)
        if (success) {
            Log.e("PostViewModel", "Updated viewed by for post: $postId")

        } else {
            Log.e("PostViewModel", "Failed to update viewed by for post: $postId")

        }
    }
}
    init {
        onNewpost()
    }

    private val _isCurrentUserPost = MutableLiveData<Boolean>()
    val isCurrentUserPost: LiveData<Boolean> get() = _isCurrentUserPost

    fun updateIsCurrentUserPost(isCurrentUser: Boolean) {
        _isCurrentUserPost.value = isCurrentUser
    }
    fun onNewpost() {
        viewModelScope.launch {
            withContext(Dispatchers.Main) {
                postRepository.listenForNewPosts() { newCount ->
                    _newPostCount.postValue(newCount)
                }
            }
        }
    }

    fun stopListeningForNewPosts() {
        postRepository.stopListeningForNewPosts()
    }

    fun clearNewpostsize() {
        _newPostCount.postValue(0)
    }

    fun clearContentgena() {
        contentgena.value = ""
    }

    fun clearContentvoice() {
        contentvoice.value = ""
    }

    fun clearContentemp() {
        _recognizedText.value = ""
    }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }


}