package com.example.cameraprovider

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.cameraprovider.databinding.ActivityMainBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    lateinit var gestureDetector:GestureDetector
    private lateinit var authViewModel:AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val userRepository = UserRepository()
        val factory = AuthViewModelFactory(userRepository, this)
        authViewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)
        viewBinding.lifecycleOwner = this
        viewBinding.authVModel = authViewModel





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
                val cameraFragment = supportFragmentManager.findFragmentByTag("CameraFragment") as? CameraFragment
                val cameraProvider = cameraFragment?.getCameraProvider()
                if(tab?.position != 0){
                    cameraProvider?.unbindAll()
                }
                else{

                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
//                val cameraFragment = supportFragmentManager.findFragmentByTag("CameraFragment") as? CameraFragment
//                val cameraProvider = cameraFragment?.getCameraProvider()
//                cameraProvider?.unbindAll()
            }

        })
        // Khởi tạo GestureDetector và thiết lập lắng nghe sự kiện vuốt
        gestureDetector = GestureDetector(this, GestureListener())

        // Xử lý khi button được nhấn
        viewBinding.btnBottomSheetProfile.setOnClickListener {
            // Đóng activity hiện tại
            finish()
            // Mở ProfileActivity
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        val bottomSheetDialogFragment = FriendListFragment()
        viewBinding.btnBottomSheetFriends.setOnClickListener{
            bottomSheetDialogFragment.show(supportFragmentManager,"FriendListBottomSheet")
        }
    }
    private var backPressedTime: Long = 0

    override fun onBackPressed() {
        if (authViewModel.islogined()) {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                finishAffinity() // Kết thúc ứng dụng
            } else {
                Toast.makeText(this, "Chạm lại để thoát", Toast.LENGTH_SHORT).show()
                backPressedTime = System.currentTimeMillis()
            }
        } else {
            super.onBackPressed() // Hành vi mặc định nếu chưa đăng nhập
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Chuyển sự kiện onTouchEvent đến GestureDetector để xử lý
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
                    val intent = Intent(this@MainActivity, PostList::class.java)
                    val options = ActivityOptions.makeCustomAnimation(this@MainActivity,
                        R.anim.slide_in_up, R.anim.slide_out_up)
                    startActivity(intent, options.toBundle())
                    return true
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

}