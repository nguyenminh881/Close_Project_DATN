package com.example.cameraprovider.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cameraprovider.databinding.PostRowBinding
import com.example.cameraprovider.model.Post

class PostAdapter(val context: Context, val PostList: List<Post>) :
    RecyclerView.Adapter<PostAdapter.MyViewHolder>() {

    lateinit var postRowBinding: PostRowBinding

    class MyViewHolder(val postRowBinding: PostRowBinding) :
        RecyclerView.ViewHolder(postRowBinding.root) {
        fun bind(post: Post) {
//            Glide.with(postRowBinding.imageViewPost.context)
//                .load(post.imageURL)
//                .into(postRowBinding.imageViewPost);
            postRowBinding.postxml = post

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        postRowBinding = PostRowBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(postRowBinding)
    }

    override fun getItemCount(): Int {
        return PostList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val post = PostList.get(position)
        holder.bind(post)
    }


}