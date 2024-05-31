package com.example.cameraprovider

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.cameraprovider.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    lateinit var gestureDetector:GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        viewBinding.viewpp.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2 // Số lượng Fragment
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> CameraFragment() // Fragment cho chế độ Camera
                    else -> RecordFragment() // Fragment cho chế độ Ghi âm
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
        val i = Intent(this, PostList::class.java)
        val options = ActivityOptions.makeCustomAnimation(this,
            R.anim.slide_in_up, R.anim.slide_out_up)
        startActivity(intent, options.toBundle())

        gestureDetector = GestureDetector(this, GestureListener())

        viewBinding.btntest.setOnClickListener{
            val i = Intent(this,SignUp::class.java)
            startActivity(i)
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
            if (e1 != null && e2 != null) {
                if (e1.y - e2.y > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
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