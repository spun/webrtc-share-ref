package com.spundev.webrtcshare.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.spundev.webrtcshare.R
import com.spundev.webrtcshare.databinding.FragmentMainBinding
import com.spundev.webrtcshare.utils.autoCleared

/**
 * A simple [Fragment] subclass.
 */
class MainFragment : Fragment() {

    // View binding
    private var binding by autoCleared<FragmentMainBinding>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.goToCreateButton.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_create)
        }

        binding.goToJoinButton.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_join)
        }
    }

}