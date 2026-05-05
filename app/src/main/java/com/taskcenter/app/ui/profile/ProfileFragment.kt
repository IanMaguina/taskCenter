package com.taskcenter.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.taskcenter.app.databinding.FragmentProfileBinding
import com.taskcenter.app.service.TaskCenterService
import com.taskcenter.app.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show device IP
        binding.textDeviceIp.text = "IP: ${TaskCenterService.getDeviceIp(requireContext())}:${TaskCenterService.SERVER_PORT}"

        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null && binding.editUserName.text.isNullOrEmpty()) {
                binding.editUserName.setText(user.name)
            }
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.editUserName.text?.toString()?.trim() ?: ""
            viewModel.saveProfile(name)
        }

        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                Snackbar.make(binding.root, "Perfil guardado", Snackbar.LENGTH_SHORT).show()
                viewModel.clearSaved()
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
