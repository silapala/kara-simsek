package com.example.proje.data.chat

import Chat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.proje.databinding.ItemChatBinding

class ChatAdapter(private val chatList: List<Chat>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {

        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {

        val chat = chatList[position]
        holder.bind(chat)
    }

    override fun getItemCount(): Int = chatList.size

    inner class ChatViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Chat) {

            binding.textViewMessage.text = chat.prompt
            if (chat.bitmap != null) {
                binding.imageViewMessage.setImageBitmap(chat.bitmap)
                binding.imageViewMessage.visibility = View.VISIBLE
            } else {
                binding.imageViewMessage.visibility = View.GONE
            }
        }
    }
}
