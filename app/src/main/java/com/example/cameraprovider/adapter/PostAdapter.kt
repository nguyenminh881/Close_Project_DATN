package com.example.cameraprovider.adapter
//
//import android.content.Context
//import android.media.MediaPlayer
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.ViewGroup
//import androidx.core.net.toUri
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.example.cameraprovider.databinding.PostRowBinding
//import com.example.cameraprovider.databinding.PostRowVoiceBinding
//import com.example.cameraprovider.model.Post
//import com.google.firebase.Timestamp
//import com.google.firebase.storage.FirebaseStorage
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//import rm.com.audiowave.OnProgressListener
//import java.io.File
//import java.io.IOException
//import java.text.SimpleDateFormat
//
//
//class PostAdapter(val context: Context, var postList: MutableList<Post>) :
//    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//
//    companion object {
//        const val VIEW_TYPE_IMAGE = 0
//        const val VIEW_TYPE_VOICE = 1
//    }
//
//    class ImageViewHolder(val postRowBinding: PostRowBinding) :
//        RecyclerView.ViewHolder(postRowBinding.root) {
//        fun bind(post: Post) {
//            Glide.with(postRowBinding.imgAvtUserPost.context)
//                .load(post.userAvatar)
//                .into(postRowBinding.imgAvtUserPost);
//
//            Glide.with(postRowBinding.imageViewPost.context)
//                .load(post.imageURL)
//                .into(postRowBinding.imageViewPost);
//
//
//            postRowBinding.postxml = post
//        }
//    }
//
//    class VoiceViewHolder(val postRowVoiceBinding: PostRowVoiceBinding) :
//        RecyclerView.ViewHolder(postRowVoiceBinding.root) {
//        // Play the audio file
//        var mediaPlayer = MediaPlayer()
//        var isplay: Boolean = false
//        private val handler = Handler(Looper.getMainLooper())
//        private var time = 0
//
//        init {
//            setupWaveformView()
//        }
//
//        fun bind(post: Post) {
//            Glide.with(postRowVoiceBinding.imgAvtUserPost.context)
//                .load(post.userAvatar)
//                .into(postRowVoiceBinding.imgAvtUserPost)
//            // Load and play audio file
//
//
//            post.voiceURL?.let { url ->
//                downloadAudio(url)
//            } ?: run {
//                Log.e("VoiceViewHolder", "Voice URL is null")
//            }
//            postRowVoiceBinding.postvoicexml = post
//        }
//
//
//        private fun downloadAudio(url: String) {
//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
//                    val localFile = File.createTempFile("audio", "aac")
//                    storageRef.getFile(localFile).await()
//
//                    withContext(Dispatchers.Main) {
//                        Log.d("VoiceViewHolder", "Audio file downloaded successfully")
//                        postRowVoiceBinding.wave.setRawData(localFile.readBytes())
//
//
//                        mediaPlayer = MediaPlayer().apply {
//                            setDataSource(localFile.absolutePath)
//                            prepare()
//                        }
//
//                        mediaPlayer
//                        Log.d("TAGY", "${mediaPlayer.duration}")
//                        time = (mediaPlayer.duration / 1000).toInt()
//                        if (time == 60) {
//                            postRowVoiceBinding.tvTimer.text = "01:00"
//                        } else if (time >= 10) {
//                            postRowVoiceBinding.tvTimer.text = "00:${time.toString()}"
//                        } else {
//                            postRowVoiceBinding.tvTimer.text = "00:0${time.toString()}"
//                        }
//                        postRowVoiceBinding.play.setOnClickListener {
//
//                            if (!isplay) {
//                                playAudio(localFile.absolutePath)
//
//                            } else {
//                                stopAudio()
//                            }
//
//
//                        }
//                    }
//                } catch (e: Exception) {
//                    Log.e("VoiceViewHolder", "Failed to download audio file", e)
//                }
//            }
//        }
//
//
//        private fun playAudio(filePath: String) {
//            mediaPlayer = MediaPlayer().apply {
//                try {
//                    setDataSource(filePath)
//                    prepare()
//                    start()
//                    setOnCompletionListener {
//                        mediaPlayer?.stop()
//                        mediaPlayer?.release()
//                        isplay = false
//                        postRowVoiceBinding.play.text = "phát"
//                        handler.removeCallbacks(updateWaveform)
//                        postRowVoiceBinding.wave.progress = 0f
//
//                    }
//                    isplay = true
//                    postRowVoiceBinding.play.text = "dừng"
//                    handler.post(updateWaveform)
//                } catch (e: IOException) {
//                    Log.e("VoiceViewHolder", "Failed to play audio", e)
//                }
//            }
//        }
//
//        private fun stopAudio() {
//            mediaPlayer?.stop()
//            postRowVoiceBinding.play.text = "phát"
//            isplay = false
//            handler.removeCallbacks(updateWaveform)
//            postRowVoiceBinding.wave.progress = 0f
//            mediaPlayer?.reset()
//
//        }
//
//        private val updateWaveform = object : Runnable {
//            override fun run() {
//                mediaPlayer?.let { player ->
//                    if (isplay) {
//                        val progress = (player.currentPosition.toFloat() / player.duration) * 100
//                        Log.d("TAGY", "$progress , ${player.duration}")
//                        postRowVoiceBinding.wave.progress = progress
//                        handler.postDelayed(this, 1)
//                    }
//
//                }
//            }
//        }
//
//        private fun setupWaveformView() {
//            postRowVoiceBinding.wave.onProgressListener = object : OnProgressListener {
//                override fun onProgressChanged(progress: Float, byUser: Boolean) {
//                    Log.d("TAGY", "Progress set: $progress, and it's $byUser that user did this")
//                    if (byUser && isplay) {
//                        val seekToPosition = (mediaPlayer!!.duration * progress / 100.0).toInt()
////                        postRowVoiceBinding.wave.progress = progress
//                        Log.d("TAGY", "Seeking to position: $seekToPosition")
//                        mediaPlayer?.seekTo(seekToPosition)
//                    }
//                }
//
//                override fun onStartTracking(progress: Float) {
//                    Log.d("TAGY", "Started tracking from $progress")
//                }
//
//                override fun onStopTracking(progress: Float) {
//                    if (isplay) {
//                        val seekToPosition = (mediaPlayer!!.duration * progress / 100.0).toInt()
////                        postRowVoiceBinding.wave.progress = progress
//                        Log.d("TAGY", "Seeking to position: $seekToPosition")
//                        mediaPlayer?.seekTo(seekToPosition)
//                    }
//                }
//            }
//        }
//
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        return if (viewType == VIEW_TYPE_IMAGE) {
//            val postRowBinding = PostRowBinding.inflate(LayoutInflater.from(context), parent, false)
//            Log.d("PostAdapter", "Creating ImageViewHolder")
//            ImageViewHolder(postRowBinding)
//        } else {
//            val postRowVoiceBinding =
//                PostRowVoiceBinding.inflate(LayoutInflater.from(context), parent, false)
//            Log.d("PostAdapter", "Creating VoiceViewHolder")
//            VoiceViewHolder(postRowVoiceBinding)
//        }
//    }
//
//    override fun getItemCount(): Int {
//        return postList.size
//    }
//
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val post = postList[position]
//        when (holder) {
//            is ImageViewHolder -> {
//                Log.d("PostAdapter", "Binding ImageViewHolder at position $position")
//                holder.bind(post)
//            }
//
//            is VoiceViewHolder -> {
//                Log.d("PostAdapter", "Binding VoiceViewHolder at position $position")
//                holder.bind(post)
//            }
//        }
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return if (postList[position].imageURL != "" && postList[position].voiceURL == "") {
//            VIEW_TYPE_IMAGE
//        } else {
//            VIEW_TYPE_VOICE
//        }
//    }
//
//    fun updateData(newPostList: MutableList<Post>) {
//        val startPosition = postList.size
//        postList.addAll(newPostList)
//        notifyItemRangeInserted(startPosition, newPostList.size)
//    }
//
//
//}
