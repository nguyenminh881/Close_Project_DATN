package com.example.cameraprovider.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cameraprovider.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import android.view.View;
import com.example.cameraprovider.ProfileActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class AuthViewModel(private val userRepository: UserRepository, private val context: Context) : ViewModel(), Observable {

    @Bindable
    var email = MutableLiveData<String?>()

    var emailError = MutableLiveData<String?>()
    @Bindable
    var password = MutableLiveData<String>()

    var passwordError = MutableLiveData<String?>()

    private val _signupResult = MutableLiveData<Boolean>()
    val signupResult: LiveData<Boolean> get() = _signupResult

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> get() = _loginResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun signUp() {
        val emailvalue = email.value ?: ""
        val passwordvalue = password.value ?: ""

        val emailRegex = "^[A-Za-z](.*)(@gmail\\.com)$"
        val pattern = Pattern.compile(emailRegex)
        if (emailvalue.isEmpty()) {
            emailError.value = "Vui lòng điền email"
        } else {
            emailError.value = null
        }
        if (!pattern.matcher(emailvalue).matches()) {
            emailError.value = "Gmail không hợp lệ"
        } else {
            emailError.value = null
        }

        if (passwordvalue.isEmpty()) {
            passwordError.value = "Vui lòng điền mật khẩu"
        } else {
            passwordError.value = null
        }
        if (passwordvalue.length < 6) {
            passwordError.value = "Mật khẩu phải có 6 kí tự trở lên"
        } else {
            passwordError.value = null
        }

        _loading.value = true
        Log.d("TAGY", "Loading set to true, starting sign up process")

        userRepository.signUp(emailvalue, passwordvalue) {
            if (it) {
                Log.d("TAGY", "đã vào")
                val i = Intent(context, ProfileActivity::class.java)
                context.startActivity(i)
                _loading.value = false
            }else{
                Log.d("TAGY", "chịu")
            }
        }
    }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }
}