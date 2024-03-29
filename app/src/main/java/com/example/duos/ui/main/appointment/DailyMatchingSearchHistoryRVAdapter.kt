package com.example.duos.ui.main.appointment

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.duos.data.entities.dailyMatching.SearchHistoryData
import com.example.duos.databinding.ItemDailyMatchingSearchBinding

class DailyMatchingSearchHistoryRVAdapter(
    val historyDeleteClickListener: (String) -> Unit,
    val historyItemClickListener: (String) -> Unit
) :
    ListAdapter<SearchHistoryData, DailyMatchingSearchHistoryRVAdapter.ViewHolder>(diffUtil) {

    inner class ViewHolder(private val binding: ItemDailyMatchingSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(searchHistoryModel: SearchHistoryData) {
            binding.itemDailyMatchingRecentSearchTv.text = searchHistoryModel.keyword
            binding.itemDailyMatchingRecentSearchDeleteIv.setOnClickListener {
                historyDeleteClickListener(searchHistoryModel.keyword.orEmpty())
            }
            binding.itemDailyMatchingRecentSearchTv.setOnClickListener {
                historyItemClickListener(searchHistoryModel.keyword.orEmpty())
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemDailyMatchingSearchBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
        Log.d("DailyMatchingSearchActivity", "onBindViewHolder의 currentList[position] ${currentList[position]}")
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<SearchHistoryData>() {
            override fun areItemsTheSame(oldItem: SearchHistoryData, newItem: SearchHistoryData): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: SearchHistoryData,
                newItem: SearchHistoryData
            ): Boolean {
                return oldItem.keyword == newItem.keyword
            }

        }
    }
}