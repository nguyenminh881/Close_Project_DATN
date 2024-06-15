package com.example.cameraprovider

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.example.cameraprovider.adapter.RequestFriendAdapter
import com.example.cameraprovider.databinding.ActivityProfileBinding
import com.example.cameraprovider.model.Friendship
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory
import com.example.cameraprovider.viewmodel.FriendViewmodel
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    lateinit var binding:ActivityProfileBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var friendViewmodel:FriendViewmodel
    private lateinit var adapter: RequestFriendAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =DataBindingUtil.setContentView(this, R.layout.activity_profile)


        val userRepository = UserRepository()
        val factory = AuthViewModelFactory(userRepository, this)
        authViewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)


        authViewModel.getUserResult.observe(this, Observer { user ->
            user?.let {

                if(user.avatarUser!= null){
                    Glide.with(this)
                        .load(user.avatarUser)
                        .error(R.drawable.avt_defaut)
                        .transform(CircleCrop())
                        .override(300,200)
                        .into(binding.imgAvtUser)
                }

            }
        })
        binding.lifecycleOwner = this
        binding.viewModel = authViewModel
    }

    override fun finish() {
        super.finish()
        if(authViewModel.islogined()){
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
        }

        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down)
    }


}