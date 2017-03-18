package com.bytesforge.linkasanote.data.source.cloud;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.util.ArrayList;
import java.util.List;
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
    private final ContentResolver contentResolver;

    private final AccountManager accountManager;
    private final CompositeDisposable disposable;

    public CloudDataSource(
            Context context, SharedPreferences sharedPreferences,
            ContentResolver contentResolver, BaseSchedulerProvider schedulerProvider,
            AccountManager accountManager) {
        this.context = context;
        this.schedulerProvider = schedulerProvider;
        this.contentResolver = contentResolver;
        this.accountManager = accountManager;

        disposable = new CompositeDisposable();
    }

    @Override
    public Single<List<Link>> getLinks() {
        return null;
    }

    @Override
    public Single<Link> getLink(@NonNull String linkId) {
        return null;
    }

    @Override
    public void saveLink(@NonNull Link link) {
    }

    @Override
    public void deleteAllLinks() {
    }

    // Notes

    @Override
    public Single<List<Note>> getNotes() {
        return null;
    }

    @Override
    public Single<Note> getNote(@NonNull String noteId) {
        return null;
    }

    @Override
    public void saveNote(@NonNull Note note) {
    }

    @Override
    public void deleteAllNotes() {
    }

    // Favorites

    @Override
    public Observable<Favorite> getFavorites() {
        return null;
    }

    @Override
    public Single<Favorite> getFavorite(@NonNull String favoriteId) {
        return null;
    }

    @Override
    public void saveFavorite(final @NonNull Favorite favorite) {
        checkNotNull(favorite);

        if (!CloudUtils.isApplicationConnected(context)) return;

        // NOTE: do not cache, because it can be changed in any time
        Single<RemoteOperationResult> uploadFavorite = Single.fromCallable(() -> {
            // TODO: add multi-accounts support: upload to all accounts but sync with the default one
            final Account account = CloudUtils.getDefaultAccount(context, accountManager);
            if (account == null) return null;
            final OwnCloudClient ocClient = CloudUtils.getOwnCloudClient(account, context);
            if (ocClient == null) return null;
            // Check local
            String favoriteId = favorite.getId();
            SyncState oldState = null;
            boolean isReady = false;
            try {
                oldState = LocalFavorites.getFavoriteSyncState(contentResolver, favoriteId).blockingGet();
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
            final String remotePath = CloudFavorites.getRemotePath(context, favoriteId);
            try {
                RemoteFile file = CloudDataSource.getRemoteFile(ocClient, remotePath).blockingGet();
                if (oldState == null || !file.getEtag().equals(oldState.getETag())) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
                }
            } catch (NoSuchElementException e) {
                // NOTE: It's expected state if there is no file in the cloud
            }
            return CloudFavorites.uploadFavorite(favorite, ocClient, context);
        });

        Disposable disposable = uploadFavorite
                .subscribeOn(schedulerProvider.computation())
                //.observeOn(schedulerProvider.ui())
                .subscribe(result -> {
                    int numRows = -1;
                    if (result.isSuccess()) {
                        JsonFile jsonFile = (JsonFile) result.getData().get(0);
                        SyncState state = new SyncState(jsonFile.getETag(), SyncState.State.SYNCED);
                        numRows = LocalFavorites
                                .updateFavorite(contentResolver, favorite.getId(), state)
                                .blockingGet();
                    } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        SyncState state = new SyncState(SyncState.State.CONFLICTED_UPDATE);
                        numRows = LocalFavorites
                                .updateFavorite(contentResolver, favorite.getId(), state)
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

    @Override
    public void deleteAllFavorites() {
    }

    @Override
    public void deleteFavorite(@NonNull String favoriteId) {
        checkNotNull(favoriteId);

        if (!CloudUtils.isApplicationConnected(context)) return;

        Single<RemoteOperationResult> deleteFavorite = Single.fromCallable(() -> {
            final Account account = CloudUtils.getDefaultAccount(context, accountManager);
            if (account == null) return null;
            final OwnCloudClient ocClient = CloudUtils.getOwnCloudClient(account, context);
            if (ocClient == null) return null;
            // Check local
            SyncState state = null;
            boolean isReady = false;
            try {
                state = LocalFavorites.getFavoriteSyncState(contentResolver, favoriteId).blockingGet();
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
            final String remotePath = CloudFavorites.getRemotePath(context, favoriteId);
            try {
                RemoteFile file = CloudDataSource.getRemoteFile(ocClient, remotePath).blockingGet();
                if (state != null && !file.getEtag().equals(state.getETag())) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT);
                }
            } catch (NoSuchElementException e) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
            }
            return CloudFavorites.deleteFavorite(favoriteId, ocClient, context);
        });

        Disposable disposable = deleteFavorite
                .subscribeOn(schedulerProvider.computation())
                .subscribe(result -> {
                    int numRows = -1;
                    if (result.isSuccess()) {
                        numRows = LocalFavorites
                                .deleteFavorite(contentResolver, favoriteId)
                                .blockingGet();
                    } else if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        SyncState state = new SyncState(SyncState.State.CONFLICTED_DELETE);
                        numRows = LocalFavorites
                                .updateFavorite(contentResolver, favoriteId, state)
                                .blockingGet();
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

    public static Single<RemoteFile> getRemoteFile(
            final OwnCloudClient ocClient, final String remotePath) {
        return Single.fromCallable(() -> {
            final ReadRemoteFileOperation operation = new ReadRemoteFileOperation(remotePath);
            final RemoteOperationResult result = operation.execute(ocClient);
            if (result.isSuccess()) {
                return (RemoteFile) result.getData().get(0);
            } else if (result.getCode() == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
                throw new NoSuchElementException("The requested file was not found");
            }
            return null;
        });
    }

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
}
