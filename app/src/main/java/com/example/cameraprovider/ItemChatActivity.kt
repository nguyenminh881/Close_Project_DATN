package com.example.cameraprovider

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.cameraprovider.adapter.MessageAdapter
import com.example.cameraprovider.databinding.ActivityItemChatBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory
import com.example.cameraprovider.viewmodel.MessageViewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ItemChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            UserRepository(),
            this
        )
    }
    private val messviewModel: MessageViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_item_chat)

        val friendId = intent.getStringExtra("FRIEND_ID") ?: return
        val friendName = intent.getStringExtra("FRIEND_NAME") ?: return
        val friendAvatar = intent.getStringExtra("FRIEND_AVATAR") ?: return



        if (friendId == "Gemini") {
            binding.namefr.text = "Chat bot"
            binding.avtRequest.setImageResource(R.drawable.ic_chatbot)
            binding.avtRequest.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.avtRequest.strokeWidth = 0f
        } else {
            binding.namefr.text = friendName
            Glide.with(this).load(friendAvatar).into(binding.avtRequest)
        }


        val currentId = authViewModel.getCurrentId()
        messageAdapter = MessageAdapter(currentId, friendAvatar, listOf())
        binding.rcvFromchat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rcvFromchat.adapter = messageAdapter

        binding.lifecycleOwner = this
        binding.messageVmdol = messviewModel
        messviewModel.getMessages(currentId, friendId.toString())
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                messviewModel.messages.collect { messages ->
                    messageAdapter.submitList(messages)
                    binding.rcvFromchat.layoutManager?.scrollToPosition(messageAdapter.itemCount - 1)
                }
            }
        }

        messviewModel.messagesend.observe(this){
            if (it?.length!! > 0 ) {
                binding.btnSend.isEnabled = true
                binding.btnSend.backgroundTintList = ActivityCompat.getColorStateList(baseContext, R.color.color_active)
            }else{
                binding.btnSend.isEnabled = false
                binding.btnSend.backgroundTintList = ActivityCompat.getColorStateList(baseContext, R.color.bg_input)
            }
        }

        binding.btnSend.setOnClickListener {
            if (friendId == "Gemini") {
                messviewModel.sendMessageToGemini()
            } else {
                messviewModel.sendMessage("", friendId, "", "", "", "", "")
            }
            binding.message.text.clear()

        }

    }
}
