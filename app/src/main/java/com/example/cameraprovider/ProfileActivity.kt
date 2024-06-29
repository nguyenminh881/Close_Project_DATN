package com.example.cameraprovider

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    lateinit var binding:ActivityProfileBinding
    private val authViewModel:AuthViewModel by viewModels { AuthViewModelFactory(UserRepository(),this)}
    private lateinit var friendViewmodel:FriendViewmodel
    private lateinit var adapter: RequestFriendAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =DataBindingUtil.setContentView(this, R.layout.activity_profile)

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

        val dialog = EditProfileFragment()
        binding.btnEditName.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt("dialogType", EditProfileFragment.DIALOG_TYPE_NAME)
            dialog.arguments = bundle
            dialog.show(supportFragmentManager, "update_profile_dialog")
        }

        binding.btnEditPw.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt("dialogType", EditProfileFragment.DIALOG_TYPE_PASSWORD)
            dialog.arguments = bundle
            dialog.show(supportFragmentManager, "update_profile_dialog")
        }

        authViewModel.UpdateError.observe(this){
            if(it=="tên"){
                dialog.dismiss()
                Snackbar.make(binding.root, "Đổi $it thành công", Snackbar.LENGTH_SHORT).show()
            }else if(it=="mật khẩu"){
                dialog.dismiss()
                Snackbar.make(binding.root, "Đổi $it thành công",Snackbar.LENGTH_SHORT).show()
            }else{
                dialog.dismiss()
                Snackbar.make(binding.root, "$it", Snackbar.LENGTH_SHORT).show()
            }
        }


    }

    override fun finish() {
        super.finish()
        if(authViewModel.islogined()){
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
        }

        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        authViewModel.handleImageResult(requestCode, resultCode, data,binding.imgAvtUser)
        authViewModel.updateAvt()
    }

}