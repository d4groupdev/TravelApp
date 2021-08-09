package com.example.explore

import android.Manifest
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.mapbox.android.core.permissions.PermissionsManager
import com.example.R
import com.example.adapters.explore.ExploreBrowseActivityAdapter
import com.example.adapters.explore.ExploreTopRegionsAdapter
import com.example.adapters.explore.ExploreTopTrailsAdapter
import com.example.databinding.FragmentExploreBinding
import com.example.models.api.GeoPoint
import com.example.repositories.MyLocationManager
import com.example.utils.MarginItemDecoration
import com.example.viemodels.ExploreViewModel
import kotlinx.coroutines.*


class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private lateinit var binding: FragmentExploreBinding
    private val viewModel: ExploreViewModel by activityViewModels()
    private val topRegionAdapter = ExploreTopRegionsAdapter()
    private val browseByActivityAdapter = ExploreBrowseActivityAdapter()
    private val topTrailsAdapter = ExploreTopTrailsAdapter()
    private lateinit var locationManager: MyLocationManager

    @InternalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExploreBinding.inflate(inflater)
        binding.shimmerLayout.startShimmer()
        binding.shimmerActivityLayout.startShimmer()
        binding.shimmerTopTrailsLayout.startShimmer()
        binding.rvTopRegions.apply {
            adapter = topRegionAdapter
            addItemDecoration(MarginItemDecoration(0, 10))
            topRegionAdapter.setOnClickListener {
                viewModel.getSingleRegion(it.id)
                findNavController().navigate(R.id.action_exploreFragment_to_regionFragment)
            }
            topRegionAdapter.setOnMoreClickListener {
                viewModel.fetchTopRegionPagination(it)
            }
        }
        binding.rvBrowseActivity.apply {
            adapter = browseByActivityAdapter
            addItemDecoration(MarginItemDecoration(0, 10))
        }
        binding.rvTopTrails.apply {
            adapter = topTrailsAdapter
            addItemDecoration(MarginItemDecoration(0, 10))
            topTrailsAdapter.setOnClickListener {
                viewModel.setRouteId(it.id)
                findNavController().navigate(R.id.action_exploreFragment_to_routeDetailFragment)
            }
            topTrailsAdapter.setOnMoreClickListener {
                viewModel.fetchTopTrailsPagination(it)
            }
        }

        binding.ibtnFilter.setOnClickListener {
            findNavController().navigate(R.id.action_exploreFragment_to_filterAndSortFragment)
        }
        binding.btnViewMap.setOnClickListener {
            findNavController().navigate(R.id.map)
        }
        locationManager = MyLocationManager.getInstance()
        initObservers()
        return binding.root
    }


    @InternalCoroutinesApi
    override fun onResume() {
        super.onResume()
        checkLocationPermission()
        binding.etSearch.setText(viewModel.searchText)
        setOnTextChange()
    }

    @InternalCoroutinesApi
    private fun setOnTextChange() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s != viewModel.searchText) {
                    if (s.isNotBlank()) {
                        viewModel.search(s.toString(), GeoPoint(locationManager.location.value?.latitude,
                            locationManager.location.value?.longitude),null)
                        viewModel.searchText = s.toString()
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != viewModel.searchText){
                    if (s.isNotBlank()) {
                        viewModel.search(s.toString(), GeoPoint(locationManager.location.value?.latitude,
                        locationManager.location.value?.longitude),null)
                        viewModel.searchText = s.toString()
                    }
                }
            }
        })
    }

    private fun initObservers() {
        locationManager.location.observe(viewLifecycleOwner) {
            if (it != null) {
                viewModel.getTopTrails(it)
                viewModel.getTopRegions(it)
            }
        }
        viewModel.topRegionsData.observe(viewLifecycleOwner) {
            topRegionAdapter.setItems(it)
            if (it.isNotEmpty()) {
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
            } else {
                binding.shimmerLayout.startShimmer()
                binding.shimmerLayout.visibility = View.VISIBLE
            }
        }

        viewModel.topTrailsData.observe(viewLifecycleOwner) {
            topTrailsAdapter.setItems(it)
            if (it.isNotEmpty()) {
                binding.shimmerTopTrailsLayout.stopShimmer()
                binding.shimmerTopTrailsLayout.visibility = View.GONE
            } else {
                binding.shimmerTopTrailsLayout.startShimmer()
                binding.shimmerTopTrailsLayout.visibility = View.VISIBLE
            }

        }
        viewModel.markedRouteData.observe(viewLifecycleOwner) {
            topTrailsAdapter.setMarkedData(it)
        }
        viewModel.downloadedRouteData.observe(viewLifecycleOwner) {
            topTrailsAdapter.setDownLoadedData(it)
        }

        viewModel.errorData.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
        viewModel.browseActivityData.observe(viewLifecycleOwner, {
            browseByActivityAdapter.setItems(it)
            if (it.isNotEmpty()) {
                binding.shimmerActivityLayout.stopShimmer()
                binding.shimmerActivityLayout.visibility = View.GONE
            } else {
                binding.shimmerActivityLayout.startShimmer()
                binding.shimmerActivityLayout.visibility = View.VISIBLE
            }
        })
        viewModel.searchState.observe(viewLifecycleOwner) {
            when {
                it.isSuccess -> {
                    findNavController().navigate(R.id.action_exploreFragment_to_searchFragment)
                }
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