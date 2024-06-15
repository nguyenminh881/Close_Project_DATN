package com.example.cameraprovider

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.cameraprovider.databinding.FragmentFriendListBinding
import com.example.cameraprovider.viewmodel.FriendViewmodel
import com.example.cameraprovider.viewmodel.PostViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FriendListFragment:BottomSheetDialogFragment() {

    private lateinit var binding: FragmentFriendListBinding
    private lateinit var frViewModel: FriendViewmodel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_friend_list,container,false)
        frViewModel = ViewModelProvider(requireActivity()).get(FriendViewmodel::class.java)

        arguments?.getParcelable<Intent>("intent")?.let {
            frViewModel.handleFriendRequest(it)
        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSenlink.setOnClickListener {
            frViewModel.createDynamicLink()
        }

        frViewModel.dynamicLink.observe(viewLifecycleOwner, Observer { link ->
            link?.let {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "$link")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
                Log.d("dynamiclink", "Share intent started with link: $link")
            } ?: run {
                Log.d("dynamiclink", "Dynamic link is null")
            }
        })

        frViewModel.errorMessage.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
               Toast.makeText(requireContext(),"Vui lòng thử lại",Toast.LENGTH_SHORT)
            }
        })

        frViewModel.friendshipResult.observe(viewLifecycleOwner,Observer{frship ->
            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setMessage("Bạn đã gửi một yêu cầu kết bạn tới ${frship?.uid2}.")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, id ->
                    dialog.dismiss()
                }

            val alert = dialogBuilder.create()
            alert.setTitle("Yêu cầu kết bạn")
            alert.show()
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            setupFullHeight(bottomSheetDialog)
        }
        return dialog
    }

    private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(bottomSheet!!)
        val layoutParams = bottomSheet?.layoutParams

        val windowHeight = getWindowHeight()
        if (layoutParams != null) {
            layoutParams.height = windowHeight
        }
        bottomSheet?.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        data?.let {
//            frViewModel.handleFriendRequest(it)
//        }
//    }

}