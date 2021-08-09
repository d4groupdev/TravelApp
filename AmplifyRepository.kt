package com.example.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.kotlin.core.Amplify
import com.example.models.Resource
import com.example.models.UserModel
import com.example.models.api.UserPhotoInfo
import com.example.models.auth.AuthResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.io.File


class AmplifyRepository {
    companion object {
        private var instance: AmplifyRepository? = null
        fun getInstance(): AmplifyRepository {
            if (instance == null) {
                instance = AmplifyRepository()
            }
            return instance!!
        }
    }

    private val coroutineContext = Dispatchers.IO

    private var _errorData = MutableLiveData<String>()
    val errorData: LiveData<String> = _errorData


    fun signIn(email: String, password: String): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            Amplify.Auth.signIn(email, password).run {
                if (isSignInComplete) {
                    fetchAuthSession().collect {
                        when (it) {
                            is Resource.Success -> {
                                emit(Resource.Success(it.data!!))
                            }
                            is Resource.DataError -> {
                                flowOf(it.errorMessage)
                            }
                        }
                    }
                } else {
                    flowOf("Something is going wrong")
                }
            }
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.cause?.localizedMessage ?: "Something is going wrong"))
        }

    }

    fun signUp(
        name: String, email: String, address: String, password: String
    ): Flow<Resource<AuthResponse>> {
        return flow {
            emit(Resource.Loading())
            val userAttributes = mapOf(
                AuthUserAttributeKey.preferredUsername() to name,
                AuthUserAttributeKey.name() to email,
                AuthUserAttributeKey.email() to email,
                AuthUserAttributeKey.locale() to address
            )
            val options = AuthSignUpOptions.builder()
                .userAttributes(userAttributes.map {
                    AuthUserAttribute(
                        it.key,
                        it.value as String?
                    )
                })
                .build()
            val data = Amplify.Auth.signUp(email, password, options).run {
                if (isSignUpComplete) {
                    Resource.Success(AuthResponse())
                } else {
                    Resource.DataError("Something is going wrong")
                }
            }
            emit(data)
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }
    }

    fun verifyingNumber(email: String, code: String): Flow<Resource<AuthResponse>> {
        return flow {
            emit(Resource.Loading())
            val data = Amplify.Auth.confirmSignUp(email, code).run {
                if (isSignUpComplete) {
                    Resource.Success(AuthResponse())
                } else {
                    Resource.DataError("Something is going wrong")
                }
            }
            emit(data)
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }
    }

    fun sendEmail(email: String): Flow<Resource<AuthResponse>> {
        return flow {
            emit(Resource.Loading())
            val data = Amplify.Auth.resetPassword(email).run {
                if (nextStep.resetPasswordStep.toString()
                        .contains("CONFIRM_RESET_PASSWORD_WITH_CODE")
                ) {
                    Resource.Success(AuthResponse())
                } else {
                    Resource.DataError("Something is going wrong")
                }
            }
            emit(data)
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }

    }

    fun resendSignUpCode(email: String): Flow<Resource<AuthResponse>> {
        return flow {
            emit(Resource.Loading())
            val data = Amplify.Auth.resendSignUpCode(email).run {
                if (isSignUpComplete) {
                    Resource.Success(AuthResponse())
                } else {
                    Resource.DataError("Something is going wrong")
                }
            }
            emit(data)
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }
    }


    fun saveNewPassword(password: String, confirmationCode: String): Flow<Resource<AuthResponse>> {
        return flow {
            emit(Resource.Loading())
            Amplify.Auth.confirmResetPassword(password, confirmationCode).run {
                emit(Resource.Success(AuthResponse()))
            }
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }
    }

    fun signOut(): Flow<Resource<AuthResponse>> {
        return flow {
            emit(Resource.Loading())
            Amplify.Auth.signOut()
            emit(Resource.Success(AuthResponse()))
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }
    }

    fun fetchAuthSession(): Flow<Resource<String>> {
        return flow {
            emit(Resource.Loading())
            val session = Amplify.Auth.fetchAuthSession() as AWSCognitoAuthSession
            session.run {
                if (this.isSignedIn) {
                    PreferencesManager.init(
                        this.userSub.value.toString(),
                        this.userPoolTokens.value?.idToken.toString()
                    )
                    if (PreferencesManager.getUserIdName() == this.userSub.value.toString() &&
                        PreferencesManager.getAccessToken() == this.userPoolTokens.value?.idToken.toString()
                    ) {
                        emit(Resource.Success(this.userSub.value.toString()))
                    }
                }
                if (!this.isSignedIn) {
                    emit(Resource.DataError("User is signed out"))
                }
            }
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }

    }

    fun fetchUserAttributes(): Flow<Resource<UserModel>> {
        return flow {
            emit(Resource.Loading())
            val data = Amplify.Auth.fetchUserAttributes().run {
                val map = associateBy({ it.key.keyString }, { it.value })
                val preferredName =
                    if (map.contains(AuthUserAttributeKey.preferredUsername().keyString)) map[AuthUserAttributeKey.preferredUsername().keyString]
                        ?: "" else ""
                val email =
                    if (map.contains(AuthUserAttributeKey.email().keyString)) map[AuthUserAttributeKey.email().keyString]
                        ?: "" else ""
                val location =
                    if (map.contains(AuthUserAttributeKey.locale().keyString)) map[AuthUserAttributeKey.locale().keyString]
                        ?: "" else ""
                val firstName =
                    if (map.contains(AuthUserAttributeKey.givenName().keyString)) map[AuthUserAttributeKey.givenName().keyString]
                        ?: "" else ""
                val lastName =
                    if (map.contains(AuthUserAttributeKey.familyName().keyString)) map[AuthUserAttributeKey.familyName().keyString]
                        ?: "" else ""
                val phoneNumber =
                    if (map.contains(AuthUserAttributeKey.phoneNumber().keyString)) map[AuthUserAttributeKey.phoneNumber().keyString]
                        ?: "" else ""
                val userPhoto = if (map.contains(AuthUserAttributeKey.picture().keyString))
                    map[AuthUserAttributeKey.picture().keyString] ?: "" else ""

                UserModel(
                    preferredName = preferredName,
                    email = email,
                    location = location,
                    firstName = firstName,
                    familyName = lastName,
                    phoneNumber = phoneNumber,
                    userPhoto = userPhoto
                )

            }
            emit(Resource.Success(data))
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }
    }

    fun updateUserAttributes(attributes: List<AuthUserAttribute>): Flow<Resource<AuthResponse>> {
        print(attributes)
        val list = mutableListOf<String>()
        return flow {
            for (attribute in attributes) {
                emit(Resource.Loading())
                val data = Amplify.Auth.updateUserAttribute(attribute).run {
                    if (isUpdated) {
                        Resource.Success(AuthResponse())
                    } else {
                        list.add("error")
                        Resource.DataError("Something is going wrong")
                    }
                }
                emit(data)
            }
        }.flowOn(coroutineContext).catch { error ->
            list.add("error")
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }
    }


    fun updateUserAttributePicture(
        picture: String
    ): Flow<Resource<AuthResponse>> {
        return flow {
            emit(Resource.Loading())
            val attribute = AuthUserAttribute(AuthUserAttributeKey.picture(), picture)
            val data = Amplify.Auth.updateUserAttribute(attribute).run {
                if (isUpdated) {
                    Resource.Success(AuthResponse())
                } else {
                    Resource.DataError("Something is going wrong")
                }
            }
            emit(data)
        }.flowOn(coroutineContext).catch { error ->
            emit(Resource.DataError(error.localizedMessage ?: "Something is going wrong"))
        }
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    suspend fun uploadFile(photoPath: String, key: String): Flow<Resource<UserPhotoInfo>> {
        return flow {
            emit(Resource.Loading())
            val exampleFile = File(photoPath)
            val upload =
                Amplify.Storage.uploadFile(key + PreferencesManager.getUserIdName(), exampleFile)
            val result = upload.result()
            val url = Amplify.Storage.getUrl(result.key).url
            print(result)
            Log.i("MyAmplifyApp", "Successfully uploaded: ${result.key}")
            emit(Resource.Success(UserPhotoInfo(s3Uri = url.toString())))
        }.flowOn(Dispatchers.IO).catch { error ->
            Log.e("MyAmplifyApp", "Upload failed", error)
            emit(Resource.DataError(error.localizedMessage ?: "Error"))
        }
    }
}




