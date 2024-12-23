package com.example.memoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.memoapp.R
import com.example.memoapp.data.Memo
import java.text.SimpleDateFormat
import java.util.Locale

class MemoAdapter(
    private val onDeleteClick: (Memo) -> Unit,
    private val onItemClick: (Memo) -> Unit
) : ListAdapter<Memo, MemoAdapter.MemoViewHolder>(MemoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memo, parent, false)
        return MemoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val btnDelete: View = itemView.findViewById(R.id.btnDelete)

        fun bind(memo: Memo) {
            tvTitle.text = memo.title
            tvContent.text = if (memo.content.length > 50) {
                memo.content.substring(0, 50) + "..."
            } else {
                memo.content
            }
            tvDate.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(memo.createdAt)
            
            btnDelete.setOnClickListener { onDeleteClick(memo) }
            itemView.setOnClickListener { onItemClick(memo) }
        }
    }
}

class MemoDiffCallback : DiffUtil.ItemCallback<Memo>() {
    override fun areItemsTheSame(oldItem: Memo, newItem: Memo) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Memo, newItem: Memo) = oldItem == newItem
} 