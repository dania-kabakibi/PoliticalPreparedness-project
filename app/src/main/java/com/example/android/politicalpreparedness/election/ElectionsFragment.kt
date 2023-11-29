package com.example.android.politicalpreparedness.election

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.databinding.FragmentElectionBinding
import com.example.android.politicalpreparedness.election.adapter.ElectionListAdapter

class ElectionsFragment : Fragment() {

    private lateinit var viewModel: ElectionsViewModel
    private lateinit var viewModelFactory: ElectionsViewModelFactory
    private lateinit var binding: FragmentElectionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val application = requireNotNull(this.activity).application
        val dataSource = ElectionDatabase.getInstance(application).electionDao

        viewModelFactory = ElectionsViewModelFactory(dataSource)
        viewModel = ViewModelProvider(this, viewModelFactory)[ElectionsViewModel::class.java]

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_election, container, false
        )

        binding.lifecycleOwner = this

        binding.electionsViewModel = viewModel

        val upcomingAdapter = ElectionListAdapter(ElectionListAdapter.ElectionListener {
            if (!it.division.state.isNullOrBlank()) {
                viewModel.displayVoterInfo(it)
            } else {
                viewModel.displayDialog(requireContext())
            }
        })
        binding.upcomingElectionsRecyclerView.adapter = upcomingAdapter
        binding.upcomingElectionsRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))

        viewModel.navigateToSelectedElection.observe(viewLifecycleOwner, Observer {
            this.findNavController().navigate(
                ElectionsFragmentDirections
                    .actionElectionsFragmentToVoterInfoFragment(it.id, it.division)
            )
            viewModel.setButtonStatusValue(it.id)
            Log.i("ElectionsFragment", "${it.id} ${it.division}")
        })

        viewModel.upcomingElections.observe(viewLifecycleOwner) { election ->
            upcomingAdapter.submitList(election)
        }

        val savedAdapter = ElectionListAdapter(ElectionListAdapter.ElectionListener {
            if (!it.division.state.isNullOrBlank()) {
                viewModel.displaySavedVoterInfo(it)
            } else {
                viewModel.displayDialog(requireContext())
            }
        })
        binding.savedElectionsRecyclerView.adapter = savedAdapter
        binding.savedElectionsRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))

        viewModel.navigateToSavedSelectedElection.observe(viewLifecycleOwner, Observer {
            this.findNavController().navigate(
                ElectionsFragmentDirections
                    .actionElectionsFragmentToVoterInfoFragment(it.id, it.division)
            )
            viewModel.setButtonStatusValue(it.id)

        })

        viewModel.savedElections.observe(viewLifecycleOwner) { elections ->
            savedAdapter.submitList(elections)
        }

        return binding.root
    }

}