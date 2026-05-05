package com.taskcenter.app.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.taskcenter.app.databinding.FragmentCreateTaskBinding
import com.taskcenter.app.viewmodel.SpaceViewModel
import com.taskcenter.app.viewmodel.TaskViewModel

class CreateTaskFragment : Fragment() {
    private var _binding: FragmentCreateTaskBinding? = null
    private val binding get() = _binding!!
    private val taskViewModel: TaskViewModel by viewModels()
    private val spaceViewModel: SpaceViewModel by viewModels()
    private var spaceId: String = ""
    private var hasRewards: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spaceId = arguments?.getString("spaceId") ?: return

        // Check if space has rewards to show reward field
        spaceViewModel.loadSpace(spaceId)
        spaceViewModel.space.observe(viewLifecycleOwner) { space ->
            hasRewards = space?.hasRewards ?: false
            binding.layoutReward.visibility = if (hasRewards) View.VISIBLE else View.GONE
        }

        binding.btnCreateTask.setOnClickListener {
            val name = binding.editTaskName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                binding.layoutTaskName.error = "El nombre es requerido"
                return@setOnClickListener
            }
            val description = binding.editTaskDescription.text?.toString()?.trim() ?: ""
            val estimatedStr = binding.editEstimatedTime.text?.toString()?.trim() ?: ""
            val estimated = estimatedStr.toIntOrNull() ?: 0
            val reward = if (hasRewards) {
                binding.editReward.text?.toString()?.trim()?.toIntOrNull() ?: 0
            } else 0

            taskViewModel.createTask(spaceId, name, description, estimated, reward)
        }

        taskViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnCreateTask.isEnabled = !loading
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        taskViewModel.taskSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                taskViewModel.clearTaskSaved()
                findNavController().popBackStack()
            }
        }

        taskViewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                taskViewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
