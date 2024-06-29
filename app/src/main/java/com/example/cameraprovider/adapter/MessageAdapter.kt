package com.example.cameraprovider.adapter

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cameraprovider.R
import com.example.cameraprovider.databinding.ItemReceiMessBinding
import com.example.cameraprovider.databinding.ItemSendMessBinding
import com.example.cameraprovider.model.Message
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MessageAdapter(
    private val userId: String,
    private val image: String,
    private var messages: List<Message>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SENT = 0
        const val VIEW_TYPE_RECEIVED = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages.isNotEmpty() && messages[position].senderId == userId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemSendMessBinding.inflate(inflater, parent, false)
                SentMessageViewHolder(binding)
            }

            VIEW_TYPE_RECEIVED -> {
                val binding = ItemReceiMessBinding.inflate(inflater, parent, false)
                ReceivedMessageViewHolder(binding)
            }

            else -> throw IllegalArgumentException("loi khong the inflate")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message, image)
            is ReceivedMessageViewHolder -> holder.bind(message, image)
        }
    }

    override fun getItemCount() = messages.size

    class SentMessageViewHolder(private val binding: ItemSendMessBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var mediaPlayer = MediaPlayer()
        private val handler = Handler(Looper.getMainLooper())
        private var isplay: Boolean = false
        fun bind(message: Message, image: String) {

            if (message.imageUrl == "") {
                binding.comment.visibility = View.GONE
            } else {
                binding.comment.visibility = View.VISIBLE
                Glide.with(binding.root).load(message.imageUrl).into(binding.imageViewPost)
                Glide.with(binding.root).load(message.avtpost).into(binding.avtpost)
                if (message.content == "") {
                    binding.tvDescPost.visibility = View.GONE
                }
            }



            if (message.voiceUrl == "") {
                binding.voiceCmt.visibility = View.GONE
            } else {
                binding.voiceCmt.visibility = View.VISIBLE

                Glide.with(binding.root).load(message.avtpost).into(binding.avtuser)
                message.voiceUrl?.let { downloadAudio(it) }
                if (message.content == "") {
                    binding.txtcontent.visibility = View.GONE
                }
            }
            binding.chat = message
            binding.executePendingBindings()
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
    }

    class ReceivedMessageViewHolder(private val binding: ItemReceiMessBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var mediaPlayer = MediaPlayer()
        private val handler = Handler(Looper.getMainLooper())
        private var isplay: Boolean = false
        fun bind(message: Message, image: String) {
            if (message.imageUrl == "") {
                binding.comment.visibility = View.GONE
            } else {
                binding.comment.visibility = View.VISIBLE
                Glide.with(binding.root).load(message.avtpost).into(binding.avtpost)
                Glide.with(binding.root).load(message.imageUrl).into(binding.imageViewPost)
                if (message.content == "") {
                    binding.tvDescPost.visibility = View.GONE
                }
            }


            if (message.voiceUrl == "") {
                binding.voiceCmt.visibility = View.GONE
            } else {
                Glide.with(binding.root).load(message.avtpost).into(binding.avtuser)
                binding.voiceCmt.visibility = View.VISIBLE
                message.voiceUrl?.let { downloadAudio(it) }
                if (message.content == "") {
                    binding.txtcontent.visibility = View.GONE
                }

            }

            if( message.senderId == "Gemini"){
                binding.avtRequest.setImageResource(R.drawable.ic_chatbot)
            }else{
                Glide.with(binding.root).load(image).into(binding.avtRequest)
            }
            binding.chat = message
            binding.executePendingBindings()
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
    }

    fun submitList(newMessages: List<Message>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = messages.size
            override fun getNewListSize(): Int = newMessages.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition].messageId == newMessages[newItemPosition].messageId
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition] == newMessages[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }
}
