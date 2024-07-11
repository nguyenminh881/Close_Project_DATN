package com.example.cameraprovider.bottomdialogai

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup // Thay thế bằng package và tên binding class của bạn
import androidx.lifecycle.ViewModelProvider
import com.example.cameraprovider.databinding.LayoutPromtImgBinding
import com.example.cameraprovider.viewmodel.PostViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PromptDialog(private val imageBitmap: Bitmap) : BottomSheetDialogFragment() {

    private lateinit var binding: LayoutPromtImgBinding
    private lateinit var postViewModel: PostViewModel
    private var updatingText = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutPromtImgBinding.inflate(inflater, container, false)
        postViewModel = ViewModelProvider(requireActivity()).get(PostViewModel::class.java)
        binding.viewModel = postViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.genaAgain.apply {
            setOnClickListener {


                binding.genaAgain.isEnabled = false
                postViewModel.generateContent(
                    imageBitmap
                )
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.genaAgain.isEnabled = true
                }, 4000)
                text = "Tạo lại"

            }


            binding.btnOk.setOnClickListener {
                dismiss()
            }


        }

    }
}