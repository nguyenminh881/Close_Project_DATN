import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.cameraprovider.LikesBottomSheetDialog
import com.example.cameraprovider.R
import com.example.cameraprovider.databinding.PostRowBinding
import com.example.cameraprovider.databinding.PostRowVoiceBinding
import com.example.cameraprovider.model.Post
import com.example.cameraprovider.viewmodel.PostViewModel
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import rm.com.audiowave.OnProgressListener
import java.io.File
import java.io.IOException


class PostPagingAdapter(
    private val isCurrentUser: (Post) -> Boolean,
    private val viewModel: PostViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val activity: FragmentActivity
) :
    PagingDataAdapter<Post, RecyclerView.ViewHolder>(POST_COMPARATOR) {

    companion object {
        const val VIEW_TYPE_IMAGE = 0
        const val VIEW_TYPE_VOICE = 1

        private val POST_COMPARATOR = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.postId == newItem.postId
            }

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem == newItem
            }
        }
    }

    class ImageViewHolder(val binding: PostRowBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentLikesLiveData: LiveData<List<Pair<String, List<String>>>>? = null
        fun bind(
            post: Post, isCurrentUser: (Post) -> Boolean, viewModel: PostViewModel,
            lifecycleOwner: LifecycleOwner, context: Context, activity: FragmentActivity
        ) {
            if (post.userAvatar != null) {
                Glide.with(binding.root.context)
                    .load(post.userAvatar)
                    .thumbnail(0.25f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.avt_base)
                    .transition(DrawableTransitionOptions.withCrossFade(100))
                    .override(100, 100)
                    .into(binding.imgAvtUserPost)
            } else {
                binding.imgAvtUserPost.setImageResource(R.drawable.avt_defaut)
            }

            // Tải ảnh bài đăng
            if (post.imageURL != null) {
                Glide.with(binding.root.context)
                    .load(post.imageURL)
                    .thumbnail(0.5f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.error)
                    .override(720, 720)
                    .transition(DrawableTransitionOptions.withCrossFade(100))
                    .into(binding.imageViewPost)
            } else {
                binding.imageViewPost.setImageResource(R.drawable.error)
            }
            if (isCurrentUser(post)) {
                binding.tvNameUserPost.text = "Bạn"
            } else {
                binding.tvNameUserPost.text = post.userName
            }
            val timeAgo = TimeAgo.using(post.createdAt!!.toDate().time)
            binding.timeStamp.text = timeAgo

            if (post.userId == viewModel.getcurrentId()) {

                currentLikesLiveData?.removeObservers(lifecycleOwner)
                currentLikesLiveData = viewModel.getLikes(post.postId)
                binding.btnGroupReact.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val dialog = LikesBottomSheetDialog(post.postId, viewModel)
                        dialog.show(activity.supportFragmentManager, "LikesBottomSheetDialog")
                    }
                    true // Trả về 'true' để báo hiệu rằng bạn đã xử lý sự kiện chạm
                }
                currentLikesLiveData?.observe(lifecycleOwner) { likesData ->
                    if (likesData.isNotEmpty()) {
                        binding.nameUserLike.text = "Có ${likesData.size} hoạt động \uD83D\uDC96 \n" +
                                "Ấn vào để xem"
                    }
                    else {
                        binding.nameUserLike.text = "Không có hoạt động nào ✨"
                    }
                }
            } else {
                binding.btnGroupReact.visibility = ViewGroup.GONE
            }

            binding.postxml = post


        }
    }

    class VoiceViewHolder(val binding: PostRowVoiceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var mediaPlayer = MediaPlayer()
        private val handler = Handler(Looper.getMainLooper())
        private var isplay: Boolean = false
        private var time = 0

        init {
            setupWaveformView()
        }

        private var currentLikesLiveData: LiveData<List<Pair<String, List<String>>>>? = null
        fun bind(
            post: Post,
            isCurrentUser: (Post) -> Boolean,
            viewModel: PostViewModel,
            lifecycleOwner: LifecycleOwner,
            activity: FragmentActivity
        ) {
            if (post.userAvatar != null) {
                Glide.with(binding.root.context)
                    .load(post.userAvatar)
                    .thumbnail(0.25f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.avt_base)
                    .transition(DrawableTransitionOptions.withCrossFade(100))
                    .override(100, 100)
                    .into(binding.imgAvtUserPost)
            } else {
                binding.imgAvtUserPost.setImageResource(R.drawable.avt_defaut)
            }

            post.voiceURL?.let { url ->
                downloadAudio(url)
            } ?: run {
                Log.e("VoiceViewHolder", "Voice URL is null")
            }
            if (isCurrentUser(post)) {
                binding.tvNameUserPost.text = "Bạn"
            } else {
                binding.tvNameUserPost.text = post.userName
            }
            val timeAgo = TimeAgo.using(post.createdAt!!.toDate().time)
            binding.timeStamp.text = timeAgo

            if (post.userId == viewModel.getcurrentId()) {

                currentLikesLiveData?.removeObservers(lifecycleOwner)
                currentLikesLiveData = viewModel.getLikes(post.postId)
                binding.btnGroupReact.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        Log.d("PostPagingAdapter", "Button clicked for post: ${post.postId}")
                        val dialog = LikesBottomSheetDialog(post.postId, viewModel)
                        dialog.show(activity.supportFragmentManager, "LikesBottomSheetDialog")
                    }
                    true // Trả về 'true' để báo hiệu rằng bạn đã xử lý sự kiện chạm
                }
                currentLikesLiveData?.observe(lifecycleOwner) { likesData ->
                    if (likesData.isNotEmpty()) {
                        binding.nameUserLike.text = "Có ${likesData.size} hoạt động \uD83D\uDC96 \nẤn vào để xem "
                    }
                    else {
                        binding.nameUserLike.text = "Không có hoạt động nào ✨"
                    }
                }
            } else {
                binding.btnGroupReact.visibility = ViewGroup.GONE
            }


            binding.postvoicexml = post
        }

        private fun downloadAudio(url: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                    val localFile = File.createTempFile("audio", "aac")
                    storageRef.getFile(localFile).await()

                    withContext(Dispatchers.Main) {
                        Log.d("VoiceViewHolder", "Audio file downloaded successfully")
                        binding.wave.setRawData(localFile.readBytes())

                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(localFile.absolutePath)
                        mediaPlayer.prepare()

                        Log.d("TAGY", "${mediaPlayer.duration}")
                        time = (mediaPlayer.duration / 1000).toInt()
                        if (time == 60) {
                            binding.tvTimer.text = "01:00"
                        } else if (time >= 10) {
                            binding.tvTimer.text = "00:${time.toString()}"
                        } else {
                            binding.tvTimer.text = "00:0${time.toString()}"
                        }
//                        binding.tvTimer.text = formatTime(time)
                        binding.play.setOnClickListener {

                            if (!isplay) {
                                try {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(localFile.absolutePath)
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()
                                    mediaPlayer.setOnCompletionListener {
                                        stopAudio()
                                    }
                                    isplay = true
                                    binding.play.text = "dừng"
                                    handler.post(updateWaveform)
                                } catch (e: IOException) {
                                    Log.e("VoiceViewHolder", "loi", e)
                                }

                            } else {
                                stopAudio()
                            }


                        }
                    }
                } catch (e: Exception) {
                    Log.e("VoiceViewHolder", "loi gi dau xanh", e)
                }
            }
        }

        private fun stopAudio() {
            if (isplay) {
                mediaPlayer.stop()
                mediaPlayer.reset()
                isplay = false
                binding.play.text = "phát"
                handler.removeCallbacks(updateWaveform)
                binding.wave.progress = 0f
            }
        }

        private val updateWaveform = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (isplay) {
                        val progress = (player.currentPosition.toFloat() / player.duration) * 100
                        Log.d("TAGY", "$progress , ${player.duration}")
                        binding.wave.progress = progress
                        handler.postDelayed(this, 1)
                    }

                }
            }
        }

        private fun setupWaveformView() {
            binding.wave.onProgressListener = object : OnProgressListener {
                override fun onProgressChanged(progress: Float, byUser: Boolean) {
                    Log.d("TAGY", "Progress set: $progress, and it's $byUser that user did this")
                    if (byUser && isplay) {
                        val seekToPosition = (mediaPlayer!!.duration * progress / 100.0).toInt()
//                        postRowVoiceBinding.wave.progress = progress
                        Log.d("TAGY", "Seeking to position: $seekToPosition")
                        mediaPlayer?.seekTo(seekToPosition)
                    }
                }

                override fun onStartTracking(progress: Float) {
                    Log.d("TAGY", "Started tracking from $progress")
                }

                override fun onStopTracking(progress: Float) {
                    if (isplay) {
                        val seekToPosition = (mediaPlayer!!.duration * progress / 100.0).toInt()
//                        postRowVoiceBinding.wave.progress = progress
                        Log.d("TAGY", "Seeking to position: $seekToPosition")
                        mediaPlayer?.seekTo(seekToPosition)
                    }
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_IMAGE) {
            val binding = PostRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ImageViewHolder(binding)

        } else {
            val binding =
                PostRowVoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            VoiceViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = getItem(position)
        if (post != null) {
            when (holder) {
                is ImageViewHolder -> holder.bind(
                    post,
                    isCurrentUser,
                    viewModel,
                    lifecycleOwner,
                    context,
                    activity
                )

                is VoiceViewHolder -> holder.bind(post, isCurrentUser, viewModel, lifecycleOwner,
                    activity)
            }
        }
    }

    //    override fun getItemCount(): Int {
//        return itemCount
//    }
    override fun getItemViewType(position: Int): Int {
        val post = getItem(position)
        return if (post?.imageURL?.isNotEmpty() == true && post.voiceURL.isNullOrEmpty()) {
            VIEW_TYPE_IMAGE
        } else {
            VIEW_TYPE_VOICE
        }
    }

    fun getPostId(position: Int): String? {
        return getItem(position)?.postId
    }

    fun getPostUserId(position: Int): String? {
        return getItem(position)?.userId
    }

    fun getContentFile(position: Int): String? {
        if (getItemViewType(position) == VIEW_TYPE_IMAGE) {
            return getItem(position)?.imageURL
        } else {
            return getItem(position)?.voiceURL
        }
    }

    fun getPost(position: Int): Post? {
        return getItem(position)
    }

}
