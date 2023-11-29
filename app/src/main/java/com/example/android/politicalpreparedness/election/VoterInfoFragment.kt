package com.example.android.politicalpreparedness.election

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.database.ElectionDatabase
import com.example.android.politicalpreparedness.databinding.FragmentVoterInfoBinding

class VoterInfoFragment : Fragment() {

    private lateinit var viewModel: VoterInfoViewModel
    private lateinit var viewModelFactory: VoterInfoViewModelFactory
    private lateinit var binding: FragmentVoterInfoBinding

    private val args: VoterInfoFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireNotNull(this.activity).application
        val dataSource = ElectionDatabase.getInstance(application).electionDao

        viewModelFactory = VoterInfoViewModelFactory(dataSource)
        viewModel = ViewModelProvider(this, viewModelFactory)[VoterInfoViewModel::class.java]

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_voter_info, container, false
        )
        binding.lifecycleOwner = this

        binding.voterInfoViewModel = viewModel

        viewModel.setApplicationContext(requireContext())
        viewModel.setButtonTextValue(id, binding)

        val id = args.argElectionId
        val division = args.argDivision

        //val address = "nj, us"
        val address = "${division.state}, ${division.country}"
        viewModel.getVoterInfoFromAPI(id, address)

        viewModel.voterInfo.observe(viewLifecycleOwner, Observer {
            binding.electionName.title = it.election.name
            binding.electionDate.text = it.election.electionDay.toString()
            viewModel.getUrls(binding)
        })

        binding.followButton.setOnClickListener {
            viewModel.setButtonStatus(this)
        }

        /**
        Hint: You will need to ensure proper data is provided from previous fragment.
         */

        return binding.root
    }

}