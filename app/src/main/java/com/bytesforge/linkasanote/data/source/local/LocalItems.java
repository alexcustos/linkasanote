package com.bytesforge.linkasanote.data.source.local;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.sync.SyncState;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface LocalItems<T> {

    Observable<T> getAll();
    Observable<T> getActive();
    Observable<T> getActive(String[] linkIds);
    Observable<T> getUnsynced();
    Observable<T> get(final Uri uri);
    Observable<T> get(
            final Uri uri,
            final String selection, final String[] selectionArgs, final String sortOrder);
    Single<T> get(final String itemId);
    Single<Boolean> save(final T item);
    Single<Boolean> saveDuplicated(final T item);
    Single<Boolean> update(final String itemId, final SyncState state);
    Single<Integer> resetSyncState();
    Single<Boolean> delete(final String itemId);
    Single<Integer> delete();
    Single<SyncState> getSyncState(final String itemId);
    Observable<SyncState> getSyncStates();
    Observable<String> getIds();
    Single<Boolean> isConflicted();
    Single<Boolean> isUnsynced();
    Single<T> getMain(final String duplicatedKey);
    Single<Boolean> autoResolveConflict(final String linkId);
    Single<Boolean> logSyncResult(
            long started, @NonNull final String entryId,
            @NonNull final LocalContract.SyncResultEntry.Result result);
    Single<Integer> markSyncResultsAsApplied();
    Observable<String> getSyncResultsIds();
}
