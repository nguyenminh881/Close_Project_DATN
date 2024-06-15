package com.example.cameraprovider

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.cameraprovider.databinding.ActivitySignInBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory


class SignInActivity : AppCompatActivity() {

    lateinit var binding:ActivitySignInBinding
    private lateinit var authViewModel: AuthViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =DataBindingUtil.setContentView(this,R.layout.activity_sign_in)

        val userRepository = UserRepository()
        val factory = AuthViewModelFactory(userRepository, this)
        authViewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)

        if(authViewModel.islogined()){
            startActivity(Intent(this,MainActivity::class.java))
            this.finish()
        }
        binding.lifecycleOwner = this
        binding.authVModel = authViewModel


        authViewModel.email.observe(this, { email ->
            authViewModel.validateLogin(binding.emailCreate.text.toString() ?: "")
        })
        authViewModel.password.observe(this, { password ->
            authViewModel.validateLoginpw(binding.pwCreate.text.toString() ?: "")
        })




        authViewModel.loading.observe(this, { loading ->
            binding.apply {
                if(loading == true){
                    btnSignin.text = ""
                    btnSignin.icon = null
                }else{
                    btnSignin.text = "Tiếp tục"
                    btnSignin.setIconResource(R.drawable.ic_next)
                }

            }
        })


        authViewModel.passwordHelperTextLg.observe(this, { pw ->
            binding.apply {
                if(pw ==""){
                    btnSignin.isEnabled = true
                    btnSignin.backgroundTintList =
                        ActivityCompat.getColorStateList(baseContext, R.color.color_active)
                }else{
                    btnSignin.isEnabled = false
                    btnSignin.backgroundTintList =
                        ActivityCompat.getColorStateList(baseContext, R.color.btndf)
                }
            }
        })
    }


}