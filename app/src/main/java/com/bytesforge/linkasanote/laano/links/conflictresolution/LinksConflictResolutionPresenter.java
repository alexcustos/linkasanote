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

package com.bytesforge.linkasanote.laano.links.conflictresolution;

import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.laano.links.LinkId;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LinksConflictResolutionPresenter implements
        LinksConflictResolutionContract.Presenter {

    private static final String TAG = LinksConflictResolutionPresenter.class.getSimpleName();

    private final Repository repository; // NOTE: for cache control
    private final Settings settings;
    private final LocalLinks<Link> localLinks;
    private final CloudItem<Link> cloudLinks;
    private final LocalNotes<Note> localNotes;
    private final CloudItem<Note> cloudNotes;
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
            Repository repository, Settings settings,
            LocalLinks<Link> localLinks, CloudItem<Link> cloudLinks,
            LocalNotes<Note> localNotes, CloudItem<Note> cloudNotes,
            LinksConflictResolutionContract.View view,
            LinksConflictResolutionContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider, @LinkId String linkId) {
        this.repository = repository;
        this.settings = settings;
        this.localLinks = localLinks;
        this.cloudLinks = cloudLinks;
        this.localNotes = localNotes;
        this.cloudNotes = cloudNotes;
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

    private void loadLocalLink() {
        localDisposable.clear();
        Disposable disposable = localLinks.get(linkId)
                .subscribeOn(schedulerProvider.computation()) // local
                .observeOn(schedulerProvider.ui())
                .subscribe(link -> {
                    if (!link.isConflicted()) {
                        // NOTE: to make sure that there is no problem with the cache
                        repository.refreshLinks();
                        view.finishActivity();
                    } else {
                        populateLocalLink(link);
                    }
                }, throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        // NOTE: maybe there is a problem with cache
                        repository.refreshLinks();
                        view.finishActivity(); // no item, no problem
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
            localLinks.getMain(link.getDuplicatedKey())
                    .subscribeOn(schedulerProvider.computation()) // local
                    .observeOn(schedulerProvider.ui())
                    // NOTE: recursion, but mainLink is not duplicated by definition
                    .subscribe(this::populateLocalLink, throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            // NOTE: very bad behaviour, but it's the best choice if it had happened
                            Log.e(TAG, "Fallback for the auto Link conflict resolution was called");
                            SyncState state = new SyncState(SyncState.State.SYNCED);
                            boolean success = localLinks.update(linkId, state).blockingGet();
                            if (success) {
                                repository.refreshLink(linkId);
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
        }
    }

    private void loadCloudLink() {
        cloudDisposable.clear();
        Disposable disposable = cloudLinks.download(linkId)
                .subscribeOn(schedulerProvider.io())
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
            replaceLink(viewModel.getLocalId(), viewModel.getCloudId());
        } else {
            deleteLink(viewModel.getLocalId());
        }
    }

    private void replaceLink(
            @NonNull final String localLinkId, @NonNull final String duplicatedLinkId) {
        checkNotNull(localLinkId);
        checkNotNull(duplicatedLinkId);
        viewModel.showProgressOverlay();
        deleteLinkSingle(localLinkId)
                .subscribeOn(schedulerProvider.io())
                .map(success -> {
                    if (success) {
                        SyncState state = new SyncState(SyncState.State.SYNCED);
                        success = localLinks.update(duplicatedLinkId, state).blockingGet();
                        repository.refreshLink(duplicatedLinkId);
                    }
                    return success;
                })
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private void deleteLink(@NonNull final String linkId) {
        checkNotNull(linkId);
        viewModel.showProgressOverlay();
        deleteLinkSingle(linkId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private Single<Boolean> deleteLinkSingle(@NonNull final String linkId) {
        checkNotNull(linkId);
        return cloudLinks.delete(linkId)
                .flatMap(result -> {
                    if (!result.isSuccess()) {
                        Log.e(TAG, "There was an error while deleting the Link from the cloud storage [" + linkId + "]");
                        throw new RuntimeException("Cloud storage: Link removal exception [" + result.getLogMessage() + "]");
                    }
                    return localLinks.get(linkId);
                })
                .map(link -> {
                    List<Note> notes = link.getNotes();
                    if (notes == null) return true;

                    boolean success = true;
                    for (Note note : notes) {
                        if (success) {
                            success = deleteNoteSingle(note.getId()).blockingGet();
                        } else break;
                    }
                    return success;
                })
                .map(success -> {
                    if (success) {
                        return localLinks.delete(linkId).blockingGet();
                    }
                    return false;
                })
                .doOnSuccess(success -> {
                    if (success) {
                        repository.removeCachedLink(linkId);
                        settings.resetLinkFilterId(linkId);
                    }
                });
    }

    private Single<Boolean> deleteNoteSingle(@NonNull final String noteId) {
        checkNotNull(noteId);
        return cloudNotes.delete(noteId)
                .map(result -> {
                    boolean success = false;
                    if (result.isSuccess()) {
                        success = localNotes.delete(noteId).blockingGet();
                    } else {
                        // NOTE: deferred delete here is not a good idea
                        Log.e(TAG, "There was an error while deleting the Note from the cloud storage [" + noteId + "]");
                    }
                    return success;
                })
                .doOnSuccess(success -> {
                    if (success) {
                        repository.removeCachedNote(noteId);
                        settings.resetNoteFilterId(noteId);
                    }
                });
    }

    @Override
    public void onCloudDeleteClick() {
        viewModel.deactivateButtons();
        deleteLink(viewModel.getCloudId());
    }

    @Override
    public void onCloudRetryClick() {
        viewModel.showCloudLoading();
        loadCloudLink();
    }

    @Override
    public void onLocalUploadClick() {
        viewModel.deactivateButtons();
        viewModel.showProgressOverlay();
        String linkId = viewModel.getLocalId();
        localLinks.get(linkId)
                .subscribeOn(schedulerProvider.io())
                .flatMap(cloudLinks::upload)
                .map(result -> {
                    boolean success = false;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        success = localLinks.update(linkId, state).blockingGet();
                    }
                    return success;
                })
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        repository.refreshLink(linkId);
                        refreshLinkFilter(linkId);
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    @Override
    public void onCloudDownloadClick() {
        viewModel.deactivateButtons();
        viewModel.showProgressOverlay();
        String linkId = viewModel.getCloudId();
        cloudLinks.download(linkId)
                .subscribeOn(schedulerProvider.io())
                .flatMap(localLinks::save)
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        // NOTE: most likely the same Link was updated and position remain unchanged
                        repository.refreshLink(linkId);
                        refreshLinkFilter(linkId);
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private void refreshLinkFilter(@NonNull String linkId) {
        checkNotNull(linkId);
        String linkFilterId = settings.getLinkFilterId();
        if (linkId.equals(linkFilterId)) {
            settings.setLinkFilter(null); // filter is set to be refreshed
        }
    }
}
