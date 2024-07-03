package com.example.cameraprovider

import android.app.ActivityOptions
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.cameraprovider.databinding.ActivityProfileBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory
import com.example.cameraprovider.widget.PostWidget
import com.example.cameraprovider.widget.TutorialBottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

class ProfileActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileBinding
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            UserRepository(),
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)

        authViewModel.getUserResult.observe(this, Observer { user ->
            user?.let {
                if (user.avatarUser != null) {
                    Glide.with(this)
                        .load(user.avatarUser)
                        .error(R.drawable.avt_defaut)
                        .transform(CircleCrop())
                        .override(300, 200)
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

        authViewModel.UpdateError.observe(this) {
            if (it == "tên") {
                dialog.dismiss()
                Snackbar.make(binding.root, "Đổi $it thành công", Snackbar.LENGTH_SHORT).show()
            } else if (it == "mật khẩu") {
                dialog.dismiss()
                Snackbar.make(binding.root, "Đổi $it thành công", Snackbar.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                Snackbar.make(binding.root, "$it", Snackbar.LENGTH_SHORT).show()
            }
        }

        val dialog1 = TutorialBottomSheetDialogFragment()
        binding.chipWidgetPost.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt(
                "dialogTypeWidget",
                TutorialBottomSheetDialogFragment.DIALOG_TYPE_WIDGETPOST
            )
            dialog1.arguments = bundle
            dialog1.show(supportFragmentManager, "TutorialBottomSheetDialogFragment")
        }

        binding.chipWidgetChat.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt(
                "dialogTypeWidget",
                TutorialBottomSheetDialogFragment.DIALOG_TYPE_WIDGETCHAT
            )
            dialog1.arguments = bundle
            dialog1.show(supportFragmentManager, "TutorialBottomSheetDialogFragment")
        }

        binding.btnDeleteAccount.setOnClickListener {
            deleteAccount()
        }

        binding.btngoghome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_in_down, R.anim.slide_out_down
            )
            startActivity(intent,options.toBundle())
        }

    }

    override fun finish() {
        super.finish()
        if (authViewModel.islogined()) {
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
        }

        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        authViewModel.handleImageResult(requestCode, resultCode, data, binding.imgAvtUser)
        authViewModel.updateAvt()

    }


    private fun deleteAccount() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Xác nhận xóa tài khoản")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản của mình không?")
            .setPositiveButton("Xóa") { dialog, _ ->
                authViewModel.deleteAccount()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy"){dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}