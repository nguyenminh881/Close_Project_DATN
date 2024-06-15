package com.example.cameraprovider

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.cameraprovider.databinding.ActivitySignUpBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUp : AppCompatActivity() {
    lateinit var binding: ActivitySignUpBinding
    private lateinit var authViewModel: AuthViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)


        val userRepository = UserRepository()
        val factory = AuthViewModelFactory(userRepository, this)
        authViewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)
        binding.lifecycleOwner = this
        binding.authVModel = authViewModel


        authViewModel.email.observe(this, { email ->
            authViewModel.updateEmailHelperText(binding.emailCreate.text.toString() ?: "")
        })


        authViewModel.password.observe(this, { password ->
            authViewModel.updatePasswordHelperText(binding.pwCreate.text.toString() ?: "")
        })


        authViewModel.loading.observe(this, { loading ->
            binding.apply {
                if(loading == true){
                    btnSignup.text = ""
                    btnSignup.icon = null
                }else{
                    btnSignup.text = "Tiếp tục"
                    btnSignup.setIconResource(R.drawable.ic_next)
                }

            }
        })


        authViewModel.passwordHelperText.observe(this, { pw ->
            binding.apply {
                if(pw == null){
                    btnSignup.isEnabled = true
                    btnSignup.backgroundTintList =
                        ActivityCompat.getColorStateList(baseContext, R.color.color_active)
                }else{
                    btnSignup.isEnabled = false
                    btnSignup.backgroundTintList =
                        ActivityCompat.getColorStateList(baseContext, R.color.btndf)
                }
            }
        })

//        authViewModel.signupResult.observe(this, { success ->
//            if (success) {
//                finish()
//            }
//        })
//        authViewModel.islogin()
    }
//    override fun onBackPressed() {
//        if (authViewModel.islogin() == false) {
//            // Nếu người dùng chưa đăng nhập, cho phép quay lại trang trước đó
//            super.onBackPressed()
//        }
//        // Ngược lại, không làm gì cả (không cho phép quay lại trang đăng nhập)
//    }

}