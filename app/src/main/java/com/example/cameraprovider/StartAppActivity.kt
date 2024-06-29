package com.example.cameraprovider

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.cameraprovider.databinding.ActivityStartAppBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory
import com.example.cameraprovider.viewmodel.FriendViewmodel
import androidx.lifecycle.Observer


class StartAppActivity : AppCompatActivity() {
    lateinit var binding: ActivityStartAppBinding
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            UserRepository(),
            this
        )
    }
    private val frVModel: FriendViewmodel by viewModels()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_start_app)

        binding.lifecycleOwner = this
        binding.vmodel = authViewModel



        handleIntent(intent)

        frVModel.friendshipResult.observe(this@StartAppActivity, Observer { result ->

            Log.d("StartApp", "onCreate: $result")
            if (authViewModel.islogined() && !authViewModel.isadmin()) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("userId", result)
                startActivity(intent)
            }
        })


        if (authViewModel.islogined()) {
            val i = Intent(this, MainActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }else{
            if(authViewModel.isadmin()){
                val intent = Intent(this, AdminActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }


        binding.btnDk.setOnClickListener {
            val i = Intent(this, SignUp::class.java)
            startActivity(i)
        }

        binding.btnDn.setOnClickListener {
            val i = Intent(this, SignInActivity::class.java)
            startActivity(i)
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("channel_id", "Channel name", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }

    override fun onStart() {
        super.onStart()
//        if (authViewModel.islogined()) {
//            onRegistration()
//        }

    }

    //    private fun onRegistration() {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
//        startActivity(intent)
//    }
    private fun handleIntent(intent: Intent) {
        intent.data?.let {
            frVModel.handleDynamicLink(intent)
        }
    }



}