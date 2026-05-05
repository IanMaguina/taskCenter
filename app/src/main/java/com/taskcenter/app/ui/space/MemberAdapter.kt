package com.taskcenter.app.ui.space

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskcenter.app.data.database.entity.SpaceMember
import com.taskcenter.app.databinding.ItemMemberBinding

class MemberAdapter : ListAdapter<SpaceMember, MemberAdapter.MemberViewHolder>(DiffCallback) {

    inner class MemberViewHolder(private val binding: ItemMemberBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(member: SpaceMember) {
            binding.textMemberName.text = member.userName
            binding.textMemberIp.text = "${member.deviceIp}:${member.devicePort}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SpaceMember>() {
        override fun areItemsTheSame(oldItem: SpaceMember, newItem: SpaceMember) =
            oldItem.spaceId == newItem.spaceId && oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: SpaceMember, newItem: SpaceMember) = oldItem == newItem
    }
}
