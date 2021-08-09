package com.example.explore

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.R
import com.example.adapters.explore.ExploreTopRegionsAdapter
import com.example.adapters.explore.RegionTopTrailsAdapter
import com.example.databinding.FragmentRegionBinding
import com.example.models.api.GeoPoint
import com.example.repositories.MyLocationManager
import com.example.utils.MarginItemDecoration
import com.example.viemodels.ExploreViewModel
import kotlinx.coroutines.InternalCoroutinesApi

class RegionFragment : Fragment() {

    private lateinit var binding: FragmentRegionBinding
    private lateinit var topTrailsAdapter: RegionTopTrailsAdapter
    private val viewModel: ExploreViewModel by activityViewModels()
    private val topRegionAdapter = ExploreTopRegionsAdapter()
    private var regionDatabaseId = ""
    private var activityDatabaseId = ""
    private var search = ""
    private lateinit var locationManager: MyLocationManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegionBinding.inflate(inflater, container, false)
        binding.shimmerTopTrailsRegionLayout.startShimmer()
        binding.shimmerTopTrailsRegionLayout.startShimmer()
        binding.shimmerTopTrailsRegionLayout.visibility = View.VISIBLE
        binding.rvTopTrails.apply {
            topTrailsAdapter = RegionTopTrailsAdapter()
            adapter = topTrailsAdapter
            addItemDecoration(MarginItemDecoration(0, 0, 0, 10))
            topTrailsAdapter.setOnClickListener {
                viewModel.setRouteId(it.id)
                findNavController().navigate(R.id.action_regionFragment_to_routeDetailFragment)
            }
        }
        val appBarConfiguration = AppBarConfiguration(findNavController().graph)
        binding.toolbar.setupWithNavController(findNavController(), appBarConfiguration)

        binding.rvTopRegions.apply {
            adapter = topRegionAdapter
            addItemDecoration(MarginItemDecoration(0, 10))
            topRegionAdapter.setOnClickListener {
                viewModel.getSingleRegion(it.id)
            }
        }
        binding.ibtnFilter.setOnClickListener {
            findNavController().navigate(R.id.action_regionFragment_to_filterAndSortFragment)
        }
        locationManager = MyLocationManager.getInstance()
        initObservers()
        return binding.root
    }

    @InternalCoroutinesApi
    override fun onResume() {
        super.onResume()
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
                        viewModel.search(
                            s.toString(), GeoPoint(
                                locationManager.location.value?.latitude,
                                locationManager.location.value?.longitude
                            ), null
                        )
                        viewModel.searchText = s.toString()
                    }
                }
            }

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != viewModel.searchText) {
                    if (s.isNotBlank()) {
                        viewModel.search(
                            s.toString(), GeoPoint(
                                locationManager.location.value?.latitude,
                                locationManager.location.value?.longitude
                            ), null
                        )
                        viewModel.searchText = s.toString()
                    }
                }
            }
        })
    }

    private fun initObservers() {

        viewModel.singleRegionData.observe(viewLifecycleOwner) {
            regionDatabaseId = it.databaseID.toString()
            Glide.with(requireActivity()).load(it.imgUrl).centerCrop()
                .placeholder(R.drawable.bg_tv_blue_rounded_4dp).into(binding.ivRouteMap)
            binding.tvTopTrailsIn.text = "Top trails in ${it.name}"
            binding.toolbar.title = it?.name
            if (regionDatabaseId != "") {
                if (activityDatabaseId != "") {
                    viewModel.getTopTrail(regionDatabaseId, activityDatabaseId)
                } else {
                    viewModel.getAllTopTrailsRegion(regionDatabaseId)
                }
            }
        }

        viewModel.topRegionsData.observe(viewLifecycleOwner) {
            topRegionAdapter.setItems(it)
        }
        viewModel.singleRegionDownloadState.observe(viewLifecycleOwner) {
            when {
                it.isLoading -> {
                    binding.tvTopTrailsIn.visibility = View.GONE
                    binding.shimmerRegionLayout.startShimmer()
                    binding.shimmerRegionLayout.visibility = View.VISIBLE
                }
                it.isError -> {
                    Toast.makeText(requireContext(), it.errorDescription, Toast.LENGTH_SHORT).show()
                }
                it.isSuccess -> {
                    binding.shimmerRegionLayout.stopShimmer()
                    binding.shimmerRegionLayout.visibility = View.GONE
                    binding.tvTopTrailsIn.visibility = View.VISIBLE
                }
            }
        }
        viewModel.regionRoutesDownloadState.observe(viewLifecycleOwner) {
            when {
                it.isLoading -> {
                    binding.rvTopTrails.visibility = View.GONE
                    binding.shimmerTopTrailsRegionLayout.startShimmer()
                    binding.shimmerTopTrailsRegionLayout.visibility = View.VISIBLE

                }
                it.isError -> {
                    Toast.makeText(requireContext(), it.errorDescription, Toast.LENGTH_SHORT).show()
                }
                it.isSuccess -> {
                    binding.shimmerTopTrailsRegionLayout.stopShimmer()
                    binding.shimmerTopTrailsRegionLayout.visibility = View.GONE
                    binding.rvTopTrails.visibility = View.VISIBLE
                }
                it.isEmpty -> {
                    binding.rvTopTrails.visibility = View.GONE
                    binding.shimmerTopTrailsRegionLayout.startShimmer()
                    binding.shimmerTopTrailsRegionLayout.visibility = View.VISIBLE
                }
            }
        }

        viewModel.browseRegionRoutes.observe(viewLifecycleOwner) {
            topTrailsAdapter.setItems(it)
        }

        viewModel.errorData.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        viewModel.markedRouteData.observe(viewLifecycleOwner) {
            topTrailsAdapter.setMarkedData(it)
        }
        viewModel.downloadedRouteData.observe(viewLifecycleOwner) {
            topTrailsAdapter.setDownLoadedData(it)
        }

        viewModel.searchState.observe(viewLifecycleOwner) {
            when {
                it.isSuccess -> {
                    findNavController().navigate(R.id.action_regionFragment_to_searchFragment)
                }
            }
        }
    }
}