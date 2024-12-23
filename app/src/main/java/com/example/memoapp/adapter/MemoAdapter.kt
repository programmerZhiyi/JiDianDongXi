package com.example.memoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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

    override fun onViewRecycled(holder: MemoViewHolder) {
        super.onViewRecycled(holder)
        // 清理图片资源
        Glide.with(holder.itemView.context).clear(holder.ivThumbnail)
    }

    inner class MemoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val btnDelete: View = itemView.findViewById(R.id.btnDelete)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)

        fun bind(memo: Memo) {
            if (memo.title.isNotEmpty()) {
                tvTitle.text = memo.title
                tvTitle.visibility = View.VISIBLE
            } else {
                tvTitle.visibility = View.GONE
            }

            val displayContent = memo.content.replace("\\[image\\].*?\\[/image\\]".toRegex(), "[图片]")
            
            tvContent.text = if (displayContent.length > 50) {
                displayContent.substring(0, 50) + "..."
            } else {
                displayContent
            }
            
            tvDate.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(memo.createdAt)
            
            btnDelete.setOnClickListener { onDeleteClick(memo) }
            itemView.setOnClickListener { onItemClick(memo) }

            if (memo.imagePath.isNotEmpty()) {
                ivThumbnail.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(memo.imagePath)
                    .into(ivThumbnail)
            } else {
                ivThumbnail.visibility = View.GONE
            }
        }
    }
}

class MemoDiffCallback : DiffUtil.ItemCallback<Memo>() {
    override fun areItemsTheSame(oldItem: Memo, newItem: Memo) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Memo, newItem: Memo) = oldItem == newItem
} 