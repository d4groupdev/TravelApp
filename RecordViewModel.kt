package com.example.viemodels

import android.os.CountDownTimer
import androidx.lifecycle.*
import com.example.ExampleApp
import com.example.dataBase.entity.RouteRecordingEntity
import com.example.models.ExploreByActivityModel
import com.example.models.Resource
import com.example.models.RouteModel
import com.example.models.states.DataBaseState
import com.example.models.states.DownloadRecordingState
import com.example.models.states.RecordingManagerStatus
import com.example.repositories.*
import com.mapbox.mapboxsdk.offline.OfflineRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class RecordViewModel : ViewModel() {
    val startOrFinish = MutableLiveData<Boolean>()
    val pauseOrResume = MutableLiveData<Boolean>()
    private var recordingManager: RecordingManager? = null
    var reset = MutableLiveData<Boolean>()
    var timer: CountDownTimer? = null
    var _time = MutableLiveData<String>()
    val time: LiveData<String> = _time


    private var _browseByActivityData = MutableLiveData<List<ExploreByActivityModel>>()
    val browseByActivityData: LiveData<List<ExploreByActivityModel>> = _browseByActivityData

    private var _browseRouteDetailData = MutableLiveData<RouteModel>()
    val browseRouteDetailData: LiveData<RouteModel> = _browseRouteDetailData

    private val databaseRepository = AppDatabaseRepository(ExampleApp.app)

    private val _databaseState = MutableLiveData<DataBaseState>()
    val databaseState: LiveData<DataBaseState> = _databaseState

    private val _recordState = MutableLiveData<RecordingManagerStatus>()
    val recordState: LiveData<RecordingManagerStatus> = _recordState

    private var _errorData = MutableLiveData<String>()
    val errorData: LiveData<String> = _errorData

    private var _routeRecordingIdData = MutableLiveData<Long>()
    val routeRecordingIdData: LiveData<Long> = _routeRecordingIdData

    private var _routeRecordingDatabaseId = MutableLiveData<Long>()
    val routeRecordingDatabaseId: LiveData<Long> = _routeRecordingDatabaseId

    private var _continueRecording = MutableLiveData<Boolean>()
    val continueRecording: LiveData<Boolean> = _continueRecording

    private val _offlineRegion = MutableLiveData<OfflineRegion?>()
    val offlineRegion: LiveData<OfflineRegion?> = _offlineRegion

    private val _createUserRecordingState = MutableLiveData<DownloadRecordingState>()
    val createUserRecordingState: LiveData<DownloadRecordingState> = _createUserRecordingState


    init {
        GraphQLRepository.getInstance().fetchActivities()
        recordingManager =
            RecordingManager().getInstance()
        _recordState.postValue(RecordingManagerStatus.SETUP)
        GraphQLRepository.getInstance().errorData.observeForever {
            _errorData.postValue(it)
        }

        GraphQLRepository.getInstance().browseRouteDetailData.observeForever {
            _browseRouteDetailData.postValue(it)
        }
        GraphQLRepository.getInstance().byActivityData.observeForever {
            _browseByActivityData.postValue(it)
        }
        _recordState.postValue(RecordingManagerStatus.SETUP)
    }


    fun setRouteId(routeId: String) {
        GraphQLRepository.getInstance().fetchSingleRoute(routeId)
    }

    fun setUp(delegate: RecordingDisplay) {
        recordingManager?.setUp(delegate)
        _recordState.postValue(RecordingManagerStatus.READY)
    }

    fun startRecording(delegate: RecordingDisplay, route: RouteModel?) {
        if (recordState.value == RecordingManagerStatus.READY || recordState.value == RecordingManagerStatus.PAUSED)
            recordingManager?.startRecording(delegate, route)
        _recordState.postValue(RecordingManagerStatus.RECORDING)
        _continueRecording.postValue(false)
    }

    fun pauseRecording() {
        if (recordState.value != RecordingManagerStatus.RECORDING) {
            return
        }
        recordingManager?.pauseRecording()
        _recordState.postValue(RecordingManagerStatus.PAUSED)
    }

    fun finishRecording() {
        if (recordState.value != RecordingManagerStatus.PAUSED && recordState.value != RecordingManagerStatus.RECORDING) {
            return
        }
        recordingManager?.finishRecording()
        _recordState.postValue(RecordingManagerStatus.FINISHED)
    }

    @InternalCoroutinesApi
    fun save(name: String, activity: String) {
        _recordState.postValue(RecordingManagerStatus.SETUP)
        val recording = recordingManager?.getFinishedRecording(name, activity)
        if (recording != null) {
            viewModelScope.launch {
                databaseRepository.saveRouteRecording(recording)
                    .collect(object : FlowCollector<Resource<Long>> {
                        override suspend fun emit(value: Resource<Long>) {
                            when (value) {
                                is Resource.Success -> {
                                    _databaseState.postValue(DataBaseState(isSuccess = true))
                                    _routeRecordingIdData.postValue(value.data ?: 0L)
                                }
                                is Resource.Loading -> {
                                    _databaseState.postValue(DataBaseState(isLoading = true))

                                }
                                is Resource.DataError -> {
                                    _databaseState.postValue(
                                        DataBaseState(
                                            isError = true,
                                            errorDescription = value.errorMessage ?: "Error"
                                        )
                                    )
                                }
                            }
                        }
                    })
            }
        } else {
            _databaseState.postValue(
                DataBaseState(
                    isError = true,
                    errorDescription = "Recording is empty"
                )
            )
        }
    }

    fun cancelSaving() {
        recordingManager?.cancelSaving()
        _recordState.postValue(RecordingManagerStatus.SETUP)
    }

    fun updateUI() {
        recordingManager?.updateUI()
    }


    override fun onCleared() {
        super.onCleared()
        finishRecording()
        cancelSaving()
    }

    fun getRecordedRoute(routeRecordingId: Long): LiveData<RouteRecordingEntity> {
        return databaseRepository.getRouteRecording(
            routeRecordingId,
            PreferencesManager.getUserIdName()
        ).asLiveData()
    }


    fun getOfflineRegionById(regionOfflineId: Long) {
        MyOfflineManager.getInstance().getOfflineRegionById(regionOfflineId)
        MyOfflineManager.getInstance().offlineRegion.observeForever {
            _offlineRegion.postValue(it)
        }
    }

    fun createUserRecording(userRecording: RouteRecordingEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                RetrofitRepository.getInstance().createUserRecording(userRecording).collect {
                    when (it) {
                        is Resource.Success -> {
                            _createUserRecordingState.postValue(DownloadRecordingState(isSuccess = true))
                            _routeRecordingDatabaseId.postValue(it.data?.recordingId ?: 0L)
                        }
                        is Resource.Loading -> {
                            _createUserRecordingState.postValue(DownloadRecordingState(isLoading = true))

                        }
                        is Resource.DataError -> {
                            _createUserRecordingState.postValue(
                                DownloadRecordingState(
                                    isError = true,
                                    errorDescription = it.errorMessage ?: "Error"
                                )
                            )
                        }
                    }
                }
            }
        }

    }
}