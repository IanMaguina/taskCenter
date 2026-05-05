package com.taskcenter.app.ui.space

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.taskcenter.app.databinding.FragmentCreateSpaceBinding
import com.taskcenter.app.service.TaskCenterService
import com.taskcenter.app.viewmodel.SpaceViewModel

class CreateSpaceFragment : Fragment() {
    private var _binding: FragmentCreateSpaceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SpaceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateSpaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreateSpace.setOnClickListener {
            val name = binding.editSpaceName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                binding.layoutSpaceName.error = "El nombre es requerido"
                return@setOnClickListener
            }
            val description = binding.editSpaceDescription.text?.toString()?.trim() ?: ""
            val hasRewards = binding.switchRewards.isChecked
            val myIp = TaskCenterService.getDeviceIp(requireContext())
            viewModel.createSpace(name, description, hasRewards, myIp, TaskCenterService.SERVER_PORT)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnCreateSpace.isEnabled = !loading
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.createSuccess.observe(viewLifecycleOwner) { space ->
            if (space != null) {
                viewModel.clearCreateSuccess()
                findNavController().popBackStack()
            }
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
