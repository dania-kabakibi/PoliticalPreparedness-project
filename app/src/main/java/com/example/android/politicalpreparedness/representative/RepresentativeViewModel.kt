package com.example.android.politicalpreparedness.representative

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.databinding.FragmentRepresentativeBinding
import com.example.android.politicalpreparedness.network.CivicsApi
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.network.models.RepresentativeResponse
import com.example.android.politicalpreparedness.representative.model.Representative
import retrofit2.Call
import retrofit2.Response

class RepresentativeViewModel : ViewModel() {
    private lateinit var appContext: Context

    private val _representatives = MutableLiveData<List<Representative>>()
    val representatives: LiveData<List<Representative>>
        get() = _representatives


    private val _address = MutableLiveData<String>()
    val address: LiveData<String>
        get() = _address

    fun setAddress(ad: String) {
        _address.postValue(ad)
    }

    fun setSavedList(representatives: List<Representative>) {
        _representatives.value = representatives
    }

    fun setApplicationContext(context: Context) {
        appContext = context.applicationContext
    }

    fun getRepresentatives(a: String) {
        CivicsApi.retrofitService.getRepresentativeInfoByAddress(a)
            .enqueue(object : retrofit2.Callback<RepresentativeResponse> {
                override fun onResponse(
                    call: Call<RepresentativeResponse>,
                    response: Response<RepresentativeResponse>
                ) {
                    val offices = response.body()?.offices
                    val officials = response.body()?.officials
                    val listR = mutableListOf<Representative>()
                    for (i in (offices!!.indices)) {
                        listR.add(Representative(officials!![i], offices[i]))
                    }
                    _representatives.value = listR
                    //_savedRepresentatives.value = listR
                }

                override fun onFailure(call: Call<RepresentativeResponse>, t: Throwable) {
                    Log.i("RepresentativeViewModel", t.message.toString())
                }
            })
    }

    /**
     *  The following code will prove helpful in constructing a representative from the API. This code combines the two nodes of the RepresentativeResponse into a single official :

    val (offices, officials) = getRepresentativesDeferred.await()
    _representatives.value = offices.flatMap { office -> office.getRepresentatives(officials) }

    Note: getRepresentatives in the above code represents the method used to fetch data from the API
    Note: _representatives in the above code represents the established mutable live data housing representatives

     */

    fun getAddressFromGeolocation(
        address: Address,
        binding: FragmentRepresentativeBinding
    ) {
        val stateList = appContext.resources.getStringArray(R.array.states)
        val selectedIndex = stateList.indexOf(address.state)
        binding.addressLine1.setText(address.line1)
        binding.addressLine2.setText(address.line2)
        binding.city.setText(address.city)
        binding.zip.setText(address.zip)
        binding.state.setSelection(selectedIndex)
    }

    fun getAddressManually(binding: FragmentRepresentativeBinding): String {
        val mySpinner = binding.state
        return Address(
            binding.addressLine1.text.toString(),
            binding.addressLine2.text.toString(),
            binding.city.text.toString(),
            mySpinner.selectedItem.toString(),
            binding.zip.text.toString()
        ).toFormattedString()
    }
}