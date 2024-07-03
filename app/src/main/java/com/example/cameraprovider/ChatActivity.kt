package com.example.cameraprovider

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cameraprovider.adapter.friendlist_chatAdapter
import com.example.cameraprovider.databinding.ActivityChatBinding
import com.example.cameraprovider.repository.UserRepository
import com.example.cameraprovider.viewmodel.AuthViewModel
import com.example.cameraprovider.viewmodel.AuthViewModelFactory
import com.example.cameraprovider.viewmodel.FriendViewmodel
import com.example.cameraprovider.viewmodel.MessageViewModel
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var friendsAdapter: friendlist_chatAdapter
    private val messageViewModel: MessageViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(
            UserRepository(),
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        binding.lifecycleOwner = this

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        friendsAdapter = friendlist_chatAdapter(this, listOf(), mapOf(), onGetIdfriend = { userId, nameUser, avatarUser ->
            openChatWithFriend(userId, nameUser, avatarUser)
        }, updatestate = {userId -> messageViewModel.updateMessagesToSeen(userId)})
        binding.recyclerView.adapter = friendsAdapter


        messageViewModel.friendsWithMessages.observe(this, Observer { friendsAndMessages ->
            val (friends, lastMessages) = friendsAndMessages
            friendsAdapter.updateFriends(friends, lastMessages)
            friendsAdapter.notifyDataSetChanged()
        })
        messageViewModel.fetchFriendsWithLastMessages()


        binding.chatbot.setOnClickListener {
            val intent = Intent(this, ItemChatActivity::class.java).apply {
                putExtra("FRIEND_ID", "Gemini")
                putExtra("FRIEND_NAME", "")
                putExtra("FRIEND_AVATAR", "")
            }
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_in_down, R.anim.slide_out_down
            )
            startActivity(intent,options.toBundle())
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