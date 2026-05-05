package com.taskcenter.app.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.taskcenter.app.R
import com.taskcenter.app.data.database.entity.TaskStatus
import com.taskcenter.app.databinding.FragmentTaskDetailBinding
import com.taskcenter.app.viewmodel.TaskViewModel

class TaskDetailFragment : Fragment() {
    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TaskViewModel by viewModels()
    private var taskId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taskId = arguments?.getString("taskId") ?: return

        viewModel.getTaskLive(taskId).observe(viewLifecycleOwner) { task ->
            if (task == null) return@observe
            binding.textTaskName.text = task.name
            binding.textTaskDescription.text = task.description.ifEmpty { "Sin descripción" }
            binding.textEstimatedTime.text = "Tiempo estimado: ${task.estimatedTime} min"

            if (task.reward > 0) {
                binding.textReward.visibility = View.VISIBLE
                binding.textReward.text = "🏆 Premio: ${task.reward} puntos"
            } else {
                binding.textReward.visibility = View.GONE
            }

            when (task.status) {
                TaskStatus.PENDING -> {
                    binding.textStatus.text = getString(R.string.status_pending)
                    binding.textAssignedTo.visibility = View.GONE
                    binding.textRealTime.visibility = View.GONE
                    binding.btnTakeTask.visibility = View.VISIBLE
                    binding.btnCompleteTask.visibility = View.GONE
                }
                TaskStatus.IN_PROGRESS -> {
                    binding.textStatus.text = getString(R.string.status_in_progress)
                    binding.textAssignedTo.visibility = View.VISIBLE
                    binding.textAssignedTo.text = "Asignado a: ${task.assignedToName}"
                    binding.textRealTime.visibility = View.GONE
                    binding.btnTakeTask.visibility = View.GONE
                    binding.btnCompleteTask.visibility = View.VISIBLE
                }
                TaskStatus.COMPLETED -> {
                    binding.textStatus.text = getString(R.string.status_completed)
                    binding.textAssignedTo.visibility = View.VISIBLE
                    binding.textAssignedTo.text = "Completado por: ${task.assignedToName}"
                    binding.textRealTime.visibility = View.VISIBLE
                    binding.textRealTime.text = "Tiempo real: ${task.realTime} min"
                    binding.btnTakeTask.visibility = View.GONE
                    binding.btnCompleteTask.visibility = View.GONE
                }
            }
        }

        binding.btnTakeTask.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.take_task)
                .setMessage(R.string.take_task_confirm)
                .setPositiveButton(R.string.take) { _, _ -> viewModel.takeTask(taskId) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.btnCompleteTask.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.complete_task)
                .setMessage(R.string.complete_task_confirm)
                .setPositiveButton(R.string.complete) { _, _ -> viewModel.completeTask(taskId) }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
