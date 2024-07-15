package com.example.cameraprovider.manageraccount

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.cameraprovider.R
import com.example.cameraprovider.databinding.ActivityForgotPwactivityBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory

class ForgotPWActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPwactivityBinding
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            UserRepository(),
            this
        )
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =DataBindingUtil.setContentView(this, R.layout.activity_forgot_pwactivity)
        binding.lifecycleOwner =this
        binding.viewModel = authViewModel


        binding.forgotpassword.setOnClickListener {
            authViewModel.forgotPassword()
        }
        authViewModel.btntext.observe(this) {
            binding.forgotpassword.text = it
        }
    }
}