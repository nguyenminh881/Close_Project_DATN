package com.example.cameraprovider

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.cameraprovider.databinding.ActivityStartAppBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory

class StartAppActivity : AppCompatActivity() {
    lateinit var binding: ActivityStartAppBinding
    private lateinit var authViewModel: AuthViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding  = DataBindingUtil.setContentView(this,R.layout.activity_start_app)

        val userRepository = UserRepository()
        val factory = AuthViewModelFactory(userRepository, this)
        authViewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)
        binding.lifecycleOwner = this
        binding.vmodel = authViewModel

//
//        //xuly khi mo app
//        handleIntent(intent)

        binding.btnDk.setOnClickListener{
            val i = Intent(this,SignUp::class.java)
            startActivity(i)
        }

        binding.btnDn.setOnClickListener{
            val i = Intent(this,SignInActivity::class.java)
            startActivity(i)
        }
    }
    override fun onStart() {
        super.onStart()
        if (authViewModel.islogined()) {
            onRegistration()
        }
    }
    private fun onRegistration() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(intent)
    }
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        // Handle new intent if the app is already open and receives a new intent
//        handleIntent(intent)
//    }
//    private fun handleIntent(intent: Intent) {
//        intent.data?.let {
//            val action = intent.action
//            if (action == Intent.ACTION_VIEW) {
//                // Navigate to FriendRequestFragment and pass the intent
//                val fragment = FriendListFragment()
//                val args = Bundle()
//                args.putParcelable("intent", intent)
//                fragment.arguments = args
//                supportFragmentManager.beginTransaction()
//                    .replace(androidx.fragment.R.id.listfriendbottomsheet, fragment)
//                    .commit()
//            }
//        }
//    }
}