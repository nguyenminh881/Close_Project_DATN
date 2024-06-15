import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

import com.example.cameraprovider.R
import com.example.cameraprovider.databinding.PostRowBinding
import com.example.cameraprovider.databinding.PostRowVoiceBinding
import com.example.cameraprovider.model.Post
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import rm.com.audiowave.OnProgressListener
import java.io.File
import java.io.IOException


class PostPagingAdapter( ) : PagingDataAdapter<Post, RecyclerView.ViewHolder>(POST_COMPARATOR) {


    init {

    }

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
        fun bind(post: Post) {
            if (post.userAvatar != null) {
                Glide.with(binding.root.context)
                    .load(post.userAvatar)
                    .thumbnail(0.25f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.drawable.avt_base)
                    .transition(DrawableTransitionOptions.withCrossFade(100))
                    .override(100,100)
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
                    .override(720,720)
                    .transition(DrawableTransitionOptions.withCrossFade(100))
                    .into(binding.imageViewPost)
            } else {
                binding.imageViewPost.setImageResource(R.drawable.error)
            }

            // Gán dữ liệu khác của bài đăng vào view
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
        fun bind(post: Post) {
                Glide.with(binding.root.context)
                    .load(post.imageURL)
                    .thumbnail(0.25f)
                    .error(R.drawable.avt_base)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade(100))
                    .into(binding.imgAvtUserPost)
            post.voiceURL?.let { url ->
                downloadAudio(url)
            } ?: run {
                Log.e("VoiceViewHolder", "Voice URL is null")
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


                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(localFile.absolutePath)
                            prepare()
                        }

                        mediaPlayer
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
                                playAudio(localFile.absolutePath)

                            } else {
                                stopAudio()
                            }


                        }
                    }
                } catch (e: Exception) {
                    Log.e("VoiceViewHolder", "Failed to download audio file", e)
                }
            }
        }

        private fun playAudio(filePath: String) {
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(filePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        isplay = false
                        binding.play.text = "phát"
                        handler.removeCallbacks(updateWaveform)
                        binding.wave.progress = 0f

                    }
                    isplay = true
                    binding.play.text = "dừng"
                    handler.post(updateWaveform)
                } catch (e: IOException) {
                    Log.e("VoiceViewHolder", "Failed to play audio", e)
                }
            }
        }

        private fun stopAudio() {
            mediaPlayer?.stop()
            binding.play.text = "phát"
            isplay = false
            handler.removeCallbacks(updateWaveform)
            binding.wave.progress = 0f
            mediaPlayer?.reset()

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
                is ImageViewHolder -> holder.bind(post)

                is VoiceViewHolder -> holder.bind(post)
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
//
//
//    fun onIconClick(postId: String) {
//        viewModel.setContent(postId)
//    }

}
