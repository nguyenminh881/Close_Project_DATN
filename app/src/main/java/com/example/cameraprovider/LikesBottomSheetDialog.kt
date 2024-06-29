package com.example.cameraprovider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cameraprovider.adapter.viewItemLikeAdapter
import com.example.cameraprovider.databinding.LikesBottomSheetDialogBinding
import com.example.cameraprovider.viewmodel.PostViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class LikesBottomSheetDialog(private val postId: String, private val viewModel: PostViewModel) : BottomSheetDialogFragment() {
    private lateinit var binding: LikesBottomSheetDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LikesBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.likesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel.getLikes(postId).observe(viewLifecycleOwner) { likesData ->
            val adapter = viewItemLikeAdapter(likesData)
            binding.likesRecyclerView.adapter = adapter
        }
    }
}