package com.example.viemodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.Resource
import com.example.models.auth.AuthResponse
import com.example.models.states.*
import com.example.repositories.*
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    var confirmationCode: String = ""

    var email: String = ""

    private val _singInState = MutableLiveData<SignInState>()
    val singInState: LiveData<SignInState> = _singInState

    private val _singUpState = MutableLiveData<SignUpState>()
    val singUpState: LiveData<SignUpState> = _singUpState

    private val _verifyState = MutableLiveData<VerifyState>()
    val verifyState: LiveData<VerifyState> = _verifyState

    private val _sendEmailState = MutableLiveData<SendEmailState>()
    val sendEmailState: LiveData<SendEmailState> = _sendEmailState

    private val _resetPasswordState = MutableLiveData<ResetPasswordState>()
    val resetPasswordState: LiveData<ResetPasswordState> = _resetPasswordState

    @InternalCoroutinesApi
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _singInState.postValue(
                SignInState(
                    isError = true,
                    errorDescription = "Fill all fields"
                )
            )
            return
        }
        viewModelScope.launch {
            AmplifyRepository.getInstance().signIn(email, password)
                .collect(object : FlowCollector<Resource<String>> {
                    override suspend fun emit(value: Resource<String>) {
                        when (value) {
                            is Resource.Success -> {
                                if (PreferencesManager.getUserIdName() == value.data &&
                                    PreferencesManager.getAccessToken().isNotBlank()
                                ) {
                                    getImagesAccess()
                                }
                            }
                            is Resource.Loading -> {
                                _singInState.postValue(SignInState(isLoading = true))
                            }
                            is Resource.DataError -> {
                                _singInState.postValue(
                                    SignInState(
                                        isError = true,
                                        errorDescription = value.errorMessage ?: "Error"
                                    )
                                )
                            }
                        }
                    }

                })
        }
    }

    @InternalCoroutinesApi
    fun signUp(name: String, email: String, address: String, password: String) {
        if (name.isBlank() || email.isBlank() || address.isBlank() || password.isBlank()) {
            _singUpState.postValue(
                SignUpState(
                    isError = true,
                    errorDescription = "Fill all fields"

                )
            )
            return
        }
        viewModelScope.launch {
            AmplifyRepository.getInstance().signUp(name, email, address, password).collect(
                object : FlowCollector<Resource<AuthResponse>> {
                    override suspend fun emit(value: Resource<AuthResponse>) {
                        when (value) {
                            is Resource.Success -> {
                                _singUpState.postValue(SignUpState(isNeedSetConfirmation = true))

                            }
                            is Resource.Loading -> {
                                _singUpState.postValue(SignUpState(isLoading = true))
                            }
                            is Resource.DataError -> {
                                _singUpState.postValue(
                                    SignUpState(
                                        isError = true,
                                        errorDescription = value.errorMessage ?: "Error"
                                    )
                                )
                            }
                        }
                    }
                })
        }
    }


    @InternalCoroutinesApi
    fun verifyingNumber(code: String) {
        if (email.isBlank()) {
            _verifyState.postValue(
                VerifyState(
                    isError = true,
                    errorDescription = "Email is empty"
                )
            )
            return
        }
        viewModelScope.launch {
            AmplifyRepository.getInstance().verifyingNumber(email, code).collect(
                object : FlowCollector<Resource<AuthResponse>> {
                    override suspend fun emit(value: Resource<AuthResponse>) {
                        when (value) {
                            is Resource.Success -> {
                                _verifyState.postValue(VerifyState(isSuccess = true))
                            }
                            is Resource.Loading -> {
                                _verifyState.postValue(VerifyState(isLoading = true))
                            }
                            is Resource.DataError -> {
                                _verifyState.postValue(
                                    VerifyState(
                                        isError = true,
                                        errorDescription = value.errorMessage ?: "Error"
                                    )
                                )
                            }
                        }
                    }
                })
        }
    }


    @InternalCoroutinesApi
    fun sendVerifyingNumberToEmail(email: String) {
        if (email.isBlank()) {
            _sendEmailState.postValue(
                SendEmailState(
                    isError = true,
                    errorDescription = "Email is empty"
                )
            )
            return
        }
        viewModelScope.launch {
            AmplifyRepository.getInstance().sendEmail(email).collect {
                when (it) {
                    is Resource.Success -> {
                        _sendEmailState.postValue(SendEmailState(isSuccess = true))
                    }
                    is Resource.Loading -> {
                        _sendEmailState.postValue(SendEmailState(isLoading = true))
                    }
                    is Resource.DataError -> {
                        _sendEmailState.postValue(
                            SendEmailState(
                                isError = true,
                                errorDescription = it.errorMessage ?: "Error"
                            )
                        )
                    }
                }
            }
        }
    }

    @InternalCoroutinesApi
    fun sendVerifyingNumberToEmail() {
        if (email.isBlank()) {
            _sendEmailState.postValue(
                SendEmailState(
                    isError = true,
                    errorDescription = "Email is empty"
                )
            )
            return
        }
        viewModelScope.launch {
            AmplifyRepository.getInstance().sendEmail(email).collect {
                when (it) {
                    is Resource.Success -> {
                        _sendEmailState.postValue(SendEmailState(isSuccess = true))
                    }
                    is Resource.Loading -> {
                        _sendEmailState.postValue(SendEmailState(isLoading = true))
                    }
                    is Resource.DataError -> {
                        _sendEmailState.postValue(
                            SendEmailState(
                                isError = true,
                                errorDescription = it.errorMessage ?: "Error"
                            )
                        )
                    }
                }
            }
        }
    }

    @InternalCoroutinesApi
    fun saveNewPassword(password: String, confirmPassword: String) {
        if (!validateNewPassword(password, confirmPassword)) {
            _resetPasswordState.postValue(
                ResetPasswordState(
                    isError = true,
                    errorDescription = "New password not equal to confirm password"
                )
            )
            return
        }
        if (confirmationCode.isBlank()) {
            _resetPasswordState.postValue(
                ResetPasswordState(
                    isError = true,
                    errorDescription = "Confirmation code is not correct"
                )
            )
            return
        }
        viewModelScope.launch {
            AmplifyRepository.getInstance().saveNewPassword(password, confirmationCode)
                .collect(
                    object : FlowCollector<Resource<AuthResponse>> {
                        override suspend fun emit(value: Resource<AuthResponse>) {
                            when (value) {
                                is Resource.Success -> {
                                    _resetPasswordState.postValue(ResetPasswordState(isSuccess = true))
                                }
                                is Resource.Loading -> {
                                    _resetPasswordState.postValue(
                                        ResetPasswordState(
                                            isLoading = true
                                        )
                                    )
                                }
                                is Resource.DataError -> {
                                    _resetPasswordState.postValue(
                                        ResetPasswordState(
                                            isError = true,
                                            errorDescription = value.errorMessage ?: "Error"
                                        )
                                    )
                                }
                            }
                        }
                    })
        }
    }

    @InternalCoroutinesApi
    fun resendSignUpCode() {
        if (email.isBlank()) {
            _sendEmailState.postValue(
                SendEmailState(
                    isError = true,
                    errorDescription = "Email is empty"
                )
            )
            return
        }
        print(email)
        viewModelScope.launch {
            AmplifyRepository.getInstance().resendSignUpCode(email).collect {
                when (it) {
                    is Resource.Success -> {
                        _sendEmailState.postValue(SendEmailState(isSuccess = true))
                    }
                    is Resource.Loading -> {
                        _sendEmailState.postValue(SendEmailState(isLoading = true))
                    }
                    is Resource.DataError -> {
                        _sendEmailState.postValue(
                            SendEmailState(
                                isError = true,
                                errorDescription = it.errorMessage ?: "Error"
                            )
                        )
                    }
                }
            }
        }
    }

    fun fetchAuthSession() {
        viewModelScope.launch {
            AmplifyRepository.getInstance().fetchAuthSession().collect {
                when (it) {
                    is Resource.Success -> {
                        if (PreferencesManager.getUserIdName() == it.data &&
                            PreferencesManager.getAccessToken().isNotBlank()
                        ) {
                            getImagesAccess()
                        }
                    }
                    is Resource.Loading -> {
                        _singInState.postValue(SignInState(isLoading = true))
                    }
                    is Resource.DataError -> {
                        _singInState.postValue(
                            SignInState(
                                isError = true,
                                errorDescription = it.errorMessage ?: "Something is going wrong"
                            )
                        )
                    }
                }
            }
        }
    }
}