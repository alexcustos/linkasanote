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
