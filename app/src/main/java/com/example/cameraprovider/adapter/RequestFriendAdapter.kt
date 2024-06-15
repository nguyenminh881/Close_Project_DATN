package com.example.cameraprovider.adapter

import android.content.Context
import android.view.LayoutInflater

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cameraprovider.databinding.ItemRequestFriendBinding
import com.example.cameraprovider.databinding.PostRowBinding
import com.example.cameraprovider.model.Friendship

class RequestFriendAdapter(
    private val context:Context,
    private val friendRequests: List<Friendship>,
    private val onAcceptClick: (Friendship) -> Unit,
    private val onDeclineClick: (Friendship) -> Unit
) : RecyclerView.Adapter<RequestFriendAdapter.MyViewHolder>() {

    lateinit var itemRequestFriendBinding: ItemRequestFriendBinding

    class MyViewHolder(val itemRequestFriendBinding: ItemRequestFriendBinding) :
        RecyclerView.ViewHolder(itemRequestFriendBinding.root) {
        fun bind(friendship: Friendship, onAcceptClick: (Friendship) -> Unit, onDeclineClick: (Friendship) -> Unit) {
            itemRequestFriendBinding.frRequest = friendship
            itemRequestFriendBinding.btnAccept.setOnClickListener { onAcceptClick(friendship) }
            itemRequestFriendBinding.btnUnaccept.setOnClickListener { onDeclineClick(friendship) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val  binding = ItemRequestFriendBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return friendRequests.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val friendship = friendRequests.get(position)
        holder.bind(friendship, onAcceptClick, onDeclineClick)
    }
}