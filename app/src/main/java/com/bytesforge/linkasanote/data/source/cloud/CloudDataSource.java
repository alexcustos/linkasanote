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

package com.bytesforge.linkasanote.data.source.cloud;

import android.accounts.NetworkErrorException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;

@Singleton
public class CloudDataSource {

    private static final String TAG = CloudDataSource.class.getSimpleName();

    private final LocalLinks<Link> localLinks;
    private final CloudItem<Link> cloudLinks;
    private final LocalFavorites<Favorite> localFavorites;
    private final CloudItem<Favorite> cloudFavorites;
    private final LocalNotes<Note> localNotes;
    private final CloudItem<Note> cloudNotes;

    public CloudDataSource(
            LocalLinks<Link> localLinks, CloudItem<Link> cloudLinks,
            LocalFavorites<Favorite> localFavorites, CloudItem<Favorite> cloudFavorites,
            LocalNotes<Note> localNotes, CloudItem<Note> cloudNotes) {
        this.localLinks = localLinks;
        this.cloudLinks = cloudLinks;
        this.localFavorites = localFavorites;
        this.cloudFavorites = cloudFavorites;
        this.localNotes = localNotes;
        this.cloudNotes = cloudNotes;
    }

    // Links

    public Single<Link> getLink(@NonNull String linkId) {
        checkNotNull(linkId);
        return cloudLinks.download(linkId);
    }

    public Single<DataSource.ItemState> saveLink(@NonNull final String linkId) {
        checkNotNull(linkId);
        return localLinks.get(linkId)
                .doOnSuccess(link -> {
                    if (link.isDeleted() || link.isConflicted()) {
                        // NOTE: show sync error notification because it should not be happening
                        throw new IllegalStateException(
                                "The deleted or conflicted Link cannot be uploaded to the Cloud storage");
                    }
                })
                .flatMap(link -> {
                    return cloudLinks.readFile(linkId).onErrorReturn(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return new RemoteFile(); // eTag == null
                        }
                        return null;
                    }).flatMap(file -> {
                        String cloudETag = file.getEtag();
                        if (cloudETag != null && !cloudETag.equals(link.getETag())) {
                            return Single.just(new RemoteOperationResult(
                                    RemoteOperationResult.ResultCode.SYNC_CONFLICT));
                        }
                        return cloudLinks.upload(link);
                    }).flatMap(result -> {
                        if (result.isSuccess()) {
                            JsonFile jsonFile = (JsonFile) result.getData().get(0);
                            SyncState syncState = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                            return localLinks.update(linkId, syncState)
                                    .map(success -> success
                                            ? DataSource.ItemState.SAVED
                                            : DataSource.ItemState.ERROR_LOCAL);
                        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                            SyncState syncState = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                            return localLinks.update(linkId, syncState)
                                    .map(success -> success
                                            ? DataSource.ItemState.CONFLICTED
                                            : DataSource.ItemState.ERROR_LOCAL);
                        }
                        return Single.just(DataSource.ItemState.ERROR_CLOUD);
                    });
                }).flatMap(itemState -> {
                    LocalContract.SyncResultEntry.Result result;
                    switch (itemState) {
                        case SAVED:
                            result = LocalContract.SyncResultEntry.Result.UPLOADED;
                            break;
                        case CONFLICTED:
                            result = LocalContract.SyncResultEntry.Result.CONFLICT;
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            result = LocalContract.SyncResultEntry.Result.ERROR;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud saveLink()");
                    }
                    Single<Boolean> logSingle = localLinks.logSyncResult(
                            currentTimeMillis(), linkId, result, true);
                    return Single.zip(Single.just(itemState), logSingle, (ret, success) -> ret);
                });
    }

    public Single<DataSource.ItemState> deleteLink(@NonNull String linkId, long started) {
        checkNotNull(linkId);
        return localLinks.getSyncState(linkId)
                .doOnSuccess(state -> {
                    if (!state.isDeleted()) {
                        throw new IllegalStateException(
                                "Cloud deletion can only be completed if local Link is deleted");
                    }
                })
                .onErrorReturn(throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        return new SyncState(SyncState.State.DELETED); // eTag == null
                    }
                    return null;
                })
                .flatMap(state -> {
                    return cloudLinks.readFile(linkId).flatMap(file -> {
                        String localETag = state.getETag();
                        if (localETag != null && !file.getEtag().equals(localETag)) {
                            return Single.just(new RemoteOperationResult(
                                    RemoteOperationResult.ResultCode.SYNC_CONFLICT));
                        }
                        return cloudLinks.delete(linkId);
                    }).onErrorReturn(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
                        }
                        return null;
                    }).flatMap(result -> {
                        if (result.isSuccess()) {
                            // NOTE: success == false if unsynced item has been already deleted
                            return localLinks.delete(linkId)
                                    .map(success -> DataSource.ItemState.DELETED);
                        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                            SyncState conflictedState = new SyncState(
                                    state, SyncState.State.CONFLICTED_DELETE);
                            return localLinks.update(linkId, conflictedState)
                                    .map(success -> success
                                            ? DataSource.ItemState.CONFLICTED
                                            : DataSource.ItemState.ERROR_LOCAL);
                        }
                        return Single.just(DataSource.ItemState.ERROR_CLOUD);
                    });
                }).flatMap(itemState -> {
                    LocalContract.SyncResultEntry.Result result;
                    switch (itemState) {
                        case DELETED:
                            result = LocalContract.SyncResultEntry.Result.DELETED;
                            break;
                        case CONFLICTED:
                            result = LocalContract.SyncResultEntry.Result.CONFLICT;
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            result = LocalContract.SyncResultEntry.Result.ERROR;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud deleteLink()");
                    }
                    Single<Boolean> logSingle = localLinks.logSyncResult(
                            started, linkId, result, true);
                    return Single.zip(Single.just(itemState), logSingle, (ret, success) -> ret);
                });
    }

    // Favorites

    public Single<Favorite> getFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);
        return cloudFavorites.download(favoriteId);
    }

    public Single<DataSource.ItemState> saveFavorite(@NonNull final String favoriteId) {
        checkNotNull(favoriteId);
        return localFavorites.get(favoriteId)
                .doOnSuccess(favorite -> {
                    if (favorite.isDeleted() || favorite.isConflicted()) {
                        throw new IllegalStateException(
                                "The deleted or conflicted Favorite cannot be uploaded to the Cloud storage");
                    }
                })
                .flatMap(favorite -> {
                    return cloudFavorites.readFile(favoriteId).onErrorReturn(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return new RemoteFile(); // eTag == null
                        }
                        return null;
                    }).flatMap(file -> {
                        String cloudETag = file.getEtag();
                        if (cloudETag != null && !cloudETag.equals(favorite.getETag())) {
                            return Single.just(new RemoteOperationResult(
                                    RemoteOperationResult.ResultCode.SYNC_CONFLICT));
                        }
                        return cloudFavorites.upload(favorite);
                    }).flatMap(result -> {
                        if (result.isSuccess()) {
                            JsonFile jsonFile = (JsonFile) result.getData().get(0);
                            SyncState syncState = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                            return localFavorites.update(favoriteId, syncState)
                                    .map(success -> success
                                            ? DataSource.ItemState.SAVED
                                            : DataSource.ItemState.ERROR_LOCAL);
                        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                            SyncState syncState = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                            return localFavorites.update(favoriteId, syncState)
                                    .map(success -> success
                                            ? DataSource.ItemState.CONFLICTED
                                            : DataSource.ItemState.ERROR_LOCAL);
                        }
                        return Single.just(DataSource.ItemState.ERROR_CLOUD);
                    });
                }).flatMap(itemState -> {
                    LocalContract.SyncResultEntry.Result result;
                    switch (itemState) {
                        case SAVED:
                            result = LocalContract.SyncResultEntry.Result.UPLOADED;
                            break;
                        case CONFLICTED:
                            result = LocalContract.SyncResultEntry.Result.CONFLICT;
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            result = LocalContract.SyncResultEntry.Result.ERROR;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud saveFavorite()");
                    }
                    Single<Boolean> logSingle = localFavorites.logSyncResult(
                            currentTimeMillis(), favoriteId, result, true);
                    return Single.zip(Single.just(itemState), logSingle, (ret, success) -> ret);
                });
    }

    public Single<DataSource.ItemState> deleteFavorite(@NonNull String favoriteId, long started) {
        checkNotNull(favoriteId);
        return localFavorites.getSyncState(favoriteId)
                .doOnSuccess(state -> {
                    if (!state.isDeleted()) {
                        throw new IllegalStateException(
                                "Cloud deletion can only be completed if local Favorite is deleted");
                    }
                })
                .onErrorReturn(throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        return new SyncState(SyncState.State.DELETED); // eTag == null
                    }
                    return null;
                })
                .flatMap(state -> {
                    return cloudFavorites.readFile(favoriteId).flatMap(file -> {
                        String localETag = state.getETag();
                        if (localETag != null && !file.getEtag().equals(localETag)) {
                            return Single.just(new RemoteOperationResult(
                                    RemoteOperationResult.ResultCode.SYNC_CONFLICT));
                        }
                        return cloudFavorites.delete(favoriteId);
                    }).onErrorReturn(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
                        }
                        return null;
                    }).flatMap(result -> {
                        if (result.isSuccess()) {
                            // NOTE: success == false if unsynced item has been already deleted
                            return localFavorites.delete(favoriteId)
                                    .map(success -> DataSource.ItemState.DELETED);
                        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                            SyncState conflictedState = new SyncState(
                                    state, SyncState.State.CONFLICTED_DELETE);
                            return localFavorites.update(favoriteId, conflictedState)
                                    .map(success -> success
                                            ? DataSource.ItemState.CONFLICTED
                                            : DataSource.ItemState.ERROR_LOCAL);
                        }
                        return Single.just(DataSource.ItemState.ERROR_CLOUD);
                    });
                }).flatMap(itemState -> {
                    LocalContract.SyncResultEntry.Result result;
                    switch (itemState) {
                        case DELETED:
                            result = LocalContract.SyncResultEntry.Result.DELETED;
                            break;
                        case CONFLICTED:
                            result = LocalContract.SyncResultEntry.Result.CONFLICT;
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            result = LocalContract.SyncResultEntry.Result.ERROR;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud deleteFavorite()");
                    }
                    Single<Boolean> logSingle = localFavorites.logSyncResult(
                            started, favoriteId, result, true);
                    return Single.zip(Single.just(itemState), logSingle, (ret, success) -> ret);
                });
    }

    // Notes

    public Single<Note> getNote(@NonNull String noteId) {
        checkNotNull(noteId);
        return cloudNotes.download(noteId);
    }

    public Single<DataSource.ItemState> saveNote(@NonNull final String noteId) {
        checkNotNull(noteId);
        return localNotes.get(noteId)
                .doOnSuccess(note -> {
                    if (note.isDeleted() || note.isConflicted()) {
                        throw new IllegalStateException(
                                "The deleted or conflicted Note cannot be uploaded to the Cloud storage");
                    }
                })
                .flatMap(note -> {
                    return cloudNotes.readFile(noteId).onErrorReturn(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return new RemoteFile(); // eTag == null
                        }
                        return null;
                    }).flatMap(file -> {
                        String cloudETag = file.getEtag();
                        if (cloudETag != null && !cloudETag.equals(note.getETag())) {
                            return Single.just(new RemoteOperationResult(
                                    RemoteOperationResult.ResultCode.SYNC_CONFLICT));
                        }
                        return cloudNotes.upload(note);
                    }).flatMap(result -> {
                        if (result.isSuccess()) {
                            JsonFile jsonFile = (JsonFile) result.getData().get(0);
                            SyncState syncState = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                            return localNotes.update(noteId, syncState)
                                    .map(success -> success
                                            ? DataSource.ItemState.SAVED
                                            : DataSource.ItemState.ERROR_LOCAL);
                        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                            SyncState syncState = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                            return localNotes.update(noteId, syncState)
                                    .map(success -> success
                                            ? DataSource.ItemState.CONFLICTED
                                            : DataSource.ItemState.ERROR_LOCAL);
                        }
                        return Single.just(DataSource.ItemState.ERROR_CLOUD);
                    });
                }).flatMap(itemState -> {
                    LocalContract.SyncResultEntry.Result result;
                    switch (itemState) {
                        case SAVED:
                            result = LocalContract.SyncResultEntry.Result.UPLOADED;
                            break;
                        case CONFLICTED:
                            result = LocalContract.SyncResultEntry.Result.CONFLICT;
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            result = LocalContract.SyncResultEntry.Result.ERROR;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud saveNote()");
                    }
                    Single<Boolean> logSingle = localNotes.logSyncResult(
                            currentTimeMillis(), noteId, result, true);
                    return Single.zip(Single.just(itemState), logSingle, (ret, success) -> ret);
                });
    }

    public Single<DataSource.ItemState> deleteNote(@NonNull String noteId, long started) {
        checkNotNull(noteId);
        return localNotes.getSyncState(noteId)
                .doOnSuccess(state -> {
                    if (!state.isDeleted()) {
                        throw new IllegalStateException(
                                "Cloud deletion can only be completed if local Note is deleted");
                    }
                })
                .onErrorReturn(throwable -> {
                    if (throwable instanceof NoSuchElementException) {
                        return new SyncState(SyncState.State.DELETED); // eTag == null
                    }
                    return null;
                })
                .flatMap(state -> {
                    return cloudNotes.readFile(noteId).flatMap(file -> {
                        String localETag = state.getETag();
                        if (localETag != null && !file.getEtag().equals(localETag)) {
                            return Single.just(new RemoteOperationResult(
                                    RemoteOperationResult.ResultCode.SYNC_CONFLICT));
                        }
                        return cloudNotes.delete(noteId);
                    }).onErrorReturn(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
                        }
                        return null;
                    }).flatMap(result -> {
                        if (result.isSuccess()) {
                            // NOTE: success == false if unsynced item has been already deleted
                            return localNotes.delete(noteId)
                                    .map(success -> DataSource.ItemState.DELETED);
                        } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                            SyncState conflictedState = new SyncState(
                                    state, SyncState.State.CONFLICTED_DELETE);
                            return localNotes.update(noteId, conflictedState)
                                    .map(success -> success
                                            ? DataSource.ItemState.CONFLICTED
                                            : DataSource.ItemState.ERROR_LOCAL);
                        }
                        return Single.just(DataSource.ItemState.ERROR_CLOUD);
                    });
                }).flatMap(itemState -> {
                    LocalContract.SyncResultEntry.Result result;
                    switch (itemState) {
                        case DELETED:
                            result = LocalContract.SyncResultEntry.Result.DELETED;
                            break;
                        case CONFLICTED:
                            result = LocalContract.SyncResultEntry.Result.CONFLICT;
                            break;
                        case ERROR_LOCAL:
                        case ERROR_CLOUD:
                            result = LocalContract.SyncResultEntry.Result.ERROR;
                            break;
                        default:
                            throw new IllegalStateException("Unexpected state from Cloud deleteNote()");
                    }
                    Single<Boolean> logSingle = localNotes.logSyncResult(
                            started, noteId, result, true);
                    return Single.zip(Single.just(itemState), logSingle, (ret, success) -> ret);
                });
    }

    // Statics

    public static Observable<RemoteFile> getRemoteFiles(
            final OwnCloudClient ocClient, final String remotePath) {
        return Observable.generate(() -> {
            ReadFolderRemoteOperation operation = new ReadFolderRemoteOperation(remotePath);
            RemoteOperationResult result =
                    CloudDataSource.executeRemoteOperation(operation, ocClient)
                            .blockingGet();
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
            @NonNull OwnCloudClient ocClient, @NonNull String dataSourceDirectory,
            boolean createDataSource) {
        checkNotNull(ocClient);
        checkNotNull(dataSourceDirectory);
        final ReadFileRemoteOperation readOperation =
                new ReadFileRemoteOperation(dataSourceDirectory);
        RemoteOperationResult result =
                executeRemoteOperation(readOperation, ocClient).blockingGet();
        if (result.isSuccess()) {
            RemoteFile file = (RemoteFile) result.getData().get(0);
            return file.getEtag();
        } else if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND
                && createDataSource) {
            CreateFolderRemoteOperation writeOperation =
                    new CreateFolderRemoteOperation(dataSourceDirectory, true);
            result = executeRemoteOperation(writeOperation, ocClient).blockingGet();
            if (result.isSuccess()) {
                Log.d(TAG, "New folder has been created");
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
        CloudDataSource.getRemoteFiles(ocClient, dataSourceDirectory).subscribe(file -> {
            String fileMimeType = file.getMimeType();
            String fileRemotePath = file.getRemotePath();
            long fileSize = file.getSize();
            String id = JsonFile.getId(fileMimeType, fileRemotePath);
            if (id != null && fileSize <= Settings.GLOBAL_JSON_MAX_BODY_SIZE_BYTES) {
                dataSourceMap.put(id, file.getEtag());
            } else {
                Log.w(TAG, "A problem was found in cloud dataSource "
                        + "[" + fileRemotePath + "; mimeType=" + fileMimeType + "; size=" + fileSize + "]");
            }
        }, throwable -> { /* skip the corrupted files */ });
        return dataSourceMap;
    }

    public static Single<RemoteOperationResult> executeRemoteOperation(
            @NonNull final RemoteOperation operation, @NonNull final OwnCloudClient ocClient) {
        checkNotNull(operation);
        checkNotNull(ocClient);
        final AtomicInteger retryCount = new AtomicInteger(0);
        return Single.fromCallable(() -> operation.execute(ocClient))
                .flatMap(result -> {
                    RemoteOperationResult.ResultCode code = result.getCode();
                    if (retryCount.getAndAdd(1) < Settings.GLOBAL_RETRY_ON_NETWORK_ERROR) {
                        if (isNetworkError(code)) {
                            Log.e(TAG, "Retry on Network error [" +
                                    retryCount.get() + "/" + Settings.GLOBAL_RETRY_ON_NETWORK_ERROR +
                                    "; operation=" + operation.getClass().getSimpleName() +
                                    "; code=" + code.name() +
                                    "; delay=" + Settings.GLOBAL_DELAY_ON_NETWORK_ERROR_MILLIS + "ms]");
                            return Single.timer(
                                    Settings.GLOBAL_DELAY_ON_NETWORK_ERROR_MILLIS,
                                    TimeUnit.MILLISECONDS).flatMap(
                                            aLong -> Single.error(new NetworkErrorException()));
                        }
                    }
                    if (isNetworkError(code)) {
                        Log.e(TAG, "An unrecoverable Network error [" + code.name() + "]");
                    }
                    return Single.just(result);
                })
                .retry(Settings.GLOBAL_RETRY_ON_NETWORK_ERROR);
    }

    private static boolean isNetworkError(RemoteOperationResult.ResultCode code) {
        // RemoteOperationResult.ResultCode.TIMEOUT
        return code == RemoteOperationResult.ResultCode.SSL_ERROR // it seems it is some sort of bug
                || code == RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE; // if connection is not ready yet
    }
}
