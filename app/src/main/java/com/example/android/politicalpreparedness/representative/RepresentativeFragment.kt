package com.example.android.politicalpreparedness.representative

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.databinding.FragmentRepresentativeBinding
import com.example.android.politicalpreparedness.network.models.Address
import com.example.android.politicalpreparedness.representative.adapter.RepresentativeListAdapter
import com.example.android.politicalpreparedness.representative.model.Representative
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import java.util.Locale


class DetailFragment : Fragment() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    val TAG = "RepresentativeFragment"

    private lateinit var viewModel: RepresentativeViewModel
    private lateinit var binding: FragmentRepresentativeBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this)[RepresentativeViewModel::class.java]

        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_representative, container, false
        )

        binding.representativeViewModel = viewModel


        savedInstanceState?.let {
            // Restore MotionLayout state
            val motionLayout = binding.representativeFragment
            val motionLayoutState = it.getInt("motion_layout_state")
            motionLayout.transitionToState(motionLayoutState)

            // Restore representatives list data
            val listData = it.getParcelableArrayList<Representative>("list_data")
            listData.let { viewModel.setSavedList(it!!.toList()) }
        }

        val spinner: Spinner = binding.state
        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.states,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            spinner.adapter = adapter
        }
        spinner.setSelection(0)


        viewModel.setApplicationContext(requireContext())

        binding.buttonLocation.setOnClickListener {
            checkLocationPermissions()
        }

        val adapter = RepresentativeListAdapter()
        binding.representativesRecyclerView.adapter = adapter
        binding.representativesRecyclerView.setLayoutManager(LinearLayoutManager(requireContext()))

        viewModel.representatives.observe(viewLifecycleOwner) { representative ->
            adapter.submitList(representative)
        }

        binding.buttonSearch.setOnClickListener {
            hideKeyboard()
            val address = viewModel.getAddressManually(binding)
            viewModel.setAddress(address)
            viewModel.address.observe(viewLifecycleOwner, Observer { add2 ->
                viewModel.getRepresentatives(add2)
            })
        }

        return binding.root
    }

    @Suppress("DEPRECATION")
    private fun showPermissionDeniedSnackbar() {
        Snackbar.make(
            binding.representativeFragment,
            R.string.location_required_error,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Retry") {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }.show()
    }

    @Suppress("DEPRECATION")
    private fun checkLocationPermissions(): Boolean {
        return if (isPermissionGranted()) {
            getLocation()
            true
        } else {
            //Request Location permissions
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            false
        }
    }

    private fun isPermissionGranted(): Boolean {
        return PackageManager.PERMISSION_GRANTED ===
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
    }

    @SuppressLint("MissingPermission")
    fun getLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentAddress = geoCodeLocation(location)
                //Log.i(TAG, currentAddress.toString())
                val address = Address(
                    currentAddress.line1,
                    currentAddress.line2,
                    currentAddress.city,
                    currentAddress.state,
                    currentAddress.zip
                ).toFormattedString()

                viewModel.setAddress(address)
                viewModel.address.observe(viewLifecycleOwner, Observer { add1 ->
                    viewModel.getRepresentatives(add1)
                    viewModel.getAddressFromGeolocation(currentAddress, binding)
                })
            } else {
                Log.i(TAG, "location is null")
            }
        }
    }

    private fun geoCodeLocation(location: Location): Address {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        return geocoder.getFromLocation(location.latitude, location.longitude, 1)
            ?.map { address ->
                Address(
                    address.thoroughfare,
                    address.subThoroughfare,
                    address.locality,
                    address.adminArea,
                    address.postalCode
                )
            }!!.first()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                getLocation()
            } else {
                showPermissionDeniedSnackbar()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save MotionLayout state
        val motionLayout = binding.representativeFragment
        val motionLayoutState = motionLayout.currentState
        outState.putInt("motion_layout_state", motionLayoutState)

        // Save representatives list data
        viewModel.representatives.value.let { representatives ->
            outState.putParcelableArrayList(
                "list_data",
                ArrayList(representatives!!.toMutableList())
            )
        }
    }
}