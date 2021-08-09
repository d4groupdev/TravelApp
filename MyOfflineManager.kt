package com.example.repositories

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.offline.*
import com.mapbox.mapboxsdk.offline.OfflineManager.CreateOfflineRegionCallback
import com.mapbox.mapboxsdk.offline.OfflineRegion.OfflineRegionObserver
import com.example.ExampleApp
import com.example.ExampleApp.Companion.context
import com.example.models.RouteModel
import com.example.models.states.DownloadRegionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject


class MyOfflineManager {
    private val offlineManager = OfflineManager.getInstance(context)
    private val _downloadState = MutableLiveData<DownloadRegionState>()
    val downloadState: LiveData<DownloadRegionState> = _downloadState

    private val _downloadPercent = MutableLiveData<Int>()
    val downloadPercent: LiveData<Int> = _downloadPercent

    private val _downloadTilesCount = MutableLiveData<Long>()
    val downloadTilesCount: LiveData<Long> = _downloadTilesCount

    private val _offlineRegion = MutableLiveData<OfflineRegion?>()
    val offlineRegion: LiveData<OfflineRegion?> = _offlineRegion

    val appDatabaseRepository = AppDatabaseRepository(ExampleApp.app)
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _offlineRegionId = MutableLiveData<Long>()
    val offlineRegionId: LiveData<Long> = _offlineRegionId


    companion object {
        private var instance: MyOfflineManager? = null
        private const val STYLE_STANDARD = "mapbox://styles/my_style"
        private const val JSON_FIELD_REGION_NAME = "routeName + regionName"


        fun getInstance(): MyOfflineManager {
            if (instance == null) {
                instance = MyOfflineManager()
            }
            return instance!!
        }
    }


    fun downLoadRegion(routeModel: RouteModel) {
        _offlineRegionId.postValue(0L)
        _downloadState.postValue(DownloadRegionState(isLoading = true))
        _downloadPercent.postValue(0)
        _downloadTilesCount.postValue(0)

        val latLngBounds =
            LatLngBounds.Builder()
                .include(
                    LatLng(
                        routeModel.routeModelEntity.maxLat,
                        routeModel.routeModelEntity.maxLong
                    )
                ) // Northeast
                .include(
                    LatLng(
                        routeModel.routeModelEntity.minLat,
                        routeModel.routeModelEntity.minLong
                    )
                ) // Southwest
                .build()
        val title = routeModel.routeModelEntity.title
        val region = routeModel.routeModelEntity.region

        val definition = OfflineTilePyramidRegionDefinition(
            STYLE_STANDARD,
            latLngBounds,
            10.0,
            20.0,
            context.resources.displayMetrics.density
        )

        val metadata = try {
            val jsonObject = JSONObject()
            jsonObject.put(JSON_FIELD_REGION_NAME, title + region)
            val json = jsonObject.toString()
            json.encodeToByteArray()
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to encode metadata: " + exception.message)
            _downloadState.postValue(
                DownloadRegionState(
                    isError = true,
                    errorDescription = "Failed to encode metadata: " + exception.message
                )
            )
            null
        }
        offlineManager.setOfflineMapboxTileCountLimit(20000000)
        metadata?.let {
            offlineManager.createOfflineRegion(definition, it,
                object : CreateOfflineRegionCallback {
                    override fun onCreate(offlineRegion: OfflineRegion) {
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)

                        offlineRegion.setObserver(object : OfflineRegionObserver {
                            override fun onStatusChanged(status: OfflineRegionStatus) {
                                val percentage =
                                    if (status.requiredResourceCount >= 0) 100.0 * status.completedResourceCount / status.requiredResourceCount else 0.0
                                _downloadTilesCount.postValue(status.completedTileCount)
                                if (percentage == 100.0 && status.isComplete) {
                                    _downloadState.postValue(
                                        DownloadRegionState(
                                            isNeedDownload = true,
                                            offlineRegionId = offlineRegion.id
                                        )
                                    )
                                } else {
                                    _downloadState.postValue(DownloadRegionState(isLoading = true))
                                    _downloadPercent.postValue(percentage.toInt())
                                }
                            }

                            override fun onError(error: OfflineRegionError) {
                                _downloadState.postValue(
                                    DownloadRegionState(
                                        isError = true,
                                        errorDescription = "${error.reason} ${error.message}"
                                    )
                                )

                            }

                            override fun mapboxTileCountLimitExceeded(limit: Long) {
                                _downloadState.postValue(
                                    DownloadRegionState(
                                        isError = true,
                                        errorDescription = "Mapbox tile count limit exceeded: $limit"
                                    )
                                )
                            }
                        })
                    }

                    override fun onError(error: String) {
                        _downloadState.postValue(
                            DownloadRegionState(
                                isError = true,
                                errorDescription = error
                            )
                        )
                    }
                })
        }
    }


    fun getOfflineRegionById(regionOfflineId: Long) {

        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<out OfflineRegion>?) {
                if (offlineRegions != null) {
                    for (item in offlineRegions) {
                        print(item)
                        if (item.id == regionOfflineId)
                            _offlineRegion.postValue(item)
                    }
                }
            }

            override fun onError(error: String?) {
                _offlineRegion.postValue(null)
            }
        })
    }
}
