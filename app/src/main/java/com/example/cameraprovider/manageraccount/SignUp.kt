package com.example.cameraprovider.manageraccount

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.cameraprovider.R
import com.example.cameraprovider.StartAppActivity
import com.example.cameraprovider.databinding.ActivitySignUpBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory

class SignUp : AppCompatActivity() {
    lateinit var binding: ActivitySignUpBinding
    private val authViewModel:AuthViewModel by viewModels { AuthViewModelFactory(UserRepository(),this)}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
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

        authViewModel.emailHelperText.observe(this) { email ->
        authViewModel.passwordHelperText.observe(this, { pw ->
            binding.apply {
                if(pw == null && email == null){
                    btnSignup.isEnabled = true
                    btnSignup.backgroundTintList =
                        ActivityCompat.getColorStateList(baseContext, R.color.color_active)
                }else{
                    btnSignup.isEnabled = false
                    btnSignup.backgroundTintList =
                        ActivityCompat.getColorStateList(baseContext, R.color.btndf)
                }
            }
        })}

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, StartAppActivity::class.java)
            startActivity(intent)
        }
    }


}