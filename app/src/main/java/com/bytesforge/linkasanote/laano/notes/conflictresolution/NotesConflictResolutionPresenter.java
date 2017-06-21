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

package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.laano.notes.NoteId;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class NotesConflictResolutionPresenter implements
        NotesConflictResolutionContract.Presenter {

    private static final String TAG = NotesConflictResolutionPresenter .class.getSimpleName();
    private static final String TAG_E = NotesConflictResolutionPresenter .class.getCanonicalName();

    private final Repository repository; // NOTE: for cache control
    private final Settings settings;
    private final LocalNotes<Note> localNotes;
    private final CloudItem<Note> cloudNotes;
    private final LocalLinks<Link> localLinks;
    private final NotesConflictResolutionContract.View view;
    private final NotesConflictResolutionContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;

    private String noteId;

    @NonNull
    private final CompositeDisposable localDisposable;

    @NonNull
    private final CompositeDisposable cloudDisposable;

    @Inject
    NotesConflictResolutionPresenter(
            Repository repository, Settings settings,
            LocalNotes<Note> localNotes, CloudItem<Note> cloudNotes,
            LocalLinks<Link> localLinks, NotesConflictResolutionContract.View view,
            NotesConflictResolutionContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider, @NoteId String noteId) {
        this.repository = repository;
        this.settings = settings;
        this.localNotes = localNotes;
        this.cloudNotes = cloudNotes;
        this.localLinks = localLinks;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.noteId = noteId;
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
        loadLocalNote(); // first step, then cloud one will be loaded
    }

    private void loadLocalNote() {
        localDisposable.clear();
        Disposable disposable = localNotes.get(noteId)
                .subscribeOn(schedulerProvider.computation()) // local
                .observeOn(schedulerProvider.ui())
                .subscribe(note -> {
                    if (!note.isConflicted()) {
                        // NOTE: to make sure that there is no problem with the cache
                        repository.refreshNotes();
                        view.finishActivity();
                    } else {
                        populateLocalNote(note);
                    }
                }, throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        repository.refreshNotes(); // NOTE: maybe there is a problem with cache
                        view.finishActivity(); // NOTE: no item, no problem
                    } else {
                        viewModel.showDatabaseError();
                        loadCloudNote();
                    }
                });
        localDisposable.add(disposable);
    }

    private void populateLocalNote(@NonNull final Note note) {
        checkNotNull(note);
        viewModel.populateLocalNote(note);
        if (!viewModel.isCloudPopulated()) {
            loadCloudNote();
        }
    }

    private void loadCloudNote() {
        cloudDisposable.clear();
        Disposable disposable = cloudNotes.download(noteId)
                .subscribeOn(schedulerProvider.io())
                .flatMap(note -> {
                    String linkId = note.getLinkId();
                    Single<Boolean> orphanedNoteSingle;
                    if (linkId != null) {
                        orphanedNoteSingle = localLinks.getSyncState(linkId)
                                .flatMap(syncState -> Single.just(false))
                                .onErrorReturn(throwable -> {
                                    if (throwable instanceof NoSuchElementException) {
                                        return true;
                                    } else {
                                        CommonUtils.logStackTrace(TAG_E, throwable);
                                        // NOTE: treat it as normal if it is sill possible
                                        return false;
                                    }
                                });
                    } else {
                        orphanedNoteSingle = Single.just(false);
                    }
                    return Single.zip(Single.just(note), orphanedNoteSingle, Pair::new);
                })
                .observeOn(schedulerProvider.ui())
                .subscribe(pair -> {
                    viewModel.populateCloudNote(pair.first, pair.second);
                }, throwable -> {
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
        deleteNote(viewModel.getLocalId());
    }

    private void deleteNote(@NonNull final String noteId) {
        checkNotNull(noteId);
        viewModel.showProgressOverlay();
        deleteNoteSingle(noteId)
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

    private Single<Boolean> deleteNoteSingle(@NonNull final String noteId) {
        checkNotNull(noteId);
        return cloudNotes.delete(noteId)
                .map(result -> {
                    boolean success = false;
                    if (result.isSuccess()) {
                        success = localNotes.delete(noteId).blockingGet();
                    } else {
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
        deleteNote(viewModel.getCloudId());
    }

    @Override
    public void onCloudRetryClick() {
        viewModel.showCloudLoading();
        loadCloudNote();
    }

    @Override
    public void onLocalUploadClick() {
        viewModel.deactivateButtons();
        viewModel.showProgressOverlay();
        String noteId = viewModel.getLocalId();
        localNotes.get(noteId)
                .subscribeOn(schedulerProvider.io())
                .flatMap(cloudNotes::upload)
                .map(result -> {
                    boolean success = false;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        success = localNotes.update(noteId, state).blockingGet();
                    }
                    return success;
                })
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        repository.refreshNote(noteId);
                        refreshNoteFilter(noteId);
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
        String noteId = viewModel.getCloudId();
        cloudNotes.download(noteId)
                .subscribeOn(schedulerProvider.io())
                .flatMap(localNotes::save)
                .observeOn(schedulerProvider.ui())
                .doFinally(viewModel::hideProgressOverlay)
                .subscribe(success -> {
                    if (success) {
                        // NOTE: most likely the same Note was updated and position remain unchanged
                        repository.refreshNote(noteId);
                        refreshNoteFilter(noteId);
                        view.finishActivity();
                    } else {
                        view.cancelActivity();
                    }
                }, throwable -> view.cancelActivity());
    }

    private void refreshNoteFilter(@NonNull String noteId) {
        checkNotNull(noteId);
        String noteFilterId = settings.getNoteFilterId();
        if (noteId.equals(noteFilterId)) {
            settings.setNoteFilter(null); // filter is set to be refreshed
        }
    }
}
