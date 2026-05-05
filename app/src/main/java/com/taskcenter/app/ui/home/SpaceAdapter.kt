package com.taskcenter.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskcenter.app.data.database.entity.Space
import com.taskcenter.app.databinding.ItemSpaceBinding

class SpaceAdapter(private val onItemClick: (Space) -> Unit) :
    ListAdapter<Space, SpaceAdapter.SpaceViewHolder>(DiffCallback) {

    inner class SpaceViewHolder(private val binding: ItemSpaceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(space: Space) {
            binding.textSpaceName.text = space.name
            binding.textSpaceOwner.text = "Creado por: ${space.ownerName}"
            binding.textSpaceDescription.text = space.description.ifEmpty { "Sin descripción" }
            binding.chipRewards.visibility =
                if (space.hasRewards) android.view.View.VISIBLE else android.view.View.GONE
            binding.root.setOnClickListener { onItemClick(space) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpaceViewHolder {
        val binding = ItemSpaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SpaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SpaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Space>() {
        override fun areItemsTheSame(oldItem: Space, newItem: Space) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Space, newItem: Space) = oldItem == newItem
    }
}
