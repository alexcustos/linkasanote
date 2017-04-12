package com.bytesforge.linkasanote.laano.links.conflictresolution;

import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudLinks;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.laano.links.LinkId;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LinksConflictResolutionPresenter implements
        LinksConflictResolutionContract.Presenter {

    private final Repository repository; // NOTE: for cache control
    private final LocalLinks localLinks;
    private final CloudLinks cloudLinks;
    private final LinksConflictResolutionContract.View view;
    private final LinksConflictResolutionContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;

    private String linkId;

    @NonNull
    private final CompositeDisposable localDisposable;

    @NonNull
    private final CompositeDisposable cloudDisposable;

    @Inject
    LinksConflictResolutionPresenter(
            Repository repository, LocalLinks localLinks, CloudLinks cloudLinks,
            LinksConflictResolutionContract.View view,
            LinksConflictResolutionContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider, @LinkId String linkId) {
        this.repository = repository;
        this.localLinks = localLinks;
        this.cloudLinks = cloudLinks;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.linkId = linkId;
        localDisposable = new CompositeDisposable();
        cloudDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
        populate();
    }

    @Override
    public void unsubscribe() {
        localDisposable.clear();
        cloudDisposable.clear();
    }

    private void populate() {
        if (viewModel.isLocalPopulated() && viewModel.isCloudPopulated()) {
            return;
        }
        loadLocalLink(); // first step, then cloud one will be loaded
    }

    @Override
    public Single<Boolean> autoResolve() {
        return Single.fromCallable(() -> {
            Link link = localLinks.getLink(linkId).blockingGet();
            if (link.isDuplicated()) {
                try {
                    localLinks.getMainLink(link.getName()).blockingGet();
                } catch (NoSuchElementException e) {
                    SyncState state = new SyncState(SyncState.State.SYNCED);
                    int numRows = localLinks.updateLink(linkId, state).blockingGet();
                    if (numRows == 1) {
                        repository.refreshLinks();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void loadLocalLink() {
        localDisposable.clear();
        Disposable disposable = localLinks.getLink(linkId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(link -> {
                    if (!link.isConflicted()) {
                        repository.refreshLinks(); // NOTE: maybe there is a problem with cache
                        view.finishActivity();
                    } else {
                        populateLocalLink(link);
                    }
                }, throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        repository.refreshLinks(); // NOTE: maybe there is a problem with cache
                        view.finishActivity(); // NOTE: no item, no problem
                    } else {
                        viewModel.showDatabaseError();
                        loadCloudLink();
                    }
                });
        localDisposable.add(disposable);
    }

    private void populateLocalLink(@NonNull final Link link) {
        checkNotNull(link);

        if (link.isDuplicated()) {
            viewModel.populateCloudLink(link);
            localLinks.getMainLink(link.getName())
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    // NOTE: recursion, but mainLink is not duplicated by definition
                    .subscribe(this::populateLocalLink, throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            // NOTE: main position is empty, so the conflict can be resolved automatically
                            // TODO: remove in favor of autoResolve
                            SyncState state = new SyncState(SyncState.State.SYNCED);
                            int numRows = localLinks.updateLink(linkId, state).blockingGet();
                            if (numRows == 1) {
                                repository.refreshLinks();
                                view.finishActivity();
                            } else {
                                view.cancelActivity();
                            }
                        } else {
                            viewModel.showDatabaseError();
                            loadCloudLink();
                        }
                    });
        } else {
            viewModel.populateLocalLink(link);
            if (!viewModel.isCloudPopulated()) {
                loadCloudLink();
            }
        } // if
    }

    private void loadCloudLink() {
        cloudDisposable.clear();
        Disposable disposable = cloudLinks.downloadLink(linkId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(viewModel::populateCloudLink, throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        viewModel.showCloudNotFound();
                    } else {
                        viewModel.showCloudDownloadError();
                    }
                });
        cloudDisposable.add(disposable);
    }

    @Override
    public void onLocalDeleteClick() {
        viewModel.deactivateButtons();
        if (viewModel.isStateDuplicated()) {
            localLinks.getMainLink(viewModel.getLocalName())
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    .subscribe(
                            link -> replaceLink(link.getId(), linkId),
                            throwable -> view.cancelActivity());
        } else {
            deleteLink(linkId);
        }
    }

    private void replaceLink(
            @NonNull final String mainLinkId, @NonNull final String linkId) {
        checkNotNull(mainLinkId);
        checkNotNull(linkId);

        // DB operation is blocking; Cloud is on computation
        cloudLinks.deleteLink(mainLinkId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    boolean isSuccess = false;
                    if (result.isSuccess()) {
                        int numRows = localLinks.deleteLink(mainLinkId).blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.deleteCachedLink(mainLinkId);
                        SyncState state = new SyncState(SyncState.State.SYNCED);
                        int numRows = localLinks.updateLink(linkId, state).blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.refreshLinks();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private void deleteLink(@NonNull final String linkId) {
        checkNotNull(linkId);

        cloudLinks.deleteLink(linkId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    boolean isSuccess = false;
                    if (result.isSuccess()) {
                        int numRows = localLinks.deleteLink(linkId).blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.deleteCachedLink(linkId);
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    @Override
    public void onCloudDeleteClick() {
        viewModel.deactivateButtons();
        deleteLink(linkId);
    }

    @Override
    public void onCloudRetryClick() {
        viewModel.showCloudLoading();
        loadCloudLink();
    }

    @Override
    public void onLocalUploadClick() {
        viewModel.deactivateButtons();
        Link link = localLinks.getLink(linkId).blockingGet();
        cloudLinks.uploadLink(link)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    boolean isSuccess = false;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        int numRows = localLinks.updateLink(link.getId(), state)
                                .blockingGet();
                        isSuccess = (numRows == 1);
                    }
                    if (isSuccess) {
                        repository.refreshLinks();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    @Override
    public void onCloudDownloadClick() {
        viewModel.deactivateButtons();
        cloudLinks.downloadLink(linkId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(link -> {
                    long rowId = localLinks.saveLink(link).blockingGet();
                    if (rowId > 0) {
                        repository.refreshLinks();
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }
}
