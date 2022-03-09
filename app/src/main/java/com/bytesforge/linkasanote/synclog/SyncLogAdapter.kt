/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.bytesforge.linkasanote.synclog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bytesforge.linkasanote.data.SyncResult
import com.bytesforge.linkasanote.databinding.ItemSyncLogBinding

class SyncLogAdapter(
    syncResults: List<SyncResult>,
    viewModel: SyncLogViewModel
) : RecyclerView.Adapter<SyncLogAdapter.ViewHolder>() {
    private val viewModel: SyncLogViewModel

    // Items
    var syncResults: List<SyncResult>
        private set

    class ViewHolder(private val binding: ItemSyncLogBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(
            position: Int, showStarted: Boolean,
            syncResult: SyncResult?, viewModel: SyncLogViewModel?
        ) {
            binding.position = position
            binding.showStarted = showStarted
            binding.syncResult = syncResult
            binding.viewModel = viewModel // NOTE: global viewModel for fragment and all items
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSyncLogBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val syncResult = syncResults[position]
        var showStarted = false
        if (position <= 0) {
            showStarted = true
        } else {
            val prevSyncResult = syncResults[position - 1]
            if (syncResult.started != prevSyncResult.started) {
                showStarted = true
            }
        }
        holder.bind(position, showStarted, syncResult, viewModel)
    }

    override fun getItemCount(): Int {
        return syncResults.size
    }

    override fun getItemId(position: Int): Long {
        return syncResults[position].rowId
    }

    fun swapItems(syncResults: List<SyncResult>) {
        this.syncResults = syncResults
        notifyDataSetChanged()
    }

    companion object {
        private val TAG = SyncLogAdapter::class.java.simpleName
        private val TAG_E = SyncLogAdapter::class.java.canonicalName
    }

    init {
        this.syncResults = syncResults
        this.viewModel = viewModel
        setHasStableIds(true)
    }
}