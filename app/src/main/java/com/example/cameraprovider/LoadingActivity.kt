package com.example.cameraprovider

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.cameraprovider.databinding.ActivityLoadingBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoadingActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoadingBinding
    private lateinit var authViewModel: AuthViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_loading)

        val userRepository = UserRepository()
        val factory = AuthViewModelFactory(userRepository, this)
        authViewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)
        binding.lifecycleOwner = this
        binding.authVModel = authViewModel


        authViewModel.nameUser.observe(this, { name ->
            authViewModel.updatenamehelper(binding.nameCreate.text.toString() ?: "")
        })

        authViewModel.loading.observe(this, { loading ->
            binding.apply {
                if(loading != false){
                    btnNextSignUp.text =""
                    btnNextSignUp.icon = null
                   progressBar.visibility = View.VISIBLE
                }else{
                    progressBar.visibility = View.INVISIBLE
                    btnNextSignUp.text = "Tiếp tục"
                    btnNextSignUp.setIconResource(R.drawable.ic_next)
                }

            }
        })


        authViewModel.namehelper.observe(this, { name ->
            binding.apply {
                if(name ==""){
                    btnNextSignUp.isEnabled = true
                    btnNextSignUp.backgroundTintList = ActivityCompat.getColorStateList(baseContext, R.color.color_active)
                }
            }
        })



    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        authViewModel.handleImageResult(requestCode, resultCode, data,binding.imgvAvtUser)
    }
}