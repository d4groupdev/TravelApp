package com.example.auth

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mapbox.android.core.permissions.PermissionsManager
import com.example.R
import com.example.databinding.FragmentSignUpBinding
import com.example.repositories.MyLocationManager
import com.example.viemodels.AuthViewModel
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
class SignUpFragment : Fragment() {
    private lateinit var binding: FragmentSignUpBinding
    private val viewModel: AuthViewModel by activityViewModels()
    private lateinit var locationManager: MyLocationManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up, container, false)
        binding.progressSignUp.hide()
        binding.tvSignIn.setOnClickListener {
            findNavController().popBackStack()
        }
        locationManager = MyLocationManager.getInstance()


        binding.signUpButton.setOnClickListener {
            viewModel.signUp(
                binding.etName.text.toString(),
                binding.etEmail.text.toString(),
                binding.etLocation.text.toString(),
                binding.etPassword.text.toString()
            )
        }
        observeViewModel()
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        viewModel.onSignUpPause()
    }

    private fun observeViewModel() {
        viewModel.singUpState.observe(viewLifecycleOwner) {
            when {
                it.isError -> {
                    binding.progressSignUp.hide()
                    Toast.makeText(requireContext(), it.errorDescription, Toast.LENGTH_SHORT)
                        .show()
                }
                it.isLoading -> {
                    binding.progressSignUp.show()
                }
                it.isNeedSetConfirmation -> {
                    binding.progressSignUp.hide()
                    val action =
                        SignUpFragmentDirections.actionSignUpFragmentToVeryfyYourNumberFragment(
                            true
                        )
                    viewModel.email = binding.etEmail.text.toString()
                    findNavController().navigate(action)
                }
            }
        }
        locationManager.errorData.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT)
                .show()
        }
        binding.inputLocation.setEndIconOnClickListener {
            checkLocationPermission()
        }
        locationManager.location.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                binding.etLocation.setText(locationManager.getAddress(location))
            }
        }
    }

    private fun checkLocationPermission() {
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            locationManager.initLocationEngine()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MyLocationManager.LOCATION_REQUEST_CODE
            )
        }
    }
}