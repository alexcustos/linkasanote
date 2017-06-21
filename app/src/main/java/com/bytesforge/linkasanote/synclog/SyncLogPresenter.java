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

package com.bytesforge.linkasanote.synclog;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public final class SyncLogPresenter implements SyncLogContract.Presenter {

    private static final String TAG = SyncLogPresenter.class.getSimpleName();
    private static final String TAG_E = SyncLogPresenter.class.getCanonicalName();

    private final Repository repository;
    private final SyncLogContract.View view;
    private final SyncLogContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;

    @NonNull
    private final CompositeDisposable compositeDisposable;

    @Inject
    public SyncLogPresenter(
            Repository repository, SyncLogContract.View view,
            SyncLogContract.ViewModel viewModel, BaseSchedulerProvider schedulerProvider) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        compositeDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
        loadSyncLog(true);
    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
    }

    private void loadSyncLog(final boolean showLoading) {
        compositeDisposable.clear();
        if (showLoading) {
            viewModel.showProgressOverlay();
        }
        Disposable disposable = repository.getFreshSyncResults()
                .subscribeOn(schedulerProvider.computation())
                .toList()
                .observeOn(schedulerProvider.ui())
                .doFinally(() -> {
                    if (showLoading) {
                        viewModel.hideProgressOverlay();
                    }
                })
                .subscribe(view::showSyncResults, throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    viewModel.showDatabaseErrorSnackbar();
                });
        compositeDisposable.add(disposable);
    }
}
