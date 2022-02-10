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

import com.bytesforge.linkasanote.data.SyncResult
import com.bytesforge.linkasanote.data.source.Repository
import com.bytesforge.linkasanote.utils.CommonUtils
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

class SyncLogPresenter @Inject constructor(
    private val repository: Repository,
    private val view: SyncLogContract.View,
    private val viewModel: SyncLogContract.ViewModel,
    private val schedulerProvider: BaseSchedulerProvider
) : SyncLogContract.Presenter {
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    @Inject
    fun setupView() {
        view.setPresenter(this)
        view.setViewModel(viewModel)
        viewModel.setPresenter(this)
    }

    override fun subscribe() {
        loadSyncLog(true)
    }

    override fun unsubscribe() {
        compositeDisposable.clear()
    }

    private fun loadSyncLog(showLoading: Boolean) {
        compositeDisposable.clear()
        if (showLoading) {
            viewModel.showProgressOverlay()
        }
        val disposable = repository.freshSyncResults
            .subscribeOn(schedulerProvider.computation())
            .toList()
            .observeOn(schedulerProvider.ui())
            .doFinally {
                if (showLoading) {
                    viewModel.hideProgressOverlay()
                }
            }
            .subscribe(
                { syncResults: List<SyncResult> -> view.showSyncResults(syncResults) }
            ) { throwable: Throwable? -> CommonUtils.logStackTrace(TAG_E!!, throwable!!)
                viewModel.showDatabaseErrorSnackbar()
            }
        compositeDisposable.add(disposable)
    }

    companion object {
        private val TAG = SyncLogPresenter::class.java.simpleName
        private val TAG_E = SyncLogPresenter::class.java.canonicalName
    }
}