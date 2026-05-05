package com.taskcenter.app.ui.space

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.taskcenter.app.R
import com.taskcenter.app.databinding.FragmentSpaceDetailBinding
import com.taskcenter.app.viewmodel.SpaceViewModel
import com.taskcenter.app.viewmodel.TaskViewModel

class SpaceDetailFragment : Fragment() {
    private var _binding: FragmentSpaceDetailBinding? = null
    private val binding get() = _binding!!
    private val spaceViewModel: SpaceViewModel by viewModels()
    private val taskViewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter
    private var spaceId: String = ""
    private var isOwner = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpaceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spaceId = arguments?.getString("spaceId") ?: return

        taskAdapter = TaskAdapter(
            onItemClick = { task ->
                findNavController().navigate(
                    R.id.action_spaceDetailFragment_to_taskDetailFragment,
                    bundleOf("taskId" to task.id, "spaceId" to spaceId)
                )
            }
        )
        binding.recyclerTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTasks.adapter = taskAdapter

        binding.fabAddTask.setOnClickListener {
            findNavController().navigate(
                R.id.action_spaceDetailFragment_to_createTaskFragment,
                bundleOf("spaceId" to spaceId)
            )
        }

        spaceViewModel.loadSpace(spaceId)
        spaceViewModel.space.observe(viewLifecycleOwner) { space ->
            if (space != null) {
                binding.textSpaceTitle.text = space.name
                binding.textSpaceDescription.text = space.description
                binding.chipRewards.visibility =
                    if (space.hasRewards) View.VISIBLE else View.GONE
            }
        }

        taskViewModel.getTasksForSpace(spaceId).observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
            binding.emptyTasksView.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
        }

        taskViewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                taskViewModel.clearError()
            }
        }

        // Menu for members / delete space
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu_space_detail, menu)
            }
            override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
                R.id.action_members -> {
                    findNavController().navigate(
                        R.id.action_spaceDetailFragment_to_membersFragment,
                        bundleOf("spaceId" to spaceId)
                    )
                    true
                }
                R.id.action_delete_space -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete_space)
                        .setMessage(R.string.delete_space_confirm)
                        .setPositiveButton(R.string.delete) { _, _ ->
                            spaceViewModel.deleteSpace(spaceId)
                            findNavController().popBackStack()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                    true
                }
                else -> false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
