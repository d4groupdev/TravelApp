package com.example.adapters.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.R
import com.example.ExampleApp
import com.example.databinding.ItemExploreTopTrailsBinding
import com.example.models.ExploreTopTrailsModel
import com.example.repositories.PreferencesManager
import com.example.utils.MyUtils
import kotlin.math.roundToInt

class RegionTopTrailsAdapter:
    RecyclerView.Adapter<RegionTopTrailsAdapter.RegionTopTrailsViewHolder>() {

    private var items: List<ExploreTopTrailsModel> = listOf()
    private var markedRoutes: List<String?> = listOf()
    private var downloadedData: List<String?> = listOf()

    private var onClick: ((ExploreTopTrailsModel) -> Unit)? = null

    fun setItems(items: List<ExploreTopTrailsModel>) {
        if (items.size > 10) {
            this.items = items.subList(0, 10)
        } else this.items = items
        notifyDataSetChanged()
    }

    fun setMarkedData(markedRoutes: List<String?>) {
        this.markedRoutes = markedRoutes
        notifyDataSetChanged()
    }

    fun setDownLoadedData(downloadedData: List<String?>) {
        this.downloadedData = downloadedData
        notifyDataSetChanged()
    }

    fun clear() {
        items = listOf()
        markedRoutes = listOf()
        downloadedData = listOf()
        notifyDataSetChanged()
    }

    fun setOnClickListener(onClick: ((ExploreTopTrailsModel) -> Unit)) {
        this.onClick = onClick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegionTopTrailsViewHolder {
        return RegionTopTrailsViewHolder(
            ItemExploreTopTrailsBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RegionTopTrailsViewHolder, position: Int) {
        if (position < 10) {
            holder.bind(items[position], markedRoutes, downloadedData)
        }

    }

    override fun getItemCount() = items.count()

    inner class RegionTopTrailsViewHolder(binding: ItemExploreTopTrailsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val mBinding = binding
        fun bind(
            topTrailModel: ExploreTopTrailsModel,
            markedRoutes: List<String?>,
            downloadedData: List<String?>
        ) {
            mBinding.tvName.text = topTrailModel.name
            val rate = if (topTrailModel.rate > 0 && topTrailModel.rate < 1) {
                topTrailModel.rate.times(10).toInt()
            } else topTrailModel.rate.toInt()
            if (markedRoutes.contains(topTrailModel.databaseId.toString())) {
                mBinding.ivStar.setImageResource(R.drawable.ic_star_yellow)
            } else {
                mBinding.ivStar.setImageResource(R.drawable.ic_star)
            }
            if (downloadedData.contains(topTrailModel.databaseId.toString())) {
                mBinding.ivDownload.setColorFilter(ContextCompat.getColor(ExampleApp.context, R.color.adventureYellow), android.graphics.PorterDuff.Mode.SRC_IN)
            }
            mBinding.tvDifficult.text = MyUtils.getDifficulty(topTrailModel.difficult)
            when (PreferencesManager.getUnits()) {
                1 -> {
                    val elevationFeet = (topTrailModel.elevation * 3.28084).roundToInt()
                    val distanceMiles = (topTrailModel.distance * 62.1371).roundToInt() / 100.0
                    mBinding.tvElevation.text = "$elevationFeet ft"
                    mBinding.tvDistance.text = "$distanceMiles mi"
                }
                else -> {
                    mBinding.tvElevation.text = "${topTrailModel.elevation.roundToInt()} m"
                    mBinding.tvDistance.text = "${topTrailModel.distance} km"
                }
            }
            mBinding.tvEstimatedTime.text = topTrailModel.duration
            mBinding.tvRate.text = "$rate / 10"
            mBinding.tvLocation.text = topTrailModel.location
            Glide.with(mBinding.root).load(topTrailModel.imgUrl).centerCrop()
                .placeholder(R.drawable.placeholder_image).into(mBinding.ivBackground)
            mBinding.root.setOnClickListener { onClick?.let { it1 -> it1(topTrailModel) } }
        }
    }
}