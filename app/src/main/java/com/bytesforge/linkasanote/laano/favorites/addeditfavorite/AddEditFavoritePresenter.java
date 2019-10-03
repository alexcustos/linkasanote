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

package com.bytesforge.linkasanote.laano.favorites.addeditfavorite;

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.ClipboardService;
import com.bytesforge.linkasanote.laano.favorites.FavoriteId;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncState;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AddEditFavoritePresenter implements
        AddEditFavoriteContract.Presenter, TokenCompleteTextView.TokenListener<Tag> {

    private static final String TAG = AddEditFavoritePresenter.class.getSimpleName();
    private static final String TAG_E = AddEditFavoritePresenter.class.getCanonicalName();

    private final Repository repository;
    private final AddEditFavoriteContract.View view;
    private final AddEditFavoriteContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final Settings settings;

    private String favoriteId; // NOTE: can be reset to null if NoSuchElementException

    @NonNull
    private final CompositeDisposable tagsDisposable;

    @NonNull
    private final CompositeDisposable favoriteDisposable;

    @Inject
    AddEditFavoritePresenter(
            Repository repository, AddEditFavoriteContract.View view,
            AddEditFavoriteContract.ViewModel viewModel,
            BaseSchedulerProvider schedulerProvider,
            Settings settings, @Nullable @FavoriteId String favoriteId) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.settings = settings;
        this.favoriteId = favoriteId;
        tagsDisposable = new CompositeDisposable();
        favoriteDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
        loadTags();
    }

    @Override
    public void unsubscribe() {
        tagsDisposable.clear();
        favoriteDisposable.clear();
    }

    @Override
    public void loadTags() {
        tagsDisposable.clear(); // stop previous requests
        Disposable disposable = repository.getTags()
                .subscribeOn(schedulerProvider.computation())
                .toList()
                .observeOn(schedulerProvider.ui())
                .subscribe(view::swapTagsCompletionViewItems, throwable -> {
                    CommonUtils.logStackTrace(TAG_E, throwable);
                    view.swapTagsCompletionViewItems(new ArrayList<>());
                });
        tagsDisposable.add(disposable);
    }

    @Override
    public boolean isNewFavorite() {
        return favoriteId == null;
    }

    @Override
    public void populateFavorite() {
        if (favoriteId == null) {
            throw new RuntimeException("populateFavorite() was called but favoriteId is null");
        }
        favoriteDisposable.clear();
        Disposable disposable = repository.getFavorite(favoriteId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(viewModel::populateFavorite, throwable -> {
                    favoriteId = null;
                    viewModel.showFavoriteNotFoundSnackbar();
                });
        favoriteDisposable.add(disposable);
    }

    @Override
    public void saveFavorite(String name, boolean andGate, List<Tag> tags) {
        if (isNewFavorite()) {
            createFavorite(name, andGate, tags);
        } else {
            updateFavorite(name, andGate, tags);
        }
    }

    private void createFavorite(String name, boolean andGate, List<Tag> tags) {
        saveFavorite(new Favorite(name, andGate, tags));
    }

    private void updateFavorite(String name, boolean andGate, List<Tag> tags) {
        if (favoriteId == null) {
            throw new RuntimeException("updateFavorite() was called but favoriteId is null");
        }
        SyncState state = new SyncState(
                viewModel.getFavoriteSyncState(), SyncState.State.UNSYNCED);
        saveFavorite(new Favorite(favoriteId, name, andGate, tags, state));
    }

    private void saveFavorite(@NonNull final Favorite favorite) {
        checkNotNull(favorite);
        if (favorite.isEmpty()) {
            viewModel.showEmptyFavoriteSnackbar();
            return;
        }
        final String favoriteId = favorite.getId();
        repository.saveFavorite(favorite, false) // sync after save
                // NOTE: Sync will be concatenated on .io() scheduler
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                            String favoriteFilterId = settings.getFavoriteFilterId();
                            if (favoriteId.equals(favoriteFilterId)) {
                                settings.setFavoriteFilter(favorite);
                            }
                            if (view.isActive()) {
                                view.finishActivity(favoriteId);
                            }
                            break;
                    }
                }, throwable -> {
                    if (throwable instanceof SQLiteConstraintException) {
                        viewModel.showDuplicateKeyError();
                    } else {
                        CommonUtils.logStackTrace(TAG_E, throwable);
                        viewModel.showDatabaseErrorSnackbar();
                    }
                });
    }

    @Override
    public void onClipboardChanged(int clipboardType, boolean force) {
        view.setFavoritePaste(clipboardType);
        // NOTE: if data is ready either this method or linkExtraReady is called
        if (force || (viewModel.isEmpty() && settings.isClipboardFillInForms())) {
            view.fillInForm();
        }
    }

    @Override
    public void onClipboardLinkExtraReady(boolean force) {
        view.setFavoritePaste(ClipboardService.CLIPBOARD_EXTRA);
        if (force || (viewModel.isEmpty() && settings.isClipboardFillInForms())) {
            view.fillInForm();
        }
    }

    @Override
    public void setShowFillInFormInfo(boolean show) {
        settings.setShowFillInFormInfo(show);
    }

    @Override
    public boolean isShowFillInFormInfo() {
        return settings.isShowFillInFormInfo();
    }

    // Tags

    @Override
    public void onTokenAdded(Tag tag) {
        viewModel.afterTagsChanged();
    }

    @Override
    public void onTokenRemoved(Tag tag) {
        viewModel.afterTagsChanged();
    }

    @Override
    public void onDuplicateRemoved(Tag tag) {
        viewModel.showTagsDuplicateRemovedToast();
    }
}
