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

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bytesforge.linkasanote.data.SyncResult
import com.bytesforge.linkasanote.databinding.FragmentSyncLogBinding
import com.google.common.base.Preconditions

class SyncLogFragment : Fragment(), SyncLogContract.View {
    private var presenter: SyncLogContract.Presenter? = null
    private var viewModel: SyncLogContract.ViewModel? = null
    var adapter: SyncLogAdapter? = null
    var rvLayoutManager: LinearLayoutManager? = null
    private var rvLayoutState: Parcelable? = null

    override fun onResume() {
        super.onResume()
        presenter!!.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter!!.unsubscribe()
    }

    override val isActive: Boolean
        get() = isAdded

    override fun setPresenter(presenter: SyncLogContract.Presenter) {
        this.presenter = Preconditions.checkNotNull(presenter)
    }

    override fun setViewModel(viewModel: SyncLogContract.ViewModel) {
        this.viewModel = Preconditions.checkNotNull(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSyncLogBinding.inflate(inflater, container, false)
        viewModel!!.setInstanceState(savedInstanceState)
        setRvLayoutState(savedInstanceState)
        binding.viewModel = viewModel as SyncLogViewModel?
        setupSyncLogRecyclerView(binding.rvSyncLog)
        return binding.root
    }

    private fun setupSyncLogRecyclerView(rvSyncLog: RecyclerView) {
        val syncResults: List<SyncResult> = ArrayList()
        adapter = SyncLogAdapter(syncResults, (viewModel as SyncLogViewModel?)!!)
        rvSyncLog.adapter = adapter
        rvLayoutManager = LinearLayoutManager(context)
        rvSyncLog.layoutManager = rvLayoutManager
    }

    private fun setRvLayoutState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            rvLayoutState = savedInstanceState.getParcelable(
                SyncLogViewModel.Companion.STATE_RECYCLER_LAYOUT
            )
        }
    }

    private fun saveRvLayoutState(outState: Bundle) {
        Preconditions.checkNotNull(outState)
        outState.putParcelable(
            SyncLogViewModel.Companion.STATE_RECYCLER_LAYOUT,
            rvLayoutManager!!.onSaveInstanceState()
        )
    }

    private fun applyRvLayoutState() {
        if (rvLayoutManager != null && rvLayoutState != null) {
            rvLayoutManager!!.onRestoreInstanceState(rvLayoutState)
            rvLayoutState = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel!!.saveInstanceState(outState)
        saveRvLayoutState(outState)
    }

    override fun showSyncResults(syncResults: List<SyncResult>) {
        Preconditions.checkNotNull(syncResults)
        adapter!!.swapItems(syncResults)
        viewModel!!.setListSize(syncResults.size)
        applyRvLayoutState()
    }

    companion object {
        private val TAG = SyncLogFragment::class.java.simpleName
        fun newInstance(): SyncLogFragment {
            return SyncLogFragment()
        }
    }
}