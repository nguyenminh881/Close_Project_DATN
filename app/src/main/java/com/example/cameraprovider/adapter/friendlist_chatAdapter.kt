package com.example.cameraprovider.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cameraprovider.databinding.FriendlistChatItemBinding
import com.example.cameraprovider.model.Friendship
import com.example.cameraprovider.model.Message
import com.example.cameraprovider.model.User
import com.example.cameraprovider.viewmodel.FriendViewmodel
import com.example.cameraprovider.viewmodel.MessageViewModel
import kotlinx.coroutines.flow.last


class friendlist_chatAdapter( private val context: Context,
                              private var friends: List<User>,
                              private var lastMessages: Map<String, Message>,
                              private val onGetIdfriend: (String, String, String) -> Unit) :
    RecyclerView.Adapter<friendlist_chatAdapter.MyViewHolder>() {
    class MyViewHolder(private val binding: FriendlistChatItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User, lastMessage: Message?, friendsUid: Set<String>, onGetIdfriend: (String, String, String) -> Unit) {
            Glide.with(binding.root)
                .load(user.avatarUser)
                .into(binding.avtRequest)
            binding.friend = user

            binding.viewFriend.setOnClickListener {  onGetIdfriend(user.UserId, user.nameUser, user.avatarUser) }
            val senderName = if (lastMessage != null && friendsUid.contains(lastMessage.senderId)) {
                user.nameUser+": "
            } else {
                "Bạn: "
            }

            binding.nameLastUser.text = senderName
            binding.lastMessage.text = lastMessage?.message ?: ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = FriendlistChatItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val user = friends[position]
        val lastMessage = lastMessages[user.UserId]
        val friendsUid = friends.map { it.UserId }.toSet()
        holder.bind(user, lastMessage,friendsUid, onGetIdfriend)
    }

    override fun getItemCount(): Int {
        return friends.size
    }

    fun updateFriends(newFriends: List<User>, newLastMessages: Map<String, Message>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = friends.size

            override fun getNewListSize() = newFriends.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return friends[oldItemPosition].UserId == newFriends[newItemPosition].UserId
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldUser = friends[oldItemPosition]
                val newUser = newFriends[newItemPosition]
                val oldMessage = lastMessages[oldUser.UserId]
                val newMessage = newLastMessages[newUser.UserId]

                return oldUser == newUser && oldMessage == newMessage
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        friends = newFriends.sortedByDescending { user ->
            newLastMessages[user.UserId]?.createdAt?.toLongOrNull() ?: 0L
        }
        lastMessages = newLastMessages
        diffResult.dispatchUpdatesTo(this)
    }
}