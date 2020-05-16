package com.spundev.webrtcshare.ui.join

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spundev.webrtcshare.databinding.RvMessageItemBinding

class JoinAdapter internal constructor(
    context: Context
) : ListAdapter<String, JoinAdapter.MessageViewHolder>(object :
    DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
}) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding: RvMessageItemBinding = RvMessageItemBinding.inflate(inflater, parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) =
        holder.bindTo(getItem(position))

    inner class MessageViewHolder(
        private val binding: RvMessageItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindTo(item: String) {
            binding.messageTextView.text = item
        }
    }
}