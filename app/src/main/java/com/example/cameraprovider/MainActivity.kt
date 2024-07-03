package com.example.cameraprovider

import android.app.ActivityOptions
import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.example.cameraprovider.databinding.ActivityMainBinding
import com.example.cameraprovider.databinding.FriendRequestDialogBinding
import com.example.cameraprovider.notification.NotificationService
import com.example.cameraprovider.repository.PostRepository
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory
import com.example.cameraprovider.viewmodel.FriendViewmodel
import com.example.cameraprovider.viewmodel.MessageViewModel
import com.example.cameraprovider.viewmodel.PostViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging


class MainActivity : AppCompatActivity() {
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private val CAMERA_PERMISSION_REQUEST_CODE = 1002
    private lateinit var viewBinding: ActivityMainBinding
    lateinit var gestureDetector: GestureDetector
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            UserRepository(),
            this
        )
    }
    private val frVModel: FriendViewmodel by viewModels()
    private val postVmodel: PostViewModel by viewModels()
    private val messageViewModel: MessageViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        checkAndRequestNotificationPermission()



        viewBinding.lifecycleOwner = this

        viewBinding.authVModel = authViewModel


        viewBinding.frVmodel = frVModel

        viewBinding.btnBottomSheetProfile.setOnClickListener {
            // Đóng activity hiện tại
            finish()
            // Mở ProfileActivity
            val intent = Intent(this, ProfileActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this@MainActivity,
                R.anim.slide_in_up, R.anim.slide_out_up
            )
            startActivity(intent,options.toBundle())
        }




///go chat
        viewBinding.btnMessage.setOnClickListener {
            intent = Intent(this, ChatActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this@MainActivity,
                R.anim.slide_in_up, R.anim.slide_out_up
            )
            startActivity(intent,options.toBundle())
        }








        frVModel.listFriend.observe(this) {
            if (it == null) {
                viewBinding.btnBottomSheetFriends.text = "(0) Bạn bè"
            } else {
                viewBinding.btnBottomSheetFriends.text = "(${it.size}) Bạn bè"
            }
        }
        frVModel.getFriendAccepted()
        authViewModel.getInfo()


        //viewpp2
        viewBinding.viewpp.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2 // Số lượng Fragment
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> CameraFragment() // Fragment camera
                    else -> RecordFragment() // Fragment ghi âm
                }
            }
        }
        TabLayoutMediator(viewBinding.tabLayout, viewBinding.viewpp) { tab, position ->
            tab.icon = when (position) {
                0 -> resources.getDrawable(R.drawable.ic_cam, null)
                else -> resources.getDrawable(R.drawable.ic_mic, null)
            }
        }.attach()

        viewBinding.viewpp.isUserInputEnabled = false

        viewBinding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewBinding.viewpp.currentItem = tab?.position ?: 0
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val cameraFragment =
                    supportFragmentManager.findFragmentByTag("CameraFragment") as? CameraFragment
                val cameraProvider = cameraFragment?.getCameraProvider()
                if (tab?.position != 0) {
                    cameraProvider?.unbindAll()
                } else {

                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })
        // Khởi tạo GestureDetector và thiết lập lắng nghe sự kiện vuốt
        gestureDetector = GestureDetector(this, GestureListener())

        // Xử lý khi button được nhấn


        val bottomSheetDialogFragment = FriendListFragment()
        viewBinding.btnBottomSheetFriends.setOnClickListener {
            bottomSheetDialogFragment.show(supportFragmentManager, "FriendListBottomSheet")
        }


//diALOGKETBAN
        //  `uid`
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val uid = sharedPreferences.getString("uid", null)

        Log.d("MainActivityApplink","$uid")
        if (uid != null) {
            frVModel.getinforUserSendlink(uid)

            frVModel.infoUserSendlink.observe(this, Observer { result ->
                if (result.first != null && result.second != null) {
                    showFriendRequestDialog(uid, result.first, result.second)
                    sharedPreferences.edit().clear().apply()
                } else {
                    sharedPreferences.edit().clear().apply()
                    Toast.makeText(this, "Lỗi, vui lòng thử lại sau!", Toast.LENGTH_SHORT).show()
                }
            })
        }

        frVModel.friendRequest.observe(this) { isSuccess ->
            if (isSuccess == true) {
                Snackbar.make(viewBinding.root, "Gửi thành công!", Snackbar.LENGTH_SHORT)
                    .show()
            } else {
                frVModel.errorMessage.observe(this) {

                    Snackbar.make(viewBinding.root, "$it", Snackbar.LENGTH_SHORT)
                        .show()
                }

            }
        }


            ///////////// xu li sau dang bai
        postVmodel.postResultLiveData.observe(this) { result ->
            when (result) {
                is PostRepository.PostResult.Success -> {
                    val intent = Intent(this@MainActivity, PostList::class.java)
                    val options = ActivityOptions.makeCustomAnimation(
                        this@MainActivity,
                        R.anim.slide_in_up, R.anim.slide_out_up
                    )
                    startActivity(intent, options.toBundle())
                }
                else-> {
                    Toast.makeText(this, "Vui lòng thử lại sau", Toast.LENGTH_SHORT).show()
                }
            }
        }





        //go pot
        viewBinding.xembai.setOnClickListener {
            viewBinding.newposttxt.visibility = View.GONE
            postVmodel.clearNewpostsize()
            val intent = Intent(this, PostList::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this@MainActivity,
                R.anim.slide_in_up, R.anim.slide_out_up
            )
            startActivity(intent,options.toBundle())
        }



        postVmodel.newPostCount.observe(this){
            if(it > 0){
                viewBinding.newposttxt.visibility = View.VISIBLE
                viewBinding.newposttxt.text =it.toString()
            }else{
                viewBinding.newposttxt.visibility = View.INVISIBLE
            }
        }



    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {

        return gestureDetector.onTouchEvent(event!!)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // Kiểm tra e1 và e2 có null không trước khi sử dụng
            if (e1 != null && e2 != null) {
                // Xác định hướng và tốc độ vuốt
                if (e1.y - e2.y > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    // Nếu có vuốt lên, mở PostListActivity
                    viewBinding.newposttxt.visibility = View.GONE
                    postVmodel.clearNewpostsize()
                    val intent = Intent(this@MainActivity, PostList::class.java)
                    val options = ActivityOptions.makeCustomAnimation(
                        this@MainActivity,
                        R.anim.slide_in_up, R.anim.slide_out_up
                    )
                    startActivity(intent, options.toBundle())
                    return true
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }


    private fun showFriendRequestDialog(senderuid: String, userName: String?, userAvt: String?) {
        val dialogBinding = FriendRequestDialogBinding.inflate(layoutInflater)


        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        Glide.with(this).load(userAvt).error(R.drawable.avt_base).into(dialogBinding.imgAvtUserPost)
        dialogBinding.nameFrRequets.text = userName

        dialogBinding.btnSend.setOnClickListener {
            frVModel.handleFriendRequest(senderuid)
            frVModel.errorMessage.observe(this) {
                dialogBinding.btnSend.text = "Bạn bè"
            }
            frVModel.loading.observe(this) { isLoading ->
                if (isLoading == true) {
                    dialogBinding.progressBar.isVisible = isLoading
                    dialogBinding.btnSend.text = ""
                } else {
                    dialogBinding.progressBar.isVisible = isLoading
                }
            }

            frVModel.friendRequest.observe(this) { isSuccess ->
                if (isSuccess == true) {
                    dialogBinding.btnSend.text = "Đã gửi"
                    dialog.dismiss()
                } else {
                    dialogBinding.btnSend.text = ""
                    dialog.dismiss()
                }
            }
        }
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            } else {
                checkAndRequestCameraPermissions()
            }
        } else {
            checkAndRequestCameraPermissions()
        }
    }

    private fun checkAndRequestCameraPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        CameraFragment.getRequiredPermissions().forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(this, NotificationService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    checkAndRequestCameraPermissions()
                } else {
                    Toast.makeText(this, "Bạn chưa cấp quyền để nhận thông báo", Toast.LENGTH_SHORT).show()
                }
            }
            CAMERA_PERMISSION_REQUEST_CODE -> {
                val cameraPermissionsGranted = CameraFragment.getRequiredPermissions().all { permission ->
                    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                }
                if (!cameraPermissionsGranted) {
                    Toast.makeText(this, "Bạn chưa cấp quyền để sử dụng camera", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}