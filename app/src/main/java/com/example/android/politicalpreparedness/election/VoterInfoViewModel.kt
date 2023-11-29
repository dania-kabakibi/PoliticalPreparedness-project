package com.example.android.politicalpreparedness.election

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.database.ElectionDao
import com.example.android.politicalpreparedness.databinding.FragmentVoterInfoBinding
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.network.models.VoterInfoResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response

enum class ButtonStatus { FOLLOW, UNFOLLOW }

var buttonStatus = ButtonStatus.FOLLOW

class VoterInfoViewModel(private val dataSource: ElectionDao) : ViewModel() {
    private val TAG = "VoterInfoViewModel"
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private lateinit var appContext: Context

    private val _voterInfo = MutableLiveData<VoterInfoResponse>()
    val voterInfo: LiveData<VoterInfoResponse>
        get() = _voterInfo

    fun getVoterInfoFromAPI(electionId: Int, address: String) {
        CivicsApi.retrofitService.getVoterInfo(address, electionId)
            .enqueue(object : retrofit2.Callback<VoterInfoResponse> {
                override fun onResponse(
                    call: Call<VoterInfoResponse>,
                    response: Response<VoterInfoResponse>
                ) {
                    //Log.i(TAG, "Success: ${response.body()?.election}")
                    _voterInfo.value = response.body()
                }

                override fun onFailure(call: Call<VoterInfoResponse>, t: Throwable) {
                    Log.i(TAG, "Failure")
                }
            })
    }

    fun getUrls(binding: FragmentVoterInfoBinding) {
        val electionAdministrationBody = _voterInfo.value?.state!![0].electionAdministrationBody

        val votingLocationFinderUrl = electionAdministrationBody.votingLocationFinderUrl
        if (!votingLocationFinderUrl.isNullOrBlank()) {
            enableLink(binding.stateLocations, votingLocationFinderUrl)
        }

        val ballotInfoUrl = electionAdministrationBody.ballotInfoUrl
        if (!ballotInfoUrl.isNullOrBlank()) {
            enableLink(binding.stateBallot, ballotInfoUrl)
        }
    }

    private fun enableLink(view: TextView, url: String) {
        view.setOnClickListener { setIntent(url, view) }
    }

    private fun setIntent(url: String, view: TextView) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        view.context.startActivity(intent)
    }

    fun setButtonStatus(fragment: VoterInfoFragment) {
        when (buttonStatus) {
            ButtonStatus.FOLLOW -> {
                insertElectionToDb()
                fragment.findNavController()
                    .navigate(VoterInfoFragmentDirections.actionVoterInfoFragmentToElectionsFragment())
                Toast.makeText(fragment.context, R.string.saved_election_toast, Toast.LENGTH_SHORT)
                    .show()
            }

            ButtonStatus.UNFOLLOW -> {
                deleteElectionFromDb()
                fragment.findNavController()
                    .navigate(VoterInfoFragmentDirections.actionVoterInfoFragmentToElectionsFragment())
                Toast.makeText(
                    fragment.context,
                    R.string.removed_election_toast,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteElectionFromDb() {
        val e = _voterInfo.value!!.election
        uiScope.launch {
            delete(e)
        }
    }

    private fun insertElectionToDb() {
        val e = _voterInfo.value!!.election
        uiScope.launch {
            insert(e)
        }
    }

    private suspend fun insert(e: Election) {
        withContext(Dispatchers.IO) {
            dataSource.insert(e)
        }
    }

    private suspend fun delete(e: Election) {
        withContext(Dispatchers.IO) {
            dataSource.deleteElection(e)
        }
    }

    private fun checkIsElectionAdded(id: Int, callback: (Boolean) -> Unit) {
        uiScope.launch {
            var result = false
            val election = getAnElectionFromDb(id)
            val allElections = getElectionsFromDb()
            if (election != null && allElections != null) {
                for (i in allElections.indices) {
                    if (election.id == allElections[i].id) result = true
                }
            } else result = false
            callback.invoke(result)
        }
    }

    private suspend fun getAnElectionFromDb(id: Int): Election {
        return withContext(Dispatchers.IO) {
            dataSource.getElection(id)
        }
    }

    private suspend fun getElectionsFromDb(): List<Election> {
        return withContext(Dispatchers.IO) {
            dataSource.getAllElections()
        }
    }

    fun setButtonTextValue(id: Int, binding: FragmentVoterInfoBinding) {
        checkIsElectionAdded(id) { result ->
            if (result) {
                binding.followButton.text = appContext.getString(R.string.unfollow_election)
            } else {
                binding.followButton.text = appContext.getString(R.string.follow_election)
            }
        }
    }

    fun setApplicationContext(context: Context) {
        appContext = context.applicationContext
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

}