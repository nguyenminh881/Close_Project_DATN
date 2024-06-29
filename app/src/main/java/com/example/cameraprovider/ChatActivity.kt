package com.example.cameraprovider

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cameraprovider.adapter.friendlist_chatAdapter
import com.example.cameraprovider.databinding.ActivityChatBinding
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.FriendViewmodel
import com.example.cameraprovider.viewmodel.MessageViewModel

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var friendsAdapter: friendlist_chatAdapter
    private val messageViewModel: MessageViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        binding.lifecycleOwner = this

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        friendsAdapter = friendlist_chatAdapter(this, listOf(), mapOf(), onGetIdfriend = { userId, nameUser, avatarUser ->
            openChatWithFriend(userId, nameUser, avatarUser)
        })
        binding.recyclerView.adapter = friendsAdapter

        messageViewModel.friendList.observe(this) { friends ->
            messageViewModel.lastMessages.observe(this) { lastMessages ->
                friendsAdapter.updateFriends(friends, lastMessages)
            }
        }

        messageViewModel.getlistchats()

        binding.chatbot.setOnClickListener {
            val intent = Intent(this, ItemChatActivity::class.java).apply {
                putExtra("FRIEND_ID", "Gemini")
                putExtra("FRIEND_NAME", "")
                putExtra("FRIEND_AVATAR", "")
            }
            startActivity(intent)
        }
    }

    private fun openChatWithFriend(friendId: String, friendName: String, friendAvatar: String) {
        val intent = Intent(this, ItemChatActivity::class.java).apply {
            putExtra("FRIEND_ID", friendId)
            putExtra("FRIEND_NAME", friendName)
            putExtra("FRIEND_AVATAR", friendAvatar)
        }
        startActivity(intent)
    }


}