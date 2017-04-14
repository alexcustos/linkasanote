package com.bytesforge.linkasanote.data.source.cloud;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class CloudDataSource implements DataSource {

    private static final String TAG = CloudDataSource.class.getSimpleName();

    private final Context context;
    private final BaseSchedulerProvider schedulerProvider;
    private final LocalLinks localLinks;
    private final CloudLinks cloudLinks;
    private final LocalFavorites localFavorites;
    private final CloudFavorites cloudFavorites;
    private final LocalNotes localNotes;
    private final CloudNotes cloudNotes;

    private final CompositeDisposable disposable;

    public CloudDataSource(
            Context context, BaseSchedulerProvider schedulerProvider,
            LocalLinks localLinks, CloudLinks cloudLinks,
            LocalFavorites localFavorites, CloudFavorites cloudFavorites,
            LocalNotes localNotes, CloudNotes cloudNotes) {
        this.context = context;
        this.schedulerProvider = schedulerProvider;
        this.localLinks = localLinks;
        this.cloudLinks = cloudLinks;
        this.localFavorites = localFavorites;
        this.cloudFavorites = cloudFavorites;
        this.localNotes = localNotes;
        this.cloudNotes = cloudNotes;

        disposable = new CompositeDisposable();
    }

    // Links

    @Override
    public Observable<Link> getLinks() {
        return null;
    }

    @Override
    public Single<Link> getLink(@NonNull String linkId) {
        checkNotNull(linkId);

        return cloudLinks.downloadLink(linkId);
    }

    @Override
    public void saveLink(@NonNull final Link link) {
        checkNotNull(link);

        // NOTE: do not cache, because it can be changed in any time
        Disposable disposable = getSaveLink(link)
                .subscribeOn(schedulerProvider.computation())
                //.observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    int numRows = -1;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        numRows = localLinks.updateLink(link.getId(), state)
                                .blockingGet();
                    } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                        numRows = localLinks.updateLink(link.getId(), state)
                                .blockingGet();
                    }
                    if (numRows != 1) {
                        Log.d(TAG, "Unexpected number of rows were updated [" + numRows + "]");
                    }
                }, throwable -> {
                    if (throwable instanceof NullPointerException) {
                        Log.e(TAG, "Current state is not suitable for upload [" + link.getId() + "]");
                    } else {
                        Log.e(TAG, "An unknown error while preparing to upload. ", throwable);
                    }
                });
        this.disposable.add(disposable);
    }

    private Single<RemoteOperationResult> getSaveLink(@NonNull final Link link) {
        return Single.fromCallable(() -> {
            // Check local
            String linkId = link.getId();
            SyncState oldState = null;
            boolean isReady = false;
            try {
                oldState = localLinks.getLinkSyncState(linkId).blockingGet();
            } catch (NoSuchElementException e) {
                isReady = true;
            } catch (NullPointerException e) {
                return null;
            }
            if (oldState != null && !oldState.isDeleted()) {
                isReady = true;
            }
            if (!isReady) return null;
            // Check cloud
            try {
                RemoteFile file = cloudLinks.readLinkFile(linkId).blockingGet();
                if (oldState == null || !file.getEtag().equals(oldState.getETag())) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
                }
            } catch (NoSuchElementException e) {
                // NOTE: It's expected state if there is no file in the cloud
            }
            return cloudLinks.uploadLink(link).blockingGet();
        });
    }

    @Override
    public void deleteAllLinks() {
    }

    @Override
    public void deleteLink(@NonNull String linkId) {
        checkNotNull(linkId);

        Disposable disposable = getDeleteLink(linkId)
                .subscribeOn(schedulerProvider.computation())
                .subscribe(result -> {
                    int numRows = -1;
                    if (result.isSuccess()) {
                        numRows = localLinks.deleteLink(linkId).blockingGet();
                    } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        SyncState state = new SyncState(SyncState.State.CONFLICTED_DELETE);
                        numRows = localLinks.updateLink(linkId, state).blockingGet();
                    }
                    if (numRows != 1) {
                        Log.e(TAG, "Unexpected number of rows were processed [" + numRows + "]");
                    }
                }, throwable -> {
                    if (throwable instanceof NullPointerException) {
                        Log.e(TAG, "Current state is not suitable for delete [" + linkId + "]");
                    } else {
                        Log.e(TAG, "An unknown error while preparing to delete. ", throwable);
                    }
                });
        this.disposable.add(disposable);
    }

    private Single<RemoteOperationResult> getDeleteLink(@NonNull final String linkId) {
        return Single.fromCallable(() -> {
            // Check local
            SyncState state = null;
            boolean isReady = false;
            try {
                state = localLinks.getLinkSyncState(linkId).blockingGet();
            } catch (NoSuchElementException e) {
                isReady = true;
            } catch (NullPointerException e) {
                return null;
            }
            if (state != null && state.isDeleted()) {
                isReady = true;
            }
            if (!isReady) return null;
            // Check cloud
            try {
                RemoteFile file = cloudLinks.readLinkFile(linkId).blockingGet();
                if (state != null && !file.getEtag().equals(state.getETag())) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
                }
            } catch (NoSuchElementException e) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
            }
            return cloudLinks.deleteLink(linkId).blockingGet();
        });
    }

    @Override
    public Single<Boolean> isConflictedLinks() {
        return null;
    }

    // Favorites

    @Override
    public Observable<Favorite> getFavorites() {
        return null;
    }

    @Override
    public Single<Favorite> getFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);

        return cloudFavorites.downloadFavorite(favoriteId);
    }

    @Override
    public void saveFavorite(@NonNull final Favorite favorite) {
        checkNotNull(favorite);

        // NOTE: do not cache, because it can be changed in any time
        Disposable disposable = getSaveFavorite(favorite)
                .subscribeOn(schedulerProvider.computation())
                //.observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    int numRows = -1;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        numRows = localFavorites.updateFavorite(favorite.getId(), state)
                                .blockingGet();
                    } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                        numRows = localFavorites.updateFavorite(favorite.getId(), state)
                                .blockingGet();
                    }
                    if (numRows != 1) {
                        Log.d(TAG, "Unexpected number of rows were updated [" + numRows + "]");
                    }
                }, throwable -> {
                    if (throwable instanceof NullPointerException) {
                        Log.e(TAG, "Current state is not suitable for upload [" + favorite.getId() + "]");
                    } else {
                        Log.e(TAG, "An unknown error while preparing to upload. ", throwable);
                    }
                });
        this.disposable.add(disposable);
    }

    private Single<RemoteOperationResult> getSaveFavorite(@NonNull final Favorite favorite) {
        return Single.fromCallable(() -> {
            // Check local
            String favoriteId = favorite.getId();
            SyncState oldState = null;
            boolean isReady = false;
            try {
                oldState = localFavorites.getFavoriteSyncState(favoriteId).blockingGet();
            } catch (NoSuchElementException e) {
                isReady = true;
            } catch (NullPointerException e) {
                return null;
            }
            if (oldState != null && !oldState.isDeleted()) {
                isReady = true;
            }
            if (!isReady) return null;
            // Check cloud
            try {
                RemoteFile file = cloudFavorites.readFavoriteFile(favoriteId).blockingGet();
                if (oldState == null || !file.getEtag().equals(oldState.getETag())) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
                }
            } catch (NoSuchElementException e) {
                // NOTE: It's expected state if there is no file in the cloud
            }
            return cloudFavorites.uploadFavorite(favorite).blockingGet();
        });
    }

    @Override
    public void deleteAllFavorites() {
    }

    @Override
    public void deleteFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);

        Disposable disposable = getDeleteFavorite(favoriteId)
                .subscribeOn(schedulerProvider.computation())
                .subscribe(result -> {
                    int numRows = -1;
                    if (result.isSuccess()) {
                        numRows = localFavorites.deleteFavorite(favoriteId).blockingGet();
                    } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        SyncState state = new SyncState(SyncState.State.CONFLICTED_DELETE);
                        numRows = localFavorites.updateFavorite(favoriteId, state).blockingGet();
                    }
                    if (numRows != 1) {
                        Log.e(TAG, "Unexpected number of rows were processed [" + numRows + "]");
                    }
                }, throwable -> {
                    if (throwable instanceof NullPointerException) {
                        Log.e(TAG, "Current state is not suitable for delete [" + favoriteId + "]");
                    } else {
                        Log.e(TAG, "An unknown error while preparing to delete. ", throwable);
                    }
                });
        this.disposable.add(disposable);
    }

    private Single<RemoteOperationResult> getDeleteFavorite(@NonNull final String favoriteId) {
        return Single.fromCallable(() -> {
            // Check local
            SyncState state = null;
            boolean isReady = false;
            try {
                state = localFavorites.getFavoriteSyncState(favoriteId).blockingGet();
            } catch (NoSuchElementException e) {
                isReady = true;
            } catch (NullPointerException e) {
                return null;
            }
            if (state != null && state.isDeleted()) {
                isReady = true;
            }
            if (!isReady) return null;
            // Check cloud
            try {
                RemoteFile file = cloudFavorites.readFavoriteFile(favoriteId).blockingGet();
                if (state != null && !file.getEtag().equals(state.getETag())) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
                }
            } catch (NoSuchElementException e) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
            }
            return cloudFavorites.deleteFavorite(favoriteId).blockingGet();
        });
    }

    @Override
    public Single<Boolean> isConflictedFavorites() {
        return null;
    }

    // Notes

    @Override
    public Observable<Note> getNotes() {
        return null;
    }

    @Override
    public Single<Note> getNote(@NonNull String noteId) {
        checkNotNull(noteId);

        return cloudNotes.downloadNote(noteId);
    }

    @Override
    public void saveNote(@NonNull final Note note) {
        checkNotNull(note);

        // NOTE: do not cache, because it can be changed in any time
        Disposable disposable = getSaveNote(note)
                .subscribeOn(schedulerProvider.computation())
                //.observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    int numRows = -1;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        numRows = localNotes.updateNote(note.getId(), state)
                                .blockingGet();
                    } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                        numRows = localNotes.updateNote(note.getId(), state)
                                .blockingGet();
                    }
                    if (numRows != 1) {
                        Log.d(TAG, "Unexpected number of rows were updated [" + numRows + "]");
                    }
                }, throwable -> {
                    if (throwable instanceof NullPointerException) {
                        Log.e(TAG, "Current state is not suitable for upload [" + note.getId() + "]");
                    } else {
                        Log.e(TAG, "An unknown error while preparing to upload. ", throwable);
                    }
                });
        this.disposable.add(disposable);
    }

    private Single<RemoteOperationResult> getSaveNote(@NonNull final Note note) {
        return Single.fromCallable(() -> {
            // Check local
            String noteId = note.getId();
            SyncState oldState = null;
            boolean isReady = false;
            try {
                oldState = localNotes.getNoteSyncState(noteId).blockingGet();
            } catch (NoSuchElementException e) {
                isReady = true;
            } catch (NullPointerException e) {
                return null;
            }
            if (oldState != null && !oldState.isDeleted()) {
                isReady = true;
            }
            if (!isReady) return null;
            // Check cloud
            try {
                RemoteFile file = cloudNotes.readNoteFile(noteId).blockingGet();
                if (oldState == null || !file.getEtag().equals(oldState.getETag())) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
                }
            } catch (NoSuchElementException e) {
                // NOTE: It's expected state if there is no file in the cloud
            }
            return cloudNotes.uploadNote(note).blockingGet();
        });
    }

    @Override
    public void deleteAllNotes() {
    }

    @Override
    public void deleteNote(@NonNull String noteId) {
        checkNotNull(noteId);

        Disposable disposable = getDeleteNote(noteId)
                .subscribeOn(schedulerProvider.computation())
                .subscribe(result -> {
                    int numRows = -1;
                    if (result.isSuccess()) {
                        numRows = localNotes.deleteNote(noteId).blockingGet();
                    } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        SyncState state = new SyncState(SyncState.State.CONFLICTED_DELETE);
                        numRows = localNotes.updateNote(noteId, state).blockingGet();
                    }
                    if (numRows != 1) {
                        Log.e(TAG, "Unexpected number of rows were processed [" + numRows + "]");
                    }
                }, throwable -> {
                    if (throwable instanceof NullPointerException) {
                        Log.e(TAG, "Current state is not suitable for delete [" + noteId + "]");
                    } else {
                        Log.e(TAG, "An unknown error while preparing to delete. ", throwable);
                    }
                });
        this.disposable.add(disposable);
    }

    private Single<RemoteOperationResult> getDeleteNote(@NonNull final String noteId) {
        return Single.fromCallable(() -> {
            // Check local
            SyncState state = null;
            boolean isReady = false;
            try {
                state = localNotes.getNoteSyncState(noteId).blockingGet();
            } catch (NoSuchElementException e) {
                isReady = true;
            } catch (NullPointerException e) {
                return null;
            }
            if (state != null && state.isDeleted()) {
                isReady = true;
            }
            if (!isReady) return null;
            // Check cloud
            try {
                RemoteFile file = cloudNotes.readNoteFile(noteId).blockingGet();
                if (state != null && !file.getEtag().equals(state.getETag())) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
                }
            } catch (NoSuchElementException e) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
            }
            return cloudNotes.deleteNote(noteId).blockingGet();
        });
    }

    @Override
    public Single<Boolean> isConflictedNotes() {
        return null;
    }

    // Tags

    @Override
    public Observable<Tag> getTags() {
        throw new RuntimeException("getTags() was called but this operation cannot be applied to the cloud");
    }

    @Override
    public Single<Tag> getTag(@NonNull String tagId) {
        throw new RuntimeException("getTag() was called but this operation cannot be applied to the cloud");
    }

    @Override
    public void saveTag(@NonNull Tag tag) {
        throw new RuntimeException("saveTag() was called but this operation cannot be applied to the cloud");
    }

    @Override
    public void deleteAllTags() {
        throw new RuntimeException("deleteAllTags() was called but this operation cannot be applied to the cloud");
    }

    // Statics

    public static Observable<RemoteFile> getRemoteFiles(
            final OwnCloudClient ocClient, final String remotePath) {
        return Observable.generate(() -> {
            ReadRemoteFolderOperation operation = new ReadRemoteFolderOperation(remotePath);
            RemoteOperationResult result = operation.execute(ocClient);
            if (!result.isSuccess()) return null;

            ArrayList<Object> dataSourceContent = result.getData();
            dataSourceContent.remove(0); // dataSourceDirectory
            return dataSourceContent.iterator();
        }, (objectIterator, remoteFileEmitter) -> {
            if (objectIterator == null) {
                remoteFileEmitter.onError(new NullPointerException("An error while retrieving cloud directory"));
                return null;
            }
            if (objectIterator.hasNext()) {
                remoteFileEmitter.onNext((RemoteFile) objectIterator.next());
            } else {
                remoteFileEmitter.onComplete();
            }
            return objectIterator;
        });
    }

    @Nullable
    public static String getDataSourceETag(
            OwnCloudClient ocClient, String dataSourceDirectory, boolean createDataSource) {
        if (dataSourceDirectory == null) return null;

        final ReadRemoteFileOperation readOperation =
                new ReadRemoteFileOperation(dataSourceDirectory);
        RemoteOperationResult result = readOperation.execute(ocClient);
        if (result.isSuccess()) {
            RemoteFile file = (RemoteFile) result.getData().get(0);
            return file.getEtag();
        } else if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND
                && createDataSource) {
            CreateRemoteFolderOperation writeOperation =
                    new CreateRemoteFolderOperation(dataSourceDirectory, true);
            result = writeOperation.execute(ocClient);
            if (result.isSuccess()) {
                Log.i(TAG, "New folder has been created");
                // NOTE: recursion, but with !createDataSource
                return getDataSourceETag(ocClient, dataSourceDirectory, false);
            }
        }
        return null;
    }

    @NonNull
    public static Map<String, String> getDataSourceMap(
            @NonNull OwnCloudClient ocClient, @NonNull String dataSourceDirectory) {
        checkNotNull(ocClient);
        checkNotNull(dataSourceDirectory);

        final Map<String, String> dataSourceMap = new HashMap<>();
        getRemoteFiles(ocClient, dataSourceDirectory).subscribe(file -> {
            String fileMimeType = file.getMimeType();
            String fileRemotePath = file.getRemotePath();
            String id = JsonFile.getId(fileMimeType, fileRemotePath);
            // TODO: check file size and reject if above reasonable limit
            if (id != null) {
                dataSourceMap.put(id, file.getEtag());
            } else {
                Log.w(TAG, "A problem was found in cloud dataSource "
                        + "[" + fileRemotePath + ", mimeType=" + fileMimeType + "]");
            }
        }, throwable -> { /* skip the corrupted files */ });
        return dataSourceMap;
    }
}
