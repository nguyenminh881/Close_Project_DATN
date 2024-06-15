package com.example.cameraprovider

import PostPagingAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer

import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

import androidx.recyclerview.widget.SnapHelper
import com.bumptech.glide.Glide
import com.example.cameraprovider.databinding.ActivityPostListBinding
import com.example.cameraprovider.viewmodel.PostViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class PostList : AppCompatActivity() {

    private lateinit var binding: ActivityPostListBinding
    lateinit var postApdapter: PostPagingAdapter
    private val postViewModel: PostViewModel by viewModels()
    private var currentPostPosition = 0
    private var isListEmpty = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_post_list)

        setupRecyclerView()

        lifecycleScope.launch {
            postViewModel.posts
                .collect { pagingData ->
                    postApdapter.submitData(pagingData)
                }

        }
        postApdapter.addLoadStateListener { loadState ->

            if (loadState.refresh is LoadState.Loading) {
                binding.shimmerLayout.startShimmer()
            } else {
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
            }

            //kiem tra xem co bai dang nao k

            val isListEmpty = postApdapter.itemCount == 0

            if (isListEmpty) {
                binding.postsContainer.visibility = View.INVISIBLE
                binding.emptyListPost.visibility = View.VISIBLE
            } else {
                binding.postsContainer.visibility = View.VISIBLE
                binding.emptyListPost.visibility = View.GONE
                val errorState = when {//k load đc thì thì là lòad lỗi
                    loadState.refresh is LoadState.Error -> loadState.refresh as LoadState.Error ///khi du lieu dc lam moi
                    loadState.prepend is LoadState.Error -> loadState.prepend as LoadState.Error //khi du lieu dc keo len
                    loadState.append is LoadState.Error -> loadState.append as LoadState.Error //khi keo xuong load them dl
                    else -> null
                }

                errorState?.let {
                    // Hiển thị dialog thông báo lỗi
                    val dialog = AlertDialog.Builder(this)
                        .setTitle("Lỗi kết nối")
                        .setMessage("Không thể kết nối, vui lòng kiểm tra kết nối mạng của bạn.")
                        .setCancelable(true)
                        .show()

                    // Đóng dialog sau 3 giây
                    Handler(Looper.getMainLooper()).postDelayed({
                        dialog.dismiss()
                    }, 2000) // 3000 milliseconds = 3 giây
                }
            }

        }
        // Theo dõi vị trí của item hiện tại trong RecyclerView
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                currentPostPosition = layoutManager.findFirstVisibleItemPosition()
            }
        })
        //tạo animation
        val likeAnimation = listOf(
            AnimationUtils.loadAnimation(this, R.anim.heartflyy),
            AnimationUtils.loadAnimation(this, R.anim.heartflyy),
            AnimationUtils.loadAnimation(this, R.anim.heartflyy),
            AnimationUtils.loadAnimation(this, R.anim.heartflyy)
        )
        postViewModel.likeEvent.observe(this, Observer { postId ->
            Toast.makeText(this, "Liked post with ID: $postId", Toast.LENGTH_SHORT).show()
        })
        //set clicked icon
        binding.apply {
            btnHeart.setOnClickListener {
                val postId = postApdapter.getPostId(currentPostPosition)
                postViewModel.likePost(postId.toString(), "ic_heart")
                imgHeart.startAnimation(likeAnimation[0])
                likeAnimation[0].setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        imgHeart.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        imgHeart.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {

                    }

                })

            }
            btnHaha.setOnClickListener {
                val postId = postApdapter.getPostId(currentPostPosition)
                postViewModel.likePost(postId.toString(), "ic_haha")
                imgHaha.startAnimation(likeAnimation[1])
                likeAnimation[1].setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        imgHaha.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        imgHaha.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {

                    }

                })
            }
            btnSad.setOnClickListener {
                val postId = postApdapter.getPostId(currentPostPosition)
                postViewModel.likePost(postId.toString(), "ic_sad")
                imgSad.startAnimation(likeAnimation[2])
                likeAnimation[2].setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        imgSad.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        imgSad.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {

                    }

                })
            }
            btnAngry.setOnClickListener {
                val postId = postApdapter.getPostId(currentPostPosition)
                postViewModel.likePost(postId.toString(), "ic_angry")
                imgAngry.startAnimation(likeAnimation[3])
                likeAnimation[3].setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        imgAngry.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        imgAngry.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation?) {

                    }
                })
            }
        }

    }


    private fun setupRecyclerView() {

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerView)

        postApdapter = PostPagingAdapter()
        binding.recyclerView.adapter = postApdapter
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down)
    }

}

