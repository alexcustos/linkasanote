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

import android.content.Context
import com.bytesforge.linkasanote.synclog.SyncLogViewModel
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import com.bytesforge.linkasanote.synclog.SyncLogAdapter
import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject
import com.bytesforge.linkasanote.synclog.SyncLogPresenter
import android.os.Bundle
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.synclog.SyncLogFragment
import com.bytesforge.linkasanote.utils.ActivityUtils
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.synclog.SyncLogPresenterModule
import com.bytesforge.linkasanote.BaseView
import com.bytesforge.linkasanote.BasePresenter
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Parcelable
import android.view.View
import com.bytesforge.linkasanote.FragmentScoped
import dagger.Subcomponent
import com.bytesforge.linkasanote.synclog.SyncLogActivity
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import com.bytesforge.linkasanote.utils.CommonUtils
import com.bytesforge.linkasanote.BR
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.*
import com.bytesforge.linkasanote.data.SyncResult
import com.google.android.material.snackbar.Snackbar
import com.google.common.base.Preconditions
import dagger.Provides
import java.lang.IllegalArgumentException
import java.util.*

class SyncLogViewModel(context: Context) : BaseObservable(), SyncLogContract.ViewModel {
    override val listSize = ObservableInt()

    @Bindable
    var progressOverlay = false
    private val context: Context

    enum class SnackbarId {
        DATABASE_ERROR
    }

    @Bindable
    var snackbarId: SnackbarId? = null

    @get:Bindable
    val isListEmpty: Boolean
        get() = listSize.get() <= 0

    override fun setPresenter(presenter: SyncLogContract.Presenter) {}
    override fun setInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            applyInstanceState(defaultInstanceState)
        } else {
            applyInstanceState(savedInstanceState)
        }
    }

    override fun saveInstanceState(outState: Bundle) {
        Preconditions.checkNotNull(outState)
        outState.putInt(STATE_LIST_SIZE, listSize.get())
        outState.putBoolean(STATE_PROGRESS_OVERLAY, progressOverlay)
    }

    override fun applyInstanceState(state: Bundle) {
        Preconditions.checkNotNull(state)
        listSize.set(state.getInt(STATE_LIST_SIZE))
        progressOverlay = state.getBoolean(STATE_PROGRESS_OVERLAY)
        notifyChange()
    }

    // NOTE: do not show empty list warning if empty state is not confirmed
    override val defaultInstanceState: Bundle
        get() {
            val defaultState = Bundle()
            // NOTE: do not show empty list warning if empty state is not confirmed
            defaultState.putInt(STATE_LIST_SIZE, Int.MAX_VALUE)
            defaultState.putBoolean(STATE_PROGRESS_OVERLAY, false)
            return defaultState
        }

    override fun getListSize(): Int {
        return listSize.get()
    }

    fun getSyncResult(position: Int, syncResult: SyncResult): String {
        Preconditions.checkNotNull(syncResult)
        return "$position. $syncResult"
    }

    fun getStarted(started: Long): String {
        val date = Date(started)
        return CommonUtils.formatDateTime(context, date)
    }

    fun isLast(position: Int): Boolean {
        return position + 1 >= getListSize()
    }

    /**
     * @return Returns true if listSize has never been set before
     */
    override fun setListSize(listSize: Int): Boolean {
        val firstLoad = this.listSize.get() == Int.MAX_VALUE
        this.listSize.set(listSize)
        notifyPropertyChanged(BR.listEmpty)
        return firstLoad
    }

    // Progress
    override fun showProgressOverlay() {
        if (!progressOverlay) {
            progressOverlay = true
            notifyPropertyChanged(BR.progressOverlay)
        }
    }

    override fun hideProgressOverlay() {
        if (progressOverlay) {
            progressOverlay = false
            notifyPropertyChanged(BR.progressOverlay)
        }
    }

    // Snackbar
    override fun showDatabaseErrorSnackbar() {
        snackbarId = SnackbarId.DATABASE_ERROR
        notifyPropertyChanged(BR.snackbarId)
    }

    companion object {
        private const val STATE_LIST_SIZE = "LIST_SIZE"
        private const val STATE_PROGRESS_OVERLAY = "PROGRESS_OVERLAY"
        const val STATE_RECYCLER_LAYOUT = "RECYCLER_LAYOUT"
        @BindingAdapter("snackbarId")
        fun showSnackbar(view: CoordinatorLayout?, snackbarId: SnackbarId?) {
            if (snackbarId == null) return
            when (snackbarId) {
                SnackbarId.DATABASE_ERROR -> Snackbar.make(
                    view!!, R.string.error_database, Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.snackbar_button_ok) { v: View? -> }
                    .show()
                else -> throw IllegalArgumentException("Unexpected snackbar has been requested")
            }
        }
    }

    init {
        this.context = Preconditions.checkNotNull(context)
    }
}