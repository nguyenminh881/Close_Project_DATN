package com.example.cameraprovider

import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup // Thay thế bằng package và tên binding class của bạn
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.cameraprovider.databinding.LayoutPromtImgBinding
import com.example.cameraprovider.viewmodel.PostViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PromptDialog(private val imageBitmap: Bitmap) : BottomSheetDialogFragment() {

    private lateinit var binding:LayoutPromtImgBinding
    private lateinit var postViewModel: PostViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutPromtImgBinding.inflate(inflater, container, false)
        postViewModel = ViewModelProvider(requireActivity()).get(PostViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.genaAgain.apply {
            setOnClickListener{
                binding.tvGeneratedContent.text = "Quá trình xử lý có thể mất thời gian, vui lòng đợi!"
                postViewModel.generateContent(imageBitmap)
                postViewModel.contentgena.observe(viewLifecycleOwner){
                    binding.tvGeneratedContent.text =it?:"Vui lòng thử lại"
                }
                text = "Tạo lại"
        }


        }

        binding.btnOk.setOnClickListener{
            dismiss()
        }
    }
}