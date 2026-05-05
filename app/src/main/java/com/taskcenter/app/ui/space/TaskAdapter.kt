package com.taskcenter.app.ui.space

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskcenter.app.R
import com.taskcenter.app.data.database.entity.Task
import com.taskcenter.app.data.database.entity.TaskStatus
import com.taskcenter.app.databinding.ItemTaskBinding

class TaskAdapter(private val onItemClick: (Task) -> Unit) :
    ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallback) {

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.textTaskName.text = task.name
            binding.textTaskDescription.text = task.description.ifEmpty { "Sin descripción" }
            binding.textEstimatedTime.text = "${task.estimatedTime} min estimado"

            when (task.status) {
                TaskStatus.PENDING -> {
                    binding.chipStatus.text = binding.root.context.getString(R.string.status_pending)
                    binding.chipStatus.chipBackgroundColor =
                        ContextCompat.getColorStateList(binding.root.context, R.color.status_pending)
                }
                TaskStatus.IN_PROGRESS -> {
                    binding.chipStatus.text = binding.root.context.getString(R.string.status_in_progress)
                    binding.chipStatus.chipBackgroundColor =
                        ContextCompat.getColorStateList(binding.root.context, R.color.status_in_progress)
                    binding.textAssignedTo.text = "Asignado a: ${task.assignedToName ?: "?"}"
                    binding.textAssignedTo.visibility = android.view.View.VISIBLE
                }
                TaskStatus.COMPLETED -> {
                    binding.chipStatus.text = binding.root.context.getString(R.string.status_completed)
                    binding.chipStatus.chipBackgroundColor =
                        ContextCompat.getColorStateList(binding.root.context, R.color.status_completed)
                    binding.textRealTime.text = "${task.realTime} min real"
                    binding.textRealTime.visibility = android.view.View.VISIBLE
                }
            }

            if (task.reward > 0) {
                binding.chipReward.visibility = android.view.View.VISIBLE
                binding.chipReward.text = "🏆 ${task.reward} pts"
            } else {
                binding.chipReward.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onItemClick(task) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
