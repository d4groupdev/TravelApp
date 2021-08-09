package com.example.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.MainActivity
import com.example.R
import com.example.databinding.FragmentSignInBinding
import com.example.viemodels.AuthViewModel
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
class SignInFragment : Fragment() {
    private lateinit var binding: FragmentSignInBinding
    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_in, container, false)
        binding.progressSignIn.hide()
        observeViewModel()

        binding.signInButton.setOnClickListener {
            viewModel.signIn(
                binding.etEmailOrPhone.text.toString(),
                binding.etPassword.text.toString()
            )
        }
        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }
        binding.tvForgotPassword.setOnClickListener { findNavController().navigate(R.id.action_signInFragment_to_forgotPassword) }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    activity?.finish()
                }
            })
    }

    private fun observeViewModel() {
        viewModel.singInState.observe(viewLifecycleOwner) {
            when {
                it.isError -> {
                    binding.progressSignIn.hide()
                    if (it.errorDescription.contains("User is not confirmed.")) {
                        viewModel.email = binding.etEmailOrPhone.text.toString()
                        viewModel.resendSignUpCode()
                        val action =
                            SignInFragmentDirections.actionSignInFragmentToVeryfyYourNumberFragment(
                                true
                            )
                        findNavController().navigate(action)
                    } else {
                      Toast.makeText(requireContext(), it.errorDescription, Toast.LENGTH_LONG)
                            .show()
                    }
                }
                it.isLoading -> {
                    binding.progressSignIn.show()
                }
                it.isSuccess -> {
                    binding.progressSignIn.hide()

                    startActivity(
                        Intent(requireContext(), MainActivity::class.java)
                    )
                    activity?.finish()
                }

            }
        }

    }

    override fun onPause() {
        super.onPause()
        viewModel.onSignInPause()
    }
}