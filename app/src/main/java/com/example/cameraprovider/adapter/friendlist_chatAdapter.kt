package com.example.cameraprovider.adapter

import android.content.Context
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cameraprovider.R
import com.example.cameraprovider.databinding.FriendlistChatItemBinding
import com.example.cameraprovider.model.Friendship
import com.example.cameraprovider.model.Message
import com.example.cameraprovider.model.MessageStatus
import com.example.cameraprovider.model.User
import com.example.cameraprovider.viewmodel.FriendViewmodel
import com.example.cameraprovider.viewmodel.MessageViewModel
import com.github.marlonlom.utilities.timeago.TimeAgo
import kotlinx.coroutines.flow.last
import org.ocpsoft.prettytime.PrettyTime
import java.util.Date
import java.util.Locale


class friendlist_chatAdapter( private val context: Context,
                              private var friends: List<User>,
                              private var lastMessages: Map<String, Message>,
                              private val onGetIdfriend: (String, String, String) -> Unit,
                              private val updatestate: (String) -> Unit,
    private val viewModel: MessageViewModel) :
    RecyclerView.Adapter<friendlist_chatAdapter.MyViewHolder>() {
    class MyViewHolder(private val binding: FriendlistChatItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User, lastMessage: Message?, onGetIdfriend: (String, String, String) -> Unit,updatestate: (String) -> Unit,viewModel: MessageViewModel) {
            Glide.with(binding.root)
                .load(user.avatarUser)
                .into(binding.avtRequest)
            binding.friend = user

            binding.viewFriend.setOnClickListener {
                Log.d("friendlist_chatAdapter", "User clicked on ${user.UserId}")
                onGetIdfriend(user.UserId, user.nameUser, user.avatarUser)
                updatestate(user.UserId)}


            // Xử lý tên người gửi tin nhắn
            val senderName = when {
                lastMessage != null && lastMessage.senderId == user.UserId -> {
                    val nameAfterSpace = user.nameUser.substringAfter(" ", user.nameUser)
                    "${nameAfterSpace}: "
                }
                lastMessage != null -> {
                    "Bạn: "
                }
                else ->"Hãy bắt gửi tới ${user.nameUser}"
            }
            val createdAtTimestamp = lastMessage?.createdAt?.toLongOrNull() ?: 0L
            val prettyTime = PrettyTime(Locale("vi"))
            val formattedTime = prettyTime.format(Date(createdAtTimestamp))

            binding.timeStamp.text =  if(lastMessage != null) formattedTime.replace(" trước", "").replace("cách đây ", "")
                .replace("giây", "vừa xong").replace("vài", "") else ""
            binding.nameLastUser.text = senderName
            

            val decodedMessage = lastMessage?.let { decodeMessage(it.message ?: "") }
            binding.lastMessage.text = decodedMessage



            if (lastMessage != null && lastMessage.status != MessageStatus.READ && lastMessage.senderId == user.UserId) {
                viewModel.incrementUnreadMessageCount()
            }


            if (lastMessage != null ){
                if( lastMessage.status == MessageStatus.SENT || lastMessage.status == MessageStatus.SENDING && lastMessage.senderId == user.UserId) {
                    binding.lastMessage.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                    binding.nameLastUser.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))

                } else {
                    binding.lastMessage.setTextColor(ContextCompat.getColor(binding.root.context, R.color.SameWhite))
                    binding.nameLastUser.setTextColor(ContextCompat.getColor(binding.root.context, R.color.SameWhite))

                }

            }else{
                binding.lastMessage.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                binding.nameLastUser.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
            }
        }
        private fun decodeMessage(message: String): String {
            return try {
                String(Base64.decode(message, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: IllegalArgumentException) {
                Log.e("MessageDecodeError", "Failed to decode message: $message", e)
                message
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = FriendlistChatItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val user = friends[position]
        val lastMessage = lastMessages[user.UserId]
        holder.bind(user, lastMessage, onGetIdfriend,updatestate,viewModel)
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

                return oldUser == newUser && oldMessage?.message == newMessage?.message
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)

        lastMessages = newLastMessages

        friends = newFriends.sortedByDescending { user ->
            newLastMessages[user.UserId]?.createdAt?.toLongOrNull() ?: 0L
        }

        diffResult.dispatchUpdatesTo(this)
        Log.d("updateFriends", "Updated lastMessages: $lastMessages")
        Log.d("updateFriends", "Updated friends: $friends")
    }
}