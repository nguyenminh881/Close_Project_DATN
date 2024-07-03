package com.example.cameraprovider.Admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cameraprovider.R
import com.example.cameraprovider.databinding.FragmentUsersBinding
import com.example.cameraprovider.viewmodel.PostViewModel

class UsersFragment : Fragment() {

    private lateinit var binding:FragmentUsersBinding
    private lateinit var adminViewModel: AdminViewModel
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_users, container, false)
        adminViewModel = ViewModelProvider(requireActivity()).get(AdminViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        userAdapter =UserAdapter(emptyList())

        binding.ryc.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        adminViewModel.userList.observe(viewLifecycleOwner) { users ->
            userAdapter.updateUsers(users)
            binding.title.text = "Tổng số người dùng (${users.size})"
        }

        adminViewModel.getUserkhongavtvsten()
    }
}