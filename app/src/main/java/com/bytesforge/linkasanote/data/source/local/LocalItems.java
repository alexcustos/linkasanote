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

package com.bytesforge.linkasanote.data.source.local;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Pair;

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
    Single<Integer> getNextDuplicated(final String duplicatedKey);
    Single<T> getMain(final String duplicatedKey);
    Single<Boolean> autoResolveConflict(final String linkId);
    Single<Boolean> logSyncResult(
            long started, @NonNull final String entryId,
            @NonNull final LocalContract.SyncResultEntry.Result result);
    Single<Boolean> logSyncResult(
            long started, @NonNull final String entryId,
            @NonNull final LocalContract.SyncResultEntry.Result result, boolean applied);
    Single<Integer> markSyncResultsAsApplied();
    Observable<Pair<String, LocalContract.SyncResultEntry.Result>> getSyncResultsIds();
}
