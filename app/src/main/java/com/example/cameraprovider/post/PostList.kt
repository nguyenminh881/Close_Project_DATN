package com.example.cameraprovider.post

import PostPagingAdapter
import PostPagingAdapter.Companion.VIEW_TYPE_IMAGE
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil

import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

import androidx.recyclerview.widget.SnapHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.cameraprovider.R
import com.example.cameraprovider.chat.ChatActivity
import com.example.cameraprovider.databinding.ActivityPostListBinding
import com.example.cameraprovider.home.MainActivity
import com.example.cameraprovider.profile.ProfileActivity
import com.example.cameraprovider.viewmodel.FriendViewmodel
import com.example.cameraprovider.viewmodel.MessageViewModel

import com.example.cameraprovider.viewmodel.PostViewModel
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class PostList : AppCompatActivity() {

    private lateinit var binding: ActivityPostListBinding
    lateinit var postApdapter: PostPagingAdapter
    private val postViewModel: PostViewModel by viewModels()
    private val messViewModel: MessageViewModel by viewModels()
    private var currentPostPosition = -1
    private var isKeyboardVisible = false
    private var isedtVisible = false
    private val frVModel: FriendViewmodel by viewModels()
    private lateinit var imm: InputMethodManager
    private var isCurrentUserPost: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_post_list)

        setupRecyclerView()

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = true
            binding.btnNewpost.visibility = View.INVISIBLE
            postViewModel.stopListeningForNewPosts()
            postApdapter.refresh()
            binding.shimmerLayout.startShimmer()
            binding.shimmerLayout.visibility = View.VISIBLE


            //  trạng thái tải dữ liệu
            lifecycleScope.launch {
                postApdapter.loadStateFlow.collectLatest { loadStates ->
                    if (loadStates.refresh is LoadState.NotLoading) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        binding.recyclerView.scrollToPosition(0)
                    }
                }
            }
        }


///gan dl vao paging
        lifecycleScope.launch {
            postViewModel.posts
                .collectLatest { pagingData ->
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
//xu ly loi indexbounds
        postApdapter.addOnPagesUpdatedListener {
            if (postApdapter.itemCount == 0) {
                currentPostPosition = -1
            }
        }
//lang nghe cuon
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val newPosition = layoutManager.findFirstVisibleItemPosition()

                if (newPosition != currentPostPosition) {
                    currentPostPosition = newPosition
                    Log.d("PostListposition", "Current post position: $currentPostPosition")
                    Log.d("PostListposition", "new post position: $newPosition")
                    if (currentPostPosition in 0 until postApdapter.itemCount) {
                        if (postApdapter.getPostUserId(currentPostPosition) == postViewModel.getcurrentId()) {
                            binding.btnGroupLayout.visibility = View.GONE
                        } else {
                            binding.btnGroupLayout.visibility = View.VISIBLE
                        }

                        binding.btnShare.setOnClickListener {
                            val url = postApdapter.getContentFile(currentPostPosition)
                            if (url != null) {
                                if (postApdapter.getItemViewType(currentPostPosition) == VIEW_TYPE_IMAGE) {
                                    shareImage(this@PostList, url)
                                } else {
                                    shareAudio(this@PostList, url)
                                }
                            }
                        }
                    }
                }
            }
        })
        //tạo animation
        val likeAnimation = listOf(
            AnimationUtils.loadAnimation(this, R.anim.heartflyy),
            AnimationUtils.loadAnimation(this, R.anim.heartflyy),
            AnimationUtils.loadAnimation(this, R.anim.heartflyy),
            AnimationUtils.loadAnimation(this, R.anim.heartflyy)
        )

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



        //back ve main
        binding.dangbai.setOnClickListener {
            this.onStop()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_out_down, R.anim.slide_in_down)
        }
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager


        //
        binding.fakeedittext.setOnClickListener {
            toggleEditTextVisibility()
        }

        // để ẩn bàn phím
        binding.realedittextLayout.setOnClickListener {
            hideKeyboard()
            binding.realedittextLayout.visibility = View.GONE
        }

        binding.mVmodel = messViewModel

//cmt
        messViewModel.messagesend.observe(this) {
            if (messViewModel.messagesend.value != null) {
                binding.btnSend.isEnabled = true
            } else {
                binding.btnSend.isEnabled = false
            }
        }
        // Xử lý khi click vào button gửi để ẩn bàn phím
        binding.btnSend.setOnClickListener {
            val content = postApdapter.getPost(currentPostPosition)!!.content ?: ""
            val userAvt = postApdapter.getPost(currentPostPosition)!!.userAvatar ?: ""
            val imgUrl = postApdapter.getPost(currentPostPosition)!!.imageURL
            val VoiceUrl = postApdapter.getPost(currentPostPosition)!!.voiceURL
            val receiverId = postApdapter.getPost(currentPostPosition)!!.userId
            val postId = postApdapter.getPost(currentPostPosition)!!.postId
            val timeAgo =
                TimeAgo.using(postApdapter.getPost(currentPostPosition)!!.createdAt!!.toDate().time)
            val createAt = timeAgo


            messViewModel.sendMessage(
                postId,
                receiverId,
                imgUrl.toString(),
                VoiceUrl.toString(),
                content,
                createAt,
                userAvt
            )

            messViewModel.sendSuccess.observe(this) {
                hideKeyboard()
                binding.realedittextLayout.visibility = View.GONE
                binding.realedittext.setText("")
                Snackbar.make(binding.root, "Gửi thành công!", Snackbar.LENGTH_SHORT).show()
            }
        }

//xoa bai
        postViewModel.deletePost.observe(this) { isDeleted ->
            if (isDeleted == true) {

                    postApdapter.notifyItemRemoved(currentPostPosition)

                postViewModel.invalidatePagingSource()

                Snackbar.make(binding.root, "Xóa thành công!", Snackbar.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Vui lòng thử lại sau", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDelete.setOnClickListener {

            val post = postApdapter.getPost(currentPostPosition) ?: return@setOnClickListener
            val postId = post.postId
            val userPostId = post.userId
            Log.d(
                "PostListdelete",
                "Deleting post at position: $currentPostPosition with postId: $userPostId"
            )
            if (userPostId == postViewModel.getcurrentId()) {
                MaterialAlertDialogBuilder(this@PostList, R.style.AlertDialogTheme)
                    .setTitle("Xóa bài viết")
                    .setMessage("Bạn có chắc chắn muốn xóa bài đăng này?")
                    .setPositiveButton("Xóa") { dialog, _ ->
                        postViewModel.deletePost(postId)

                        dialog.dismiss()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()

            } else {
                MaterialAlertDialogBuilder(this@PostList, R.style.AlertDialogTheme)
                    .setTitle("Xóa bài viết")
                    .setMessage("Bài đăng sẽ không hiển thị cho bạn nhưng vẫn có thể hiển thị ở một nơi khác!")
                    .setPositiveButton("Ẩn") { dialog, _ ->
                        postViewModel.deletePost(postId)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        }
//


        //profile
        binding.btnBottomSheetProfile.setOnClickListener {
            // Đóng activity hiện tại
            finish()
            // Mở ProfileActivity
            val intent = Intent(this, ProfileActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_in_up, R.anim.slide_out_up
            )
            startActivity(intent, options.toBundle())
        }


//messs

        binding.btnMessage.setOnClickListener {
            intent = Intent(this, ChatActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_in_up, R.anim.slide_out_up
            )
            startActivity(intent, options.toBundle())
        }


        //newpost


        postViewModel.newPostCount.observe(this) {
            if (it > 0) {
                binding.btnNewpost.visibility = View.VISIBLE
                val count = if (it > 9) "9+" else it.toString()
                binding.btnNewpost.text = "Mới(${count})"
            } else {
                binding.btnNewpost.visibility = View.INVISIBLE
            }
        }

        binding.btnNewpost.setOnClickListener {
            binding.swipeRefreshLayout.isRefreshing = true
            postViewModel.stopListeningForNewPosts()
            binding.btnNewpost.visibility = View.INVISIBLE
            binding.recyclerView.smoothScrollToPosition(0)
            postApdapter.refresh()
            binding.shimmerLayout.startShimmer()
            binding.shimmerLayout.visibility = View.VISIBLE



            lifecycleScope.launch {
                postApdapter.loadStateFlow.collectLatest { loadStates ->
                    if (loadStates.refresh is LoadState.NotLoading) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        binding.shimmerLayout.stopShimmer()
                        binding.shimmerLayout.visibility = View.GONE
                        binding.recyclerView.scrollToPosition(0)
                    }
                }
            }
        }

    }

    private fun toggleEditTextVisibility() {
        if (!isKeyboardVisible) {
            binding.realedittextLayout.visibility = View.VISIBLE
            binding.realedittext.hint =
                "Trả lời ${postApdapter.getPost(currentPostPosition)?.userName}..."

            binding.realedittext.requestFocus()
            showKeyboard()




            isKeyboardVisible = true
        } else {
            hideKeyboard()
            isKeyboardVisible = false
            binding.realedittextLayout.visibility = View.GONE
        }
    }

    private fun showKeyboard() {
        imm.showSoftInput(binding.realedittext, InputMethodManager.SHOW_IMPLICIT)
        isKeyboardVisible = true
    }

    private fun hideKeyboard() {
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
            currentFocus == null
            isKeyboardVisible = false
        }
    }


    private fun setupRecyclerView() {

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerView)

        postApdapter = PostPagingAdapter(
            isCurrentUser = { post -> post.userId == postViewModel.iscurrentId() },
            postViewModel,
            this,
            this,
            activity = this@PostList,
            onPostViewed = { postId -> postViewModel.onPostViewed(postId) }
        )
        binding.recyclerView.adapter = postApdapter
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down)
    }


    private fun shareImage(context: Context, imageUrl: String) {
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {//khi anh da san sang
                    try {
                        val file = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "shared_image.png"
                        )
                        val out =
                            FileOutputStream(file)//Mở một luồng để ghi dữ liệu vào tệp.
                        resource.compress(Bitmap.CompressFormat.PNG, 100, out)//nen
                        out.flush()//Đẩy dữ liệu còn lại vào file.
                        out.close()//Đóng luồng đầu ra.
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        shareUri(context, uri, "image/*")
                    } catch (e: IOException) {
                        Log.e("ShareImage", "không thể lưu ảnh", e)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    //tải file
    private fun shareAudio(context: Context, audioUrl: String) {
        Thread {//tạo luồng tải dưới nền
            try {
                val url = URL(audioUrl)
                val connect = url.openConnection() as HttpURLConnection//mo ket noi http
                connect.requestMethod = "GET"
                connect.connect()

                if (connect.responseCode == HttpURLConnection.HTTP_OK) {
                    val file = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                        "shared_audio.mp3"
                    )
                    val inputStream = connect.inputStream
                    val outputStream = FileOutputStream(file)

                    val buffer =
                        ByteArray(1024)//tao bo nho dem byte de luu tru du lieu vao file am thanh
                    var len: Int //tao bien dee luu du lieu doc dc từ luồng
                    while (inputStream.read(buffer).also { len = it } != -1) {
                        outputStream.write(buffer, 0, len)
                    }//Đọc dữ liệu từ luồng đầu vào vào bộ đệm cho đến khi kết thúc luồng.

                    outputStream.close()//Đóng luồng đầu ra.
                    inputStream.close()//Đóng luồng đầu vào.

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    shareUri(context, uri, "audio/*")
                }
            } catch (e: IOException) {
                Log.e("ShareAudio", "không thể lưu voice", e)
            }
        }.start()
    }

    private fun shareUri(context: Context, uri: Uri, ty: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = ty
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, null))
    }

}

