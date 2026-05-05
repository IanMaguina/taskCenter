package com.taskcenter.app.ui.space

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskcenter.app.databinding.FragmentMembersBinding
import com.taskcenter.app.viewmodel.SpaceViewModel

class MembersFragment : Fragment() {
    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SpaceViewModel by viewModels()
    private lateinit var adapter: MemberAdapter
    private var spaceId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spaceId = arguments?.getString("spaceId") ?: return

        adapter = MemberAdapter()
        binding.recyclerMembers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMembers.adapter = adapter

        viewModel.getMembersLive(spaceId).observe(viewLifecycleOwner) { members ->
            adapter.submitList(members)
            binding.emptyView.visibility = if (members.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
