package com.example.android.politicalpreparedness.election

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.database.ElectionDao
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.models.Election
import com.example.android.politicalpreparedness.network.models.ElectionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response

class ElectionsViewModel(private val dataSource: ElectionDao) : ViewModel() {
    val TAG = "ElectionsViewModel"
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _upcomingElections = MutableLiveData<List<Election>>()
    val upcomingElections: LiveData<List<Election>>
        get() = _upcomingElections

    private val _navigateToSelectedElection = MutableLiveData<Election>()
    val navigateToSelectedElection: LiveData<Election>
        get() = _navigateToSelectedElection

    private val _navigateToSavedSelectedElection = MutableLiveData<Election>()
    val navigateToSavedSelectedElection: LiveData<Election>
        get() = _navigateToSavedSelectedElection

    private val _savedElections = MutableLiveData<List<Election>>()
    val savedElections: LiveData<List<Election>>
        get() = _savedElections

    private fun getUpcomingElections() {
        CivicsApi.retrofitService.getElections()
            .enqueue(object : retrofit2.Callback<ElectionResponse> {
                override fun onResponse(
                    call: Call<ElectionResponse>,
                    response: Response<ElectionResponse>
                ) {
                    if (response.isSuccessful) {
                        _upcomingElections.value = response.body()?.elections

                    } else {
                        Log.i(TAG, "response is null")
                    }
                }

                override fun onFailure(call: Call<ElectionResponse>, t: Throwable) {
                    Log.i(TAG, t.message.toString())
                }
            })
    }

    init {
        getUpcomingElections()
        initialSavedElections()
    }

    private fun initialSavedElections() {
        uiScope.launch {
            _savedElections.value = getElectionsFromDb()
        }
    }

    private suspend fun getElectionsFromDb(): List<Election> {
        return withContext(Dispatchers.IO) {
            dataSource.getAllElections()
        }
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            dataSource.clear()
        }
    }

    private suspend fun getAnElectionFromDb(id: Int): Election {
        return withContext(Dispatchers.IO) {
            dataSource.getElection(id)
        }
    }

    fun displayVoterInfo(election: Election) {
        _navigateToSelectedElection.value = election
    }

    fun displaySavedVoterInfo(election: Election) {
        _navigateToSavedSelectedElection.value = election
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

    fun setButtonStatusValue(id: Int) {
        checkIsElectionAdded(id) { result ->
            buttonStatus = if (result) {
                ButtonStatus.UNFOLLOW
            } else {
                ButtonStatus.FOLLOW
            }
        }
    }

    fun displayDialog(context: Context) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage(R.string.dialog_message)
            .setPositiveButton("Ok") { dialog, which ->
                dialog.cancel()
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}