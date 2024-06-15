package com.example.cameraprovider.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
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
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.example.cameraprovider.LoadingActivity
import com.example.cameraprovider.MainActivity
import com.example.cameraprovider.StartAppActivity
import com.example.cameraprovider.model.User
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AuthViewModel(private val userRepository: UserRepository, private val context: Context) :
    ViewModel(), Observable {

    @Bindable
    var email = MutableLiveData<String?>()

    @Bindable
    var nameUser = MutableLiveData<String?>()

    private var imgvAvtUser: ShapeableImageView? = null

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> get() = _emailError


    private val _pwErrorLg = MutableLiveData<String?>()
    val pwErrorLg: LiveData<String?> get() = _pwErrorLg


    @Bindable
    var password = MutableLiveData<String>()

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> get() = _passwordError

    private val _nameError = MutableLiveData<String?>()
    val nameError: LiveData<String?> get() = _nameError

    //
    private val _signupResult = MutableLiveData<Boolean>()
    val signupResult: LiveData<Boolean> get() = _signupResult

    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> get() = _updateResult

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> get() = _loginResult
    private val _getUserResult = MutableLiveData<User?>()
    val getUserResult: LiveData<User?> get() = _getUserResult
    private val _logoutResult = MutableLiveData<Boolean>()
    val logoutResult: LiveData<Boolean> get() = _logoutResult


    //helpertxt
    private val _emailHelperText = MutableLiveData<String?>()
    val emailHelperText: LiveData<String?> get() = _emailHelperText

    private val _passwordHelperText = MutableLiveData<String?>()
    val passwordHelperText: LiveData<String?> get() = _passwordHelperText

    //helpertxt
    private val _emailHelperTextLg = MutableLiveData<String?>()
    val emailHelperTextLg: LiveData<String?> get() = _emailHelperTextLg

    private val _passwordHelperTextLg = MutableLiveData<String?>()
    val passwordHelperTextLg: LiveData<String?> get() = _passwordHelperTextLg


    private val _namehelper = MutableLiveData<String?>()
    val namehelper: LiveData<String?> get() = _namehelper

    private val _avatarImageUrl = MutableLiveData<String?>()
    val avatarImageUrl: MutableLiveData<String?> get() = _avatarImageUrl

    //loading
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    lateinit var imgUri: Uri

//    private lateinit var imgURI:Uri

    //validate email
    private fun isBasicValidEmail(email: String): Boolean {
        val basicEmailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return Pattern.compile(basicEmailRegex).matcher(email).matches()
    }

    private fun isValidEmail(emailValue: String): Boolean {
        val emailRegex =
            "^[A-Za-z0-9._%+-]+@(gmail\\.com|yahoo\\.com|outlook\\.com|.*\\.edu(\\.[a-z]{2,})?)$"
        val pattern = Pattern.compile(emailRegex)
        return pattern.matcher(emailValue).matches()
    }

    //validate pw
    private fun isValidPassword(passwordValue: String): Boolean {
        val pwRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{6,}$"
        val pattern = Pattern.compile(pwRegex)
        return pattern.matcher(passwordValue).matches()
    }

    private fun handleSignUpError(exception: Throwable?): Boolean {
        var isAlreadyEmail = false
        if (exception is FirebaseAuthUserCollisionException) {
            isAlreadyEmail = true
        }
        return isAlreadyEmail
    }


    private fun validateEmailAndPassword(
        emailValue: String,
        passwordValue: String,
        exception: Throwable?
    ): Boolean {
        var isValid = true
        if (emailValue.isEmpty()) {
            isValid = false
            _emailError.value = "^"
        } else if (!isBasicValidEmail(emailValue)) {
            isValid = false
            _emailError.value = "Email không hợp lệ"
        } else if (!isValidEmail(emailValue)) {
            isValid = false
            _emailError.value = "Email này không được hỗ trợ"
        } else if (handleSignUpError(exception)) {
            isValid = false
            _emailError.value = "Tài khoản email đã tồn tại. Vui lòng thử email khác!"
        } else {
            _emailError.value = ""
        }

        if (passwordValue.isEmpty()) {
            isValid = false
            _passwordError.value = "^"
        } else if (passwordValue.length < 6) {
            isValid = false
            _passwordError.value = "Mật khẩu phải lớn hơn 6 ký tự"
        } else if (!isValidPassword(passwordValue)) {
            isValid = false
            _passwordError.value =
                "Mật khẩu phải chứa ít nhất 1 kí tự in hoa, 1 chữ thường và 1 chữ số"
        } else {
            _passwordError.value = null
        }
        return isValid
    }


    //update helpertext
    fun updateEmailHelperText(email: String) {
        when {
            email.isEmpty() -> {
                _emailHelperText.value = "^"
            }

            email.length in 1..7 -> {
                _emailHelperText.value = "Hỗ trợ email: gmail.com, outlook và edu"
            }

            else -> {
                _emailHelperText.value = null
                _emailError.value = null
            }
        }
    }

    fun updatePasswordHelperText(password: String) {
        when {
            password.isEmpty() -> {
                _passwordHelperText.value = "^"
            }

            password.length in 1..4 -> {
                _passwordHelperText.value = "Mật khẩu phải có ít nhất 6 kí tự"
            }

            !isValidPassword(password) && password.length > 4 -> {
                _passwordHelperText.value =
                    "và có ít nhất 1 kí tự in hoa, 1 chữ số"
            }

            isValidPassword(password) -> {
                _passwordHelperText.value = null
            }

            else -> {
                _passwordHelperText.value = null
                _passwordError.value = null
            }
        }
    }

    fun signUp() {
        val emailValue = email.value ?: ""
        val passwordValue = password.value ?: ""

        Log.d("TAGY", "xoay xoay")
        _loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            // Sử dụng coroutine để kiểm tra đồng thời
            try {
                val signUpResult = userRepository.signUp(emailValue, passwordValue)
                val exception = signUpResult.exceptionOrNull()
                if (signUpResult.isSuccess) {
                    withContext(Dispatchers.Main) {
                        _loading.value = true
                        Log.d("TAGY", "Sign up successful")
                        val intent = Intent(context, LoadingActivity::class.java)
                        context.startActivity(intent)
                        if (context is Activity) {
                            context.finish()
                        }
                        _signupResult.value = true
                        _loading.value = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        !validateEmailAndPassword(emailValue, passwordValue, exception)
                        _loading.value = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    !validateEmailAndPassword(emailValue, passwordValue, e)
                }
            } catch (e: FirebaseNetworkException) {
                Toast.makeText(context,"Vui lòng kiểm tra kết nối mạng của bạn",Toast.LENGTH_SHORT)
            }
        }
    }


    fun chooseAvatar() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        (context as Activity).startActivityForResult(intent, REQUEST_IMAGE_GET)
    }

    fun handleImageResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        ShapeableImageView: ShapeableImageView
    ) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_GET -> {
                    val originalImageUri = data?.data
                    if (originalImageUri != null) {
                        // Lấy Uri của ảnh đã resize
                        viewModelScope.launch(Dispatchers.IO) {
                            val resizedImageUri = getResizedImageUri(context, originalImageUri)

                            // Gán imgUri bằng Uri của ảnh đã resize
                            imgUri = resizedImageUri!!

                            // Hiển thị ảnh đã resize lên ShapeableImageView
                            withContext(Dispatchers.Main) {
                                if (resizedImageUri != null) {
                                    Glide.with(context)
                                        .load(resizedImageUri)
                                        .into(ShapeableImageView)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private suspend fun getResizedImageUri(context: Context, imgUri: Uri): Uri? {
        val resizedBitmap = Glide.with(context)
            .asBitmap()
            .load(imgUri)
            .override(300, 200)
            .submit()
            .get()

        return saveBitmapToStorage(resizedBitmap, context)
    }
    private fun saveBitmapToStorage(bitmap: Bitmap, context: Context): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return try {
            val imageFile = File.createTempFile(fileName, ".jpg", storageDir)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } catch (e: Exception) {
            Log.e("SaveBitmap", "error save bitmap: ${e.message}")
            null
        }
    }


    fun updatenamehelper(
        name: String
    ) {
        if (name.length >= 6) {
            _namehelper.value = ""
        } else {
            _namehelper.value = "^"
        }

    }

    private fun validateNameAndAvt(
        name: String
    ): Boolean {
        var isValid = true
        if (name == "") {
            _nameError.value = "Vui lòng nhập tên"
            isValid = false
        } else if (name.length < 3) {
            _nameError.value = "Vui lòng chọn lại tên khác"
            isValid = false
        }
        return isValid
    }

    fun updateAvtAndNameUser() {
        val userNameValue = nameUser.value ?: ""
        _loading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updateResult = userRepository.createAvtandNameUser(imgUri, userNameValue)
                val exception = updateResult.exceptionOrNull()
                withContext(Dispatchers.Main) {
                    if (updateResult.isSuccess) {
                        _loading.value = true
                        Log.d("TAGY", "thanh cong")
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                        if (context is Activity) {
                            context.finish()
                        }
                        _updateResult.value = true
                        _loading.value = false
                    } else {
                        !validateNameAndAvt(userNameValue)
                        Log.d("TAGY", "error update avtname: $exception $imgUri")
                        _loading.value = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    !validateNameAndAvt(userNameValue)
                    Log.d("TAGY", "error update avtnme: $e")

                }
            }
        }
    }


    private fun handleLoginError(exception: Throwable?): Boolean {
        var ischeck = true
        if (exception is FirebaseAuthInvalidCredentialsException) {
            _pwErrorLg.postValue( exception.message)
            ischeck = false
        }else if (exception is FirebaseAuthInvalidUserException) {
            _pwErrorLg.postValue( exception.message)
            ischeck = false
        }else if (exception is FirebaseNetworkException) {
            _pwErrorLg.postValue( exception.message)
            ischeck = false
        }else if (exception is FirebaseNetworkException) {
            _pwErrorLg.postValue( exception.message)
            ischeck = false
        }else{
            _pwErrorLg.postValue( exception?.message)
        }
        return ischeck
    }

    fun validateLogin(emailValue: String) {

        if (emailValue.isEmpty()) {
            _emailHelperTextLg.value = "^"

        } else {
            _emailHelperTextLg.value = ""
        }

    }


    fun validateLoginpw(passwordValue: String) {

        if (passwordValue.isEmpty()) {
            _passwordHelperTextLg.value = "^"
        } else {
            _passwordHelperTextLg.value = ""
        }
    }

    fun login() {
        val emailValue = email.value ?: ""
        val passwordValue = password.value ?: ""

        _loading.value = true
        Log.d("TAGY", "xoayxoay")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loginResult = userRepository.login(emailValue, passwordValue)
                if (loginResult.isSuccess) {
                    withContext(Dispatchers.Main) {
                        _loading.value = true
                        context.startActivity(Intent(context, MainActivity::class.java))
                        if (context is Activity) {
                            context.finish()
                        }
                        Log.d("TAGY", "Login successful")
                        _loginResult.value = true
                    }
                } else {
                    val exception = loginResult.exceptionOrNull()
                    !handleLoginError(exception)
                    _loading.postValue(false)
                    Log.d("TAGY", "Login error: ${exception?.message}")
                    _loginResult.postValue(false)
                }


            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    !handleLoginError(e)
                    _loading.value = false
                    Log.e("TAGY", "Login exception: ${e.message}")
                    _loginResult.value == false
                }
            }
        }
    }

    init {
        getInfo()
    }

    fun getInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val getUserResult = userRepository.getInfoUser()
            if (getUserResult.isSuccess) {
                _getUserResult.postValue(getUserResult.getOrNull())
                Log.d("TAGY", "$getUserResult")
            } else {
                // Xử lý lỗi
                Log.e(
                    "ProfileViewModel",
                    "Error get current user: ${getUserResult.exceptionOrNull()}"
                )
            }
        }
    }

   fun islogin(){
       var islogin = userRepository.isUserLoggedIn()
       if(islogin ){
           context.startActivity(Intent(context,MainActivity::class.java))

       }
   }

    fun islogined():Boolean{
        var islogin = userRepository.isUserLoggedIn()
        return islogin
    }


    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            withContext(Dispatchers.Main) {
                _logoutResult.value = true
                context.startActivity(Intent(context, StartAppActivity::class.java))
                (context as? Activity)?.finish()
            }
        }
    }

    companion object {
        const val REQUEST_IMAGE_GET = 1
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("TAGY", "ViewModel is clear")
    }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {

    }
}