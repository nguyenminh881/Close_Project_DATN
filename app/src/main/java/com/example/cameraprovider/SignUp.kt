package com.example.cameraprovider

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
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
    lateinit var binding:ActivitySignUpBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var auth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)

        val userRepository = UserRepository()
        val factory = AuthViewModelFactory(userRepository,this)
        authViewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)
        auth = Firebase.auth
        binding.authVModel = authViewModel

        binding.lifecycleOwner = this
    }


}