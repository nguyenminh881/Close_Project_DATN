package com.example.cameraprovider

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.cameraprovider.databinding.ActivityAdminBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory

class AdminActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            UserRepository(),
            this
        )
    }

    private lateinit var  binding: ActivityAdminBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

      binding =DataBindingUtil.setContentView(this, R.layout.activity_admin)

        binding.lifecycleOwner = this
        binding.viewModel = authViewModel

    }

    override fun onBackPressed() {
        if(authViewModel.islogined()){
            finishAffinity()
        }else{

        }
    }
}