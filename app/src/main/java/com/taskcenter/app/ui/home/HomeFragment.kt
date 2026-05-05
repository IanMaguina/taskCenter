package com.taskcenter.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.taskcenter.app.R
import com.taskcenter.app.databinding.FragmentHomeBinding
import com.taskcenter.app.viewmodel.HomeViewModel

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: SpaceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SpaceAdapter { space ->
            findNavController().navigate(
                R.id.action_homeFragment_to_spaceDetailFragment,
                bundleOf("spaceId" to space.id)
            )
        }
        binding.recyclerSpaces.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSpaces.adapter = adapter

        binding.fabCreateSpace.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createSpaceFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        viewModel.allSpaces.observe(viewLifecycleOwner) { spaces ->
            adapter.submitList(spaces)
            binding.emptyView.visibility = if (spaces.isEmpty()) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
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
