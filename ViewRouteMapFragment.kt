package com.example.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.R
import com.example.dataBase.entity.RouteModelEntity
import com.example.databinding.FragmentViewRecordedRouteMapBinding
import com.example.viemodels.RecordViewModel
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style


class ViewRouteMapFragment : Fragment() {
    private lateinit var binding: FragmentViewRecordedRouteMapBinding
    private val viewModel: RecordViewModel by activityViewModels()
    private var mapBoxManager: MapboxMap? = null
    private var mapStyle: Style? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Mapbox.getInstance(
            requireContext(),
            resources.getString(R.string.map_box_public_token)
        )
        binding = FragmentViewRecordedRouteMapBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val route = arguments?.get("route") as RouteModelEntity
        binding.arrowLeft.setOnClickListener {
            findNavController().popBackStack()
        }
        val layer = viewModel.getStringLayer()
        binding.mapView.getMapAsync { mapboxMap ->
            mapBoxManager = mapboxMap
            mapboxMap.setStyle(
                Style.Builder()
                    .fromUri(layer)
            ) { style ->
                mapStyle = style
                getMapBounds(route)
            }
        }
    }

    private fun getMapBounds(route: RouteModelEntity) {
        val minLat = route.minLat
        val maxLat = route.maxLat
        val minLong = route.minLong
        val maxLong = route.maxLong

        val latLngBounds = LatLngBounds.Builder()
            .include(LatLng(maxLat, maxLong))
            .include(LatLng(minLat, minLong))
            .build()

        mapBoxManager?.easeCamera(
            CameraUpdateFactory.newLatLngBounds(latLngBounds, 100),
            1000
        )
    }
}