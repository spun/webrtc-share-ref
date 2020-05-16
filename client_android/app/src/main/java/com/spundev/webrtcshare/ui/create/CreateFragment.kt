package com.spundev.webrtcshare.ui.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.spundev.webrtcshare.databinding.FragmentCreateBinding
import com.spundev.webrtcshare.utils.autoCleared

/**
 * A simple [Fragment] subclass.
 */
class CreateFragment : Fragment() {

    // View binding
    private var binding by autoCleared<FragmentCreateBinding>()

    // ViewModel
    private val viewModel: CreateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // RecyclerView
        val detailsAdapter = CreateAdapter(requireContext())
        binding.createMessagesRecyclerView.apply {
            adapter = detailsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }

        // Notify if we are connected and enable or disable the button to send messages
        viewModel.isConnected.observe(viewLifecycleOwner, Observer { isConnected ->
            binding.createIsConnectedState.text = isConnected.toString()
            binding.createSendMessageButton.isEnabled = isConnected
        })

        // Update the RecyclerView values
        viewModel.messages.observe(viewLifecycleOwner, Observer {
            detailsAdapter.submitList(it)
        })

        // Send message button
        binding.createSendMessageButton.setOnClickListener {
            viewModel.sendMessage("Eagle")
        }
    }
}