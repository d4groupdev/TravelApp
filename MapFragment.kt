package com.example.map

import android.Manifest
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.github.mikephil.charting.data.Entry
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.example.R
import com.example.adapters.map.BottomSheetAdapter
import com.example.adapters.map.MapListViewAdapter
import com.example.databinding.FragmentMapBinding
import com.example.models.MapRoute
import com.example.repositories.MyLocationManager
import com.example.repositories.PreferencesManager
import com.example.utils.MyUtils
import com.example.utils.ChartFormatter
import com.example.viemodels.MapViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.math.roundToInt


class MapFragment : Fragment(), LocationEngineCallback<LocationEngineResult> {
    private lateinit var binding: FragmentMapBinding
    private val viewModel: MapViewModel by activityViewModels()
    private val bottomSheetAdapter = BottomSheetAdapter()
    private var mapListAdapter = MapListViewAdapter()
    private var mapBoxManager: MapboxMap? = null
    private lateinit var mapStyle: Style
    var routesList = mutableListOf<MapRoute>()


    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Mapbox.getInstance(
            requireContext(),
            resources.getString(R.string.map_box_public_token)
        )
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false)
        binding.btnListView.setOnClickListener {
            binding.isMap = false
        }
        binding.btnMapView.setOnClickListener { binding.isMap = true }
        initObservers()
        binding.layoutBottomSheet.shimmerChart.visibility = View.GONE
        binding.layoutBottomSheet.chartElevationGraph.visibility = View.VISIBLE

        binding.rvListView.apply {
            adapter = mapListAdapter
            mapListAdapter.setOnClickListener {
                viewModel.getSingleRoute(it.postId)
                val currentCameraPosition: CameraPosition = mapBoxManager!!.cameraPosition
                viewModel.initCamera = currentCameraPosition
                viewModel.getSingleRouteByPostId(it.postId)
                viewModel.initCamera = currentCameraPosition
                val deepLink =
                    Uri.parse("com.example://internal_navigation_fragment_route_detail")
                findNavController().navigate(deepLink)

            }
        }
        binding.layoutBottomSheet.btnSeeDetails.setOnClickListener {
            val currentCameraPosition: CameraPosition =
                mapBoxManager!!.cameraPosition
            viewModel.initCamera = currentCameraPosition
            val deepLink =
                Uri.parse("com.example://internal_navigation_fragment_route_detail")
            findNavController().navigate(deepLink)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layer = viewModel.getStringLayer()
        print(layer)
        binding.mapView.getMapAsync { mapboxMap ->
            mapBoxManager = mapboxMap
            mapboxMap.setStyle(
                Style.Builder()
                    .fromUri(layer)
            ) { style ->
                mapStyle = style
                enableLocationComponent()

                val locationComponentOptions = LocationComponentOptions.builder(requireContext())
                    .pulseEnabled(true)
                    .bearingTintColor(requireActivity().getColor(R.color.adventureBlue))
                    .accuracyAlpha(0.4f)
                    .build()

                val locationComponentActivationOptions = LocationComponentActivationOptions
                    .builder(requireContext(), mapStyle)
                    .locationComponentOptions(locationComponentOptions)
                    .useDefaultLocationEngine(true)
                    .build()

                val locationComponent = mapboxMap.locationComponent
                locationComponent.activateLocationComponent(locationComponentActivationOptions)
            }
            onMapCameraMove()
        }
        binding.btnPointToNorth.setOnClickListener {
            mapBoxManager?.resetNorth()
        }
        binding.ivFilter.setOnClickListener {
            val currentCameraPosition: CameraPosition = mapBoxManager!!.cameraPosition
            viewModel.initCamera = currentCameraPosition
            findNavController().navigate(R.id.action_mapFragment_to_filterAndSortFragment2)
        }

        binding.btnMaximize.setOnClickListener {
            binding.isMinimized = false
        }
        binding.btnMinimize.setOnClickListener {
            binding.isMinimized = true
        }
    }

    private fun initLocationEngine() {
        val locationEngine = LocationEngineProvider.getBestLocationEngine(requireContext())
        val request = LocationEngineRequest.Builder(1000)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(5000).build()
        locationEngine.requestLocationUpdates(request, this, Looper.getMainLooper())
        locationEngine.getLastLocation(this)
    }

    private fun enableLocationComponent() {
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            val locationComponent = mapBoxManager?.locationComponent
            locationComponent?.activateLocationComponent(
                LocationComponentActivationOptions.builder(
                    requireContext(),
                    mapStyle
                ).build()
            )
            locationComponent?.isLocationComponentEnabled = true

            locationComponent?.renderMode = RenderMode.COMPASS
            locationComponent?.cameraMode = CameraMode.TRACKING

            initLocationEngine()
            if (viewModel.initCamera != null) {
                mapBoxManager?.animateCamera(CameraUpdateFactory.newCameraPosition(viewModel.initCamera!!))
                viewModel.initCamera = null
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MyLocationManager.LOCATION_REQUEST_CODE
            )
        }
    }

    override fun onSuccess(result: LocationEngineResult?) {
        if (result != null) {
            if (result.lastLocation != null) {
                val position = CameraPosition.Builder()
                    .target(
                        LatLng(
                            result.lastLocation!!.latitude,
                            result.lastLocation!!.longitude
                        )
                    )
                    .zoom(10.0)
                    .build()
                mapBoxManager?.animateCamera(
                    CameraUpdateFactory.newCameraPosition(position),
                    10
                )
            }
        }
        binding.btnZoomToUser.setOnClickListener {
            if (result != null)
                if (result.lastLocation != null) {
                    val position = CameraPosition.Builder()
                        .target(
                            LatLng(
                                result.lastLocation!!.latitude,
                                result.lastLocation!!.longitude
                            )
                        )
                        .zoom(10.0)
                        .build()
                    mapBoxManager?.animateCamera(
                        CameraUpdateFactory.newCameraPosition(position),
                        10
                    )
                }
        }
    }

    override fun onFailure(exception: Exception) {
        Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
    }


    private fun initObservers() {
        viewModel.routesList.observe(viewLifecycleOwner) {
            bottomSheetAdapter.list = it
            mapListAdapter.list = it
        }

        viewModel.errorData.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }

        viewModel.downloadRouteState.observe(viewLifecycleOwner) {
            when {
                it.isLoading -> {
                    binding.layoutBottomSheet.shimmerChart.startShimmer()
                    binding.layoutBottomSheet.shimmerChart.visibility = View.VISIBLE
                    binding.layoutBottomSheet.chartElevationGraph.visibility = View.GONE
                }
                it.isError -> {
                    Toast.makeText(requireContext(), it.errorDescription, Toast.LENGTH_SHORT)
                        .show()
                }
                it.isSuccess -> {
                    binding.layoutBottomSheet.shimmerChart.stopShimmer()
                    binding.layoutBottomSheet.shimmerChart.visibility = View.GONE
                    binding.layoutBottomSheet.chartElevationGraph.visibility = View.VISIBLE
                }
            }

        }
        viewModel.browseRouteDetailData.observe(viewLifecycleOwner) { model ->
            if (model != null) {
                binding.layoutBottomSheet.tvName.text = model.routeModelEntity.title
                binding.layoutBottomSheet.tvLocation.text = model.routeModelEntity.region
                binding.layoutBottomSheet.tvTimeValue.text = model.routeModelEntity.duration
                Glide.with(binding.layoutBottomSheet.imageView.context)
                    .load(model.routeModelEntity.headerMobile)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_image)
                    .into(binding.layoutBottomSheet.imageView)

                binding.layoutBottomSheet.tvDifficult.text =
                    model.routeModelEntity.difficulty.let { it1 -> MyUtils.getDifficulty(it1) }
                binding.layoutBottomSheet.tvRate.text =
                    "${model.routeModelEntity.ratings.toInt()} / 10"
                when (PreferencesManager.getUnits()) {
                    1 -> {
                        if (model.routeModelEntity.elevationUnit == "m") {
                            binding.layoutBottomSheet.tvElevationValue.text =
                                "${MyUtils.transformMetresToFeet(model.routeModelEntity.elevationValue)} ft"
                        } else {
                            binding.layoutBottomSheet.tvElevationValue.text =
                                "${model.routeModelEntity.elevationValue} ft"
                        }
                        if (model.routeModelEntity.distanceUnit == "km") {
                            binding.layoutBottomSheet.tvDistanceValue.text =
                                "${MyUtils.transformKmToMiles(model.routeModelEntity.distanceValue)} mi"
                        } else {
                            binding.layoutBottomSheet.tvDistanceValue.text =
                                "${model.routeModelEntity.distanceValue} mi"
                        }
                    }
                    else -> {
                        if (model.routeModelEntity.elevationUnit == "m") {
                            binding.layoutBottomSheet.tvElevationValue.text =
                                "${model.routeModelEntity.elevationValue} m"
                        } else {
                            binding.layoutBottomSheet.tvElevationValue.text =
                                "${MyUtils.transformFeetToMetres(model.routeModelEntity.elevationValue)} m"
                        }
                        if (model.routeModelEntity.distanceUnit == "km") {
                            binding.layoutBottomSheet.tvDistanceValue.text =
                                "${model.routeModelEntity.distanceValue} km"

                        } else {
                            binding.layoutBottomSheet.tvDistanceValue.text =
                                "${MyUtils.transformMilesToKm(model.routeModelEntity.distanceValue)} km"
                        }
                    }
                }
                val entryList = arrayListOf<Entry>()
                if (model.elevationData?.isNotEmpty() == true) {
                    for (i in model.elevationData!!) {
                        when (PreferencesManager.getUnits()) {
                            1 -> {
                                val roundDistance = (i.distance * 62).roundToInt() / 100.0
                                val roundElevation = (i.elevation * 3.28).roundToInt()
                                entryList.add(
                                    Entry(
                                        roundDistance.toFloat(),
                                        roundElevation.toFloat()
                                    )
                                )
                            }
                            else -> {
                                val roundDistance = (i.distance.times(10.0)).roundToInt().div(10.0)
                                val roundElevation = i.elevation.roundToInt()
                                entryList.add(
                                    Entry(
                                        roundDistance.toFloat(),
                                        roundElevation.toFloat()
                                    )
                                )
                            }
                        }

                    }
                    setChartData(entryList)
                }
            }
        }
    }

    private fun setChartData(plotValues: ArrayList<Entry>) {
        val data = ChartFormatter.getData(
            plotValues,
            requireContext(),
            binding.layoutBottomSheet.chartElevationGraph
        )
        data.let {
            ChartFormatter.setupChart(
                binding.layoutBottomSheet.chartElevationGraph,
                it,
                Color.rgb(255, 255, 255), requireContext()
            )
        }
    }

    private fun onMapCameraMove() {
        mapBoxManager?.addOnCameraMoveListener {
            queryMapboxFeatures()
        }

    }

    private fun queryMapboxFeatures() {
        val rectF = RectF(
            binding.mapView.left.toFloat(),
            binding.mapView.top.toFloat(),
            binding.mapView.right.toFloat(),
            binding.mapView.bottom.toFloat()
        )
        val features: MutableList<Feature> =
            mapBoxManager!!.queryRenderedFeatures(rectF, "newLayer")
        viewModel.getRoutesList(features)
    }
}