package com.example.cameraprovider

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.SnapHelper
import com.example.cameraprovider.adapter.PostAdapter
import com.example.cameraprovider.databinding.ActivityPostListBinding
import com.example.cameraprovider.model.Post

class PostList : AppCompatActivity() {

    private lateinit var binding: ActivityPostListBinding
    lateinit var postList: MutableList<Post>
      lateinit var postApdapter:PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_post_list)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.recyclerView)
        val postList = ArrayList<Post>()
        postList.add(Post(1, "minh", R.drawable.bg_post, "Long si é, một con vịt ", R.drawable.bg_post, null, null, null))
//        postList.add(Post(1,"minh",R.drawable.bg_post,"Long si é, một con vịt ",R.drawable.bg_post,null,null,null))
//
//
        postApdapter = PostAdapter(this,postList)
        binding.recyclerView.adapter = postApdapter

    }
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down)
    }
}